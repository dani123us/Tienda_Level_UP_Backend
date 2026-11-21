package cl.duoc.levelup.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Documentación Swagger / OpenAPI.
 * Disponible en http://localhost:8080/swagger-ui.html
 * El esquema "bearerAuth" habilita el botón "Authorize" para pegar el token de acceso.
 */
@Configuration
public class OpenApiConfig {

    public static final String ESQUEMA_SEGURIDAD = "bearerAuth";

    @Bean
    public OpenAPI apiLevelUp() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Tienda Level-Up Gamer")
                        .version("v1")
                        .description("""
                                API REST de la Tienda Level-Up Gamer (DSY1104 - Evaluación Parcial 3).

                                **Cómo probar:** ejecuta `POST /api/v1/auth/login` (por ejemplo admin@duoc.cl / admin123),
                                copia el `accessToken` y pégalo en el botón **Authorize**.

                                Roles: **Administrador** (todo), **Vendedor** (ver productos y órdenes), **Cliente** (comprar).
                                """)
                        .contact(new Contact().name("Equipo Level-Up Gamer")))
                .servers(List.of(new Server().url("http://localhost:8080").description("Servidor local")))
                .components(new Components().addSecuritySchemes(ESQUEMA_SEGURIDAD, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Token de acceso obtenido en /api/v1/auth/login")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_SEGURIDAD));
    }
}
