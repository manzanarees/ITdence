package com.itdence.controlador;

import com.itdence.modelo.Actividad;
import com.itdence.modelo.Adjunto;
import com.itdence.modelo.Comentario;
import com.itdence.modelo.Plantilla;
import com.itdence.modelo.Ticket;
import com.itdence.modelo.Usuario;
import com.itdence.servicio.AdjuntoServicio;
import com.itdence.servicio.ComentarioServicio;
import com.itdence.servicio.NotificacionServicio;
import com.itdence.servicio.PlantillaServicio;
import com.itdence.servicio.UsuarioServicio;
import com.itdence.servicio.TicketServicio;
import com.itdence.servicio.ValidacionException;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.geometry.VPos;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

// Ventana del detalle de un ticket.
public class DetalleTicketControlador {

    private final Stage owner;
    private final Usuario usuario;
    private final int ticketId;

    private final TicketServicio ticketServicio = new TicketServicio();
    private final ComentarioServicio comentarioServicio = new ComentarioServicio();
    private final PlantillaServicio plantillaServicio = new PlantillaServicio();
    private final UsuarioServicio usuarioServicio = new UsuarioServicio();
    private final AdjuntoServicio adjuntoServicio = new AdjuntoServicio();

    private final StackPane hueco = new StackPane();
    private Ticket ticket;

    public DetalleTicketControlador(Stage owner, Usuario usuario, int ticketId) {
        this.owner = owner;
        this.usuario = usuario;
        this.ticketId = ticketId;
    }

    public void mostrar() {
        ticket = ticketServicio.buscarPorId(ticketId);
        if (ticket == null) {
            return;
        }
        new NotificacionServicio().marcarTicketLeido(usuario, ticketId);
        hueco.getStyleClass().add("fondo-app");
        double alto = Math.min(800, Screen.getPrimary().getVisualBounds().getHeight() - 40);
        Stage ventana = Componentes.crearVentanaModal(owner, "Ticket " + ticket.getCodigo(), hueco, 1100, alto);
        recargar(false);
        ventana.showAndWait();
    }

    // Vuelve a leer el ticket de la BD y monta la pantalla otra vez.
    private void recargar(boolean bajarAlFinal) {
        ticket = ticketServicio.buscarPorId(ticketId);

        ScrollPane scroll = new ScrollPane(crearColumnaIzquierda());
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-transparente");

        ScrollPane derecha = new ScrollPane(crearColumnaDerecha());
        derecha.setFitToWidth(true);
        derecha.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        derecha.getStyleClass().add("scroll-transparente");
        derecha.setPrefWidth(318);
        derecha.setMinWidth(318);
        HBox columnas = new HBox(22, scroll, derecha);
        HBox.setHgrow(scroll, Priority.ALWAYS);
        columnas.setPadding(new Insets(0, 28, 24, 28));
        VBox.setVgrow(columnas, Priority.ALWAYS);

        VBox todo = new VBox(18, crearCabecera(), columnas);
        hueco.getChildren().setAll(todo);
        Platform.runLater(() -> scroll.setVvalue(bajarAlFinal ? 1.0 : 0.0));
    }

    private VBox crearCabecera() {
        Label codigo = new Label("Ticket " + ticket.getCodigo());
        codigo.getStyleClass().add("etiqueta-campo");

        HBox etiquetas = new HBox(8,
                Componentes.pillNeutra(ticket.getCategoriaNombre()),
                Componentes.pillPrioridad(ticket.getPrioridad()),
                Componentes.pillEstado(ticket.getEstado()));
        etiquetas.setAlignment(Pos.CENTER_LEFT);

        Label tituloLabel = new Label(ticket.getTitulo());
        tituloLabel.getStyleClass().add("titulo");
        tituloLabel.setWrapText(true);

        HBox arriba = new HBox(14, codigo, etiquetas);
        arriba.setAlignment(Pos.CENTER_LEFT);

        VBox cabecera = new VBox(10, arriba, tituloLabel, crearLineaEstados());
        cabecera.setPadding(new Insets(24, 28, 4, 28));
        return cabecera;
    }

    // abierto -> en progreso -> resuelto -> cerrado, marcando por donde va
    private HBox crearLineaEstados() {
        List<String> estados = TicketServicio.ESTADOS;
        int actual = estados.indexOf(ticket.getEstado());
        boolean terminado = "cerrado".equals(ticket.getEstado());

        HBox linea = new HBox(10);
        linea.setAlignment(Pos.CENTER_LEFT);
        linea.getStyleClass().add("linea-estados");
        List<Node> pasos = new ArrayList<>();

        for (int i = 0; i < estados.size(); i++) {
            boolean hecho = i < actual || terminado;
            boolean esActual = i == actual && !terminado;

            StackPane circulo = new StackPane();
            circulo.getStyleClass().add("paso-circulo");
            if (hecho) {
                circulo.getChildren().add(Iconos.crear("check", 14));
            } else {
                Label numero = new Label(String.valueOf(i + 1));
                numero.getStyleClass().add("paso-numero");
                circulo.getChildren().add(numero);
            }
            Label texto = new Label(Componentes.capitalizar(estados.get(i)));
            texto.getStyleClass().add("paso-texto");

            HBox paso = new HBox(8, circulo, texto);
            paso.setAlignment(Pos.CENTER_LEFT);
            paso.getStyleClass().add("paso");
            if (hecho) {
                paso.getStyleClass().add("hecho");
            }
            if (esActual) {
                paso.getStyleClass().add("actual");
            }
            linea.getChildren().add(paso);
            pasos.add(paso);

            if (i < estados.size() - 1) {
                Region raya = new Region();
                raya.getStyleClass().add("paso-linea");
                if (i < actual || terminado) {
                    raya.getStyleClass().add("hecha");
                }
                HBox.setHgrow(raya, Priority.ALWAYS);
                raya.setMinWidth(20);
                linea.getChildren().add(raya);
            }
        }
        Animaciones.escalonada(pasos, 70);
        return linea;
    }

    private VBox crearColumnaIzquierda() {
        String textoDescripcion = ticket.getDescripcion() == null || ticket.getDescripcion().isBlank()
                ? "(Sin descripción)" : ticket.getDescripcion();
        Label descripcionLabel = new Label(textoDescripcion);
        descripcionLabel.getStyleClass().add("descripcion");
        descripcionLabel.setWrapText(true);
        VBox descripcion = new VBox(10, Componentes.etiquetaCampo("Descripción"), descripcionLabel);
        descripcion.getStyleClass().addAll("tarjeta", "tarjeta-padding");

        VBox columna = new VBox(18, descripcion, crearAdjuntos(), crearComentarios(), crearCajaComentario());
        columna.setPadding(new Insets(2, 8, 2, 2));
        return columna;
    }

    private Node crearAdjuntos() {
        List<Adjunto> adjuntos = adjuntoServicio.listar(ticket);
        boolean puedeAnadir = adjuntoServicio.puedeAdjuntar(ticket, usuario) && adjuntos.size() < AdjuntoServicio.MAX_POR_TICKET;
        if (adjuntos.isEmpty() && !puedeAnadir) {
            VBox nada = new VBox();
            nada.setManaged(false);
            nada.setVisible(false);
            return nada;
        }

        HBox titulo = new HBox(8, Iconos.crear("clip", 16), Componentes.etiquetaCampo("Imágenes adjuntas (" + adjuntos.size() + ")"));
        titulo.setAlignment(Pos.CENTER_LEFT);
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        HBox cabecera = new HBox(8, titulo, espacio);
        cabecera.setAlignment(Pos.CENTER_LEFT);

        if (puedeAnadir) {
            Button anadir = new Button("Añadir captura");
            anadir.setGraphic(Iconos.crear("mas", 16));
            anadir.getStyleClass().addAll("boton-secundario", "boton-pequeno");
            anadir.setOnAction(e -> elegirYAdjuntar());
            cabecera.getChildren().add(anadir);
        }

        FlowPane miniaturas = new FlowPane(12, 12);
        for (Adjunto adjunto : adjuntos) {
            miniaturas.getChildren().add(crearMiniatura(adjunto));
        }
        if (adjuntos.isEmpty()) {
            miniaturas.getChildren().add(Componentes.textoTenue("Una captura de pantalla ayuda mucho al técnico a entender el problema."));
        }

        VBox tarjeta = new VBox(12, cabecera, miniaturas);
        tarjeta.getStyleClass().addAll("tarjeta", "tarjeta-padding");
        return tarjeta;
    }

    private VBox crearMiniatura(Adjunto adjunto) {
        File fichero = adjuntoServicio.ficheroDe(adjunto);
        ImageView vista = new ImageView(new Image(fichero.toURI().toString(), 260, 184, true, true, true));
        vista.setFitWidth(130);
        vista.setFitHeight(92);
        vista.setPreserveRatio(true);
        StackPane marco = new StackPane(vista);
        marco.getStyleClass().add("miniatura");
        marco.setMinSize(130, 92);
        marco.setMaxSize(130, 92);
        Rectangle recorte = new Rectangle(130, 92);
        recorte.setArcWidth(10);
        recorte.setArcHeight(10);
        marco.setClip(recorte);

        Label nombre = new Label(adjunto.getNombre());
        nombre.getStyleClass().add("texto-tenue");
        nombre.setMaxWidth(130);
        VBox caja = new VBox(5, marco, nombre);
        caja.getStyleClass().add("miniatura-caja");
        Tooltip.install(caja, new Tooltip(adjunto.getNombre() + " · subida por " + adjunto.getUsuarioNombre()));
        caja.setOnMouseClicked(e -> VisorImagen.mostrar(hueco.getScene().getWindow(), fichero, adjunto.getNombre()));
        return caja;
    }

    private void elegirYAdjuntar() {
        FileChooser selector = new FileChooser();
        selector.setTitle("Elige las capturas");
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"));
        List<File> elegidos = selector.showOpenMultipleDialog(hueco.getScene().getWindow());
        if (elegidos == null || elegidos.isEmpty()) {
            return;
        }
        int subidas = 0;
        String problema = null;
        for (File f : elegidos) {
            try {
                adjuntoServicio.adjuntar(ticket, usuario, f);
                subidas++;
            } catch (ValidacionException ex) {
                problema = ex.getMessage();
                break;
            }
        }
        recargar(false);
        if (subidas > 0) {
            Notificaciones.exito(hueco, subidas == 1 ? "Imagen añadida" : subidas + " imágenes añadidas");
        }
        if (problema != null) {
            Notificaciones.error(hueco, problema);
        }
    }

    private VBox crearComentarios() {
        List<Comentario> comentarios = comentarioServicio.listarPorTicket(ticket, usuario);

        HBox titulo = new HBox(8, Iconos.crear("chat", 16), Componentes.etiquetaCampo("Conversación (" + comentarios.size() + ")"));
        titulo.setAlignment(Pos.CENTER_LEFT);
        VBox lista = new VBox(12, titulo);

        if (comentarios.isEmpty()) {
            lista.getChildren().add(Componentes.textoTenue("Todavía no hay comentarios. Escribe el primero."));
        }
        List<Node> burbujas = new ArrayList<>();
        for (Comentario comentario : comentarios) {
            Node burbuja = crearComentario(comentario);
            burbujas.add(burbuja);
            lista.getChildren().add(burbuja);
        }
        Animaciones.escalonada(burbujas, 50);
        return lista;
    }

    // Tipo chat: los mios a la derecha y los de los demas a la izquierda
    private HBox crearComentario(Comentario comentario) {
        boolean esMio = comentario.getUsuarioId() == usuario.getId();

        Label autor = new Label(esMio ? "Tú" : comentario.getAutorNombre());
        autor.getStyleClass().add("comentario-autor");
        Label rol = new Label(Componentes.nombreRol(comentario.getAutorRol()));
        rol.getStyleClass().add("comentario-rol");
        Label fecha = new Label(Componentes.formatearFechaHora(comentario.getFecha()));
        fecha.getStyleClass().add("comentario-fecha");

        HBox cabecera = new HBox(8, autor, rol, fecha);
        cabecera.setAlignment(Pos.BASELINE_LEFT);

        Label texto = new Label(comentario.getTexto());
        texto.setWrapText(true);
        texto.getStyleClass().add("comentario-texto");

        if (comentario.isInterno()) {
            Label etiqueta = new Label("Nota interna");
            etiqueta.setGraphic(Iconos.crear("candado", 12));
            etiqueta.getStyleClass().add("etiqueta-interna");
            cabecera.getChildren().add(etiqueta);
        }

        VBox burbuja = new VBox(5, cabecera, texto);
        burbuja.getStyleClass().add("comentario-burbuja");
        burbuja.setMaxWidth(520);
        if (comentario.isInterno()) {
            burbuja.getStyleClass().add("comentario-interno");
        } else if (esMio) {
            burbuja.getStyleClass().add("comentario-mio");
        } else if (!"cliente".equals(comentario.getAutorRol())) {
            // las respuestas del equipo de soporte en otro color
            burbuja.getStyleClass().add("comentario-soporte");
        }

        Label avatar = Componentes.crearAvatar(comentario.getAutorNombre(), 32);
        HBox fila = esMio ? new HBox(10, burbuja, avatar) : new HBox(10, avatar, burbuja);
        fila.setAlignment(esMio ? Pos.TOP_RIGHT : Pos.TOP_LEFT);
        return fila;
    }

    private Node crearCajaComentario() {
        if (!comentarioServicio.puedeComentar(ticket, usuario)) {
            String motivo = "cerrado".equals(ticket.getEstado())
                    ? "El ticket está cerrado y ya no admite comentarios."
                    : "No puedes comentar en este ticket.";
            HBox aviso = new HBox(10, Iconos.crear("candado", 16), new Label(motivo));
            aviso.setAlignment(Pos.CENTER_LEFT);
            aviso.getStyleClass().add("alerta-info");
            return aviso;
        }

        TextArea comentarioArea = new TextArea();
        comentarioArea.setPromptText("Escribe una respuesta...  (Ctrl + Enter para enviar)");
        comentarioArea.setPrefRowCount(3);
        comentarioArea.setWrapText(true);

        // Respuestas rapidas (solo el equipo)
        ComboBox<Plantilla> plantillaCombo = new ComboBox<>();
        plantillaCombo.setPromptText("Respuesta rápida...");
        plantillaCombo.getItems().addAll(plantillaServicio.listarTodas());
        plantillaCombo.setMaxWidth(Double.MAX_VALUE);
        boolean esEquipo = comentarioServicio.puedeVerNotasInternas(usuario);
        plantillaCombo.setVisible(esEquipo && !plantillaCombo.getItems().isEmpty());
        plantillaCombo.setManaged(plantillaCombo.isVisible());
        plantillaCombo.valueProperty().addListener((obs, antes, elegida) -> {
            if (elegida == null) {
                return;
            }
            String texto = plantillaServicio.aplicar(elegida, ticket, usuario);
            String actual = comentarioArea.getText();
            comentarioArea.setText(actual == null || actual.isBlank() ? texto : actual + "\n\n" + texto);
            comentarioArea.requestFocus();
            comentarioArea.positionCaret(comentarioArea.getText().length());
            Platform.runLater(() -> plantillaCombo.setValue(null));
        });

        // solo el equipo tecnico puede marcar el comentario como nota interna
        CheckBox internoCheck = new CheckBox("Nota interna (el cliente no la verá)");
        internoCheck.setVisible(comentarioServicio.puedeVerNotasInternas(usuario));
        internoCheck.setManaged(internoCheck.isVisible());
        internoCheck.selectedProperty().addListener((obs, antes, ahora) ->
                comentarioArea.setPromptText(ahora ? "Escribe una nota solo para el equipo técnico..."
                        : "Escribe una respuesta...  (Ctrl + Enter para enviar)"));

        Button comentarButton = new Button("Enviar");
        comentarButton.setGraphic(Iconos.crear("enviar", 16));
        comentarButton.setOnAction(event -> {
            try {
                boolean interno = internoCheck.isVisible() && internoCheck.isSelected();
                comentarioServicio.comentar(ticket, usuario, comentarioArea.getText(), interno);
                recargar(true);
                Notificaciones.exito(hueco, "Comentario enviado");
            } catch (ValidacionException e) {
                Notificaciones.error(hueco, e.getMessage());
                Animaciones.sacudir(comentarioArea);
            }
        });
        comentarioArea.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER && e.isControlDown()) {
                comentarButton.fire();
            }
        });

        Region espacioAcciones = new Region();
        HBox.setHgrow(espacioAcciones, Priority.ALWAYS);
        HBox acciones = new HBox(10, internoCheck, espacioAcciones, comentarButton);
        acciones.setAlignment(Pos.CENTER_LEFT);
        VBox caja = new VBox(10, Componentes.etiquetaCampo("Añadir comentario"), plantillaCombo, comentarioArea, acciones);
        caja.getStyleClass().addAll("tarjeta", "tarjeta-padding");
        return caja;
    }

    private VBox crearColumnaDerecha() {
        GridPane datos = new GridPane();
        datos.setHgap(12);
        datos.setVgap(6);
        agregarPersona(datos, 0, "Creado por", ticket.getCreadorNombre());
        agregarPersona(datos, 1, "Asignado a", ticket.getTecnicoNombre());
        agregarDato(datos, 2, "calendario", "Creado", Componentes.formatearFechaHora(ticket.getFechaCreacion()));
        agregarDato(datos, 3, "reloj", "Actualizado", Componentes.formatearFechaHora(ticket.getFechaActualizacion()));
        boolean esCliente = "cliente".equals(usuario.getRol());
        agregarDato(datos, 4, "bandera", esCliente ? "Prevista" : "Límite SLA", Componentes.formatearFechaHora(ticket.getFechaLimite()));
        datos.add(Componentes.etiquetaCampo("SLA"), 0, 5);
        datos.add(Componentes.pillSla(ticket), 1, 5);
        if (ticket.getValoracion() != null) {
            int fila = datos.getRowCount();
            datos.add(Componentes.etiquetaCampo("Valoración"), 0, fila);
            datos.add(Componentes.estrellas(ticket.getValoracion(), 16), 1, fila);
        }
        if (ticket.isCancelado()) {
            agregarDato(datos, datos.getRowCount(), "candado", "Cierre", "Cancelado por el cliente");
        }
        if (ticket.getReaperturas() > 0) {
            agregarDato(datos, datos.getRowCount(), "recargar", "Reabierto", ticket.getReaperturas() == 1 ? "1 vez" : ticket.getReaperturas() + " veces");
        }
        for (int i = 0; i < datos.getRowCount(); i++) {
            RowConstraints altura = new RowConstraints();
            altura.setMinHeight(30);
            altura.setValignment(VPos.CENTER);
            datos.getRowConstraints().add(altura);
        }

        VBox detalles = new VBox(16, Componentes.etiquetaCampo("Detalles"), datos);
        detalles.getStyleClass().addAll("tarjeta", "tarjeta-padding");

        VBox columna = new VBox(18, detalles, crearTarjetaEstado());
        if ("admin".equals(usuario.getRol()) && !"cerrado".equals(ticket.getEstado())) {
            columna.getChildren().add(crearTarjetaAsignacion());
        }
        columna.getChildren().add(crearHistorial());
        // la pregunta al cliente va la primera: es lo que tiene que hacer ahora
        if (ticketServicio.puedeResponderResolucion(ticket, usuario)) {
            columna.getChildren().add(0, crearTarjetaCierre());
        }
        columna.setPadding(new Insets(0, 14, 0, 0));
        return columna;
    }

    // Linea de tiempo con lo que ha pasado en el ticket
    private VBox crearHistorial() {
        List<Actividad> actividades = ticketServicio.listarActividad(ticket);
        HBox titulo = new HBox(8, Iconos.crear("reloj", 16), Componentes.etiquetaCampo("Historial de actividad"));
        titulo.setAlignment(Pos.CENTER_LEFT);
        VBox linea = new VBox(14, titulo);

        if (actividades.isEmpty()) {
            linea.getChildren().add(Componentes.textoTenue("Sin actividad registrada."));
        }
        for (Actividad a : actividades) {
            StackPane punto = new StackPane(Iconos.crear(iconoActividad(a.getTipo()), 12));
            punto.getStyleClass().addAll("actividad-punto", "actividad-" + a.getTipo());

            String quien = a.getUsuarioId() == usuario.getId() ? "Tú" : a.getUsuarioNombre();
            Label texto = new Label(quien + " " + textoActividad(a));
            texto.setWrapText(true);
            texto.getStyleClass().add("actividad-texto");
            Label fecha = new Label(Componentes.formatearFechaHora(a.getFecha()));
            fecha.getStyleClass().add("comentario-fecha");

            VBox textos = new VBox(2, texto, fecha);
            HBox.setHgrow(textos, Priority.ALWAYS);
            HBox fila = new HBox(10, punto, textos);
            fila.setAlignment(Pos.TOP_LEFT);
            linea.getChildren().add(fila);
        }
        linea.getStyleClass().addAll("tarjeta", "tarjeta-padding");
        return linea;
    }

    private String iconoActividad(String tipo) {
        switch (tipo) {
            case "creado": return "mas";
            case "asignado": return "usuario";
            case "valoracion": return "estrella";
            case "cancelado": return "candado";
            case "adjunto": return "clip";
            default: return "recargar";
        }
    }

    private String textoActividad(Actividad a) {
        if ("estado".equals(a.getTipo()) && a.getDetalle() != null && a.getDetalle().contains(" → ")) {
            return "cambió el estado: " + a.getDetalle();
        }
        return a.getDetalle();
    }

    // Solo el admin: desplegable con los tecnicos para asignar o reasignar el ticket
    private VBox crearTarjetaAsignacion() {
        ComboBox<Usuario> tecnicoCombo = new ComboBox<>();
        tecnicoCombo.getItems().addAll(usuarioServicio.listarTecnicos());
        tecnicoCombo.setPromptText("Elige un técnico");
        tecnicoCombo.setMaxWidth(Double.MAX_VALUE);
        tecnicoCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Usuario u) {
                return u == null ? "" : u.getNombre();
            }

            @Override
            public Usuario fromString(String texto) {
                return null;
            }
        });

        Button asignarButton = new Button(ticket.getTecnicoAsignadoId() == null ? "Asignar" : "Reasignar");
        asignarButton.setGraphic(Iconos.crear("usuario", 16));
        asignarButton.getStyleClass().add("boton-secundario");
        asignarButton.setMaxWidth(Double.MAX_VALUE);
        asignarButton.setOnAction(event -> {
            try {
                ticketServicio.asignarA(ticket, tecnicoCombo.getValue(), usuario);
                recargar(false);
                Notificaciones.exito(hueco, "Ticket asignado");
            } catch (ValidacionException e) {
                Notificaciones.error(hueco, e.getMessage());
            }
        });

        VBox tarjeta = new VBox(12, Componentes.etiquetaCampo("Asignar a"), tecnicoCombo, asignarButton);
        tarjeta.getStyleClass().addAll("tarjeta", "tarjeta-padding");
        return tarjeta;
    }

    // Solo para el cliente cuando el ticket esta resuelto: cerrarlo (valorando) o reabrirlo
    private VBox crearTarjetaCierre() {
        Label pregunta = new Label("¿Se ha resuelto tu problema?");
        pregunta.getStyleClass().add("subtitulo");
        Label explicacion = Componentes.textoTenue("El técnico ha marcado el ticket como resuelto. "
                + "Si estás de acuerdo, ciérralo y valora la atención. Si no, reábrelo y cuéntale qué sigue fallando.");

        // estrellas
        int[] nota = {0};
        List<Button> botonesEstrella = new ArrayList<>();
        HBox estrellas = new HBox(2);
        for (int i = 1; i <= 5; i++) {
            int valor = i;
            Button estrella = new Button();
            estrella.setGraphic(Iconos.crear("estrella", 24));
            estrella.getStyleClass().add("estrella-boton");
            estrella.setOnAction(e -> {
                nota[0] = valor;
                for (int j = 0; j < botonesEstrella.size(); j++) {
                    botonesEstrella.get(j).getStyleClass().remove("llena");
                    if (j < valor) {
                        botonesEstrella.get(j).getStyleClass().add("llena");
                    }
                }
            });
            botonesEstrella.add(estrella);
            estrellas.getChildren().add(estrella);
        }

        Button cerrarButton = new Button("Sí, cerrar el ticket");
        cerrarButton.setGraphic(Iconos.crear("check", 16));
        cerrarButton.setMaxWidth(Double.MAX_VALUE);
        cerrarButton.setOnAction(event -> {
            try {
                ticketServicio.confirmarResolucion(ticket, usuario, nota[0]);
                recargar(false);
                Notificaciones.exito(hueco, "Ticket cerrado. ¡Gracias por tu valoración!");
            } catch (ValidacionException e) {
                Notificaciones.error(hueco, e.getMessage());
                Animaciones.sacudir(estrellas);
            }
        });

        Button reabrirButton = new Button("No, reabrir");
        reabrirButton.getStyleClass().add("boton-secundario");
        reabrirButton.setMaxWidth(Double.MAX_VALUE);
        reabrirButton.setOnAction(event -> {
            String motivo = Dialogos.pedirTexto(hueco.getScene().getWindow(), "Reabrir el ticket",
                    "Cuéntale al técnico qué sigue sin funcionar. Quedará escrito en la conversación.",
                    "Por ejemplo: sigue sin imprimir después de reiniciar...", "Reabrir ticket");
            if (motivo == null) {
                return;
            }
            try {
                ticketServicio.reabrir(ticket, usuario, motivo);
                recargar(true);
                Notificaciones.exito(hueco, "Ticket reabierto. El técnico lo verá en su bandeja.");
            } catch (ValidacionException e) {
                Notificaciones.error(hueco, e.getMessage());
            }
        });

        VBox tarjeta = new VBox(12, pregunta, explicacion, Componentes.etiquetaCampo("Tu valoración"), estrellas,
                cerrarButton, reabrirButton);
        tarjeta.getStyleClass().addAll("tarjeta", "tarjeta-padding", "tarjeta-destacada");
        return tarjeta;
    }

    private VBox crearTarjetaEstado() {
        VBox tarjeta = new VBox(12, Componentes.etiquetaCampo("Estado"));
        tarjeta.getStyleClass().addAll("tarjeta", "tarjeta-padding");

        if (ticketServicio.puedeCambiarEstado(ticket, usuario)) {
            ComboBox<String> estadoCombo = new ComboBox<>();
            estadoCombo.getItems().addAll(TicketServicio.ESTADOS);
            estadoCombo.setConverter(Componentes.convertidorCapitalizado());
            estadoCombo.setValue(ticket.getEstado());
            estadoCombo.setMaxWidth(Double.MAX_VALUE);

            Button guardarButton = new Button("Guardar cambios");
            guardarButton.setGraphic(Iconos.crear("check", 16));
            guardarButton.setMaxWidth(Double.MAX_VALUE);
            guardarButton.setOnAction(event -> {
                try {
                    ticketServicio.cambiarEstado(ticket, estadoCombo.getValue(), usuario);
                    recargar(false);
                    Notificaciones.exito(hueco, "Estado cambiado a \"" + Componentes.capitalizar(estadoCombo.getValue()) + "\"");
                } catch (ValidacionException e) {
                    Notificaciones.error(hueco, e.getMessage());
                }
            });
            tarjeta.getChildren().addAll(estadoCombo, guardarButton);
        } else if ("tecnico".equals(usuario.getRol())) {
            tarjeta.getChildren().add(Componentes.textoTenue("Toma el ticket desde la bandeja para poder cambiar su estado."));
        } else {
            tarjeta.getChildren().addAll(Componentes.pillEstado(ticket.getEstado()),
                    Componentes.textoTenue("El técnico asignado irá actualizando el estado. Si necesita algo más, te escribirá por aquí."));
            // el cliente puede cancelar su ticket mientras no este resuelto
            if (ticketServicio.puedeCancelar(ticket, usuario)) {
                Button cancelarButton = new Button("Cancelar ticket");
                cancelarButton.setGraphic(Iconos.crear("cerrar", 16));
                cancelarButton.getStyleClass().add("boton-peligro");
                cancelarButton.setMaxWidth(Double.MAX_VALUE);
                cancelarButton.setOnAction(event -> {
                    boolean confirmado = Dialogos.confirmar(hueco.getScene().getWindow(), "Cancelar el ticket",
                            "Se cerrará sin resolverse. Si lo necesitas más adelante tendrás que abrir uno nuevo.", "Cancelar ticket");
                    if (!confirmado) {
                        return;
                    }
                    try {
                        ticketServicio.cancelar(ticket, usuario);
                        recargar(false);
                        Notificaciones.exito(hueco, "Ticket cancelado");
                    } catch (ValidacionException e) {
                        Notificaciones.error(hueco, e.getMessage());
                    }
                });
                tarjeta.getChildren().add(cancelarButton);
            }
        }
        return tarjeta;
    }

    private void agregarPersona(GridPane grid, int fila, String nombre, String persona) {
        Node valor;
        if (persona == null) {
            Label nadie = new Label("Sin asignar");
            nadie.getStyleClass().add("texto-tenue");
            valor = nadie;
        } else {
            HBox caja = new HBox(8, Componentes.crearAvatar(persona, 24), new Label(persona));
            caja.setAlignment(Pos.CENTER_LEFT);
            valor = caja;
        }
        grid.add(Componentes.etiquetaCampo(nombre), 0, fila);
        grid.add(valor, 1, fila);
    }

    private void agregarDato(GridPane grid, int fila, String icono, String nombre, String valor) {
        HBox caja = new HBox(8, Iconos.crear(icono, 16), new Label(valor));
        caja.setAlignment(Pos.CENTER_LEFT);
        grid.add(Componentes.etiquetaCampo(nombre), 0, fila);
        grid.add(caja, 1, fila);
    }
}
