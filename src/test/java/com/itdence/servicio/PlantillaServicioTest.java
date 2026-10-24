package com.itdence.servicio;

import com.itdence.modelo.Plantilla;
import com.itdence.modelo.Ticket;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlantillaServicioTest extends BaseTest {

    private final PlantillaServicio servicio = new PlantillaServicio();

    private Plantilla buscar(String titulo) {
        return servicio.listarTodas().stream().filter(p -> p.getTitulo().equals(titulo)).findFirst().orElse(null);
    }

    private String tituloUnico() {
        return "Plantilla " + System.nanoTime();
    }

    @Test
    void existenLasRespuestasDeEjemplo() {
        assertNotNull(buscar("Reiniciar el equipo"));
        assertNotNull(buscar("Incidencia resuelta"));
    }

    @Test
    void elAdminPuedeCrearEditarYEliminar() {
        String titulo = tituloUnico();
        servicio.crear(admin(), titulo, "  Hola {cliente}  ");
        Plantilla p = buscar(titulo);
        assertNotNull(p);
        assertEquals("Hola {cliente}", p.getTexto());

        servicio.actualizar(admin(), p, titulo + " B", "Otro texto");
        assertEquals("Otro texto", buscar(titulo + " B").getTexto());

        servicio.eliminar(admin(), buscar(titulo + " B"));
        assertNull(buscar(titulo + " B"));
    }

    @Test
    void soloElAdminPuedeGestionarlasPeroElEquipoLasLee() {
        assertThrows(ValidacionException.class, () -> servicio.crear(tecnico(), tituloUnico(), "texto"));
        assertThrows(ValidacionException.class, () -> servicio.crear(cliente(), tituloUnico(), "texto"));
        assertThrows(ValidacionException.class, () -> servicio.eliminar(tecnico(), buscar("Reiniciar el equipo")));
        assertFalse(servicio.listarTodas().isEmpty());
    }

    @Test
    void validaTituloTextoYDuplicados() {
        assertThrows(ValidacionException.class, () -> servicio.crear(admin(), "  ", "texto"));
        assertThrows(ValidacionException.class, () -> servicio.crear(admin(), tituloUnico(), "  "));
        assertThrows(ValidacionException.class, () -> servicio.crear(admin(), "x".repeat(61), "texto"));
        assertThrows(ValidacionException.class, () -> servicio.crear(admin(), tituloUnico(), "x".repeat(1001)));
        // el titulo repetido da igual en mayusculas
        assertThrows(ValidacionException.class, () -> servicio.crear(admin(), "REINICIAR EL EQUIPO", "texto"));
    }

    @Test
    void alEditarSePuedeConservarElMismoTitulo() {
        String titulo = tituloUnico();
        servicio.crear(admin(), titulo, "uno");
        servicio.actualizar(admin(), buscar(titulo), titulo, "dos");
        assertEquals("dos", buscar(titulo).getTexto());
    }

    @Test
    void aplicarSustituyeLasVariables() {
        Ticket t = new Ticket();
        t.setId(42);
        t.setCreadorNombre("Marta Ruiz");
        Plantilla p = new Plantilla(0, "x", "Hola {cliente}, ticket {ticket}. — {tecnico}");

        assertEquals("Hola Marta Ruiz, ticket #0042. — Técnico de prueba", servicio.aplicar(p, t, tecnico()));
    }

    @Test
    void aplicarNoFallaSiNoHayVariables() {
        Ticket t = new Ticket();
        t.setId(1);
        assertEquals("Sin variables", servicio.aplicar(new Plantilla(0, "x", "Sin variables"), t, tecnico()));
    }
}
