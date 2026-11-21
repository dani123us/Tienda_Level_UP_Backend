package cl.duoc.levelup.service;

import cl.duoc.levelup.exception.ReglaNegocioException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CalculadoraDescuentosTest {

    private final CalculadoraDescuentos calculadora = new CalculadoraDescuentos();

    @Test
    void sinDescuentosElTotalEsIgualAlSubtotal() {
        CalculadoraDescuentos.Resumen r = calculadora.calcular(100000, "cliente@gmail.com", null);
        assertThat(r.porcentaje()).isZero();
        assertThat(r.total()).isEqualTo(100000);
        assertThat(r.cupon()).isNull();
    }

    @Test
    void correoDuocTiene20PorCiento() {
        assertThat(calculadora.calcular(100000, "alumno@duoc.cl", "").total()).isEqualTo(80000);
        assertThat(calculadora.calcular(100000, "PROFE@profesor.duoc.cl", null).porcentaje()).isEqualTo(20);
        assertThat(calculadora.esCorreoDuoc("falso@duoc.cl.com")).isFalse();
    }

    @Test
    void losCuponesSeSumanAlDescuentoDuoc() {
        CalculadoraDescuentos.Resumen r = calculadora.calcular(100000, "alumno@duoc.cl", " gamer15 ");
        assertThat(r.porcentaje()).isEqualTo(35);
        assertThat(r.descuento()).isEqualTo(35000);
        assertThat(r.total()).isEqualTo(65000);
        assertThat(r.cupon()).isEqualTo("GAMER15");
        assertThat(calculadora.calcular(50000, "x@gmail.com", "LEVELUP10").total()).isEqualTo(45000);
    }

    @Test
    void cuponInvalidoLanzaReglaDeNegocio() {
        assertThatThrownBy(() -> calculadora.calcular(1000, "x@gmail.com", "GRATIS100"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("GRATIS100");
    }

    @Test
    void elPorcentajeTieneTopeDe100() {
        CalculadoraDescuentos.Resumen r = calculadora.calcular(0, "alumno@duoc.cl", "GAMER15");
        assertThat(r.total()).isZero();
        assertThat(r.porcentaje()).isLessThanOrEqualTo(100);
    }
}
