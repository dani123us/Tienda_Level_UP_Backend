package cl.duoc.levelup.service;

import cl.duoc.levelup.dto.CategoriaRequest;
import cl.duoc.levelup.dto.CategoriaResponse;
import cl.duoc.levelup.exception.RecursoNoEncontradoException;
import cl.duoc.levelup.exception.ReglaNegocioException;
import cl.duoc.levelup.model.Categoria;
import cl.duoc.levelup.repository.CategoriaRepository;
import cl.duoc.levelup.repository.ProductoRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;

    public CategoriaService(CategoriaRepository categoriaRepository, ProductoRepository productoRepository) {
        this.categoriaRepository = categoriaRepository;
        this.productoRepository = productoRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listar() {
        return categoriaRepository.findAll(Sort.by("nombre")).stream().map(CategoriaResponse::desde).toList();
    }

    @Transactional(readOnly = true)
    public CategoriaResponse obtener(Long id) {
        return CategoriaResponse.desde(buscar(id));
    }

    @Transactional
    public CategoriaResponse crear(CategoriaRequest req) {
        String nombre = req.nombre().trim();
        if (categoriaRepository.existsByNombreIgnoreCase(nombre)) {
            throw new ReglaNegocioException("Ya existe la categoría " + nombre);
        }
        return CategoriaResponse.desde(categoriaRepository.save(new Categoria(nombre)));
    }

    @Transactional
    public CategoriaResponse actualizar(Long id, CategoriaRequest req) {
        Categoria categoria = buscar(id);
        String nombre = req.nombre().trim();
        categoriaRepository.findByNombreIgnoreCase(nombre)
                .filter(otra -> !otra.getId().equals(id))
                .ifPresent(otra -> { throw new ReglaNegocioException("Ya existe la categoría " + nombre); });
        categoria.setNombre(nombre);
        return CategoriaResponse.desde(categoria);
    }

    @Transactional
    public void eliminar(Long id) {
        Categoria categoria = buscar(id);
        if (productoRepository.existsByCategoriaId(id)) {
            throw new ReglaNegocioException("No se puede eliminar la categoría " + categoria.getNombre()
                    + " porque tiene productos asociados");
        }
        categoriaRepository.delete(categoria);
    }

    /** Usado al crear/editar productos: si la categoría no existe, se crea. */
    @Transactional
    public Categoria buscarOCrear(String nombre) {
        String limpio = nombre.trim();
        return categoriaRepository.findByNombreIgnoreCase(limpio)
                .orElseGet(() -> categoriaRepository.save(new Categoria(limpio)));
    }

    private Categoria buscar(Long id) {
        return categoriaRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Categoría", id));
    }
}
