package cl.duoc.levelup.service;

import cl.duoc.levelup.dto.BoletaRequest;
import cl.duoc.levelup.dto.BoletaResponse;
import cl.duoc.levelup.dto.ItemBoletaRequest;
import cl.duoc.levelup.exception.RecursoNoEncontradoException;
import cl.duoc.levelup.exception.ReglaNegocioException;
import cl.duoc.levelup.model.Boleta;
import cl.duoc.levelup.model.Categoria;
import cl.duoc.levelup.model.EstadoBoleta;
import cl.duoc.levelup.model.Producto;
import cl.duoc.levelup.model.Rol;
import cl.duoc.levelup.model.Usuario;
import cl.duoc.levelup.repository.BoletaRepository;
import cl.duoc.levelup.repository.ProductoRepository;
import cl.duoc.levelup.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pruebas unitarias del checkout con Mockito (sin base de datos). */
@ExtendWith(MockitoExtension.class)
class BoletaServiceTest {

    @Mock
    private BoletaRepository boletaRepository;
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;

    private BoletaService service;
    private Usuario cliente;
    private Producto mouse;

    @BeforeEach
    void preparar() {
        service = new BoletaService(boletaRepository, productoRepository, usuarioRepository,
                new CalculadoraDescuentos(), new PasarelaPagoSimulada());

        cliente = new Usuario();
        cliente.setId(5L);
        cliente.setCorreo("alumno@duoc.cl");
        cliente.setRol(Rol.CLIENTE);

        mouse = new Producto();
        mouse.setId(1L);
        mouse.setCodigo("MS001");
        mouse.setNombre("Mouse Gamer");
        mouse.setPrecio(10000);
        mouse.setStock(5);
        mouse.setStockCritico(1);
        mouse.setCategoria(new Categoria("Mouse"));

        when(usuarioRepository.findByCorreoIgnoreCase("alumno@duoc.cl")).thenReturn(Optional.of(cliente));
    }

    private BoletaRequest pedido(String digitos, String cupon, ItemBoletaRequest... items) {
        return new BoletaRequest(List.of(items), "Ana Pérez", "alumno@duoc.cl", "+56911112222",
                "Av. Siempre Viva 123", "Región Metropolitana", "Santiago", null, cupon, "Crédito", digitos);
    }

    @Test
    void stockInsuficienteLanza409YNoGuardaNada() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(mouse));

        // Dos líneas del mismo producto: 3 + 3 = 6 > stock 5 (se agrupan antes de validar).
        BoletaRequest req = pedido("4242", null, new ItemBoletaRequest(1L, 3), new ItemBoletaRequest(1L, 3));

        assertThatThrownBy(() -> service.crear(req, "alumno@duoc.cl"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("Mouse Gamer")
                .hasMessageContaining("5");
        verify(boletaRepository, never()).save(any());
        assertThat(mouse.getStock()).isEqualTo(5);
    }

    @Test
    void productoInexistenteLanza404() {
        when(productoRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.crear(pedido("4242", null, new ItemBoletaRequest(99L, 1)), "alumno@duoc.cl"))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void pagoAprobadoDescuentaStockYAplicaDescuentos() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(mouse));
        when(boletaRepository.save(any(Boleta.class))).thenAnswer(inv -> inv.getArgument(0));

        BoletaResponse r = service.crear(pedido("4242", "LEVELUP10", new ItemBoletaRequest(1L, 2)), "alumno@duoc.cl");

        assertThat(r.estado()).isEqualTo(EstadoBoleta.PAGADA);
        assertThat(r.subtotal()).isEqualTo(20000);
        assertThat(r.porcentajeDescuento()).isEqualTo(30); // 20% Duoc + 10% cupón
        assertThat(r.total()).isEqualTo(14000);
        assertThat(r.detalles()).hasSize(1);
        assertThat(mouse.getStock()).isEqualTo(3);
    }

    @Test
    void pagoRechazadoNoTocaElStock() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(mouse));
        when(boletaRepository.save(any(Boleta.class))).thenAnswer(inv -> inv.getArgument(0));

        BoletaResponse r = service.crear(pedido("0000", null, new ItemBoletaRequest(1L, 2)), "alumno@duoc.cl");

        assertThat(r.estado()).isEqualTo(EstadoBoleta.RECHAZADA);
        assertThat(r.motivoRechazo()).isNotBlank();
        assertThat(mouse.getStock()).isEqualTo(5);
    }

    @Test
    void usaElPrecioDeLaBaseDeDatos() {
        mouse.setPrecio(12345);
        when(productoRepository.findById(1L)).thenReturn(Optional.of(mouse));
        when(boletaRepository.save(any(Boleta.class))).thenAnswer(inv -> inv.getArgument(0));
        cliente.setCorreo("alumno@duoc.cl");

        BoletaResponse r = service.crear(pedido("4242", null, new ItemBoletaRequest(1L, 1)), "alumno@duoc.cl");

        assertThat(r.detalles().get(0).precioUnitario()).isEqualTo(12345);
    }
}
