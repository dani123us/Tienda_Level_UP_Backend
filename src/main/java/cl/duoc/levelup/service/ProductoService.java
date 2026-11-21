package cl.duoc.levelup.service;

import cl.duoc.levelup.dto.ProductoRequest;
import cl.duoc.levelup.dto.ProductoResponse;
import cl.duoc.levelup.exception.RecursoNoEncontradoException;
import cl.duoc.levelup.exception.ReglaNegocioException;
import cl.duoc.levelup.model.Producto;
import cl.duoc.levelup.repository.DetalleBoletaRepository;
import cl.duoc.levelup.repository.ProductoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final DetalleBoletaRepository detalleBoletaRepository;
    private final CategoriaService categoriaService;

    public ProductoService(ProductoRepository productoRepository, DetalleBoletaRepository detalleBoletaRepository,
                           CategoriaService categoriaService) {
        this.productoRepository = productoRepository;
        this.detalleBoletaRepository = detalleBoletaRepository;
        this.categoriaService = categoriaService;
    }

    @Transactional(readOnly = true)
    public List<ProductoResponse> listar(String categoria) {
        List<Producto> productos = (categoria == null || categoria.isBlank())
                ? productoRepository.findAllByOrderByIdAsc()
                : productoRepository.findByCategoriaNombreIgnoreCase(categoria.trim());
        return productos.stream().map(ProductoResponse::desde).toList();
    }

    @Transactional(readOnly = true)
    public ProductoResponse obtener(Long id) {
        return ProductoResponse.desde(buscar(id));
    }

    @Transactional
    public ProductoResponse crear(ProductoRequest req) {
        String codigo = req.codigo().trim().toUpperCase();
        if (productoRepository.existsByCodigoIgnoreCase(codigo)) {
            throw new ReglaNegocioException("Ya existe un producto con el código " + codigo);
        }
        Producto producto = new Producto();
        copiarDatos(req, producto);
        return ProductoResponse.desde(productoRepository.save(producto));
    }

    @Transactional
    public ProductoResponse actualizar(Long id, ProductoRequest req) {
        Producto producto = buscar(id);
        String codigo = req.codigo().trim().toUpperCase();
        if (!producto.getCodigo().equalsIgnoreCase(codigo) && productoRepository.existsByCodigoIgnoreCase(codigo)) {
            throw new ReglaNegocioException("Ya existe un producto con el código " + codigo);
        }
        copiarDatos(req, producto);
        return ProductoResponse.desde(producto);
    }

    @Transactional
    public void eliminar(Long id) {
        Producto producto = buscar(id);
        // Las boletas antiguas conservan nombre y precio del producto, solo se quita la referencia.
        detalleBoletaRepository.desvincularProducto(id);
        productoRepository.delete(producto);
    }

    private void copiarDatos(ProductoRequest req, Producto producto) {
        producto.setCodigo(req.codigo().trim().toUpperCase());
        producto.setNombre(req.nombre().trim());
        producto.setDescripcion(req.descripcion());
        producto.setPrecio(req.precio());
        producto.setStock(req.stock());
        producto.setStockCritico(req.stockCritico());
        producto.setImagen(req.imagen());
        producto.setIcono(req.icono());
        producto.setDestacado(req.destacado());
        producto.setCategoria(categoriaService.buscarOCrear(req.categoria()));
    }

    private Producto buscar(Long id) {
        return productoRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Producto", id));
    }
}
