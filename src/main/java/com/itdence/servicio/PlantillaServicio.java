package com.itdence.servicio;

import com.itdence.dao.PlantillaDAO;
import com.itdence.modelo.Plantilla;
import com.itdence.modelo.Ticket;
import com.itdence.modelo.Usuario;

import java.util.List;

public class PlantillaServicio {

    private static final int MAX_TITULO = 60;
    private static final int MAX_TEXTO = 1000;

    private final PlantillaDAO plantillaDAO = new PlantillaDAO();

    public List<Plantilla> listarTodas() {
        return plantillaDAO.listarTodas();
    }

    public void crear(Usuario actor, String titulo, String texto) {
        exigirAdmin(actor);
        String tituloLimpio = validar(titulo, texto, -1);
        plantillaDAO.crear(new Plantilla(0, tituloLimpio, texto.trim()));
    }

    public void actualizar(Usuario actor, Plantilla plantilla, String titulo, String texto) {
        exigirAdmin(actor);
        String tituloLimpio = validar(titulo, texto, plantilla.getId());
        plantillaDAO.actualizar(new Plantilla(plantilla.getId(), tituloLimpio, texto.trim()));
    }

    public void eliminar(Usuario actor, Plantilla plantilla) {
        exigirAdmin(actor);
        plantillaDAO.eliminar(plantilla.getId());
    }

    public String aplicar(Plantilla plantilla, Ticket ticket, Usuario autor) {
        return plantilla.getTexto()
                .replace("{cliente}", ticket.getCreadorNombre() == null ? "" : ticket.getCreadorNombre())
                .replace("{tecnico}", autor.getNombre())
                .replace("{ticket}", ticket.getCodigo());
    }

    private void exigirAdmin(Usuario actor) {
        if (!"admin".equals(actor.getRol())) {
            throw new ValidacionException("Solo el administrador puede gestionar las respuestas predefinidas");
        }
    }

    private String validar(String titulo, String texto, int idActual) {
        if (titulo == null || titulo.isBlank()) {
            throw new ValidacionException("El título es obligatorio");
        }
        String limpio = titulo.trim();
        if (limpio.length() > MAX_TITULO) {
            throw new ValidacionException("El título no puede tener más de " + MAX_TITULO + " caracteres");
        }
        if (texto == null || texto.isBlank()) {
            throw new ValidacionException("El texto de la respuesta es obligatorio");
        }
        if (texto.trim().length() > MAX_TEXTO) {
            throw new ValidacionException("El texto no puede tener más de " + MAX_TEXTO + " caracteres");
        }
        for (Plantilla p : plantillaDAO.listarTodas()) {
            if (p.getTitulo().equalsIgnoreCase(limpio) && p.getId() != idActual) {
                throw new ValidacionException("Ya existe una respuesta con ese título");
            }
        }
        return limpio;
    }
}
