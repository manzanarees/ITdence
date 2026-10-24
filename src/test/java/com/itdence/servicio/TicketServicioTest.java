package com.itdence.servicio;

import com.itdence.dao.TicketDAO;
import com.itdence.modelo.Actividad;
import com.itdence.modelo.Sla;
import com.itdence.modelo.Ticket;
import com.itdence.modelo.Usuario;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TicketServicioTest extends BaseTest {

    private final TicketServicio servicio = new TicketServicio();

    private Ticket crearTicketDePrueba() {
        int id = servicio.crearTicket("Prueba " + System.nanoTime(), "desc", "media", primeraCategoria(), cliente());
        return servicio.buscarPorId(id);
    }

    @Test
    void crearTicketLoGuardaAbiertoYSinTecnico() {
        Ticket t = crearTicketDePrueba();
        assertNotNull(t);
        assertEquals("abierto", t.getEstado());
        assertNull(t.getTecnicoAsignadoId());
        assertEquals(cliente().getId(), t.getCreadorId());
    }

    @Test
    void elTicketCreadoApareceEnMisTickets() {
        Ticket t = crearTicketDePrueba();
        boolean esta = servicio.listarMisTickets(cliente()).stream().anyMatch(x -> x.getId() == t.getId());
        assertTrue(esta);
    }

    @Test
    void noSePuedeCrearSinTitulo() {
        ValidacionException e = assertThrows(ValidacionException.class,
                () -> servicio.crearTicket("   ", "desc", "media", primeraCategoria(), cliente()));
        assertEquals("El título es obligatorio", e.getMessage());
    }

    @Test
    void noSePuedeCrearSinCategoriaNiPrioridad() {
        assertThrows(ValidacionException.class, () -> servicio.crearTicket("Algo", "", "media", null, cliente()));
        assertThrows(ValidacionException.class, () -> servicio.crearTicket("Algo", "", null, primeraCategoria(), cliente()));
    }

    @Test
    void noSePuedeCrearConTituloDemasiadoLargo() {
        String largo = "x".repeat(101);
        assertThrows(ValidacionException.class, () -> servicio.crearTicket(largo, "", "baja", primeraCategoria(), cliente()));
    }

    @Test
    void tomarTicketLoAsignaYLoPasaAEnProgreso() {
        Ticket t = crearTicketDePrueba();
        servicio.tomarTicket(t, tecnico());

        Ticket actualizado = servicio.buscarPorId(t.getId());
        assertEquals(tecnico().getId(), actualizado.getTecnicoAsignadoId());
        assertEquals("en progreso", actualizado.getEstado());
        assertTrue(servicio.listarAsignadosA(tecnico()).stream().anyMatch(x -> x.getId() == t.getId()));
        assertFalse(servicio.listarSinAsignar().stream().anyMatch(x -> x.getId() == t.getId()));
    }

    @Test
    void unTicketNoSePuedeTomarDosVeces() {
        Ticket t = crearTicketDePrueba();
        servicio.tomarTicket(t, tecnico());
        assertThrows(ValidacionException.class, () -> servicio.tomarTicket(t, tecnico()));
    }

    @Test
    void unClienteNoPuedeTomarTickets() {
        Ticket t = crearTicketDePrueba();
        assertThrows(ValidacionException.class, () -> servicio.tomarTicket(t, cliente()));
    }

    @Test
    void elClienteNoPuedeCambiarElEstado() {
        Ticket t = crearTicketDePrueba();
        assertFalse(servicio.puedeCambiarEstado(t, cliente()));
        assertThrows(ValidacionException.class, () -> servicio.cambiarEstado(t, "cerrado", cliente()));
    }

    @Test
    void elTecnicoSoloCambiaElEstadoDeSusTickets() {
        Ticket t = crearTicketDePrueba();
        Usuario tecnico = tecnico();
        assertFalse(servicio.puedeCambiarEstado(t, tecnico));

        servicio.tomarTicket(t, tecnico);
        Ticket asignado = servicio.buscarPorId(t.getId());
        assertTrue(servicio.puedeCambiarEstado(asignado, tecnico));

        servicio.cambiarEstado(asignado, "resuelto", tecnico);
        assertEquals("resuelto", servicio.buscarPorId(t.getId()).getEstado());
    }

    @Test
    void elAdminPuedeCambiarCualquierTicket() {
        Ticket t = crearTicketDePrueba();
        servicio.cambiarEstado(t, "cerrado", admin());
        assertEquals("cerrado", servicio.buscarPorId(t.getId()).getEstado());
    }

    @Test
    void noSeAceptaUnEstadoQueNoExiste() {
        Ticket t = crearTicketDePrueba();
        // con guion bajo no vale, en la BD es 'en progreso' con espacio
        assertThrows(ValidacionException.class, () -> servicio.cambiarEstado(t, "en_progreso", admin()));
    }

    @Test
    void elHistorialRegistraCreacionAsignacionYCambiosDeEstado() {
        Ticket t = crearTicketDePrueba();
        servicio.tomarTicket(t, tecnico());
        servicio.cambiarEstado(servicio.buscarPorId(t.getId()), "resuelto", tecnico());

        List<Actividad> historial = servicio.listarActividad(t);
        List<String> resumen = historial.stream().map(a -> a.getTipo() + ":" + a.getDetalle()).toList();
        assertEquals(List.of(
                "creado:creó el ticket",
                "asignado:tomó el ticket",
                "estado:abierto → en progreso",
                "estado:en progreso → resuelto"), resumen);
        assertEquals("Cliente de prueba", historial.get(0).getUsuarioNombre());
        assertEquals("Técnico de prueba", historial.get(3).getUsuarioNombre());
    }

    @Test
    void siSeRechazaLaAccionNoQuedaNadaEnElHistorial() {
        Ticket t = crearTicketDePrueba();
        assertThrows(ValidacionException.class, () -> servicio.cambiarEstado(t, "cerrado", cliente()));
        assertEquals(1, servicio.listarActividad(t).size());
    }

    @Test
    void unAdminConHistorialNoSePuedeBorrarConUnMensajeClaro() {
        String email = emailUnico();
        new UsuarioServicio().crear("Admin secundario", email, "123456", "admin");
        Usuario otroAdmin = auth.login(email, "123456");
        Ticket t = crearTicketDePrueba();
        servicio.cambiarEstado(t, "en progreso", otroAdmin);

        ValidacionException e = assertThrows(ValidacionException.class,
                () -> new UsuarioServicio().eliminar(otroAdmin, admin()));
        assertTrue(e.getMessage().contains("No se puede eliminar"));
    }

    @Test
    void alArrastrarAlaMismaColumnaNoPasaNada() {
        Ticket t = crearTicketDePrueba();
        servicio.moverEstado(t, "abierto", tecnico());
        Ticket despues = servicio.buscarPorId(t.getId());
        assertEquals("abierto", despues.getEstado());
        assertNull(despues.getTecnicoAsignadoId());
        assertEquals(1, servicio.listarActividad(t).size());
    }

    @Test
    void unTecnicoQueArrastraUnTicketSinAsignarSeLoQuedaYCambiaElEstado() {
        Ticket t = crearTicketDePrueba();
        servicio.moverEstado(t, "resuelto", tecnico());

        Ticket despues = servicio.buscarPorId(t.getId());
        assertEquals("resuelto", despues.getEstado());
        assertEquals(tecnico().getId(), despues.getTecnicoAsignadoId());
        assertNotNull(despues.getFechaResolucion());
    }

    @Test
    void siElTecnicoLoArrastraAEnProgresoBastaConTomarlo() {
        Ticket t = crearTicketDePrueba();
        servicio.moverEstado(t, "en progreso", tecnico());
        assertEquals("en progreso", servicio.buscarPorId(t.getId()).getEstado());
        // creado + asignado + abierto->en progreso, sin un segundo cambio de estado
        assertEquals(3, servicio.listarActividad(t).size());
    }

    @Test
    void elTableroRespetaLosPermisos() {
        Ticket t = crearTicketDePrueba();
        // un cliente no puede
        assertThrows(ValidacionException.class, () -> servicio.moverEstado(t, "cerrado", cliente()));

        // un tecnico no puede mover el ticket de otro
        String email = emailUnico();
        new UsuarioServicio().crear("Otro tecnico", email, "123456", "tecnico");
        Usuario otro = auth.login(email, "123456");
        servicio.tomarTicket(t, otro);
        assertThrows(ValidacionException.class, () -> servicio.moverEstado(t, "resuelto", tecnico()));
        assertEquals("en progreso", servicio.buscarPorId(t.getId()).getEstado());

        // el admin si
        servicio.moverEstado(t, "cerrado", admin());
        assertEquals("cerrado", servicio.buscarPorId(t.getId()).getEstado());
    }

    @Test
    void elAdminAsignaYReasignaTecnicos() {
        Ticket t = crearTicketDePrueba();
        servicio.asignarA(t, tecnico(), admin());

        Ticket asignado = servicio.buscarPorId(t.getId());
        assertEquals(tecnico().getId(), asignado.getTecnicoAsignadoId());
        assertEquals("en progreso", asignado.getEstado());

        String email = emailUnico();
        new UsuarioServicio().crear("Segundo tecnico", email, "123456", "tecnico");
        Usuario otro = auth.login(email, "123456");
        servicio.asignarA(asignado, otro, admin());
        assertEquals(otro.getId(), servicio.buscarPorId(t.getId()).getTecnicoAsignadoId());

        List<String> historial = servicio.listarActividad(t).stream().map(Actividad::getDetalle).toList();
        assertTrue(historial.contains("asignó el ticket a Técnico de prueba"));
        assertTrue(historial.contains("reasignó el ticket de Técnico de prueba a Segundo tecnico"));
    }

    @Test
    void soloElAdminAsignaYSoloATecnicos() {
        Ticket t = crearTicketDePrueba();
        assertThrows(ValidacionException.class, () -> servicio.asignarA(t, tecnico(), tecnico()));
        assertThrows(ValidacionException.class, () -> servicio.asignarA(t, tecnico(), cliente()));
        assertThrows(ValidacionException.class, () -> servicio.asignarA(t, cliente(), admin()));
        assertNull(servicio.buscarPorId(t.getId()).getTecnicoAsignadoId());
    }

    @Test
    void noSePuedeAsignarUnTicketCerradoNiAlMismoTecnicoDosVeces() {
        Ticket t = crearTicketDePrueba();
        servicio.asignarA(t, tecnico(), admin());
        assertThrows(ValidacionException.class, () -> servicio.asignarA(t, tecnico(), admin()));

        servicio.cambiarEstado(servicio.buscarPorId(t.getId()), "cerrado", admin());
        String email = emailUnico();
        new UsuarioServicio().crear("Tecnico tardio", email, "123456", "tecnico");
        assertThrows(ValidacionException.class, () -> servicio.asignarA(t, auth.login(email, "123456"), admin()));
    }

    @Test
    void elBuscadorEncuentraPorNumeroTituloYPersona() {
        String marca = "zxq" + System.nanoTime();
        int id = servicio.crearTicket("Fallo " + marca, "la descripcion habla de otra cosa", "baja", primeraCategoria(), cliente());

        // por titulo (da igual mayusculas) y por numero con o sin almohadilla
        assertEquals(id, servicio.buscar(marca.toUpperCase(), admin(), 10).get(0).getId());
        assertEquals(id, servicio.buscar("#" + id, admin(), 10).get(0).getId());
        assertEquals(id, servicio.buscar(String.valueOf(id), admin(), 10).get(0).getId());
        // por descripcion
        assertTrue(servicio.buscar("habla de otra cosa", admin(), 10).stream().anyMatch(t -> t.getId() == id));
        // por quien lo creo y por categoria
        assertTrue(servicio.buscar("cliente de prueba", tecnico(), 100).stream().anyMatch(t -> t.getId() == id));
        assertTrue(servicio.buscar(primeraCategoria().getNombre(), tecnico(), 100).stream().anyMatch(t -> t.getId() == id));
        assertTrue(servicio.buscar("palabra que no existe " + marca, admin(), 10).isEmpty());
    }

    @Test
    void elBuscadorPoneAntesLoQueCoincidePorNumeroYLuegoPorTitulo() {
        int id = servicio.crearTicket("Numero", "", "baja", primeraCategoria(), cliente());
        // un ticket cuyo titulo contiene ese mismo numero: debe salir despues del que tiene ese id
        servicio.crearTicket("Fallo en el equipo " + id, "", "baja", primeraCategoria(), cliente());
        List<Ticket> resultado = servicio.buscar(String.valueOf(id), admin(), 10);
        assertEquals(id, resultado.get(0).getId());
        assertTrue(resultado.size() >= 2);
    }

    @Test
    void unClienteSoloBuscaEntreSusTickets() {
        String marca = "priv" + System.nanoTime();
        int mio = servicio.crearTicket("Mio " + marca, "", "baja", primeraCategoria(), cliente());

        String email = emailUnico();
        new UsuarioServicio().crear("Otro cliente", email, "123456", "cliente");
        Usuario otro = auth.login(email, "123456");
        servicio.crearTicket("Ajeno " + marca, "", "baja", primeraCategoria(), otro);

        List<Ticket> suyos = servicio.buscar(marca, cliente(), 10);
        assertEquals(1, suyos.size());
        assertEquals(mio, suyos.get(0).getId());
        assertEquals(2, servicio.buscar(marca, admin(), 10).size());
    }

    @Test
    void sinTextoElBuscadorDevuelveLosMasRecientesRespetandoElLimite() {
        int ultimo = servicio.crearTicket("El mas reciente " + System.nanoTime(), "", "baja", primeraCategoria(), cliente());
        List<Ticket> recientes = servicio.buscar("   ", admin(), 3);
        assertEquals(3, recientes.size());
        assertEquals(ultimo, recientes.get(0).getId());
        assertEquals(3, servicio.buscar(null, admin(), 3).size());
    }

    @Test
    void laActividadPorDiaTieneUnaEntradaPorDiaYCuentaCreadosYResueltos() {
        List<TicketServicio.ActividadDia> antes = servicio.actividadPorDia(14);
        assertEquals(14, antes.size());
        assertEquals(java.time.LocalDate.now(), antes.get(13).dia());
        assertEquals(java.time.LocalDate.now().minusDays(13), antes.get(0).dia());

        Ticket t = crearTicketDePrueba();
        servicio.cambiarEstado(t, "resuelto", admin());
        List<TicketServicio.ActividadDia> despues = servicio.actividadPorDia(14);
        assertEquals(antes.get(13).creados() + 1, despues.get(13).creados());
        assertEquals(antes.get(13).resueltos() + 1, despues.get(13).resueltos());
    }

    @Test
    void laActividadPorDiaColocaCadaTicketEnSuDiaYIgnoraLosMuyAntiguos() {
        List<TicketServicio.ActividadDia> antes = servicio.actividadPorDia(14);
        int enTresDias = antes.get(10).creados();
        int fuera = antes.stream().mapToInt(TicketServicio.ActividadDia::creados).sum();

        Ticket reciente = crearTicketDePrueba();
        new TicketDAO().ponerAntiguedad(reciente.getId(), 72);
        Ticket antiguo = crearTicketDePrueba();
        new TicketDAO().ponerAntiguedad(antiguo.getId(), 24 * 30);

        List<TicketServicio.ActividadDia> despues = servicio.actividadPorDia(14);
        assertEquals(enTresDias + 1, despues.get(10).creados());
        // el de hace 30 dias no entra en la ventana de 14
        assertEquals(fuera + 1, despues.stream().mapToInt(TicketServicio.ActividadDia::creados).sum());
    }

    @Test
    void requierenAtencionLosVencidosLosQueVanADescontarseYLosSinTecnico() {
        // sin tecnico y recien creado: pide atencion
        Ticket sinTecnico = crearTicketDePrueba();
        assertTrue(servicio.requierenAtencion().stream().anyMatch(t -> t.getId() == sinTecnico.getId()));

        // con tecnico y de sobra de plazo: no
        Ticket tranquilo = crearTicketDePrueba();
        servicio.tomarTicket(tranquilo, tecnico());
        assertFalse(servicio.requierenAtencion().stream().anyMatch(t -> t.getId() == tranquilo.getId()));

        // con tecnico pero vencido: si
        int idAlta = servicio.crearTicket("Alta vencida", "", "alta", primeraCategoria(), cliente());
        servicio.tomarTicket(servicio.buscarPorId(idAlta), tecnico());
        new TicketDAO().ponerAntiguedad(idAlta, 10);
        assertTrue(servicio.requierenAtencion().stream().anyMatch(t -> t.getId() == idAlta));

        // y al resolverlo deja de pedir atencion
        servicio.cambiarEstado(servicio.buscarPorId(idAlta), "resuelto", tecnico());
        assertFalse(servicio.requierenAtencion().stream().anyMatch(t -> t.getId() == idAlta));
    }

    @Test
    void unTicketNuevoTieneSuLimiteDeSlaSegunLaPrioridad() {
        for (String prioridad : TicketServicio.PRIORIDADES) {
            int id = servicio.crearTicket("SLA " + prioridad, "", prioridad, primeraCategoria(), cliente());
            Ticket t = servicio.buscarPorId(id);
            long horas = Duration.between(Sla.parsear(t.getFechaCreacion()), Sla.parsear(t.getFechaLimite())).toHours();
            assertEquals(Sla.horas(prioridad), horas, "prioridad " + prioridad);
            assertNull(t.getFechaResolucion());
        }
    }

    @Test
    void laFechaDeResolucionSeGuardaAlResolverYSeBorraAlReabrir() {
        Ticket t = crearTicketDePrueba();
        servicio.cambiarEstado(t, "resuelto", admin());
        String resolucion = servicio.buscarPorId(t.getId()).getFechaResolucion();
        assertNotNull(resolucion);

        // de resuelto a cerrado se conserva la misma fecha
        servicio.cambiarEstado(servicio.buscarPorId(t.getId()), "cerrado", admin());
        assertEquals(resolucion, servicio.buscarPorId(t.getId()).getFechaResolucion());

        // si se reabre vuelve a contar como pendiente
        servicio.cambiarEstado(servicio.buscarPorId(t.getId()), "en progreso", admin());
        assertNull(servicio.buscarPorId(t.getId()).getFechaResolucion());
    }

    @Test
    void elResumenDeSlaCuentaLosVencidosYLosCumplidos() {
        TicketServicio.ResumenSla antes = servicio.resumenSla();

        // alta creado hace 10 h con limite de 4 h: vencido
        int idAlta = servicio.crearTicket("Vencido", "", "alta", primeraCategoria(), cliente());
        new TicketDAO().ponerAntiguedad(idAlta, 10);
        // otro resuelto a tiempo
        Ticket aTiempo = crearTicketDePrueba();
        servicio.cambiarEstado(aTiempo, "resuelto", admin());

        TicketServicio.ResumenSla despues = servicio.resumenSla();
        assertEquals(antes.vencidos() + 1, despues.vencidos());
        assertEquals(antes.resueltos() + 1, despues.resueltos());
        assertEquals(antes.resueltosEnPlazo() + 1, despues.resueltosEnPlazo());
    }

    @Test
    void elPorcentajeDeCumplimientoSeRedondeaYSinDatosDaMenosUno() {
        assertEquals(75, new TicketServicio.ResumenSla(0, 0, 4, 3).porcentajeCumplido());
        assertEquals(67, new TicketServicio.ResumenSla(0, 0, 3, 2).porcentajeCumplido());
        assertEquals(-1, new TicketServicio.ResumenSla(2, 1, 0, 0).porcentajeCumplido());
    }

    @Test
    void elConteoPorEstadoTieneLosCuatroEstadosYCuadraConElTotal() {
        Map<String, Integer> conteo = servicio.contarPorEstado();
        assertEquals(TicketServicio.ESTADOS, conteo.keySet().stream().toList());
        int suma = conteo.values().stream().mapToInt(Integer::intValue).sum();
        assertEquals(servicio.listarTodos().size(), suma);
    }
}
