package com.itdence.servicio;

import com.itdence.dao.ComentarioDAO;
import com.itdence.dao.TicketDAO;
import com.itdence.modelo.Comentario;
import com.itdence.modelo.Ticket;
import com.itdence.modelo.Usuario;

import java.util.ArrayList;
import java.util.List;

public class ComentarioServicio {

    private static final int MAX_TEXTO = 1000;

    private final ComentarioDAO comentarioDAO = new ComentarioDAO();
    private final TicketDAO ticketDAO = new TicketDAO();
    private final NotificacionServicio avisos = new NotificacionServicio();

    //filtro de las notas internas
    public List<Comentario> listarPorTicket(Ticket ticket, Usuario usuario) {
        List<Comentario> todos = comentarioDAO.listarPorTicket(ticket.getId());
        if (puedeVerNotasInternas(usuario)) {
            return todos;
        }
        List<Comentario> visibles = new ArrayList<>();
        for (Comentario c : todos) {
            if (!c.isInterno()) {
                visibles.add(c);
            }
        }
        return visibles;
    }

    private void avisarDelComentario(Ticket ticket, Usuario autor) {
        Ticket actual = ticketDAO.buscarPorId(ticket.getId());
        if (actual == null) {
            return;
        }
        if (autor.getId() == actual.getCreadorId()) {
            avisos.avisar(actual.getTecnicoAsignadoId(), autor, actual.getId(), autor.getNombre() + " ha comentado en el ticket");
        } else {
            avisos.avisar(actual.getCreadorId(), autor, actual.getId(), autor.getNombre() + " ha respondido en tu ticket");
        }
    }

    public boolean puedeVerNotasInternas(Usuario usuario) {
        return !"cliente".equals(usuario.getRol());
    }

    public boolean puedeComentar(Ticket ticket, Usuario usuario) {
        if ("cerrado".equals(ticket.getEstado())) {
            return false;
        }
        if ("cliente".equals(usuario.getRol())) {
            return ticket.getCreadorId() == usuario.getId();
        }
        return true;
    }

    public void comentar(Ticket ticket, Usuario usuario, String texto) {
        comentar(ticket, usuario, texto, false);
    }

    public void comentar(Ticket ticket, Usuario usuario, String texto, boolean interno) {
        comentar(ticket, usuario, texto, interno, true);
    }

    void comentar(Ticket ticket, Usuario usuario, String texto, boolean interno, boolean avisar) {
        if ("cerrado".equals(ticket.getEstado())) {
            throw new ValidacionException("El ticket está cerrado y ya no admite comentarios");
        }
        if (!puedeComentar(ticket, usuario)) {
            throw new ValidacionException("No puedes comentar en este ticket");
        }
        if (interno && !puedeVerNotasInternas(usuario)) {
            throw new ValidacionException("Los clientes no pueden escribir notas internas");
        }
        if (texto == null || texto.isBlank()) {
            throw new ValidacionException("Escribe algo antes de enviar el comentario");
        }
        if (texto.length() > MAX_TEXTO) {
            throw new ValidacionException("El comentario no puede tener más de " + MAX_TEXTO + " caracteres");
        }

        Comentario comentario = new Comentario();
        comentario.setTicketId(ticket.getId());
        comentario.setUsuarioId(usuario.getId());
        comentario.setTexto(texto.trim());
        comentario.setInterno(interno);
        comentarioDAO.crear(comentario);
        if (!interno) {
            ticketDAO.actualizarFecha(ticket.getId());
            if (avisar) {
                avisarDelComentario(ticket, usuario);
            }
        }
    }
}
