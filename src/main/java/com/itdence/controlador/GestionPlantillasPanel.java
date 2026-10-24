package com.itdence.controlador;

import com.itdence.modelo.Plantilla;
import com.itdence.modelo.Usuario;
import com.itdence.servicio.PlantillaServicio;
import com.itdence.servicio.ValidacionException;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

// Pantalla "Respuestas" del admin
public class GestionPlantillasPanel {

    private final Stage stage;
    private final Usuario usuarioActual;
    private final PlantillaServicio plantillaServicio = new PlantillaServicio();

    private final TableView<Plantilla> tabla = new TableView<>();
    private final Label tituloFormulario = new Label();
    private final StackPane iconoFormulario = new StackPane();
    private final TextField tituloField = new TextField();
    private final TextArea textoArea = new TextArea();
    private final Button eliminarButton = new Button("Eliminar");

    private Plantilla seleccionada;

    public GestionPlantillasPanel(Stage stage, Usuario usuarioActual) {
        this.stage = stage;
        this.usuarioActual = usuarioActual;
    }

    public Node crearVista() {
        tabla.getColumns().add(crearColumnaTitulo());
        Componentes.configurarTabla(tabla, Componentes.estadoVacio("rayo", "No hay respuestas",
                "Crea alguna para que los técnicos contesten más rápido."));
        tabla.getSelectionModel().selectedItemProperty().addListener((obs, antes, ahora) -> cargarEnFormulario(ahora));

        tituloFormulario.getStyleClass().add("subtitulo");
        iconoFormulario.getStyleClass().add("modal-icono");
        tituloField.setPromptText("Ej: Reiniciar el equipo");
        textoArea.setPromptText("Hola {cliente}, ...");
        textoArea.setPrefRowCount(6);
        textoArea.setWrapText(true);

        Label variables = Componentes.textoTenue("Puedes usar {cliente} (quien abrió el ticket), "
                + "{tecnico} (quien escribe) y {ticket} (por ejemplo #0042). Se rellenan al insertarla.");

        Button guardarButton = new Button("Guardar");
        guardarButton.setGraphic(Iconos.crear("check", 16));
        guardarButton.setMaxWidth(Double.MAX_VALUE);
        guardarButton.setOnAction(event -> guardar());

        Button nuevaButton = new Button("Nueva");
        nuevaButton.getStyleClass().add("boton-secundario");
        nuevaButton.setMaxWidth(Double.MAX_VALUE);
        nuevaButton.setOnAction(event -> tabla.getSelectionModel().clearSelection());

        eliminarButton.setGraphic(Iconos.crear("borrar", 16));
        eliminarButton.getStyleClass().add("boton-peligro");
        eliminarButton.setMaxWidth(Double.MAX_VALUE);
        eliminarButton.setOnAction(event -> eliminar());

        HBox.setHgrow(nuevaButton, Priority.ALWAYS);
        HBox.setHgrow(eliminarButton, Priority.ALWAYS);
        VBox botones = new VBox(8, guardarButton, new HBox(8, nuevaButton, eliminarButton));
        botones.setPadding(new Insets(6, 0, 0, 0));

        HBox cabeceraFormulario = new HBox(12, iconoFormulario, tituloFormulario);
        cabeceraFormulario.setAlignment(Pos.CENTER_LEFT);

        VBox formulario = new VBox(14,
                cabeceraFormulario,
                new VBox(7, Componentes.etiquetaCampo("Título"), tituloField),
                new VBox(7, Componentes.etiquetaCampo("Texto"), textoArea, variables),
                botones);
        formulario.getStyleClass().add("tarjeta");
        formulario.setPadding(new Insets(22));
        formulario.setPrefWidth(360);
        formulario.setMinWidth(360);
        formulario.setMaxHeight(Region.USE_PREF_SIZE);

        Label tituloTabla = new Label("Respuestas predefinidas");
        tituloTabla.getStyleClass().add("tarjeta-titulo");
        HBox cabeceraTabla = new HBox(tituloTabla);
        cabeceraTabla.getStyleClass().add("tarjeta-cabecera");
        VBox tarjetaTabla = new VBox(cabeceraTabla, tabla);
        VBox.setVgrow(tabla, Priority.ALWAYS);
        tarjetaTabla.getStyleClass().add("tarjeta");
        HBox.setHgrow(tarjetaTabla, Priority.ALWAYS);

        HBox contenido = new HBox(18, tarjetaTabla, formulario);
        VBox.setVgrow(contenido, Priority.ALWAYS);

        Label titulo = new Label("Respuestas rápidas");
        titulo.getStyleClass().add("titulo");
        Label subtitulo = new Label("Textos listos para que los técnicos contesten en un clic desde el detalle de un ticket.");
        subtitulo.getStyleClass().add("texto-secundario");

        refrescar();
        cargarEnFormulario(null);
        return new VBox(22, new VBox(4, Componentes.fechaDeHoy(), titulo, subtitulo), contenido);
    }

    private TableColumn<Plantilla, Plantilla> crearColumnaTitulo() {
        TableColumn<Plantilla, Plantilla> columna = new TableColumn<>("Respuesta");
        columna.setCellValueFactory(datos -> new ReadOnlyObjectWrapper<>(datos.getValue()));
        columna.setComparator((a, b) -> a.getTitulo().compareToIgnoreCase(b.getTitulo()));
        columna.setPrefWidth(420);
        columna.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Plantilla p, boolean vacio) {
                super.updateItem(p, vacio);
                setText(null);
                if (vacio || p == null) {
                    setGraphic(null);
                    return;
                }
                StackPane icono = new StackPane(Iconos.crear("rayo", 16));
                icono.getStyleClass().add("celda-icono");
                Label titulo = new Label(p.getTitulo());
                titulo.getStyleClass().add("celda-titulo");
                Label inicio = new Label(p.getTexto().replace("\n", " "));
                inicio.getStyleClass().add("celda-codigo");
                inicio.setMaxWidth(340);
                HBox caja = new HBox(10, icono, Componentes.centrado(new VBox(3, titulo, inicio)));
                caja.setAlignment(Pos.CENTER_LEFT);
                setGraphic(caja);
            }
        });
        return columna;
    }

    private void cargarEnFormulario(Plantilla plantilla) {
        seleccionada = plantilla;
        if (plantilla == null) {
            tituloFormulario.setText("Nueva respuesta");
            iconoFormulario.getChildren().setAll(Iconos.crear("mas", 20));
            tituloField.clear();
            textoArea.clear();
            eliminarButton.setDisable(true);
        } else {
            tituloFormulario.setText("Editar respuesta");
            iconoFormulario.getChildren().setAll(Iconos.crear("editar", 20));
            tituloField.setText(plantilla.getTitulo());
            textoArea.setText(plantilla.getTexto());
            eliminarButton.setDisable(false);
        }
    }

    private void guardar() {
        try {
            if (seleccionada == null) {
                plantillaServicio.crear(usuarioActual, tituloField.getText(), textoArea.getText());
                Notificaciones.exito(tabla, "Respuesta creada");
            } else {
                plantillaServicio.actualizar(usuarioActual, seleccionada, tituloField.getText(), textoArea.getText());
                Notificaciones.exito(tabla, "Respuesta actualizada");
            }
            refrescar();
        } catch (ValidacionException e) {
            Notificaciones.error(tabla, e.getMessage());
        }
    }

    private void eliminar() {
        if (seleccionada == null) {
            return;
        }
        boolean confirmado = Dialogos.confirmar(stage, "Eliminar respuesta",
                "¿Seguro que quieres eliminar \"" + seleccionada.getTitulo() + "\"?", "Eliminar");
        if (!confirmado) {
            return;
        }
        try {
            plantillaServicio.eliminar(usuarioActual, seleccionada);
            refrescar();
            Notificaciones.exito(tabla, "Respuesta eliminada");
        } catch (ValidacionException e) {
            Notificaciones.error(tabla, e.getMessage());
        }
    }

    public void refrescar() {
        tabla.getItems().setAll(plantillaServicio.listarTodas());
    }
}
