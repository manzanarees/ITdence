package com.itdence.servicio;

import com.itdence.modelo.Usuario;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UsuarioServicioTest extends BaseTest {

    private final UsuarioServicio servicio = new UsuarioServicio();

    @Test
    void crearUsuarioYPoderHacerLogin() {
        String email = emailUnico();
        servicio.crear("Nuevo Técnico", email, "secreta1", "tecnico");
        Usuario u = auth.login(email, "secreta1");
        assertNotNull(u);
        assertEquals("tecnico", u.getRol());
    }

    @Test
    void noSePuedenRepetirEmails() {
        assertThrows(ValidacionException.class,
                () -> servicio.crear("Repetido", "admin@itdence.com", "123456", "cliente"));
    }

    @Test
    void elEmailTieneQueTenerFormatoValido() {
        assertThrows(ValidacionException.class, () -> servicio.crear("Mal", "sinarroba.com", "123456", "cliente"));
    }

    @Test
    void laContrasenaTieneQueTenerAlMenosSeisCaracteres() {
        assertThrows(ValidacionException.class, () -> servicio.crear("Corta", emailUnico(), "123", "cliente"));
    }

    @Test
    void editarSinContrasenaMantieneLaAnterior() {
        String email = emailUnico();
        servicio.crear("Antes", email, "original1", "cliente");
        Usuario u = auth.login(email, "original1");

        servicio.actualizar(u, "Despues", email, "cliente", "", admin());

        Usuario editado = auth.login(email, "original1");
        assertNotNull(editado);
        assertEquals("Despues", editado.getNombre());
    }

    @Test
    void editarConContrasenaNuevaLaCambia() {
        String email = emailUnico();
        servicio.crear("Usuario", email, "vieja123", "cliente");
        Usuario u = auth.login(email, "vieja123");

        servicio.actualizar(u, "Usuario", email, "cliente", "nueva123", admin());

        assertNull(auth.login(email, "vieja123"));
        assertNotNull(auth.login(email, "nueva123"));
    }

    @Test
    void elAdminNoPuedeQuitarseSuRolNiBorrarse() {
        Usuario admin = admin();
        assertThrows(ValidacionException.class,
                () -> servicio.actualizar(admin, admin.getNombre(), admin.getEmail(), "cliente", "", admin));
        assertThrows(ValidacionException.class, () -> servicio.eliminar(admin, admin));
    }

    @Test
    void noSePuedeBorrarUnUsuarioConTickets() {
        assertThrows(ValidacionException.class, () -> servicio.eliminar(cliente(), admin()));
    }

    @Test
    void seBorraUnUsuarioSinTickets() {
        String email = emailUnico();
        servicio.crear("Temporal", email, "123456", "cliente");
        servicio.eliminar(auth.login(email, "123456"), admin());
        assertNull(auth.login(email, "123456"));
    }

    // ---------- Mi perfil ----------

    private Usuario usuarioNuevo() {
        String email = emailUnico();
        servicio.crear("Perfil de prueba", email, "original1", "cliente");
        return auth.login(email, "original1");
    }

    @Test
    void sePuedeCambiarElPropioNombre() {
        Usuario u = usuarioNuevo();
        servicio.cambiarNombre(u, "  Nombre Nuevo  ");
        assertEquals("Nombre Nuevo", u.getNombre());
        assertEquals("Nombre Nuevo", auth.login(u.getEmail(), "original1").getNombre());
        assertThrows(ValidacionException.class, () -> servicio.cambiarNombre(u, "  "));
        assertThrows(ValidacionException.class, () -> servicio.cambiarNombre(u, "x".repeat(61)));
    }

    @Test
    void cambiarLaContrasenaExigeLaActualYLaNuevaSeUsaDespues() {
        Usuario u = usuarioNuevo();
        servicio.cambiarContrasena(u, "original1", "nueva1234", "nueva1234");

        assertNull(auth.login(u.getEmail(), "original1"));
        assertNotNull(auth.login(u.getEmail(), "nueva1234"));
        // y el nombre y el rol no se tocan
        assertEquals("Perfil de prueba", auth.login(u.getEmail(), "nueva1234").getNombre());
        assertEquals("cliente", auth.login(u.getEmail(), "nueva1234").getRol());
    }

    @Test
    void cambiarLaContrasenaValidaTodo() {
        Usuario u = usuarioNuevo();
        assertThrows(ValidacionException.class, () -> servicio.cambiarContrasena(u, "mala", "nueva1234", "nueva1234"));
        assertThrows(ValidacionException.class, () -> servicio.cambiarContrasena(u, "original1", "corta", "corta"));
        assertThrows(ValidacionException.class, () -> servicio.cambiarContrasena(u, "original1", "nueva1234", "otra12345"));
        assertThrows(ValidacionException.class, () -> servicio.cambiarContrasena(u, "original1", "original1", "original1"));
        // despues de tantos intentos fallidos la contraseña sigue siendo la de siempre
        assertNotNull(auth.login(u.getEmail(), "original1"));
    }
}
