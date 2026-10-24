package com.itdence.servicio;

import com.itdence.dao.ActividadDAO;
import com.itdence.dao.TicketDAO;
import com.itdence.dao.UsuarioDAO;
import com.itdence.modelo.Actividad;
import com.itdence.modelo.Sla;
import com.itdence.modelo.Categoria;
import com.itdence.modelo.Ticket;
import com.itdence.modelo.Usuario;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class TicketServicio {

    public static final List<String> ESTADOS = List.of("abierto", "en progreso", "resuelto", "cerrado");
    public static final List<String> PRIORIDADES = List.of("baja", "media", "alta");

    private static final int MAX_TITULO = 100;
    private static final int MAX_DESCRIPCION = 2000;

    private final TicketDAO ticketDAO = new TicketDAO();
    private final ActividadDAO actividadDAO = new ActividadDAO();
    private final ComentarioServicio comentarioServicio = new ComentarioServicio();
    private final NotificacionServicio avisos = new NotificacionServicio();
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    public int crearTicket(String titulo, String descripcion, String prioridad, Categoria categoria, Usuario creador) {
        if (titulo == null || titulo.isBlank()) {
            throw new ValidacionException("El título es obligatorio");
        }
        if (titulo.trim().length() > MAX_TITULO) {
            throw new ValidacionException("El título no puede tener más de " + MAX_TITULO + " caracteres");
        }
        if (descripcion != null && descripcion.length() > MAX_DESCRIPCION) {
            throw new ValidacionException("La descripción no puede tener más de " + MAX_DESCRIPCION + " caracteres");
        }
        if (prioridad == null || !PRIORIDADES.contains(prioridad)) {
            throw new ValidacionException("Elige una prioridad");
        }
        if (categoria == null) {
            throw new ValidacionException("Elige una categoría");
        }

        Ticket ticket = new Ticket();
        ticket.setTitulo(titulo.trim());
        ticket.setDescripcion(descripcion == null ? "" : descripcion.trim());
        ticket.setPrioridad(prioridad);
        ticket.setCategoriaId(categoria.getId());
        ticket.setCreadorId(creador.getId());
        int id = ticketDAO.crear(ticket);
        actividadDAO.crear(id, creador.getId(), "creado", "creó el ticket");
        if ("alta".equals(prioridad)) {
            for (Usuario tecnico : usuarioDAO.listarTodos()) {
                if ("tecnico".equals(tecnico.getRol())) {
                    avisos.avisar(tecnico.getId(), creador, id, "Nuevo ticket urgente: " + ticket.getTitulo());
                }
            }
        }
        return id;
    }

    public List<Ticket> listarMisTickets(Usuario cliente) {
        return ticketDAO.listarPorCreador(cliente.getId());
    }

    public List<Ticket> listarSinAsignar() {
        return ticketDAO.listarSinAsignar();
    }

    public List<Ticket> listarAsignadosA(Usuario tecnico) {
        return ticketDAO.listarPorTecnico(tecnico.getId());
    }

    public List<Ticket> listarTodos() {
        return ticketDAO.listarTodos();
    }

    // buscador global
    public List<Ticket> buscar(String texto, Usuario usuario, int limite) {
        List<Ticket> candidatos = "cliente".equals(usuario.getRol())
                ? ticketDAO.listarPorCreador(usuario.getId())
                : ticketDAO.listarTodos();
        String buscado = texto == null ? "" : texto.trim().toLowerCase();
        if (buscado.isEmpty()) {
            return candidatos.stream().limit(limite).toList();
        }
        String sinAlmohadilla = buscado.startsWith("#") ? buscado.substring(1) : buscado;

        java.util.function.ToIntFunction<Ticket> grupo = ticket -> {
            if (sinAlmohadilla.matches("[0-9]+") && ticket.getId() == Integer.parseInt(sinAlmohadilla)) {
                return 0;
            }
            if (ticket.getTitulo().toLowerCase().contains(buscado)) {
                return 1;
            }
            String resto = (ticket.getDescripcion() == null ? "" : ticket.getDescripcion()) + " "
                    + (ticket.getCategoriaNombre() == null ? "" : ticket.getCategoriaNombre()) + " "
                    + (ticket.getCreadorNombre() == null ? "" : ticket.getCreadorNombre()) + " "
                    + (ticket.getTecnicoNombre() == null ? "" : ticket.getTecnicoNombre());
            return resto.toLowerCase().contains(buscado) ? 2 : -1;
        };
        return candidatos.stream()
                .filter(ticket -> grupo.applyAsInt(ticket) >= 0)
                .sorted(java.util.Comparator.comparingInt(grupo))
                .limit(limite)
                .toList();
    }

    public Ticket buscarPorId(int id) {
        return ticketDAO.buscarPorId(id);
    }

    public void tomarTicket(Ticket ticket, Usuario tecnico) {
        if (!"tecnico".equals(tecnico.getRol())) {
            throw new ValidacionException("Solo los técnicos pueden tomar tickets");
        }
        Ticket actual = ticketDAO.buscarPorId(ticket.getId());
        boolean asignado = ticketDAO.asignarTecnico(ticket.getId(), tecnico.getId());
        if (!asignado) {
            throw new ValidacionException("Otro técnico ha tomado este ticket antes que tú");
        }
        actividadDAO.crear(ticket.getId(), tecnico.getId(), "asignado", "tomó el ticket");
        if (actual != null && "abierto".equals(actual.getEstado())) {
            actividadDAO.crear(ticket.getId(), tecnico.getId(), "estado", "abierto → en progreso");
        }
        if (actual != null) {
            avisos.avisar(actual.getCreadorId(), tecnico, ticket.getId(), tecnico.getNombre() + " ha tomado tu ticket");
        }
    }
    public boolean puedeCambiarEstado(Ticket ticket, Usuario usuario) {
        if ("admin".equals(usuario.getRol())) {
            return true;
        }
        return "tecnico".equals(usuario.getRol())
                && ticket.getTecnicoAsignadoId() != null
                && ticket.getTecnicoAsignadoId() == usuario.getId();
    }

    public void cambiarEstado(Ticket ticket, String nuevoEstado, Usuario usuario) {
        if (!puedeCambiarEstado(ticket, usuario)) {
            throw new ValidacionException("No tienes permiso para cambiar el estado de este ticket");
        }
        if (nuevoEstado == null || !ESTADOS.contains(nuevoEstado)) {
            throw new ValidacionException("Estado no válido");
        }
        if (nuevoEstado.equals(ticket.getEstado())) {
            throw new ValidacionException("El ticket ya está en ese estado");
        }
        ticketDAO.cambiarEstado(ticket.getId(), nuevoEstado);
        actividadDAO.crear(ticket.getId(), usuario.getId(), "estado", ticket.getEstado() + " → " + nuevoEstado);
        Ticket actualizado = ticketDAO.buscarPorId(ticket.getId());
        if (actualizado != null) {
            avisos.avisar(actualizado.getCreadorId(), usuario, ticket.getId(), textoDelAviso(nuevoEstado));
        }
    }

    private String textoDelAviso(String nuevoEstado) {
        switch (nuevoEstado) {
            case "resuelto": return "Tu ticket está resuelto: confirma si se ha solucionado o reábrelo";
            case "cerrado": return "Tu ticket se ha cerrado";
            case "abierto": return "Tu ticket ha vuelto a abrirse";
            default: return "Tu ticket está en progreso";
        }
    }

    public void asignarA(Ticket ticket, Usuario tecnico, Usuario actor) {
        if (!"admin".equals(actor.getRol())) {
            throw new ValidacionException("Solo el administrador puede asignar tickets a otro técnico");
        }
        if (tecnico == null || !"tecnico".equals(tecnico.getRol())) {
            throw new ValidacionException("Elige un técnico");
        }
        Ticket actual = ticketDAO.buscarPorId(ticket.getId());
        if (actual == null) {
            throw new ValidacionException("El ticket ya no existe");
        }
        if ("cerrado".equals(actual.getEstado())) {
            throw new ValidacionException("El ticket está cerrado y ya no se puede asignar");
        }
        if (actual.getTecnicoAsignadoId() != null && actual.getTecnicoAsignadoId() == tecnico.getId()) {
            throw new ValidacionException(tecnico.getNombre() + " ya tiene asignado este ticket");
        }

        ticketDAO.asignarForzado(ticket.getId(), tecnico.getId());
        String detalle = actual.getTecnicoNombre() == null
                ? "asignó el ticket a " + tecnico.getNombre()
                : "reasignó el ticket de " + actual.getTecnicoNombre() + " a " + tecnico.getNombre();
        actividadDAO.crear(ticket.getId(), actor.getId(), "asignado", detalle);
        if ("abierto".equals(actual.getEstado())) {
            actividadDAO.crear(ticket.getId(), actor.getId(), "estado", "abierto → en progreso");
        }
        avisos.avisar(tecnico.getId(), actor, ticket.getId(), "Te han asignado este ticket");
        avisos.avisar(actual.getCreadorId(), actor, ticket.getId(), tecnico.getNombre() + " se encarga de tu ticket");
    }

    public void moverEstado(Ticket ticket, String nuevoEstado, Usuario usuario) {
        // se vuelve a leer: el objeto de la tarjeta puede estar desfasado
        Ticket actual = ticketDAO.buscarPorId(ticket.getId());
        if (actual == null) {
            throw new ValidacionException("El ticket ya no existe");
        }
        if (nuevoEstado == null || nuevoEstado.equals(actual.getEstado())) {
            return;
        }
        if ("tecnico".equals(usuario.getRol()) && actual.getTecnicoAsignadoId() == null) {
            tomarTicket(actual, usuario);
            actual = ticketDAO.buscarPorId(ticket.getId());
            if (nuevoEstado.equals(actual.getEstado())) {
                return;
            }
        }
        cambiarEstado(actual, nuevoEstado, usuario);
    }

    public boolean puedeResponderResolucion(Ticket ticket, Usuario usuario) {
        return "cliente".equals(usuario.getRol())
                && ticket.getCreadorId() == usuario.getId()
                && "resuelto".equals(ticket.getEstado());
    }

    public void confirmarResolucion(Ticket ticket, Usuario cliente, int valoracion) {
        Ticket actual = ticketParaResponder(ticket, cliente);
        if (valoracion < 1 || valoracion > 5) {
            throw new ValidacionException("Valora la atención de 1 a 5 estrellas para cerrar el ticket");
        }
        ticketDAO.cambiarEstado(actual.getId(), "cerrado");
        ticketDAO.guardarValoracion(actual.getId(), valoracion);
        actividadDAO.crear(actual.getId(), cliente.getId(), "estado", "resuelto → cerrado");
        actividadDAO.crear(actual.getId(), cliente.getId(), "valoracion", "valoró la atención con " + valoracion + "/5");
        avisos.avisar(actual.getTecnicoAsignadoId(), cliente, actual.getId(),
                cliente.getNombre() + " ha cerrado el ticket y lo ha valorado con " + valoracion + "/5");
    }

    public void reabrir(Ticket ticket, Usuario cliente, String motivo) {
        Ticket actual = ticketParaResponder(ticket, cliente);
        if (motivo == null || motivo.isBlank()) {
            throw new ValidacionException("Cuéntale al técnico por qué sigue sin resolverse");
        }
        if (motivo.trim().length() > 900) {
            throw new ValidacionException("El motivo no puede tener más de 900 caracteres");
        }
        String nuevoEstado = actual.getTecnicoAsignadoId() == null ? "abierto" : "en progreso";
        comentarioServicio.comentar(actual, cliente, "Reabro el ticket: " + motivo.trim(), false, false);
        ticketDAO.reabrir(actual.getId(), nuevoEstado);
        actividadDAO.crear(actual.getId(), cliente.getId(), "reabierto", "reabrió el ticket");
        actividadDAO.crear(actual.getId(), cliente.getId(), "estado", "resuelto → " + nuevoEstado);
        avisos.avisar(actual.getTecnicoAsignadoId(), cliente, actual.getId(), cliente.getNombre() + " ha reabierto el ticket");
    }

    private Ticket ticketParaResponder(Ticket ticket, Usuario cliente) {
        Ticket actual = ticketDAO.buscarPorId(ticket.getId());
        if (actual == null) {
            throw new ValidacionException("El ticket ya no existe");
        }
        if (!puedeResponderResolucion(actual, cliente)) {
            throw new ValidacionException("Solo puedes cerrar o reabrir tus propios tickets cuando están resueltos");
        }
        return actual;
    }

    //Cancelar

    public boolean puedeCancelar(Ticket ticket, Usuario usuario) {
        return "cliente".equals(usuario.getRol())
                && ticket.getCreadorId() == usuario.getId()
                && ("abierto".equals(ticket.getEstado()) || "en progreso".equals(ticket.getEstado()));
    }

    public void cancelar(Ticket ticket, Usuario cliente) {
        Ticket actual = ticketDAO.buscarPorId(ticket.getId());
        if (actual == null) {
            throw new ValidacionException("El ticket ya no existe");
        }
        if (!puedeCancelar(actual, cliente)) {
            throw new ValidacionException("Solo puedes cancelar tus propios tickets mientras no estén resueltos");
        }
        ticketDAO.cancelar(actual.getId());
        actividadDAO.crear(actual.getId(), cliente.getId(), "cancelado", "canceló el ticket");
        avisos.avisar(actual.getTecnicoAsignadoId(), cliente, actual.getId(), cliente.getNombre() + " ha cancelado el ticket");
    }

    //calidad del servicio

    public record ResumenCalidad(double mediaValoracion, int valoraciones, int reaperturas, long minutosMedios) {
    }

    public ResumenCalidad resumenCalidad() {
        int suma = 0;
        int valoraciones = 0;
        int reaperturas = 0;
        long minutos = 0;
        int resueltos = 0;
        for (Ticket t : ticketDAO.listarTodos()) {
            if (t.getValoracion() != null) {
                suma += t.getValoracion();
                valoraciones++;
            }
            reaperturas += t.getReaperturas();
            LocalDateTime creacion = Sla.parsear(t.getFechaCreacion());
            LocalDateTime resolucion = Sla.parsear(t.getFechaResolucion());
            if (creacion != null && resolucion != null) {
                minutos += Math.max(0, Duration.between(creacion, resolucion).toMinutes());
                resueltos++;
            }
        }
        return new ResumenCalidad(valoraciones == 0 ? 0 : (double) suma / valoraciones, valoraciones, reaperturas,
                resueltos == 0 ? -1 : minutos / resueltos);
    }

    public List<Actividad> listarActividad(Ticket ticket) {
        return actividadDAO.listarPorTicket(ticket.getId());
    }

    public Map<String, Integer> contarPorEstado() {
        return ticketDAO.contarPorEstado();
    }

    // Numeros del SLA para el panel del admin
    public record ResumenSla(int vencidos, int enRiesgo, int resueltos, int resueltosEnPlazo) {
        public int porcentajeCumplido() {
            return resueltos == 0 ? -1 : Math.round(100f * resueltosEnPlazo / resueltos);
        }
    }

    public ResumenSla resumenSla() {
        return resumenSla(LocalDateTime.now());
    }

    public ResumenSla resumenSla(LocalDateTime ahora) {
        int vencidos = 0;
        int enRiesgo = 0;
        int resueltos = 0;
        int enPlazo = 0;
        for (Ticket t : ticketDAO.listarTodos()) {
            switch (Sla.situacion(t, ahora)) {
                case VENCIDO -> vencidos++;
                case POR_VENCER -> enRiesgo++;
                case CUMPLIDO -> { resueltos++; enPlazo++; }
                case INCUMPLIDO -> resueltos++;
                default -> { }
            }
        }
        return new ResumenSla(vencidos, enRiesgo, resueltos, enPlazo);
    }

    //Datos para las graficas del resumen

    public record ActividadDia(LocalDate dia, int creados, int resueltos) {
    }

    public List<ActividadDia> actividadPorDia(int dias) {
        return actividadPorDia(dias, LocalDate.now());
    }

    public List<ActividadDia> actividadPorDia(int dias, LocalDate hoy) {
        int[] creados = new int[dias];
        int[] resueltos = new int[dias];
        for (Ticket t : ticketDAO.listarTodos()) {
            sumarAlDia(creados, Sla.parsear(t.getFechaCreacion()), hoy, dias);
            sumarAlDia(resueltos, Sla.parsear(t.getFechaResolucion()), hoy, dias);
        }
        List<ActividadDia> resultado = new java.util.ArrayList<>();
        for (int i = 0; i < dias; i++) {
            resultado.add(new ActividadDia(hoy.minusDays(dias - 1 - i), creados[i], resueltos[i]));
        }
        return resultado;
    }

    private void sumarAlDia(int[] contador, LocalDateTime fecha, LocalDate hoy, int dias) {
        if (fecha == null) {
            return;
        }
        long haceDias = java.time.temporal.ChronoUnit.DAYS.between(fecha.toLocalDate(), hoy);
        if (haceDias >= 0 && haceDias < dias) {
            contador[dias - 1 - (int) haceDias]++;
        }
    }

    public List<Ticket> requierenAtencion() {
        LocalDateTime ahora = LocalDateTime.now();
        return ticketDAO.listarTodos().stream()
                .filter(t -> !"resuelto".equals(t.getEstado()) && !"cerrado".equals(t.getEstado()))
                .filter(t -> t.getTecnicoAsignadoId() == null
                        || Sla.situacion(t, ahora) == Sla.Situacion.VENCIDO
                        || Sla.situacion(t, ahora) == Sla.Situacion.POR_VENCER)
                .sorted(java.util.Comparator.comparing(Ticket::getFechaLimite, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
                .toList();
    }

    public Map<String, Integer> contarPorCategoria() {
        return ticketDAO.contarTicketsPorCategoria();
    }
}
