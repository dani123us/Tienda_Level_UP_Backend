package cl.duoc.levelup.config;

import cl.duoc.levelup.model.Categoria;
import cl.duoc.levelup.model.Producto;
import cl.duoc.levelup.model.Rol;
import cl.duoc.levelup.model.Usuario;
import cl.duoc.levelup.repository.CategoriaRepository;
import cl.duoc.levelup.repository.ProductoRepository;
import cl.duoc.levelup.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Carga los datos iniciales cuando la base de datos está vacía:
 * las categorías y los 8 productos del frontend, y un usuario por cada rol.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(CategoriaRepository categoriaRepository, ProductoRepository productoRepository,
                           UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.categoriaRepository = categoriaRepository;
        this.productoRepository = productoRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (productoRepository.count() == 0) {
            cargarProductos();
        }
        if (usuarioRepository.count() == 0) {
            crearUsuario("19011022K", "Admin", "Level-Up", "admin@duoc.cl", "admin123", Rol.ADMINISTRADOR);
            crearUsuario("187654321", "Valentina", "Rojas Soto", "vendedor@duoc.cl", "vendedor123", Rol.VENDEDOR);
            crearUsuario("211234567", "Camila", "Pérez Muñoz", "cliente@gmail.com", "cliente123", Rol.CLIENTE);
            log.info("Usuarios de prueba creados: admin@duoc.cl, vendedor@duoc.cl, cliente@gmail.com");
        }
    }

    private void cargarProductos() {
        Map<String, Categoria> categorias = new HashMap<>();
        crearProducto(categorias, "CO001", "PlayStation 5",
                "Consola de última generación con SSD ultrarrápido y control DualSense.", 549990, "Consolas", 10, 3,
                "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTcnt6Gls9W-3fXm0LcNv4nCJXqRRUyFArPmsSD0n9Ihw&s=10",
                "🎮", true);
        crearProducto(categorias, "AC001", "Teclado Mecánico Attack Shark X68",
                "Teclado 65% con switches magnéticos y retroiluminación RGB.", 79990, "Accesorios", 15, 4,
                "https://media.falabella.com/falabellaCL/150984594_01/w=1500,h=1500,fit=cover", "⌨️", true);
        crearProducto(categorias, "MS001", "Mouse Gamer Logitech G502 HERO",
                "Sensor HERO 25K, 11 botones programables y pesos ajustables.", 49990, "Mouse", 20, 5,
                "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSCGGDP1InUme74QM_Omxn9OcE-lwQL6aBGZ8ELdgq3QA&s=10",
                "🖱️", true);
        crearProducto(categorias, "AU001", "Audífonos ASUS TUF Gaming H1 Gen II Hatsune Miku Edition",
                "Audífonos inalámbricos con sonido envolvente 7.1 y micrófono desmontable.", 119990, "Audífonos", 5, 5,
                "https://media.spdigital.cl/thumbnails/products/1773677662593-audtuf1_df230968_07b34a49_thumbnail_512.jpg",
                "🎧", true);
        crearProducto(categorias, "JM001", "Juego de Mesa Catan",
                "Clásico de estrategia para 3 a 4 jugadores: comercia, construye y coloniza.", 29990, "Juegos de Mesa",
                12, 3, "", "🎲", false);
        crearProducto(categorias, "SG001", "Silla Gamer Ergonómica Pro",
                "Silla reclinable con soporte lumbar, cojín cervical y apoyabrazos 4D.", 189990, "Sillas Gamers", 2, 3,
                "", "🪑", false);
        crearProducto(categorias, "MP001", "Mousepad XL RGB",
                "Superficie extendida de 90 x 40 cm con borde iluminado RGB.", 24990, "Mousepad", 0, 2, "", "🟩", false);
        crearProducto(categorias, "PP001", "Polera Gamer Personalizada",
                "Polera de algodón con tu gamertag estampado. Tallas S a XXL.", 14990, "Poleras Personalizadas", 25, 5,
                "", "👕", false);
        log.info("Datos iniciales cargados: {} categorías y {} productos", categorias.size(), productoRepository.count());
    }

    private void crearProducto(Map<String, Categoria> categorias, String codigo, String nombre, String descripcion,
                               int precio, String categoria, int stock, int stockCritico, String imagen, String icono,
                               boolean destacado) {
        Producto p = new Producto();
        p.setCodigo(codigo);
        p.setNombre(nombre);
        p.setDescripcion(descripcion);
        p.setPrecio(precio);
        p.setStock(stock);
        p.setStockCritico(stockCritico);
        p.setImagen(imagen);
        p.setIcono(icono);
        p.setDestacado(destacado);
        p.setCategoria(categorias.computeIfAbsent(categoria, nombreCategoria -> categoriaRepository
                .findByNombreIgnoreCase(nombreCategoria)
                .orElseGet(() -> categoriaRepository.save(new Categoria(nombreCategoria)))));
        productoRepository.save(p);
    }

    private void crearUsuario(String run, String nombre, String apellidos, String correo, String password, Rol rol) {
        Usuario u = new Usuario();
        u.setRun(run);
        u.setNombre(nombre);
        u.setApellidos(apellidos);
        u.setCorreo(correo);
        u.setPassword(passwordEncoder.encode(password));
        u.setRol(rol);
        u.setFechaNacimiento(LocalDate.of(1998, 5, 10));
        u.setRegion("Región Metropolitana");
        u.setComuna("Santiago");
        u.setDireccion("Av. España 8, Santiago");
        u.setTelefono("+56 9 1234 5678");
        usuarioRepository.save(u);
    }
}
