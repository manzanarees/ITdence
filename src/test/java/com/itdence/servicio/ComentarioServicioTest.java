package com.itdence.servicio;

import com.itdence.modelo.Comentario;
import com.itdence.modelo.Ticket;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ComentarioServicioTest extends BaseTest {

    private final TicketServicio ticketServicio = new TicketServicio();
    private final ComentarioServicio servicio = new ComentarioServicio();
    private Ticket nuevoTicket() {
        int id = ticketServicio.crearTicket("Comentarios " + System.nanoTime(), "", "baja", primeraCategoria(), cliente());
        return ticketServicio.buscarPorId(id);
    }

    @Test
    void comentarGuardaElComentarioConSuAutor() {
        Ticket t = nuevoTicket();
        servicio.comentar(t, cliente(), "  No funciona  ");

        List<Comentario> lista = servicio.listarPorTicket(t, cliente());
        assertEquals(1, lista.size());
        assertEquals("No funciona", lista.get(0).getTexto());
        assertEquals("Cliente de prueba", lista.get(0).getAutorNombre());
    }

    @Test
    void noSePuedeComentarVacio() {
        Ticket t = nuevoTicket();
        assertThrows(ValidacionException.class, () -> servicio.comentar(t, cliente(), "   "));
    }

    @Test
    void noSePuedeComentarEnUnTicketCerrado() {
        Ticket t = nuevoTicket();
        ticketServicio.cambiarEstado(t, "cerrado", admin());
        Ticket cerrado = ticketServicio.buscarPorId(t.getId());

        assertFalse(servicio.puedeComentar(cerrado, cliente()));
        assertThrows(ValidacionException.class, () -> servicio.comentar(cerrado, cliente(), "hola"));
    }

    @Test
    void unClienteNoPuedeComentarTicketsDeOtro() {
        Ticket t = nuevoTicket();
        String email = emailUnico();
        new UsuarioServicio().crear("Otro cliente", email, "123456", "cliente");
        assertFalse(servicio.puedeComentar(t, auth.login(email, "123456")));
    }

    @Test
    void elClienteNoVeLasNotasInternasPeroElEquipoSi() {
        Ticket t = nuevoTicket();
        servicio.comentar(t, cliente(), "Visible para todos");
        servicio.comentar(t, tecnico(), "Cuidado, el cliente ya llamó antes", true);

        assertEquals(1, servicio.listarPorTicket(t, cliente()).size());
        assertEquals("Visible para todos", servicio.listarPorTicket(t, cliente()).get(0).getTexto());
        assertEquals(2, servicio.listarPorTicket(t, tecnico()).size());
        assertEquals(2, servicio.listarPorTicket(t, admin()).size());
        assertTrue(servicio.listarPorTicket(t, tecnico()).get(1).isInterno());
    }

    @Test
    void unClienteNoPuedeEscribirNotasInternas() {
        Ticket t = nuevoTicket();
        assertThrows(ValidacionException.class, () -> servicio.comentar(t, cliente(), "intento colarla", true));
        assertEquals(0, servicio.listarPorTicket(t, tecnico()).size());
    }

    @Test
    void unaNotaInternaNoCambiaLaFechaDeActualizacion() throws InterruptedException {
        Ticket t = nuevoTicket();
        String antes = ticketServicio.buscarPorId(t.getId()).getFechaActualizacion();
        Thread.sleep(1100);
        servicio.comentar(t, tecnico(), "nota privada", true);
        assertEquals(antes, ticketServicio.buscarPorId(t.getId()).getFechaActualizacion());

        servicio.comentar(t, tecnico(), "respuesta publica", false);
        assertNotEquals(antes, ticketServicio.buscarPorId(t.getId()).getFechaActualizacion());
    }

    @Test
    void elTecnicoPuedeComentarCualquierTicketAbierto() {
        Ticket t = nuevoTicket();
        assertTrue(servicio.puedeComentar(t, tecnico()));
    }
}
