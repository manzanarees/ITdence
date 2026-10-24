package com.itdence.controlador;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

// Avisos que salen abajo a la derecha
public final class Notificaciones {

    private static final String CLAVE = "itdence.notificaciones";

    private Notificaciones() {}

    public static VBox crearCapa(Scene scene) {
        VBox capa = new VBox(10);
        capa.setAlignment(Pos.BOTTOM_RIGHT);
        capa.setPadding(new Insets(24));
        capa.setMouseTransparent(true);
        capa.setPickOnBounds(false);
        scene.getProperties().put(CLAVE, capa);
        return capa;
    }

    public static void exito(Node cualquierNodo, String texto) {
        mostrar(cualquierNodo, texto, false);
    }

    public static void error(Node cualquierNodo, String texto) {
        mostrar(cualquierNodo, texto, true);
    }

    private static void mostrar(Node cualquierNodo, String texto, boolean esError) {
        if (cualquierNodo.getScene() == null) {
            return;
        }
        Object guardada = cualquierNodo.getScene().getProperties().get(CLAVE);
        if (!(guardada instanceof VBox)) {
            return;
        }
        VBox capa = (VBox) guardada;

        Label mensaje = new Label(texto);
        mensaje.setWrapText(true);
        mensaje.setMaxWidth(340);
        HBox aviso = new HBox(10, Iconos.crear(esError ? "error" : "check-circulo", 18), mensaje);
        aviso.setAlignment(Pos.CENTER_LEFT);
        aviso.getStyleClass().addAll("toast", esError ? "toast-error" : "toast-exito");
        aviso.setMaxWidth(Region.USE_PREF_SIZE);
        capa.getChildren().add(aviso);

        aviso.setOpacity(0);
        aviso.setTranslateY(16);
        FadeTransition entra = new FadeTransition(Duration.millis(220), aviso);
        entra.setToValue(1);
        TranslateTransition sube = new TranslateTransition(Duration.millis(220), aviso);
        sube.setToY(0);
        sube.setInterpolator(Interpolator.EASE_OUT);

        FadeTransition sale = new FadeTransition(Duration.millis(300), aviso);
        sale.setToValue(0);

        SequentialTransition todo = new SequentialTransition(
                new ParallelTransition(entra, sube),
                new PauseTransition(Duration.seconds(esError ? 4 : 2.8)),
                sale);
        todo.setOnFinished(e -> capa.getChildren().remove(aviso));
        todo.play();
    }
}
