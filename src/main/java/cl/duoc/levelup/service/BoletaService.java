package cl.duoc.levelup.service;

import cl.duoc.levelup.dto.BoletaRequest;
import cl.duoc.levelup.dto.BoletaResponse;
import cl.duoc.levelup.dto.ItemBoletaRequest;
import cl.duoc.levelup.exception.RecursoNoEncontradoException;
import cl.duoc.levelup.exception.ReglaNegocioException;
import cl.duoc.levelup.model.Boleta;
import cl.duoc.levelup.model.DetalleBoleta;
import cl.duoc.levelup.model.EstadoBoleta;
import cl.duoc.levelup.model.Producto;
import cl.duoc.levelup.model.Rol;
import cl.duoc.levelup.model.Usuario;
import cl.duoc.levelup.repository.BoletaRepository;
import cl.duoc.levelup.repository.ProductoRepository;
import cl.duoc.levelup.repository.UsuarioRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Checkout: crea la boleta, valida stock, aplica descuentos, procesa el pago y descuenta el stock.
 * Todo ocurre en una sola transacción: si algo falla, no queda nada a medias en la BD.
 */
@Service
public class BoletaService {

    private final BoletaRepository boletaRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;
    private final CalculadoraDescuentos calculadoraDescuentos;
    private final PasarelaPagoSimulada pasarelaPago;

    public BoletaService(BoletaRepository boletaRepository, ProductoRepository productoRepository,
                         UsuarioRepository usuarioRepository, CalculadoraDescuentos calculadoraDescuentos,
                         PasarelaPagoSimulada pasarelaPago) {
        this.boletaRepository = boletaRepository;
        this.productoRepository = productoRepository;
        this.usuarioRepository = usuarioRepository;
        this.calculadoraDescuentos = calculadoraDescuentos;
        this.pasarelaPago = pasarelaPago;
    }

    @Transactional
    public BoletaResponse crear(BoletaRequest req, String correoUsuario) {
        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(correoUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el usuario " + correoUsuario));

        // 1. Agrupar ítems repetidos del carrito (mismo producto en dos líneas).
        Map<Long, Integer> cantidades = new LinkedHashMap<>();
        for (ItemBoletaRequest item : req.items()) {
            cantidades.merge(item.productoId(), item.cantidad(), Integer::sum);
        }

        Boleta boleta = new Boleta();
        int subtotal = 0;
        Map<Producto, Integer> productos = new LinkedHashMap<>();

        // 2 y 3. Validar existencia y stock, y calcular con los precios de la BD (no los del front).
        for (Map.Entry<Long, Integer> entrada : cantidades.entrySet()) {
            Producto producto = productoRepository.findById(entrada.getKey())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Producto", entrada.getKey()));
            int cantidad = entrada.getValue();
            if (producto.getStock() < cantidad) {
                throw new ReglaNegocioException("Stock insuficiente para " + producto.getNombre()
                        + ": quedan " + producto.getStock() + " unidades");
            }
            int subtotalLinea = producto.getPrecio() * cantidad;
            subtotal += subtotalLinea;
            productos.put(producto, cantidad);

            DetalleBoleta detalle = new DetalleBoleta();
            detalle.setProducto(producto);
            detalle.setCodigoProducto(producto.getCodigo());
            detalle.setNombreProducto(producto.getNombre());
            detalle.setCantidad(cantidad);
            detalle.setPrecioUnitario(producto.getPrecio());
            detalle.setSubtotal(subtotalLinea);
            boleta.agregarDetalle(detalle);
        }

        // 4. Descuentos: 20% Duoc + cupón (un cupón inválido lanza 409).
        CalculadoraDescuentos.Resumen resumen = calculadoraDescuentos.calcular(subtotal, usuario.getCorreo(), req.cupon());

        boleta.setFecha(LocalDateTime.now());
        boleta.setUsuario(usuario);
        boleta.setNombreCliente(req.nombreCliente().trim());
        boleta.setCorreoCliente(req.correoCliente().trim().toLowerCase());
        boleta.setTelefono(req.telefono());
        boleta.setDireccion(req.direccion().trim());
        boleta.setRegion(req.region());
        boleta.setComuna(req.comuna());
        boleta.setIndicaciones(req.indicaciones());
        boleta.setMetodoPago(req.metodoPago());
        boleta.setUltimosDigitosTarjeta(req.ultimosDigitosTarjeta());
        boleta.setCupon(resumen.cupon());
        boleta.setSubtotal(resumen.subtotal());
        boleta.setPorcentajeDescuento(resumen.porcentaje());
        boleta.setDescuento(resumen.descuento());
        boleta.setTotal(resumen.total());

        // 5 y 6. Pago simulado: si se aprueba se descuenta el stock; si se rechaza, el stock no se toca.
        Optional<String> rechazo = pasarelaPago.procesar(req.metodoPago(), req.ultimosDigitosTarjeta(), resumen.total());
        if (rechazo.isPresent()) {
            boleta.setEstado(EstadoBoleta.RECHAZADA);
            boleta.setMotivoRechazo(rechazo.get());
        } else {
            boleta.setEstado(EstadoBoleta.PAGADA);
            productos.forEach((producto, cantidad) -> producto.setStock(producto.getStock() - cantidad));
        }

        return BoletaResponse.desde(boletaRepository.save(boleta));
    }

    @Transactional(readOnly = true)
    public List<BoletaResponse> listar() {
        return boletaRepository.findAllByOrderByFechaDesc().stream().map(BoletaResponse::desde).toList();
    }

    @Transactional(readOnly = true)
    public List<BoletaResponse> listarDelUsuario(String correoUsuario) {
        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(correoUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el usuario " + correoUsuario));
        return boletaRepository.findByUsuarioIdOrderByFechaDesc(usuario.getId()).stream()
                .map(BoletaResponse::desde).toList();
    }

    /** Admin y Vendedor ven cualquier boleta; un Cliente solo las suyas. */
    @Transactional(readOnly = true)
    public BoletaResponse obtener(Long id, String correoUsuario) {
        Boleta boleta = buscar(id);
        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(correoUsuario)
                .orElseThrow(() -> new AccessDeniedException("Usuario no válido"));
        boolean esPersonal = usuario.getRol() == Rol.ADMINISTRADOR || usuario.getRol() == Rol.VENDEDOR;
        if (!esPersonal && !boleta.getUsuario().getId().equals(usuario.getId())) {
            throw new AccessDeniedException("La boleta no pertenece al usuario");
        }
        return BoletaResponse.desde(boleta);
    }

    @Transactional
    public void eliminar(Long id) {
        boletaRepository.delete(buscar(id));
    }

    private Boleta buscar(Long id) {
        return boletaRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Boleta", id));
    }
}
