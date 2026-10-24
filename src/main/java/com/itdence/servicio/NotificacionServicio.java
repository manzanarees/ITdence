package com.itdence.servicio;

import com.itdence.dao.NotificacionDAO;
import com.itdence.modelo.Notificacion;
import com.itdence.modelo.Usuario;

import java.util.List;

public class NotificacionServicio {

    private static final int MAXIMO_AVISOS = 40;

    private final NotificacionDAO notificacionDAO = new NotificacionDAO();
    public void avisar(Integer destinoId, Usuario actor, int ticketId, String texto) {
        if (destinoId == null || (actor != null && destinoId == actor.getId())) {
            return;
        }
        notificacionDAO.crear(destinoId, ticketId, texto);
    }

    public List<Notificacion> listar(Usuario usuario) {
        return notificacionDAO.listarPorUsuario(usuario.getId(), MAXIMO_AVISOS);
    }

    public int contarNoLeidas(Usuario usuario) {
        return notificacionDAO.contarNoLeidas(usuario.getId());
    }

    public void marcarLeida(Notificacion notificacion, Usuario usuario) {
        notificacionDAO.marcarLeida(notificacion.getId(), usuario.getId());
    }

    public void marcarTodasLeidas(Usuario usuario) {
        notificacionDAO.marcarTodasLeidas(usuario.getId());
    }

    public void marcarTicketLeido(Usuario usuario, int ticketId) {
        notificacionDAO.marcarTicketLeido(usuario.getId(), ticketId);
    }
}
