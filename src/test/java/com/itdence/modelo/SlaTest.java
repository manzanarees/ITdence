package com.itdence.modelo;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

// Prueba solo los calculos del SLA, con horas fijas, sin BD.
// Ticket de prioridad alta: limite de 4 horas, creado a las 10:00 -> vence a las 14:00
class SlaTest {

    private static final LocalDateTime LUNES_10 = LocalDateTime.of(2026, 9, 28, 10, 0, 0);

    private Ticket ticketAlta(String estado) {
        Ticket t = new Ticket();
        t.setPrioridad("alta");
        t.setEstado(estado);
        t.setFechaCreacion("2026-09-28 10:00:00");
        t.setFechaActualizacion("2026-09-28 10:00:00");
        t.setFechaLimite("2026-09-28 14:00:00");
        return t;
    }

    @Test
    void lasHorasDependenDeLaPrioridad() {
        assertEquals(4, Sla.horas("alta"));
        assertEquals(24, Sla.horas("media"));
        assertEquals(72, Sla.horas("baja"));
    }

    @Test
    void conTiempoDeSobraEstaEnPlazo() {
        Ticket t = ticketAlta("abierto");
        LocalDateTime ahora = LUNES_10.plusHours(1);
        assertEquals(Sla.Situacion.EN_PLAZO, Sla.situacion(t, ahora));
        assertEquals("Vence en 3 h", Sla.texto(t, ahora));
    }

    @Test
    void cuandoQuedaElVeinticincoPorCientoOMenosAvisa() {
        Ticket t = ticketAlta("en progreso");
        // quedan 60 min de 240: justo el 25 %
        assertEquals(Sla.Situacion.POR_VENCER, Sla.situacion(t, LUNES_10.plusHours(3)));
        // quedan 61 min: todavia no
        assertEquals(Sla.Situacion.EN_PLAZO, Sla.situacion(t, LUNES_10.plusMinutes(179)));
        assertEquals("Vence en 30 min", Sla.texto(t, LUNES_10.plusMinutes(210)));
    }

    @Test
    void pasadoElLimiteEstaVencido() {
        Ticket t = ticketAlta("abierto");
        LocalDateTime ahora = LUNES_10.plusHours(5);
        assertEquals(Sla.Situacion.VENCIDO, Sla.situacion(t, ahora));
        assertEquals("Vencido hace 1 h", Sla.texto(t, ahora));
    }

    @Test
    void justoEnElLimiteTodaviaNoEstaVencido() {
        assertNotEquals(Sla.Situacion.VENCIDO, Sla.situacion(ticketAlta("abierto"), LUNES_10.plusHours(4)));
    }

    @Test
    void unTicketResueltoATiempoCuentaComoCumplidoAunqueHayaPasadoElTiempo() {
        Ticket t = ticketAlta("resuelto");
        t.setFechaResolucion("2026-09-28 13:00:00");
        // da igual lo tarde que sea ahora: ya se resolvio dentro del plazo
        assertEquals(Sla.Situacion.CUMPLIDO, Sla.situacion(t, LUNES_10.plusDays(30)));
        assertEquals("Cumplido", Sla.texto(t, LUNES_10.plusDays(30)));
    }

    @Test
    void unTicketResueltoTardeEsFueraDePlazo() {
        Ticket t = ticketAlta("cerrado");
        t.setFechaResolucion("2026-09-28 15:00:00");
        assertEquals(Sla.Situacion.INCUMPLIDO, Sla.situacion(t, LUNES_10.plusHours(6)));
        assertEquals("Fuera de plazo", Sla.texto(t, LUNES_10.plusHours(6)));
    }

    @Test
    void sinFechaDeResolucionSeUsaLaUltimaActualizacion() {
        Ticket t = ticketAlta("resuelto");
        t.setFechaActualizacion("2026-09-28 16:00:00");
        assertEquals(Sla.Situacion.INCUMPLIDO, Sla.situacion(t, LUNES_10.plusHours(8)));
    }

    @Test
    void unTicketSinLimiteNuncaSeMarcaVencido() {
        Ticket t = ticketAlta("abierto");
        t.setFechaLimite(null);
        assertEquals(Sla.Situacion.EN_PLAZO, Sla.situacion(t, LUNES_10.plusDays(100)));
    }

    @Test
    void unTicketCanceladoNoCuentaNiComoCumplidoNiComoVencido() {
        Ticket t = ticketAlta("cerrado");
        t.setCancelado(true);
        assertEquals(Sla.Situacion.CANCELADO, Sla.situacion(t, LUNES_10.plusDays(30)));
        assertEquals("Cancelado", Sla.texto(t, LUNES_10.plusDays(30)));
    }

    @Test
    void formatoDeLasDuraciones() {
        assertEquals("45 min", Sla.duracion(Duration.ofMinutes(45)));
        assertEquals("1 h 30 min", Sla.duracion(Duration.ofMinutes(90)));
        assertEquals("3 h", Sla.duracion(Duration.ofHours(3)));
        assertEquals("9 h 59 min", Sla.duracion(Duration.ofMinutes(599)));
        // desde 10 h se quitan los minutos y no se redondea hacia arriba
        assertEquals("21 h", Sla.duracion(Duration.ofMinutes(21 * 60 + 59)));
        assertEquals("1 d 1 h", Sla.duracion(Duration.ofHours(25)));
        assertEquals("2 d", Sla.duracion(Duration.ofHours(48)));
    }
}
