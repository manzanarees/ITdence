package com.itdence.controlador;

import com.itdence.modelo.Categoria;
import com.itdence.servicio.CategoriaServicio;
import com.itdence.servicio.TicketServicio;
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
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.Map;

// Pantalla "Categorías" del admin
public class GestionCategoriasPanel {

    private final Stage stage;
    private final CategoriaServicio categoriaServicio = new CategoriaServicio();
    private final TicketServicio ticketServicio = new TicketServicio();

    private final TableView<Categoria> tabla = new TableView<>();
    private final Label tituloFormulario = new Label();
    private final StackPane iconoFormulario = new StackPane();
    private final TextField nombreField = new TextField();
    private final Button eliminarButton = new Button("Eliminar");
    private Map<String, Integer> ticketsPorCategoria = new HashMap<>();

    private Categoria seleccionada;

    public GestionCategoriasPanel(Stage stage) {
        this.stage = stage;
    }

    public Node crearVista() {
        tabla.getColumns().add(crearColumnaNombre());
        tabla.getColumns().add(Componentes.columnaTexto("Tickets",
                c -> String.valueOf(ticketsPorCategoria.getOrDefault(c.getNombre(), 0)), 100));
        Componentes.configurarTabla(tabla, Componentes.estadoVacio("etiqueta", "No hay categorías",
                "Crea al menos una para que los clientes puedan abrir tickets."));
        tabla.getSelectionModel().selectedItemProperty().addListener((obs, antes, ahora) -> cargarEnFormulario(ahora));

        tituloFormulario.getStyleClass().add("subtitulo");
        iconoFormulario.getStyleClass().add("modal-icono");
        nombreField.setPromptText("Ej: Impresoras");

        Button guardarButton = new Button("Guardar");
        guardarButton.setGraphic(Iconos.crear("check", 16));
        guardarButton.setOnAction(event -> guardar());

        Button nuevaButton = new Button("Nueva");
        nuevaButton.getStyleClass().add("boton-secundario");
        nuevaButton.setOnAction(event -> tabla.getSelectionModel().clearSelection());

        eliminarButton.setGraphic(Iconos.crear("borrar", 16));
        eliminarButton.getStyleClass().add("boton-peligro");
        eliminarButton.setOnAction(event -> eliminar());

        guardarButton.setMaxWidth(Double.MAX_VALUE);
        nuevaButton.setMaxWidth(Double.MAX_VALUE);
        eliminarButton.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(nuevaButton, Priority.ALWAYS);
        HBox.setHgrow(eliminarButton, Priority.ALWAYS);
        VBox botones = new VBox(8, guardarButton, new HBox(8, nuevaButton, eliminarButton));
        botones.setPadding(new Insets(6, 0, 0, 0));

        HBox cabeceraFormulario = new HBox(12, iconoFormulario, tituloFormulario);
        cabeceraFormulario.setAlignment(Pos.CENTER_LEFT);

        VBox formulario = new VBox(14,
                cabeceraFormulario,
                new VBox(7, Componentes.etiquetaCampo("Nombre"), nombreField),
                botones,
                Componentes.textoTenue("Solo se pueden eliminar las categorías que no tienen tickets, para no perder el historial."));
        formulario.getStyleClass().add("tarjeta");
        formulario.setPadding(new Insets(22));
        formulario.setPrefWidth(330);
        formulario.setMinWidth(330);
        formulario.setMaxHeight(Region.USE_PREF_SIZE);

        Label tituloTabla = new Label("Categorías");
        tituloTabla.getStyleClass().add("tarjeta-titulo");
        HBox cabeceraTabla = new HBox(tituloTabla);
        cabeceraTabla.getStyleClass().add("tarjeta-cabecera");
        VBox tarjetaTabla = new VBox(cabeceraTabla, tabla);
        VBox.setVgrow(tabla, Priority.ALWAYS);
        tarjetaTabla.getStyleClass().add("tarjeta");
        HBox.setHgrow(tarjetaTabla, Priority.ALWAYS);

        HBox contenido = new HBox(18, tarjetaTabla, formulario);
        VBox.setVgrow(contenido, Priority.ALWAYS);

        Label titulo = new Label("Categorías");
        titulo.getStyleClass().add("titulo");
        Label subtitulo = new Label("Los tipos de incidencia que el cliente puede elegir al abrir un ticket.");
        subtitulo.getStyleClass().add("texto-secundario");

        refrescar();
        cargarEnFormulario(null);
        return new VBox(22, new VBox(4, Componentes.fechaDeHoy(), titulo, subtitulo), contenido);
    }

    private TableColumn<Categoria, Categoria> crearColumnaNombre() {
        TableColumn<Categoria, Categoria> columna = new TableColumn<>("Nombre");
        columna.setCellValueFactory(datos -> new ReadOnlyObjectWrapper<>(datos.getValue()));
        columna.setComparator((a, b) -> a.getNombre().compareToIgnoreCase(b.getNombre()));
        columna.setPrefWidth(300);
        columna.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Categoria categoria, boolean vacio) {
                super.updateItem(categoria, vacio);
                setText(null);
                if (vacio || categoria == null) {
                    setGraphic(null);
                    return;
                }
                StackPane icono = new StackPane(Iconos.crear("etiqueta", 16));
                icono.getStyleClass().add("celda-icono");
                Label nombre = new Label(categoria.getNombre());
                nombre.getStyleClass().add("celda-titulo");
                HBox caja = new HBox(10, icono, nombre);
                caja.setAlignment(Pos.CENTER_LEFT);
                setGraphic(caja);
            }
        });
        return columna;
    }

    private void cargarEnFormulario(Categoria categoria) {
        seleccionada = categoria;
        if (categoria == null) {
            tituloFormulario.setText("Nueva categoría");
            iconoFormulario.getChildren().setAll(Iconos.crear("mas", 20));
            nombreField.clear();
            eliminarButton.setDisable(true);
        } else {
            tituloFormulario.setText("Editar categoría");
            iconoFormulario.getChildren().setAll(Iconos.crear("editar", 20));
            nombreField.setText(categoria.getNombre());
            eliminarButton.setDisable(false);
        }
    }

    private void guardar() {
        try {
            if (seleccionada == null) {
                categoriaServicio.crear(nombreField.getText());
                Notificaciones.exito(tabla, "Categoría creada");
            } else {
                categoriaServicio.renombrar(seleccionada, nombreField.getText());
                Notificaciones.exito(tabla, "Categoría actualizada");
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
        boolean confirmado = Dialogos.confirmar(stage, "Eliminar categoría",
                "¿Seguro que quieres eliminar la categoría " + seleccionada.getNombre() + "?", "Eliminar");
        if (!confirmado) {
            return;
        }
        try {
            categoriaServicio.eliminar(seleccionada);
            refrescar();
            Notificaciones.exito(tabla, "Categoría eliminada");
        } catch (ValidacionException e) {
            Notificaciones.error(tabla, e.getMessage());
        }
    }

    public void refrescar() {
        ticketsPorCategoria = ticketServicio.contarPorCategoria();
        tabla.getItems().setAll(categoriaServicio.listarTodas());
    }
}
