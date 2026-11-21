package cl.duoc.levelup;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada del backend de la Tienda Level-Up Gamer.
 * Swagger queda disponible en http://localhost:8080/swagger-ui.html
 */
@SpringBootApplication
public class LevelUpApplication {

    public static void main(String[] args) {
        SpringApplication.run(LevelUpApplication.class, args);
    }
}
