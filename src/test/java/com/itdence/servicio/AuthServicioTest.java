package com.itdence.servicio;

import com.itdence.modelo.Usuario;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthServicioTest extends BaseTest {

    @Test
    void loginCorrectoDevuelveElUsuarioConSuRol() {
        Usuario u = auth.login("admin@itdence.com", "admin123");
        assertNotNull(u);
        assertEquals("admin", u.getRol());
    }

    @Test
    void loginConContrasenaMalaDevuelveNull() {
        assertNull(auth.login("admin@itdence.com", "otracosa"));
    }

    @Test
    void loginConEmailQueNoExisteDevuelveNull() {
        assertNull(auth.login("noexiste@itdence.com", "admin123"));
    }

    @Test
    void elEmailNoDistingueMayusculasNiEspacios() {
        assertNotNull(auth.login("  CLIENTE@itdence.com ", "cliente123"));
    }

    @Test
    void laContrasenaNoSeGuardaEnTextoPlano() {
        Usuario u = auth.login("cliente@itdence.com", "cliente123");
        assertNotEquals("cliente123", u.getContrasenaHash());
        // los hash de BCrypt empiezan por $2a$
        assertTrue(u.getContrasenaHash().startsWith("$2a$"));
    }

    @Test
    void existenLosTresUsuariosDePrueba() {
        assertEquals("cliente", cliente().getRol());
        assertEquals("tecnico", tecnico().getRol());
        assertEquals("admin", admin().getRol());
    }
}
