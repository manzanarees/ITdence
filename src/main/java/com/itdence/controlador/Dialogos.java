package com.itdence.controlador;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;

// Dialogo de confirmacion
public final class Dialogos {

    private Dialogos() {}

    public static boolean confirmar(Window owner, String titulo, String mensaje, String textoBoton) {
        boolean[] respuesta = {false};

        StackPane icono = new StackPane(Iconos.crear("aviso", 22));
        icono.getStyleClass().add("dialogo-icono");

        Label tituloLabel = new Label(titulo);
        tituloLabel.getStyleClass().add("subtitulo");
        Label mensajeLabel = new Label(mensaje);
        mensajeLabel.getStyleClass().add("texto-secundario");
        mensajeLabel.setWrapText(true);

        Button cancelar = new Button("Cancelar");
        cancelar.getStyleClass().add("boton-secundario");
        cancelar.setCancelButton(true);

        Button aceptar = new Button(textoBoton);
        aceptar.getStyleClass().add("boton-peligro-lleno");
        aceptar.setDefaultButton(true);

        HBox botones = new HBox(10, cancelar, aceptar);
        botones.setAlignment(Pos.CENTER_RIGHT);

        VBox contenido = new VBox(14, icono, new VBox(6, tituloLabel, mensajeLabel), botones);
        contenido.setPadding(new Insets(26));
        contenido.getStyleClass().add("fondo-superficie");

        Stage ventana = Componentes.crearVentanaModal(owner, titulo, contenido, 420, 250);
        ventana.setResizable(false);
        cancelar.setOnAction(e -> ventana.close());
        aceptar.setOnAction(e -> {
            respuesta[0] = true;
            ventana.close();
        });
        ventana.showAndWait();
        return respuesta[0];
    }

    public static String pedirTexto(Window owner, String titulo, String mensaje, String ayuda, String textoBoton) {
        String[] respuesta = {null};

        StackPane icono = new StackPane(Iconos.crear("recargar", 22));
        icono.getStyleClass().add("modal-icono");
        Label tituloLabel = new Label(titulo);
        tituloLabel.getStyleClass().add("subtitulo");
        Label mensajeLabel = new Label(mensaje);
        mensajeLabel.getStyleClass().add("texto-secundario");
        mensajeLabel.setWrapText(true);

        TextArea area = new TextArea();
        area.setPromptText(ayuda);
        area.setPrefRowCount(4);
        area.setWrapText(true);

        Button cancelar = new Button("Cancelar");
        cancelar.getStyleClass().add("boton-secundario");
        cancelar.setCancelButton(true);
        Button aceptar = new Button(textoBoton);
        aceptar.disableProperty().bind(area.textProperty().isEmpty());

        HBox botones = new HBox(10, cancelar, aceptar);
        botones.setAlignment(Pos.CENTER_RIGHT);

        VBox contenido = new VBox(14, icono, new VBox(6, tituloLabel, mensajeLabel), area, botones);
        contenido.setPadding(new Insets(26));
        contenido.getStyleClass().add("fondo-superficie");

        Stage ventana = Componentes.crearVentanaModal(owner, titulo, contenido, 460, 360);
        ventana.setResizable(false);
        cancelar.setOnAction(e -> ventana.close());
        aceptar.setOnAction(e -> {
            respuesta[0] = area.getText().trim();
            ventana.close();
        });
        ventana.showAndWait();
        return respuesta[0];
    }
}
