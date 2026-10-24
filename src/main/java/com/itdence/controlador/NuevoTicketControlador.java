package com.itdence.controlador;

import com.itdence.modelo.Categoria;
import com.itdence.modelo.Ticket;
import com.itdence.modelo.Usuario;
import com.itdence.servicio.AdjuntoServicio;
import com.itdence.servicio.CategoriaServicio;
import com.itdence.servicio.TicketServicio;
import com.itdence.servicio.ValidacionException;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

// Ventana modal con el formulario de crear ticket
public class NuevoTicketControlador {

    private static final int MAX_TITULO = 100;

    private final Stage owner;
    private final Usuario usuario;
    private final TicketServicio ticketServicio = new TicketServicio();
    private int idCreado = -1;

    private final List<File> capturas = new ArrayList<>();
    private final FlowPane chipsCapturas = new FlowPane(8, 8);
    private final List<String> problemasAdjuntos = new ArrayList<>();
    private final AdjuntoServicio adjuntoServicio = new AdjuntoServicio();

    public NuevoTicketControlador(Stage owner, Usuario usuario) {
        this.owner = owner;
        this.usuario = usuario;
    }

    public List<String> getProblemasAdjuntos() {
        return problemasAdjuntos;
    }

    private VBox crearZonaDeAdjuntos() {
        Button elegir = new Button("Adjuntar capturas");
        elegir.setGraphic(Iconos.crear("clip", 16));
        elegir.getStyleClass().addAll("boton-secundario", "boton-pequeno");
        elegir.setOnAction(e -> {
            FileChooser selector = new FileChooser();
            selector.setTitle("Elige las capturas");
            selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"));
            List<File> elegidos = selector.showOpenMultipleDialog(elegir.getScene().getWindow());
            if (elegidos != null) {
                for (File f : elegidos) {
                    if (capturas.size() < AdjuntoServicio.MAX_POR_TICKET && !capturas.contains(f)) {
                        capturas.add(f);
                    }
                }
                pintarChips();
            }
        });
        Label ayuda = Componentes.textoTenue("Opcional. Hasta " + AdjuntoServicio.MAX_POR_TICKET + " imágenes de 5 MB como máximo.");
        HBox fila = new HBox(12, elegir, ayuda);
        fila.setAlignment(Pos.CENTER_LEFT);
        return new VBox(8, Componentes.etiquetaCampo("Capturas de pantalla"), fila, chipsCapturas);
    }

    private void pintarChips() {
        chipsCapturas.getChildren().clear();
        for (File f : new ArrayList<>(capturas)) {
            Label nombre = new Label(f.getName());
            nombre.setMaxWidth(190);
            Button quitar = new Button();
            quitar.setGraphic(Iconos.crear("cerrar", 12));
            quitar.getStyleClass().add("boton-icono");
            quitar.setOnAction(e -> {
                capturas.remove(f);
                pintarChips();
            });
            HBox chip = new HBox(6, Iconos.crear("imagen", 14), nombre, quitar);
            chip.setAlignment(Pos.CENTER_LEFT);
            chip.getStyleClass().add("chip-archivo");
            chipsCapturas.getChildren().add(chip);
        }
    }

    private void adjuntarLasElegidas() {
        Ticket creado = ticketServicio.buscarPorId(idCreado);
        for (File f : capturas) {
            try {
                adjuntoServicio.adjuntar(creado, usuario, f);
            } catch (ValidacionException ex) {
                problemasAdjuntos.add(ex.getMessage());
            }
        }
    }
    public int mostrar() {
        StackPane icono = new StackPane(Iconos.crear("ticket", 22));
        icono.getStyleClass().add("modal-icono");
        Label tituloLabel = new Label("Nuevo ticket");
        tituloLabel.getStyleClass().add("titulo-modal");
        Label explicacionLabel = new Label("Cuéntanos qué pasa. Cuanto más detalle, antes lo podremos resolver.");
        explicacionLabel.getStyleClass().add("texto-secundario");
        HBox cabecera = new HBox(14, icono, new VBox(3, tituloLabel, explicacionLabel));
        cabecera.setAlignment(Pos.CENTER_LEFT);

        TextField tituloField = new TextField();
        tituloField.setPromptText("Ej: No puedo imprimir desde mi ordenador");
        // no deja escribir mas de 100 caracteres
        tituloField.setTextFormatter(new TextFormatter<String>(cambio ->
                cambio.getControlNewText().length() <= MAX_TITULO ? cambio : null));
        Label contador = new Label("0/" + MAX_TITULO);
        contador.getStyleClass().add("contador-caracteres");
        tituloField.textProperty().addListener((obs, antes, ahora) -> contador.setText(ahora.length() + "/" + MAX_TITULO));

        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        HBox etiquetaTitulo = new HBox(Componentes.etiquetaCampo("Título"), espacio, contador);

        TextArea descripcionArea = new TextArea();
        descripcionArea.setPromptText("Qué ha pasado, desde cuándo, qué has probado...");
        descripcionArea.setPrefRowCount(5);
        descripcionArea.setWrapText(true);

        ComboBox<Categoria> categoriaCombo = new ComboBox<>();
        categoriaCombo.getItems().addAll(new CategoriaServicio().listarTodas());
        categoriaCombo.setPromptText("Elige una");
        categoriaCombo.setMaxWidth(Double.MAX_VALUE);

        // prioridad
        ToggleGroup grupoPrioridad = new ToggleGroup();
        HBox prioridades = new HBox(8);
        for (String prioridad : TicketServicio.PRIORIDADES) {
            ToggleButton boton = new ToggleButton(Componentes.capitalizar(prioridad));
            boton.setUserData(prioridad);
            boton.setToggleGroup(grupoPrioridad);
            boton.getStyleClass().addAll("segmento", "segmento-" + prioridad);
            boton.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(boton, Priority.ALWAYS);
            if (prioridad.equals("media")) {
                boton.setSelected(true);
            }
            prioridades.getChildren().add(boton);
        }
        grupoPrioridad.selectedToggleProperty().addListener((obs, antes, ahora) -> {
            if (ahora == null) {
                grupoPrioridad.selectToggle(antes);
            }
        });

        VBox categoriaCaja = new VBox(7, Componentes.etiquetaCampo("Categoría"), categoriaCombo);
        VBox prioridadCaja = new VBox(7, Componentes.etiquetaCampo("Prioridad"), prioridades);
        HBox.setHgrow(categoriaCaja, Priority.ALWAYS);
        HBox.setHgrow(prioridadCaja, Priority.ALWAYS);
        HBox fila = new HBox(16, categoriaCaja, prioridadCaja);

        Label errorTexto = new Label();
        errorTexto.setWrapText(true);
        HBox errorCaja = new HBox(10, Iconos.crear("error", 18), errorTexto);
        errorCaja.setAlignment(Pos.CENTER_LEFT);
        errorCaja.getStyleClass().add("alerta-error");
        errorCaja.setVisible(false);
        errorCaja.setManaged(false);

        Button cancelarButton = new Button("Cancelar");
        cancelarButton.getStyleClass().add("boton-secundario");
        cancelarButton.setCancelButton(true);

        Button crearButton = new Button("Crear ticket");
        crearButton.setGraphic(Iconos.crear("enviar", 16));

        HBox botones = new HBox(10, cancelarButton, crearButton);
        botones.setAlignment(Pos.CENTER_RIGHT);

        VBox formulario = new VBox(18,
                cabecera,
                new VBox(7, etiquetaTitulo, tituloField),
                new VBox(7, Componentes.etiquetaCampo("Descripción"), descripcionArea),
                fila,
                crearZonaDeAdjuntos(),
                errorCaja,
                botones);
        formulario.setPadding(new Insets(28));
        formulario.getStyleClass().add("fondo-superficie");

        Stage ventana = Componentes.crearVentanaModal(owner, "Nuevo ticket", formulario, 600, 660);

        cancelarButton.setOnAction(event -> ventana.close());
        crearButton.setOnAction(event -> {
            try {
                String prioridad = (String) grupoPrioridad.getSelectedToggle().getUserData();
                idCreado = ticketServicio.crearTicket(tituloField.getText(), descripcionArea.getText(),
                        prioridad, categoriaCombo.getValue(), usuario);
                adjuntarLasElegidas();
                ventana.close();
            } catch (ValidacionException e) {
                errorTexto.setText(e.getMessage());
                if (!errorCaja.isVisible()) {
                    errorCaja.setVisible(true);
                    errorCaja.setManaged(true);
                    Animaciones.entrada(errorCaja);
                } else {
                    Animaciones.sacudir(errorCaja);
                }
            }
        });

        ventana.showAndWait();
        return idCreado;
    }
}
