package com.itdence.servicio;

import com.itdence.modelo.Notificacion;
import com.itdence.modelo.Ticket;
import com.itdence.modelo.Usuario;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Cada test crea su propio cliente y su propio tecnico: asi sus avisos no se mezclan
// con los de los demas tests y se pueden comprobar los numeros exactos.
class NotificacionServicioTest extends BaseTest {

    private final TicketServicio tickets = new TicketServicio();
    private final ComentarioServicio comentarios = new ComentarioServicio();
    private final NotificacionServicio avisos = new NotificacionServicio();

    private Usuario nuevo(String nombre, String rol) {
        String email = emailUnico();
        new UsuarioServicio().crear(nombre, email, "123456", rol);
        return auth.login(email, "123456");
    }

    private Ticket ticketDe(Usuario creador, String prioridad) {
        int id = tickets.crearTicket("Aviso " + System.nanoTime(), "", prioridad, primeraCategoria(), creador);
        return tickets.buscarPorId(id);
    }

    private List<String> textos(Usuario u) {
        return avisos.listar(u).stream().map(Notificacion::getTexto).toList();
    }

    @Test
    void unTicketUrgenteAvisaATodoElEquipoTecnicoPeroNoAlCliente() {
        Usuario tec = nuevo("Tec uno", "tecnico");
        Usuario cli = nuevo("Cli uno", "cliente");
        Ticket t = ticketDe(cli, "alta");

        assertEquals(1, avisos.contarNoLeidas(tec));
        assertTrue(textos(tec).get(0).startsWith("Nuevo ticket urgente"));
        assertEquals(t.getId(), avisos.listar(tec).get(0).getTicketId());
        assertEquals(0, avisos.contarNoLeidas(cli));
    }

    @Test
    void unTicketNormalNoAvisaANadie() {
        Usuario tec = nuevo("Tec dos", "tecnico");
        Usuario cli = nuevo("Cli dos", "cliente");
        ticketDe(cli, "media");
        assertEquals(0, avisos.contarNoLeidas(tec));
        assertEquals(0, avisos.contarNoLeidas(cli));
    }

    @Test
    void alTomarElTicketSeAvisaAlClienteYNoAlTecnico() {
        Usuario tec = nuevo("Tec tres", "tecnico");
        Usuario cli = nuevo("Cli tres", "cliente");
        Ticket t = ticketDe(cli, "baja");

        tickets.tomarTicket(t, tec);

        assertEquals(List.of("Tec tres ha tomado tu ticket"), textos(cli));
        assertEquals(0, avisos.contarNoLeidas(tec));
    }

    @Test
    void alCambiarElEstadoSeAvisaAlClienteConUnTextoSegunElEstado() {
        Usuario tec = nuevo("Tec cuatro", "tecnico");
        Usuario cli = nuevo("Cli cuatro", "cliente");
        Ticket t = ticketDe(cli, "baja");
        tickets.tomarTicket(t, tec);

        tickets.cambiarEstado(tickets.buscarPorId(t.getId()), "resuelto", tec);

        assertTrue(textos(cli).get(0).startsWith("Tu ticket está resuelto"));
        assertEquals(2, avisos.contarNoLeidas(cli));
    }

    @Test
    void siCambiaElEstadoElPropioCreadorNoSeAvisaASiMismo() {
        Usuario cli = nuevo("Cli cinco", "cliente");
        Ticket t = ticketDe(cli, "baja");
        tickets.cambiarEstado(t, "en progreso", admin());
        assertEquals(1, avisos.contarNoLeidas(cli));
        // el admin no recibe nada por lo que ha hecho el mismo
        int antes = avisos.contarNoLeidas(admin());
        tickets.cambiarEstado(tickets.buscarPorId(t.getId()), "resuelto", admin());
        assertEquals(antes, avisos.contarNoLeidas(admin()));
    }

    @Test
    void losComentariosAvisanALaOtraParteYLasNotasInternasANadie() {
        Usuario tec = nuevo("Tec seis", "tecnico");
        Usuario cli = nuevo("Cli seis", "cliente");
        Ticket t = ticketDe(cli, "baja");
        tickets.tomarTicket(t, tec);
        int clienteAntes = avisos.contarNoLeidas(cli);
        int tecnicoAntes = avisos.contarNoLeidas(tec);

        comentarios.comentar(tickets.buscarPorId(t.getId()), tec, "Hola, ya lo miro");
        assertEquals(clienteAntes + 1, avisos.contarNoLeidas(cli));
        assertEquals("Tec seis ha respondido en tu ticket", textos(cli).get(0));

        comentarios.comentar(tickets.buscarPorId(t.getId()), cli, "Gracias");
        assertEquals(tecnicoAntes + 1, avisos.contarNoLeidas(tec));
        assertEquals("Cli seis ha comentado en el ticket", textos(tec).get(0));

        comentarios.comentar(tickets.buscarPorId(t.getId()), tec, "Nota solo para el equipo", true);
        assertEquals(clienteAntes + 1, avisos.contarNoLeidas(cli));
        assertEquals(tecnicoAntes + 1, avisos.contarNoLeidas(tec));
    }

    @Test
    void siElTicketNoTieneTecnicoElComentarioDelClienteNoAvisaANadie() {
        Usuario cli = nuevo("Cli siete", "cliente");
        Ticket t = ticketDe(cli, "baja");
        comentarios.comentar(t, cli, "¿Alguien me ayuda?");
        assertEquals(0, avisos.contarNoLeidas(cli));
    }

    @Test
    void cerrarYReabrirAvisanAlTecnicoUnaSolaVez() {
        Usuario tec = nuevo("Tec ocho", "tecnico");
        Usuario cli = nuevo("Cli ocho", "cliente");
        Ticket t = ticketDe(cli, "baja");
        tickets.tomarTicket(t, tec);
        tickets.cambiarEstado(tickets.buscarPorId(t.getId()), "resuelto", tec);

        int antes = avisos.contarNoLeidas(tec);
        tickets.reabrir(tickets.buscarPorId(t.getId()), cli, "No funciona");
        // un aviso de "ha reabierto", sin el de "ha comentado" por el motivo
        assertEquals(antes + 1, avisos.contarNoLeidas(tec));
        assertEquals("Cli ocho ha reabierto el ticket", textos(tec).get(0));

        tickets.cambiarEstado(tickets.buscarPorId(t.getId()), "resuelto", tec);
        tickets.confirmarResolucion(tickets.buscarPorId(t.getId()), cli, 4);
        assertEquals("Cli ocho ha cerrado el ticket y lo ha valorado con 4/5", textos(tec).get(0));
    }

    @Test
    void elAdminAvisaAlTecnicoYAlClienteAlAsignar() {
        Usuario tec = nuevo("Tec nueve", "tecnico");
        Usuario cli = nuevo("Cli nueve", "cliente");
        Ticket t = ticketDe(cli, "baja");

        tickets.asignarA(t, tec, admin());

        assertEquals(List.of("Te han asignado este ticket"), textos(tec));
        assertEquals(List.of("Tec nueve se encarga de tu ticket"), textos(cli));
    }

    @Test
    void marcarComoLeidasSoloAfectaAlPropietario() {
        Usuario tec = nuevo("Tec diez", "tecnico");
        Usuario cli = nuevo("Cli diez", "cliente");
        Ticket t = ticketDe(cli, "alta");
        tickets.tomarTicket(t, tec);
        assertEquals(1, avisos.contarNoLeidas(cli));
        assertEquals(1, avisos.contarNoLeidas(tec));

        // el tecnico intenta marcar el aviso del cliente: no debe poder
        avisos.marcarLeida(avisos.listar(cli).get(0), tec);
        assertEquals(1, avisos.contarNoLeidas(cli));

        avisos.marcarLeida(avisos.listar(cli).get(0), cli);
        assertEquals(0, avisos.contarNoLeidas(cli));
        assertEquals(1, avisos.contarNoLeidas(tec));
    }

    @Test
    void abrirUnTicketDaPorLeidosSusAvisosYMarcarTodasLimpiaElResto() {
        Usuario tec = nuevo("Tec once", "tecnico");
        Usuario cli = nuevo("Cli once", "cliente");
        Ticket uno = ticketDe(cli, "alta");
        ticketDe(cli, "alta");
        assertEquals(2, avisos.contarNoLeidas(tec));

        avisos.marcarTicketLeido(tec, uno.getId());
        assertEquals(1, avisos.contarNoLeidas(tec));

        avisos.marcarTodasLeidas(tec);
        assertEquals(0, avisos.contarNoLeidas(tec));
    }

    @Test
    void sePuedeBorrarUnUsuarioQueSoloTieneAvisos() {
        Usuario tec = nuevo("Tec doce", "tecnico");
        Usuario cli = nuevo("Cli doce", "cliente");
        ticketDe(cli, "alta");
        assertEquals(1, avisos.contarNoLeidas(tec));

        assertDoesNotThrow(() -> new UsuarioServicio().eliminar(tec, admin()));
        assertNull(auth.login(tec.getEmail(), "123456"));
    }
}
