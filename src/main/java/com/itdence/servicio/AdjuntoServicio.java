package com.itdence.servicio;

import com.itdence.dao.ActividadDAO;
import com.itdence.dao.AdjuntoDAO;
import com.itdence.dao.TicketDAO;
import com.itdence.modelo.Adjunto;
import com.itdence.modelo.Ticket;
import com.itdence.modelo.Usuario;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;

public class AdjuntoServicio {

    public static final int MAX_BYTES = 5 * 1024 * 1024;
    public static final int MAX_POR_TICKET = 6;
    public static final List<String> EXTENSIONES = List.of("png", "jpg", "jpeg", "gif", "bmp");

    private final AdjuntoDAO adjuntoDAO = new AdjuntoDAO();
    private final TicketDAO ticketDAO = new TicketDAO();
    private final ActividadDAO actividadDAO = new ActividadDAO();
    private final NotificacionServicio avisos = new NotificacionServicio();

    public static File carpeta() {
        return new File(System.getProperty("itdence.adjuntos", "data/adjuntos"));
    }

    public List<Adjunto> listar(Ticket ticket) {
        return adjuntoDAO.listarPorTicket(ticket.getId());
    }

    public File ficheroDe(Adjunto adjunto) {
        return new File(carpeta(), new File(adjunto.getArchivo()).getName());
    }

    public boolean puedeAdjuntar(Ticket ticket, Usuario usuario) {
        if ("cerrado".equals(ticket.getEstado())) {
            return false;
        }
        return !"cliente".equals(usuario.getRol()) || ticket.getCreadorId() == usuario.getId();
    }

    public void adjuntar(Ticket ticket, Usuario usuario, File origen) {
        Ticket actual = ticketDAO.buscarPorId(ticket.getId());
        if (actual == null) {
            throw new ValidacionException("El ticket ya no existe");
        }
        if (!puedeAdjuntar(actual, usuario)) {
            throw new ValidacionException("No puedes adjuntar imágenes a este ticket");
        }
        if (adjuntoDAO.listarPorTicket(actual.getId()).size() >= MAX_POR_TICKET) {
            throw new ValidacionException("Un ticket admite como máximo " + MAX_POR_TICKET + " imágenes");
        }
        if (origen == null || !origen.isFile()) {
            throw new ValidacionException("No se encuentra el archivo");
        }
        String nombre = origen.getName();
        if (!EXTENSIONES.contains(extension(nombre))) {
            throw new ValidacionException("Solo se pueden adjuntar imágenes (PNG, JPG, GIF o BMP)");
        }
        if (origen.length() > MAX_BYTES) {
            throw new ValidacionException("La imagen \"" + nombre + "\" pesa más de " + (MAX_BYTES / 1024 / 1024) + " MB");
        }
        try {
            if (ImageIO.read(origen) == null) {
                throw new ValidacionException("\"" + nombre + "\" no es una imagen válida");
            }
        } catch (IOException e) {
            throw new ValidacionException("\"" + nombre + "\" no es una imagen válida");
        }

        String guardado = actual.getId() + "_" + System.currentTimeMillis() + "_" + limpiar(nombre);
        File destino = new File(carpeta(), guardado);
        try {
            Files.createDirectories(carpeta().toPath());
            Files.copy(origen.toPath(), destino.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("No se ha podido guardar la imagen", e);
        }

        Adjunto adjunto = new Adjunto();
        adjunto.setTicketId(actual.getId());
        adjunto.setUsuarioId(usuario.getId());
        adjunto.setNombre(nombre);
        adjunto.setArchivo(guardado);
        adjunto.setTamano(destino.length());
        adjuntoDAO.crear(adjunto);

        ticketDAO.actualizarFecha(actual.getId());
        actividadDAO.crear(actual.getId(), usuario.getId(), "adjunto", "adjuntó la imagen \"" + nombre + "\"");
        if (usuario.getId() == actual.getCreadorId()) {
            avisos.avisar(actual.getTecnicoAsignadoId(), usuario, actual.getId(), usuario.getNombre() + " ha adjuntado una imagen");
        } else {
            avisos.avisar(actual.getCreadorId(), usuario, actual.getId(), usuario.getNombre() + " ha adjuntado una imagen");
        }
    }

    static String extension(String nombre) {
        int punto = nombre.lastIndexOf('.');
        return punto < 0 ? "" : nombre.substring(punto + 1).toLowerCase(Locale.ROOT);
    }

    static String limpiar(String nombre) {
        String soloNombre = nombre.replace('\\', '/');
        soloNombre = soloNombre.substring(soloNombre.lastIndexOf('/') + 1);
        String limpio = soloNombre.replaceAll("[^A-Za-z0-9._-]", "_");
        // sin puntos al principio para que no sea un fichero oculto ni "..".
        while (limpio.startsWith(".")) {
            limpio = limpio.substring(1);
        }
        return limpio.isEmpty() ? "imagen" : limpio;
    }
}
