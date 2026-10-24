package com.itdence.controlador;

import com.itdence.modelo.Ticket;
import com.itdence.modelo.Usuario;
import com.itdence.servicio.TicketServicio;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;


public class BuscadorControlador {

    private static final int MAXIMO_RESULTADOS = 8;

    private final Stage owner;
    private final Usuario usuario;
    private final TicketServicio servicio = new TicketServicio();

    private final ListView<Ticket> resultados = new ListView<>();
    private final Label vacioLabel = new Label();
    private Integer elegido = null;
    private Stage ventana;

    public BuscadorControlador(Stage owner, Usuario usuario) {
        this.owner = owner;
        this.usuario = usuario;
    }

    public Integer mostrar() {
        StackPane iconoBuscar = new StackPane(Iconos.crear("buscar", 20));
        TextField campo = new TextField();
        campo.setPromptText("Busca por número, título, descripción o persona...");
        campo.getStyleClass().add("buscador-campo");
        HBox.setHgrow(campo, Priority.ALWAYS);
        HBox cabecera = new HBox(12, iconoBuscar, campo);
        cabecera.setAlignment(Pos.CENTER_LEFT);
        cabecera.getStyleClass().add("buscador-cabecera");

        resultados.getStyleClass().add("buscador-lista");
        resultados.setFocusTraversable(false);
        resultados.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Ticket t, boolean vacio) {
                super.updateItem(t, vacio);
                setText(null);
                setGraphic(vacio || t == null ? null : crearFila(t));
            }
        });
        resultados.setOnMouseClicked(e -> {
            if (e.getClickCount() == 1 && resultados.getSelectionModel().getSelectedItem() != null) {
                elegir(resultados.getSelectionModel().getSelectedItem());
            }
        });
        VBox.setVgrow(resultados, Priority.ALWAYS);

        vacioLabel.getStyleClass().add("texto-tenue");
        vacioLabel.setPadding(new Insets(24));
        vacioLabel.setVisible(false);
        vacioLabel.setManaged(false);

        Label ayuda = new Label("↑ ↓ para moverte    Enter para abrir    Esc para cerrar");
        ayuda.getStyleClass().add("texto-tenue");
        HBox pie = new HBox(ayuda);
        pie.getStyleClass().add("buscador-pie");

        VBox contenido = new VBox(cabecera, vacioLabel, resultados, pie);
        contenido.getStyleClass().add("fondo-superficie");

        ventana = Componentes.crearVentanaModal(owner, "Buscar", contenido, 640, 520);
        ventana.setResizable(false);

        campo.textProperty().addListener((obs, antes, ahora) -> buscar(ahora));
        campo.setOnKeyPressed(e -> {
            KeyCode tecla = e.getCode();
            if (tecla == KeyCode.DOWN) {
                mover(1);
                e.consume();
            } else if (tecla == KeyCode.UP) {
                mover(-1);
                e.consume();
            } else if (tecla == KeyCode.ENTER) {
                Ticket t = resultados.getSelectionModel().getSelectedItem();
                if (t != null) {
                    elegir(t);
                }
                e.consume();
            } else if (tecla == KeyCode.ESCAPE) {
                ventana.close();
                e.consume();
            }
        });

        buscar("");
        ventana.setOnShown(e -> Platform.runLater(campo::requestFocus));
        ventana.showAndWait();
        return elegido;
    }

    private void buscar(String texto) {
        List<Ticket> encontrados = servicio.buscar(texto, usuario, MAXIMO_RESULTADOS);
        resultados.getItems().setAll(encontrados);
        boolean vacio = encontrados.isEmpty();
        vacioLabel.setText("No hay tickets que coincidan con \"" + texto.trim() + "\".");
        vacioLabel.setVisible(vacio);
        vacioLabel.setManaged(vacio);
        resultados.setVisible(!vacio);
        resultados.setManaged(!vacio);
        if (!vacio) {
            resultados.getSelectionModel().selectFirst();
        }
    }

    private void mover(int paso) {
        int total = resultados.getItems().size();
        if (total == 0) {
            return;
        }
        int nuevo = Math.floorMod(resultados.getSelectionModel().getSelectedIndex() + paso, total);
        resultados.getSelectionModel().select(nuevo);
        resultados.scrollTo(nuevo);
    }

    private void elegir(Ticket t) {
        elegido = t.getId();
        ventana.close();
    }

    private HBox crearFila(Ticket t) {
        Label codigo = new Label(t.getCodigo());
        codigo.getStyleClass().add("celda-codigo");
        codigo.setMinWidth(52);

        Label titulo = new Label(t.getTitulo());
        titulo.getStyleClass().add("celda-titulo");
        Label detalle = Componentes.textoTenue(t.getCategoriaNombre() + " · " + t.getCreadorNombre());
        VBox textos = new VBox(2, titulo, detalle);
        HBox.setHgrow(textos, Priority.ALWAYS);

        Region espacio = new Region();
        HBox fila = new HBox(12, codigo, textos, espacio, Componentes.pillEstado(t.getEstado()));
        fila.setAlignment(Pos.CENTER_LEFT);
        return fila;
    }
}
