package cl.duoc.levelup.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PasarelaPagoSimuladaTest {

    private final PasarelaPagoSimulada pasarela = new PasarelaPagoSimulada();

    @Test
    void apruebaUnaTarjetaNormal() {
        assertThat(pasarela.procesar("Crédito", "4242", 10000)).isEmpty();
    }

    @Test
    void rechazaTarjetasQueTerminanEn0000() {
        assertThat(pasarela.procesar("Débito", "0000", 10000)).hasValueSatisfying(
                motivo -> assertThat(motivo).contains("rechazó"));
    }

    @Test
    void rechazaMontosInvalidos() {
        assertThat(pasarela.procesar("Crédito", "4242", 0)).isPresent();
    }
}
