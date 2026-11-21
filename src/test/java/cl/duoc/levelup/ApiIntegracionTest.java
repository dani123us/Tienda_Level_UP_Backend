package cl.duoc.levelup;

import cl.duoc.levelup.repository.ProductoRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integración: levantan la aplicación completa con H2 y prueban
 * la seguridad (401/403), los roles y el checkout de punta a punta.
 * Cada prueba se revierte al terminar (@Transactional), así no se afectan entre sí.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiIntegracionTest {

    private static final String PRODUCTO_NUEVO = """
            {"codigo":"ts900","nombre":"Control de prueba","descripcion":"Prueba","precio":19990,
             "stock":10,"stockCritico":2,"imagen":"","icono":"🎮","destacado":false,"categoria":"Accesorios"}
            """;

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper json;
    @Autowired
    private ProductoRepository productoRepository;

    private JsonNode login(String correo, String password) throws Exception {
        String cuerpo = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"" + correo + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(cuerpo);
    }

    private String token(String correo, String password) throws Exception {
        return login(correo, password).get("accessToken").asText();
    }

    private MockHttpServletRequestBuilder conToken(MockHttpServletRequestBuilder req, String token) {
        return req.header("Authorization", "Bearer " + token);
    }

    // ---------- Autenticación ----------

    @Test
    void loginCorrectoDevuelveTokensYUsuarioSinPassword() throws Exception {
        JsonNode r = login("admin@duoc.cl", "admin123");
        assertThat(r.get("accessToken").asText()).isNotBlank();
        assertThat(r.get("refreshToken").asText()).isNotBlank();
        assertThat(r.get("expiraEnSegundos").asLong()).isEqualTo(900);
        assertThat(r.get("usuario").get("rol").asText()).isEqualTo("Administrador");
        assertThat(r.get("usuario").has("password")).isFalse();
    }

    @Test
    void loginIncorrectoDevuelve401() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"admin@duoc.cl\",\"password\":\"malaclave\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Correo o contraseña incorrectos"));
    }

    @Test
    void refreshEntregaUnParNuevoYNoSirveComoTokenDeAcceso() throws Exception {
        String refresh = login("cliente@gmail.com", "cliente123").get("refreshToken").asText();

        // Un refresh token usado como token de acceso → 401.
        mvc.perform(conToken(get("/api/v1/auth/me"), refresh)).andExpect(status().isUnauthorized());

        // Con /auth/refresh sí funciona y entrega un token de acceso válido.
        String cuerpo = mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refresh + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String nuevoAcceso = json.readTree(cuerpo).get("accessToken").asText();
        mvc.perform(conToken(get("/api/v1/auth/me"), nuevoAcceso))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("cliente@gmail.com"));
    }

    @Test
    void refreshConTokenDeAccesoDevuelve401() throws Exception {
        String acceso = token("cliente@gmail.com", "cliente123");
        mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + acceso + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registroCreaUnClienteYValidaDuplicados() throws Exception {
        String registro = """
                {"run":"12.345.678-5","nombre":"Pedro","apellidos":"Soto","correo":"Pedro@Gmail.com","password":"clave1",
                 "fechaNacimiento":"2000-01-01","region":"Región Metropolitana","comuna":"Santiago","direccion":"Calle 1"}
                """;
        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content(registro))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuario.rol").value("Cliente"))
                .andExpect(jsonPath("$.usuario.correo").value("pedro@gmail.com"))
                .andExpect(jsonPath("$.usuario.run").value("123456785"));
        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content(registro))
                .andExpect(status().isConflict());
    }

    @Test
    void validacionDevuelve400ConLosCampos() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"no-es-correo\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.correo").exists())
                .andExpect(jsonPath("$.campos.password").exists());
    }

    // ---------- Productos y roles ----------

    @Test
    void listarProductosEsPublico() throws Exception {
        mvc.perform(get("/api/v1/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(8))
                .andExpect(jsonPath("$[0].codigo").value("CO001"));
    }

    @Test
    void crearProductoSinTokenDevuelve401() throws Exception {
        mvc.perform(post("/api/v1/productos").contentType(MediaType.APPLICATION_JSON).content(PRODUCTO_NUEVO))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.estado").value(401));
    }

    @Test
    void crearProductoComoClienteDevuelve403() throws Exception {
        String cliente = token("cliente@gmail.com", "cliente123");
        mvc.perform(conToken(post("/api/v1/productos"), cliente)
                        .contentType(MediaType.APPLICATION_JSON).content(PRODUCTO_NUEVO))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.estado").value(403));
    }

    @Test
    void adminCreaYEditaProductos() throws Exception {
        String admin = token("admin@duoc.cl", "admin123");
        String cuerpo = mvc.perform(conToken(post("/api/v1/productos"), admin)
                        .contentType(MediaType.APPLICATION_JSON).content(PRODUCTO_NUEVO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("TS900"))
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(cuerpo).get("id").asLong();

        mvc.perform(conToken(put("/api/v1/productos/" + id), admin).contentType(MediaType.APPLICATION_JSON)
                        .content(PRODUCTO_NUEVO.replace("19990", "15990")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precio").value(15990));

        // Código duplicado → 409
        mvc.perform(conToken(post("/api/v1/productos"), admin)
                        .contentType(MediaType.APPLICATION_JSON).content(PRODUCTO_NUEVO))
                .andExpect(status().isConflict());
    }

    @Test
    void vendedorSoloVeProductosYOrdenes() throws Exception {
        String vendedor = token("vendedor@duoc.cl", "vendedor123");
        mvc.perform(conToken(get("/api/v1/boletas"), vendedor)).andExpect(status().isOk());
        mvc.perform(conToken(get("/api/v1/productos"), vendedor)).andExpect(status().isOk());
        mvc.perform(conToken(post("/api/v1/productos"), vendedor)
                        .contentType(MediaType.APPLICATION_JSON).content(PRODUCTO_NUEVO))
                .andExpect(status().isForbidden());
        mvc.perform(conToken(get("/api/v1/usuarios"), vendedor)).andExpect(status().isForbidden());
    }

    @Test
    void clienteNoPuedeVerTodasLasOrdenes() throws Exception {
        String cliente = token("cliente@gmail.com", "cliente123");
        mvc.perform(conToken(get("/api/v1/boletas"), cliente)).andExpect(status().isForbidden());
        mvc.perform(conToken(get("/api/v1/boletas/mias"), cliente)).andExpect(status().isOk());
    }

    @Test
    void tokenFalsificadoDevuelve401() throws Exception {
        mvc.perform(conToken(get("/api/v1/usuarios"), "eyJhbGciOiJIUzI1NiJ9.e30.firmaFalsa"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- Checkout ----------

    private String checkout(String digitos, int cantidad, String cupon) {
        return """
                {"items":[{"productoId":%d,"cantidad":%d}],"nombreCliente":"Camila Pérez","correoCliente":"cliente@gmail.com",
                 "telefono":"+56912345678","direccion":"Av. España 8","region":"Región Metropolitana","comuna":"Santiago",
                 "indicaciones":"","cupon":%s,"metodoPago":"Crédito","ultimosDigitosTarjeta":"%s"}
                """.formatted(idPlay(), cantidad, cupon == null ? "null" : "\"" + cupon + "\"", digitos);
    }

    private long idPlay() {
        return productoRepository.findAll().stream().filter(p -> p.getCodigo().equals("CO001")).findFirst()
                .orElseThrow().getId();
    }

    private int stockPlay() {
        return productoRepository.findById(idPlay()).orElseThrow().getStock();
    }

    @Test
    void checkoutPagadoDescuentaStock() throws Exception {
        String cliente = token("cliente@gmail.com", "cliente123");
        int antes = stockPlay();

        String cuerpo = mvc.perform(conToken(post("/api/v1/boletas"), cliente)
                        .contentType(MediaType.APPLICATION_JSON).content(checkout("4242", 2, "LEVELUP10")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PAGADA"))
                .andExpect(jsonPath("$.subtotal").value(1099980))
                .andExpect(jsonPath("$.porcentajeDescuento").value(10))
                .andExpect(jsonPath("$.total").value(989982))
                .andReturn().getResponse().getContentAsString();

        assertThat(stockPlay()).isEqualTo(antes - 2);
        long id = json.readTree(cuerpo).get("id").asLong();
        mvc.perform(conToken(get("/api/v1/boletas/" + id), cliente)).andExpect(status().isOk());
        mvc.perform(conToken(get("/api/v1/boletas/mias"), cliente))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void checkoutRechazadoNoDescuentaStock() throws Exception {
        String cliente = token("cliente@gmail.com", "cliente123");
        int antes = stockPlay();

        mvc.perform(conToken(post("/api/v1/boletas"), cliente)
                        .contentType(MediaType.APPLICATION_JSON).content(checkout("0000", 1, null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("RECHAZADA"))
                .andExpect(jsonPath("$.motivoRechazo").isNotEmpty());

        assertThat(stockPlay()).isEqualTo(antes);
    }

    @Test
    void checkoutConStockInsuficienteOCuponInvalidoDevuelve409() throws Exception {
        String cliente = token("cliente@gmail.com", "cliente123");
        mvc.perform(conToken(post("/api/v1/boletas"), cliente)
                        .contentType(MediaType.APPLICATION_JSON).content(checkout("4242", 999, null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("PlayStation 5")));
        mvc.perform(conToken(post("/api/v1/boletas"), cliente)
                        .contentType(MediaType.APPLICATION_JSON).content(checkout("4242", 1, "FALSO")))
                .andExpect(status().isConflict());
    }

    @Test
    void vendedorNoPuedeComprar() throws Exception {
        String vendedor = token("vendedor@duoc.cl", "vendedor123");
        mvc.perform(conToken(post("/api/v1/boletas"), vendedor)
                        .contentType(MediaType.APPLICATION_JSON).content(checkout("4242", 1, null)))
                .andExpect(status().isForbidden());
    }

    // ---------- Usuarios ----------

    @Test
    void adminNoPuedeQuitarseElRolNiEliminarse() throws Exception {
        String admin = token("admin@duoc.cl", "admin123");
        long idAdmin = login("admin@duoc.cl", "admin123").get("usuario").get("id").asLong();
        String datos = """
                {"run":"19011022K","nombre":"Admin","apellidos":"Level-Up","correo":"admin@duoc.cl","password":"",
                 "rol":"Cliente"}
                """;
        mvc.perform(conToken(put("/api/v1/usuarios/" + idAdmin), admin)
                        .contentType(MediaType.APPLICATION_JSON).content(datos))
                .andExpect(status().isConflict());
        mvc.perform(conToken(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/v1/usuarios/" + idAdmin), admin))
                .andExpect(status().isConflict());
    }

    @Test
    void adminCreaUsuarioConPasswordValida() throws Exception {
        String admin = token("admin@duoc.cl", "admin123");
        String datos = """
                {"run":"98765432","nombre":"Nuevo","apellidos":"Vendedor","correo":"nuevo@duoc.cl","password":"%s",
                 "rol":"Vendedor"}
                """;
        mvc.perform(conToken(post("/api/v1/usuarios"), admin)
                        .contentType(MediaType.APPLICATION_JSON).content(datos.formatted("")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.password").exists());
        mvc.perform(conToken(post("/api/v1/usuarios"), admin)
                        .contentType(MediaType.APPLICATION_JSON).content(datos.formatted("clave12")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").value("Vendedor"));
        // El nuevo vendedor puede iniciar sesión (la contraseña quedó cifrada con BCrypt).
        assertThat(token("nuevo@duoc.cl", "clave12")).isNotBlank();
    }
}
