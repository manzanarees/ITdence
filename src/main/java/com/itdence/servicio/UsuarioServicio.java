package com.itdence.servicio;

import com.itdence.dao.ActividadDAO;
import com.itdence.dao.AdjuntoDAO;
import com.itdence.dao.ComentarioDAO;
import com.itdence.dao.NotificacionDAO;
import com.itdence.dao.TicketDAO;
import com.itdence.dao.UsuarioDAO;
import com.itdence.modelo.Usuario;
import com.itdence.util.PasswordUtil;

import java.util.List;
import java.util.regex.Pattern;

public class UsuarioServicio {

    public static final List<String> ROLES = List.of("cliente", "tecnico", "admin");

    private static final Pattern PATRON_EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final int MIN_CONTRASENA = 6;

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final TicketDAO ticketDAO = new TicketDAO();
    private final ComentarioDAO comentarioDAO = new ComentarioDAO();
    private final ActividadDAO actividadDAO = new ActividadDAO();
    private final NotificacionDAO notificacionDAO = new NotificacionDAO();
    private final AdjuntoDAO adjuntoDAO = new AdjuntoDAO();

    public List<Usuario> listarTodos() {
        return usuarioDAO.listarTodos();
    }

    public List<Usuario> listarTecnicos() {
        return usuarioDAO.listarTodos().stream().filter(u -> "tecnico".equals(u.getRol())).toList();
    }

    public void crear(String nombre, String email, String contrasena, String rol) {
        String emailLimpio = validarDatos(nombre, email, rol);
        if (usuarioDAO.buscarPorEmail(emailLimpio) != null) {
            throw new ValidacionException("Ya existe un usuario con ese email");
        }
        validarContrasena(contrasena);

        Usuario usuario = new Usuario(0, nombre.trim(), emailLimpio, PasswordUtil.hashear(contrasena), rol);
        usuarioDAO.crear(usuario);
    }

    public void actualizar(Usuario original, String nombre, String email, String rol, String nuevaContrasena, Usuario usuarioActual) {
        String emailLimpio = validarDatos(nombre, email, rol);

        Usuario conEseEmail = usuarioDAO.buscarPorEmail(emailLimpio);
        if (conEseEmail != null && conEseEmail.getId() != original.getId()) {
            throw new ValidacionException("Ya existe otro usuario con ese email");
        }
        if (original.getId() == usuarioActual.getId() && !"admin".equals(rol)) {
            throw new ValidacionException("No puedes quitarte el rol de administrador a ti mismo");
        }

        String hash = original.getContrasenaHash();
        if (nuevaContrasena != null && !nuevaContrasena.isEmpty()) {
            validarContrasena(nuevaContrasena);
            hash = PasswordUtil.hashear(nuevaContrasena);
        }

        Usuario actualizado = new Usuario(original.getId(), nombre.trim(), emailLimpio, hash, rol);
        usuarioDAO.actualizar(actualizado);
    }

    // Mi perfil

    public void cambiarNombre(Usuario usuario, String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new ValidacionException("El nombre es obligatorio");
        }
        if (nombre.trim().length() > 60) {
            throw new ValidacionException("El nombre no puede tener más de 60 caracteres");
        }
        Usuario actual = usuarioDAO.buscarPorEmail(usuario.getEmail());
        usuarioDAO.actualizar(new Usuario(actual.getId(), nombre.trim(), actual.getEmail(), actual.getContrasenaHash(), actual.getRol()));
        usuario.setNombre(nombre.trim());
    }

    public void cambiarContrasena(Usuario usuario, String contrasenaActual, String nueva, String repetida) {
        Usuario actual = usuarioDAO.buscarPorEmail(usuario.getEmail());
        if (contrasenaActual == null || !PasswordUtil.verificar(contrasenaActual, actual.getContrasenaHash())) {
            throw new ValidacionException("La contraseña actual no es correcta");
        }
        validarContrasena(nueva);
        if (!nueva.equals(repetida)) {
            throw new ValidacionException("Las dos contraseñas nuevas no coinciden");
        }
        if (nueva.equals(contrasenaActual)) {
            throw new ValidacionException("La contraseña nueva tiene que ser distinta de la actual");
        }
        String hash = PasswordUtil.hashear(nueva);
        usuarioDAO.actualizar(new Usuario(actual.getId(), actual.getNombre(), actual.getEmail(), hash, actual.getRol()));
        usuario.setContrasenaHash(hash);
    }

    public void eliminar(Usuario usuario, Usuario usuarioActual) {
        if (usuario.getId() == usuarioActual.getId()) {
            throw new ValidacionException("No puedes eliminar tu propio usuario");
        }

        if (ticketDAO.contarPorUsuario(usuario.getId()) > 0 || comentarioDAO.contarPorUsuario(usuario.getId()) > 0
                || actividadDAO.contarPorUsuario(usuario.getId()) > 0 || adjuntoDAO.contarPorUsuario(usuario.getId()) > 0) {
            throw new ValidacionException("No se puede eliminar: el usuario tiene tickets o comentarios. Puedes cambiarle el rol o los datos");
        }
        notificacionDAO.eliminarPorUsuario(usuario.getId());
        usuarioDAO.eliminar(usuario.getId());
    }
    private String validarDatos(String nombre, String email, String rol) {
        if (nombre == null || nombre.isBlank()) {
            throw new ValidacionException("El nombre es obligatorio");
        }
        if (email == null || email.isBlank()) {
            throw new ValidacionException("El email es obligatorio");
        }
        String emailLimpio = email.trim().toLowerCase();
        if (!PATRON_EMAIL.matcher(emailLimpio).matches()) {
            throw new ValidacionException("El email no tiene un formato válido");
        }
        if (rol == null || !ROLES.contains(rol)) {
            throw new ValidacionException("Elige un rol");
        }
        return emailLimpio;
    }

    private void validarContrasena(String contrasena) {
        if (contrasena == null || contrasena.length() < MIN_CONTRASENA) {
            throw new ValidacionException("La contraseña tiene que tener al menos " + MIN_CONTRASENA + " caracteres");
        }
    }
}
