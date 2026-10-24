package com.itdence.servicio;

import com.itdence.modelo.Actividad;
import com.itdence.modelo.Comentario;
import com.itdence.modelo.Ticket;
import com.itdence.modelo.Usuario;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CierreConElClienteTest extends BaseTest {

    private final TicketServicio servicio = new TicketServicio();
    private final ComentarioServicio comentarios = new ComentarioServicio();

    // ticket ya resuelto por el tecnico, esperando que el cliente decida
    private Ticket ticketResuelto() {
        int id = servicio.crearTicket("Cierre " + System.nanoTime(), "desc", "media", primeraCategoria(), cliente());
        servicio.tomarTicket(servicio.buscarPorId(id), tecnico());
        servicio.cambiarEstado(servicio.buscarPorId(id), "resuelto", tecnico());
        return servicio.buscarPorId(id);
    }

    @Test
    void elClienteCierraElTicketYLoValora() {
        Ticket t = ticketResuelto();
        servicio.confirmarResolucion(t, cliente(), 5);

        Ticket cerrado = servicio.buscarPorId(t.getId());
        assertEquals("cerrado", cerrado.getEstado());
        assertEquals(5, cerrado.getValoracion());
        List<String> historial = servicio.listarActividad(t).stream().map(Actividad::getDetalle).toList();
        assertTrue(historial.contains("resuelto → cerrado"));
        assertTrue(historial.contains("valoró la atención con 5/5"));
    }

    @Test
    void soloSePuedeCerrarCuandoEstaResueltoYPorSuCreador() {
        int id = servicio.crearTicket("Abierto " + System.nanoTime(), "", "baja", primeraCategoria(), cliente());
        Ticket abierto = servicio.buscarPorId(id);
        assertFalse(servicio.puedeResponderResolucion(abierto, cliente()));
        assertThrows(ValidacionException.class, () -> servicio.confirmarResolucion(abierto, cliente(), 4));

        Ticket resuelto = ticketResuelto();
        assertTrue(servicio.puedeResponderResolucion(resuelto, cliente()));
        assertFalse(servicio.puedeResponderResolucion(resuelto, tecnico()));
        assertFalse(servicio.puedeResponderResolucion(resuelto, admin()));
        assertThrows(ValidacionException.class, () -> servicio.confirmarResolucion(resuelto, tecnico(), 5));

        // otro cliente distinto del que lo abrio
        String email = emailUnico();
        new UsuarioServicio().crear("Otro cliente", email, "123456", "cliente");
        Usuario otro = auth.login(email, "123456");
        assertThrows(ValidacionException.class, () -> servicio.confirmarResolucion(resuelto, otro, 5));
        assertThrows(ValidacionException.class, () -> servicio.reabrir(resuelto, otro, "no es mio"));
        assertEquals("resuelto", servicio.buscarPorId(resuelto.getId()).getEstado());
    }

    @Test
    void laValoracionTieneQueSerDeUnoACinco() {
        Ticket t = ticketResuelto();
        assertThrows(ValidacionException.class, () -> servicio.confirmarResolucion(t, cliente(), 0));
        assertThrows(ValidacionException.class, () -> servicio.confirmarResolucion(t, cliente(), 6));
        assertEquals("resuelto", servicio.buscarPorId(t.getId()).getEstado());
        assertNull(servicio.buscarPorId(t.getId()).getValoracion());
    }

    @Test
    void reabrirVuelveAEnProgresoCuentaLaReaperturaYGuardaElMotivo() {
        Ticket t = ticketResuelto();
        assertNotNull(servicio.buscarPorId(t.getId()).getFechaResolucion());

        servicio.reabrir(t, cliente(), "  Sigue sin funcionar  ");

        Ticket despues = servicio.buscarPorId(t.getId());
        assertEquals("en progreso", despues.getEstado());
        assertEquals(1, despues.getReaperturas());
        assertNull(despues.getFechaResolucion());
        assertEquals(tecnico().getId(), despues.getTecnicoAsignadoId());

        List<Comentario> conversacion = comentarios.listarPorTicket(t, cliente());
        assertEquals("Reabro el ticket: Sigue sin funcionar", conversacion.get(conversacion.size() - 1).getTexto());
        List<String> tipos = servicio.listarActividad(t).stream().map(Actividad::getTipo).toList();
        assertTrue(tipos.contains("reabierto"));
    }

    @Test
    void sinMotivoNoSeReabre() {
        Ticket t = ticketResuelto();
        assertThrows(ValidacionException.class, () -> servicio.reabrir(t, cliente(), "   "));
        assertThrows(ValidacionException.class, () -> servicio.reabrir(t, cliente(), "x".repeat(901)));
        assertEquals("resuelto", servicio.buscarPorId(t.getId()).getEstado());
        assertEquals(0, servicio.buscarPorId(t.getId()).getReaperturas());
    }

    @Test
    void unTicketResueltoPorElAdminSinTecnicoVuelveAAbierto() {
        int id = servicio.crearTicket("Sin tecnico " + System.nanoTime(), "", "baja", primeraCategoria(), cliente());
        servicio.cambiarEstado(servicio.buscarPorId(id), "resuelto", admin());
        servicio.reabrir(servicio.buscarPorId(id), cliente(), "no era eso");
        assertEquals("abierto", servicio.buscarPorId(id).getEstado());
    }

    @Test
    void unTicketCerradoYaNoSePuedeReabrirNiCerrarOtraVez() {
        Ticket t = ticketResuelto();
        servicio.confirmarResolucion(t, cliente(), 3);
        assertThrows(ValidacionException.class, () -> servicio.reabrir(t, cliente(), "ahora si"));
        assertThrows(ValidacionException.class, () -> servicio.confirmarResolucion(t, cliente(), 5));
        assertEquals(3, servicio.buscarPorId(t.getId()).getValoracion());
    }

    @Test
    void elResumenDeCalidadSumaValoracionesYReaperturas() {
        TicketServicio.ResumenCalidad antes = servicio.resumenCalidad();

        servicio.confirmarResolucion(ticketResuelto(), cliente(), 4);
        servicio.reabrir(ticketResuelto(), cliente(), "otra vez");

        TicketServicio.ResumenCalidad despues = servicio.resumenCalidad();
        assertEquals(antes.valoraciones() + 1, despues.valoraciones());
        assertEquals(antes.reaperturas() + 1, despues.reaperturas());
        double esperada = (antes.mediaValoracion() * antes.valoraciones() + 4) / (antes.valoraciones() + 1);
        assertEquals(esperada, despues.mediaValoracion(), 0.0001);
        assertTrue(despues.minutosMedios() >= 0);
    }

    // ---------- Cancelar ----------

    @Test
    void elClientePuedeCancelarSuTicketMientrasNoEsteResuelto() {
        int id = servicio.crearTicket("Cancelar " + System.nanoTime(), "", "baja", primeraCategoria(), cliente());
        Ticket t = servicio.buscarPorId(id);
        assertTrue(servicio.puedeCancelar(t, cliente()));
        assertFalse(servicio.puedeCancelar(t, tecnico()));
        assertFalse(servicio.puedeCancelar(t, admin()));

        servicio.cancelar(t, cliente());

        Ticket cancelado = servicio.buscarPorId(id);
        assertEquals("cerrado", cancelado.getEstado());
        assertTrue(cancelado.isCancelado());
        assertNull(cancelado.getFechaResolucion());
        assertNull(cancelado.getValoracion());
        assertTrue(servicio.listarActividad(t).stream().anyMatch(a -> "canceló el ticket".equals(a.getDetalle())));
    }

    @Test
    void siTieneTecnicoSeLeAvisaDeLaCancelacion() {
        String email = emailUnico();
        new UsuarioServicio().crear("Tecnico cancelacion", email, "123456", "tecnico");
        Usuario tec = auth.login(email, "123456");
        int id = servicio.crearTicket("Aviso cancelar " + System.nanoTime(), "", "baja", primeraCategoria(), cliente());
        servicio.tomarTicket(servicio.buscarPorId(id), tec);

        servicio.cancelar(servicio.buscarPorId(id), cliente());

        assertEquals("Cliente de prueba ha cancelado el ticket", new NotificacionServicio().listar(tec).get(0).getTexto());
    }

    @Test
    void noSePuedeCancelarUnTicketResueltoCerradoNiAjeno() {
        Ticket resuelto = ticketResuelto();
        assertFalse(servicio.puedeCancelar(resuelto, cliente()));
        assertThrows(ValidacionException.class, () -> servicio.cancelar(resuelto, cliente()));

        int id = servicio.crearTicket("Ajeno " + System.nanoTime(), "", "baja", primeraCategoria(), cliente());
        String email = emailUnico();
        new UsuarioServicio().crear("Otro cliente", email, "123456", "cliente");
        Usuario otro = auth.login(email, "123456");
        assertThrows(ValidacionException.class, () -> servicio.cancelar(servicio.buscarPorId(id), otro));

        servicio.cancelar(servicio.buscarPorId(id), cliente());
        assertThrows(ValidacionException.class, () -> servicio.cancelar(servicio.buscarPorId(id), cliente()));
    }

    @Test
    void unTicketCanceladoNoFalseaLasMetricas() {
        TicketServicio.ResumenSla slaAntes = servicio.resumenSla();
        TicketServicio.ResumenCalidad calidadAntes = servicio.resumenCalidad();

        int id = servicio.crearTicket("No cuenta " + System.nanoTime(), "", "alta", primeraCategoria(), cliente());
        servicio.cancelar(servicio.buscarPorId(id), cliente());

        TicketServicio.ResumenSla slaDespues = servicio.resumenSla();
        assertEquals(slaAntes.resueltos(), slaDespues.resueltos());
        assertEquals(slaAntes.resueltosEnPlazo(), slaDespues.resueltosEnPlazo());
        assertEquals(slaAntes.vencidos(), slaDespues.vencidos());
        assertEquals(calidadAntes.minutosMedios(), servicio.resumenCalidad().minutosMedios());
        // y ya no pide atencion
        assertFalse(servicio.requierenAtencion().stream().anyMatch(t -> t.getId() == id));
    }
}
