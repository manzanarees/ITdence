package com.itdence.servicio;

import com.itdence.modelo.Adjunto;
import com.itdence.modelo.Ticket;
import com.itdence.modelo.Usuario;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AdjuntoServicioTest extends BaseTest {

    private final TicketServicio tickets = new TicketServicio();
    private final AdjuntoServicio servicio = new AdjuntoServicio();

    private Ticket ticketNuevo() {
        int id = tickets.crearTicket("Con imagen " + System.nanoTime(), "", "baja", primeraCategoria(), cliente());
        return tickets.buscarPorId(id);
    }

    // imagen de verdad (un PNG pequeño) con el nombre que se pida
    private File imagen(String nombre) throws IOException {
        File carpeta = new File("target/test-data/origen");
        carpeta.mkdirs();
        File f = new File(carpeta, nombre);
        ImageIO.write(new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB), "png", f);
        return f;
    }

    @Test
    void sePuedeAdjuntarUnaImagenYSeGuardaEnLaCarpeta() throws IOException {
        Ticket t = ticketNuevo();
        servicio.adjuntar(t, cliente(), imagen("error-vpn.png"));

        List<Adjunto> lista = servicio.listar(t);
        assertEquals(1, lista.size());
        assertEquals("error-vpn.png", lista.get(0).getNombre());
        assertEquals("Cliente de prueba", lista.get(0).getUsuarioNombre());
        assertTrue(servicio.ficheroDe(lista.get(0)).isFile());
        assertTrue(lista.get(0).getTamano() > 0);
        assertTrue(tickets.listarActividad(t).stream().anyMatch(a -> "adjunto".equals(a.getTipo())));
    }

    @Test
    void soloSeAceptanImagenesDeVerdad() throws IOException {
        Ticket t = ticketNuevo();
        File exe = new File("target/test-data/origen/programa.exe");
        exe.getParentFile().mkdirs();
        Files.writeString(exe.toPath(), "no soy una imagen");
        assertThrows(ValidacionException.class, () -> servicio.adjuntar(t, cliente(), exe));

        // una extension de imagen no basta: el contenido tiene que serlo
        File falso = new File("target/test-data/origen/falso.png");
        Files.writeString(falso.toPath(), "esto es texto con extension png");
        ValidacionException e = assertThrows(ValidacionException.class, () -> servicio.adjuntar(t, cliente(), falso));
        assertTrue(e.getMessage().contains("no es una imagen válida"));

        assertThrows(ValidacionException.class, () -> servicio.adjuntar(t, cliente(), new File("no-existe.png")));
        assertEquals(0, servicio.listar(t).size());
    }

    @Test
    void unaImagenDemasiadoGrandeSeRechaza() throws IOException {
        Ticket t = ticketNuevo();
        File grande = imagen("grande.png");
        // se le añaden bytes al final: sigue leyendose como PNG pero pesa mas del limite
        try (java.io.RandomAccessFile f = new java.io.RandomAccessFile(grande, "rw")) {
            f.setLength(AdjuntoServicio.MAX_BYTES + 1);
        }
        ValidacionException e = assertThrows(ValidacionException.class, () -> servicio.adjuntar(t, cliente(), grande));
        assertTrue(e.getMessage().contains("pesa más de"));
    }

    @Test
    void hayUnMaximoDeImagenesPorTicket() throws IOException {
        Ticket t = ticketNuevo();
        for (int i = 0; i < AdjuntoServicio.MAX_POR_TICKET; i++) {
            servicio.adjuntar(t, cliente(), imagen("img" + i + ".png"));
        }
        assertThrows(ValidacionException.class, () -> servicio.adjuntar(t, cliente(), imagen("una-mas.png")));
        assertEquals(AdjuntoServicio.MAX_POR_TICKET, servicio.listar(t).size());
    }

    @Test
    void lasReglasDePermisosSonLasDeComentar() throws IOException {
        Ticket t = ticketNuevo();
        // el equipo tecnico puede adjuntar a cualquiera
        assertTrue(servicio.puedeAdjuntar(t, tecnico()));
        servicio.adjuntar(t, tecnico(), imagen("del-tecnico.png"));

        // otro cliente no
        String email = emailUnico();
        new UsuarioServicio().crear("Otro cliente", email, "123456", "cliente");
        Usuario otro = auth.login(email, "123456");
        assertFalse(servicio.puedeAdjuntar(t, otro));
        assertThrows(ValidacionException.class, () -> servicio.adjuntar(t, otro, imagen("colada.png")));

        // y en un ticket cerrado ya nadie
        tickets.cambiarEstado(tickets.buscarPorId(t.getId()), "cerrado", admin());
        assertFalse(servicio.puedeAdjuntar(tickets.buscarPorId(t.getId()), admin()));
    }

    @Test
    void unNombreConRutaNoPuedeSacarElFicheroDeLaCarpeta() throws IOException {
        Ticket t = ticketNuevo();
        File origen = imagen("normal.png");
        // se renombra a algo con apariencia de ruta peligrosa (en Windows las barras no valen en un
        // nombre, asi que se prueba directamente la limpieza)
        assertEquals("x.png", AdjuntoServicio.limpiar("..\\..\\x.png"));
        assertEquals("passwd.png", AdjuntoServicio.limpiar("../../etc/passwd.png"));
        assertEquals("mi_captura__1_.png", AdjuntoServicio.limpiar("mi captura (1).png"));
        assertEquals("imagen", AdjuntoServicio.limpiar("..."));

        servicio.adjuntar(t, cliente(), origen);
        File guardado = servicio.ficheroDe(servicio.listar(t).get(0));
        assertEquals(AdjuntoServicio.carpeta().getCanonicalFile(), guardado.getParentFile().getCanonicalFile());
    }

    @Test
    void alAdjuntarSeAvisaALaOtraParte() throws IOException {
        String email = emailUnico();
        new UsuarioServicio().crear("Tecnico imagenes", email, "123456", "tecnico");
        Usuario tec = auth.login(email, "123456");
        Ticket t = ticketNuevo();
        tickets.tomarTicket(t, tec);

        servicio.adjuntar(t, cliente(), imagen("para-el-tecnico.png"));
        assertEquals("Cliente de prueba ha adjuntado una imagen", new NotificacionServicio().listar(tec).get(0).getTexto());

        servicio.adjuntar(tickets.buscarPorId(t.getId()), tec, imagen("para-el-cliente.png"));
        assertEquals("Tecnico imagenes ha adjuntado una imagen", new NotificacionServicio().listar(cliente()).get(0).getTexto());
    }
}
