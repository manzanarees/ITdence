package com.itdence.controlador;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public final class PantallaBienvenida {

    private PantallaBienvenida() {}

    public static Scene crear(Runnable alTerminar) {
        ImageView logo = Componentes.crearLogo(110);

        Label it = new Label("IT");
        it.getStyleClass().addAll("bienvenida-nombre", "marca-it");
        Label dence = new Label("dence");
        dence.getStyleClass().addAll("bienvenida-nombre", "bienvenida-dence");
        HBox nombre = new HBox(it, dence);
        nombre.setAlignment(Pos.CENTER);

        Label lema = new Label("Soporte técnico e incidencias");
        lema.getStyleClass().add("login-pie");

        VBox contenido = new VBox(18, logo, nombre, lema);
        contenido.setAlignment(Pos.CENTER);

        StackPane raiz = new StackPane(contenido);
        raiz.getStyleClass().add("bienvenida");
        Scene scene = new Scene(raiz, 1000, 680);
        Componentes.aplicarEstilos(scene);

        logo.setOpacity(0);
        logo.setScaleX(0.6);
        logo.setScaleY(0.6);
        nombre.setOpacity(0);
        nombre.setTranslateY(14);
        lema.setOpacity(0);

        FadeTransition logoFundido = new FadeTransition(Duration.millis(500), logo);
        logoFundido.setToValue(1);
        ScaleTransition logoZoom = new ScaleTransition(Duration.millis(600), logo);
        logoZoom.setToX(1);
        logoZoom.setToY(1);
        logoZoom.setInterpolator(Interpolator.EASE_OUT);

        FadeTransition nombreFundido = new FadeTransition(Duration.millis(350), nombre);
        nombreFundido.setToValue(1);
        TranslateTransition nombreSube = new TranslateTransition(Duration.millis(350), nombre);
        nombreSube.setToY(0);

        FadeTransition lemaFundido = new FadeTransition(Duration.millis(300), lema);
        lemaFundido.setToValue(1);

        FadeTransition salida = new FadeTransition(Duration.millis(300), contenido);
        salida.setToValue(0);

        SequentialTransition animacion = new SequentialTransition(
                new ParallelTransition(logoFundido, logoZoom),
                new ParallelTransition(nombreFundido, nombreSube),
                lemaFundido,
                new PauseTransition(Duration.millis(450)),
                salida);
        animacion.setOnFinished(e -> alTerminar.run());
        animacion.play();
        return scene;
    }
}
