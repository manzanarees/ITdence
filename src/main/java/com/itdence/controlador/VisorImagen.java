package com.itdence.controlador;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;

// Ventana para ver una imagen adjuntada en grande
public final class VisorImagen {

    private VisorImagen() {}

    public static void mostrar(Window owner, File fichero, String titulo) {
        Image imagen = new Image(fichero.toURI().toString());
        ImageView vista = new ImageView(imagen);
        vista.setPreserveRatio(true);
        vista.setSmooth(true);
        vista.setFitWidth(Math.min(imagen.getWidth(), 840));

        StackPane centro = new StackPane(vista);
        centro.getStyleClass().add("visor-fondo");
        ScrollPane scroll = new ScrollPane(centro);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.getStyleClass().add("scroll-transparente");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        Label tituloLabel = new Label(titulo);
        tituloLabel.getStyleClass().add("subtitulo");
        Label medidas = new Label((int) imagen.getWidth() + " × " + (int) imagen.getHeight() + " px");
        medidas.getStyleClass().add("texto-tenue");
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        Button cerrar = new Button("Cerrar");
        cerrar.getStyleClass().add("boton-secundario");
        cerrar.setCancelButton(true);
        HBox cabecera = new HBox(12, Iconos.crear("imagen", 18), tituloLabel, medidas, espacio, cerrar);
        cabecera.setAlignment(Pos.CENTER_LEFT);
        cabecera.setStyle("-fx-padding: 14 20;");

        VBox contenido = new VBox(cabecera, scroll);
        contenido.getStyleClass().add("fondo-superficie");

        Stage ventana = Componentes.crearVentanaModal(owner, titulo, contenido, 920, 680);
        cerrar.setOnAction(e -> ventana.close());
        ventana.showAndWait();
    }
}
