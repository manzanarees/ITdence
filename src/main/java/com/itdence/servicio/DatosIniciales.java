package com.itdence.servicio;

import com.itdence.dao.CategoriaDAO;
import com.itdence.dao.TicketDAO;
import com.itdence.dao.UsuarioDAO;
import com.itdence.modelo.Categoria;
import com.itdence.modelo.Ticket;
import com.itdence.modelo.Usuario;
import com.itdence.util.PasswordUtil;

import java.util.List;

public class DatosIniciales {

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final CategoriaDAO categoriaDAO = new CategoriaDAO();
    private final TicketDAO ticketDAO = new TicketDAO();
    private final TicketServicio ticketServicio = new TicketServicio();
    private final ComentarioServicio comentarioServicio = new ComentarioServicio();
    private final PlantillaServicio plantillaServicio = new PlantillaServicio();
    private final NotificacionServicio avisos = new NotificacionServicio();
    private final AdjuntoServicio adjuntoServicio = new AdjuntoServicio();

    public void cargar() {
        crearUsuarioSiNoExiste("Administrador", "admin@itdence.com", "admin123", "admin");
        crearUsuarioSiNoExiste("Cliente de prueba", "cliente@itdence.com", "cliente123", "cliente");
        crearUsuarioSiNoExiste("Técnico de prueba", "tecnico@itdence.com", "tecnico123", "tecnico");
        crearCategoriasSiNoExisten();
        crearPlantillasSiNoExisten();
        crearTicketsDeEjemploSiNoHay();
    }

    private void crearUsuarioSiNoExiste(String nombre, String email, String contrasena, String rol) {
        if (usuarioDAO.buscarPorEmail(email) == null) {
            usuarioDAO.crear(new Usuario(0, nombre, email, PasswordUtil.hashear(contrasena), rol));
        }
    }

    private void crearCategoriasSiNoExisten() {
        if (categoriaDAO.listarTodas().isEmpty()) {
            categoriaDAO.crear(new Categoria(0, "Hardware"));
            categoriaDAO.crear(new Categoria(0, "Software"));
            categoriaDAO.crear(new Categoria(0, "Red"));
            categoriaDAO.crear(new Categoria(0, "Cuenta"));
        }
    }

    // Respuestas rapidas
    private void crearPlantillasSiNoExisten() {
        Usuario admin = usuarioDAO.buscarPorEmail("admin@itdence.com");
        if (admin == null || !plantillaServicio.listarTodas().isEmpty()) {
            return;
        }
        plantillaServicio.crear(admin, "Reiniciar el equipo",
                "Hola {cliente}, ¿puedes reiniciar el equipo y decirme si el problema continúa? Gracias. — {tecnico}");
        plantillaServicio.crear(admin, "Pedir más información",
                "Hola {cliente}, para poder ayudarte necesito algún dato más: ¿desde cuándo ocurre y aparece algún mensaje de error? Si puedes, descríbeme qué ves en pantalla.");
        plantillaServicio.crear(admin, "Acceso concedido",
                "Hola {cliente}, ya tienes el acceso configurado. Cierra sesión y vuelve a entrar para que se apliquen los cambios.");
        plantillaServicio.crear(admin, "Incidencia resuelta",
                "Hola {cliente}, damos por resuelta la incidencia {ticket}. Si el problema vuelve a aparecer, responde a este ticket y lo reabrimos.");
    }

    private void crearTicketsDeEjemploSiNoHay() {
        if (!ticketServicio.listarTodos().isEmpty()) {
            return;
        }
        Usuario cliente = usuarioDAO.buscarPorEmail("cliente@itdence.com");
        Usuario tecnico = usuarioDAO.buscarPorEmail("tecnico@itdence.com");
        if (cliente == null || tecnico == null) {
            return;
        }

        int t1 = crearTicket("No enciende el PC de recepción",
                "El equipo de recepción no arranca desde esta mañana. El led de encendido no se ilumina, " +
                "ya hemos probado con otro cable de corriente y nada.", "alta", "Hardware", cliente);
        ticketDAO.ponerAntiguedad(t1, 6);

        int t2 = crearTicket("Outlook no sincroniza el correo",
                "Desde ayer Outlook se queda en 'Actualizando carpeta de entrada' y no llegan correos nuevos. " +
                "En el móvil sí me llegan.", "media", "Software", cliente);
        tomar(t2, tecnico);
        comentar(t2, tecnico, "Hola, voy a revisarlo. ¿Te sale algún mensaje de error abajo a la derecha?", false);
        comentar(t2, cliente, "Sí, pone 'Desconectado' aunque tengo internet.", false);
        comentar(t2, tecnico, "Posible archivo OST corrupto. Si no se arregla reconstruyo el perfil.", true);
        // 20 h de 24: le queda poco tiempo
        ticketDAO.ponerAntiguedad(t2, 20);

        int t3 = crearTicket("Solicito acceso a carpeta compartida",
                "Necesito acceso a la carpeta \\\\servidor\\Contabilidad para el cierre del mes.",
                "baja", "Cuenta", cliente);
        tomar(t3, tecnico);
        comentar(t3, tecnico, "Ya tienes permisos de lectura y escritura. Cierra sesión y vuelve a entrar para que se apliquen.", false);
        ticketServicio.cambiarEstado(ticketServicio.buscarPorId(t3), "resuelto", tecnico);
        ticketDAO.ponerAntiguedad(t3, 5);
        // se resolvio 3 h despues de abrirse; el cliente todavia tiene que confirmarlo
        ticketDAO.fijarTiempoDeResolucion(t3, 3);

        int t4 = crearTicket("La VPN se desconecta cada 10 minutos",
                "Trabajando desde casa la VPN se cae cada 10 minutos más o menos y tengo que volver a conectar.",
                "alta", "Red", cliente);
        ticketDAO.ponerAntiguedad(t4, 3);
        adjuntarCapturaDeEjemplo(t4, cliente);

        int t5 = crearTicket("Impresora de la 2ª planta atascada",
                "La impresora del pasillo de la segunda planta marca atasco de papel pero no hay nada dentro.",
                "media", "Hardware", cliente);
        ticketDAO.ponerAntiguedad(t5, 2);
        int t6 = crearTicket("Cambio de contraseña del correo",
                "He olvidado la contraseña del correo y necesito una nueva para esta tarde.",
                "baja", "Cuenta", cliente);
        tomar(t6, tecnico);
        comentar(t6, tecnico, "Te he generado una contraseña temporal. Cámbiala al entrar.", false);
        ticketServicio.cambiarEstado(ticketServicio.buscarPorId(t6), "resuelto", tecnico);
        ticketServicio.confirmarResolucion(ticketServicio.buscarPorId(t6), cliente, 5);
        ticketDAO.ponerAntiguedad(t6, 30);
        ticketDAO.fijarTiempoDeResolucion(t6, 2);
        int t7 = crearTicket("El teclado del puesto 4 no responde",
                "Algunas teclas no funcionan, sobre todo las del lado derecho.",
                "media", "Hardware", cliente);
        tomar(t7, tecnico);
        ticketServicio.cambiarEstado(ticketServicio.buscarPorId(t7), "resuelto", tecnico);
        ticketServicio.reabrir(ticketServicio.buscarPorId(t7), cliente, "Sigue sin responder después de cambiar el cable.");
        ticketDAO.ponerAntiguedad(t7, 6);

        crearHistorial(cliente, tecnico);
    }

    private void adjuntarCapturaDeEjemplo(int ticketId, Usuario cliente) {
        try {
            java.awt.image.BufferedImage imagen = new java.awt.image.BufferedImage(520, 300, java.awt.image.BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g = imagen.createGraphics();
            g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(new java.awt.Color(0x1E3A5F));
            g.fillRect(0, 0, 520, 300);
            g.setColor(java.awt.Color.WHITE);
            g.fillRoundRect(70, 60, 380, 180, 8, 8);
            g.setColor(new java.awt.Color(0xE5E7EB));
            g.fillRect(70, 60, 380, 30);
            g.setColor(new java.awt.Color(0x111827));
            g.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 14));
            g.drawString("Cliente VPN", 84, 80);
            g.setColor(new java.awt.Color(0xDC2626));
            g.fillOval(92, 112, 44, 44);
            g.setColor(java.awt.Color.WHITE);
            g.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 30));
            g.drawString("!", 108, 145);
            g.setColor(new java.awt.Color(0x111827));
            g.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 16));
            g.drawString("Conexion perdida (error 809)", 152, 128);
            g.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 13));
            g.drawString("No se ha podido mantener el tunel con el servidor.", 152, 152);
            g.setColor(new java.awt.Color(0x6B7280));
            g.drawString("Reintentando en 10 segundos...", 152, 176);
            g.dispose();

            java.io.File temporal = java.io.File.createTempFile("captura-vpn", ".png");
            temporal.deleteOnExit();
            java.io.File legible = new java.io.File(temporal.getParentFile(), "captura-error-vpn.png");
            javax.imageio.ImageIO.write(imagen, "png", legible);
            legible.deleteOnExit();
            adjuntoServicio.adjuntar(ticketServicio.buscarPorId(ticketId), cliente, legible);
        } catch (java.io.IOException | RuntimeException e) {
            System.err.println("No se ha podido crear la captura de ejemplo: " + e.getMessage());
        }
    }
    //Tickets de los ultimos 13 dias
    private void crearHistorial(Usuario cliente, Usuario tecnico) {
        Object[][] casos = {
                {"Cambiar el tóner de la impresora", "baja", "Hardware", 13, 4, 5},
                {"No funciona el wifi de la sala de reuniones", "alta", "Red", 12, 6, 3},
                {"Instalar el antivirus en el portátil nuevo", "media", "Software", 11, 5, 4},
                {"Alta de usuario para el becario", "baja", "Cuenta", 10, 20, 5},
                {"El monitor del puesto 3 parpadea", "media", "Hardware", 9, 8, 4},
                {"Error al abrir el programa de facturación", "alta", "Software", 8, 3, 5},
                {"Restablecer la contraseña del ERP", "baja", "Cuenta", 7, 2, 5},
                {"Sin conexión a la carpeta de red", "alta", "Red", 6, 9, 2},
                {"Actualizar Windows en el puesto 2", "media", "Software", 5, 12, 4},
                {"El ratón inalámbrico no responde", "baja", "Hardware", 4, 1, 5},
                {"Buzón de correo lleno", "media", "Cuenta", 3, 6, 4},
                {"La cámara no se detecta en videollamadas", "media", "Hardware", 2, 10, 5},
                {"Necesito acceso a la VPN", "baja", "Red", 2, 3, 5},
                {"Pantalla azul al arrancar", "alta", "Hardware", 1, 3, 4},
        };
        for (Object[] caso : casos) {
            int id = crearTicket((String) caso[0], "Incidencia resuelta en el puesto de trabajo.",
                    (String) caso[1], (String) caso[2], cliente);
            tomar(id, tecnico);
            ticketServicio.cambiarEstado(ticketServicio.buscarPorId(id), "resuelto", tecnico);
            ticketServicio.confirmarResolucion(ticketServicio.buscarPorId(id), cliente, (int) caso[5]);
            ticketDAO.ponerAntiguedad(id, (int) caso[3] * 24);
            ticketDAO.fijarTiempoDeResolucion(id, (int) caso[4]);
            avisos.marcarTicketLeido(cliente, id);
            avisos.marcarTicketLeido(tecnico, id);
        }
    }

    private int crearTicket(String titulo, String descripcion, String prioridad, String nombreCategoria, Usuario creador) {
        return ticketServicio.crearTicket(titulo, descripcion, prioridad, buscarCategoria(nombreCategoria), creador);
    }

    private void tomar(int ticketId, Usuario tecnico) {
        ticketServicio.tomarTicket(ticketServicio.buscarPorId(ticketId), tecnico);
    }

    private Categoria buscarCategoria(String nombre) {
        List<Categoria> categorias = categoriaDAO.listarTodas();
        for (Categoria c : categorias) {
            if (c.getNombre().equals(nombre)) {
                return c;
            }
        }
        return categorias.get(0);
    }

    private void comentar(int ticketId, Usuario autor, String texto, boolean interno) {
        Ticket ticket = ticketServicio.buscarPorId(ticketId);
        comentarioServicio.comentar(ticket, autor, texto, interno);
    }
}
