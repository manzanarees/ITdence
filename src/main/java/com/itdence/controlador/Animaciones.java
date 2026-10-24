package com.itdence.controlador;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;

import java.util.List;

// Todas las animaciones de la app

public final class Animaciones {

    private static final Duration NORMAL = Duration.millis(320);

    private Animaciones() {}


    public static void entrada(Node nodo) {
        entrada(nodo, 0);
    }

    public static void entrada(Node nodo, double retrasoMs) {
        nodo.setOpacity(0);
        nodo.setTranslateY(12);

        FadeTransition fundido = new FadeTransition(NORMAL, nodo);
        fundido.setToValue(1);
        TranslateTransition subida = new TranslateTransition(NORMAL, nodo);
        subida.setToY(0);
        subida.setInterpolator(Interpolator.EASE_OUT);

        ParallelTransition animacion = new ParallelTransition(fundido, subida);
        animacion.setDelay(Duration.millis(retrasoMs));
        animacion.play();
    }

    public static void escalonada(List<? extends Node> nodos, double pasoMs) {
        for (int i = 0; i < nodos.size(); i++) {
            entrada(nodos.get(i), i * pasoMs);
        }
    }

    // Fundido + un poco de zoom, para las ventanas modales
    public static void aparecer(Node nodo) {
        nodo.setOpacity(0);
        nodo.setScaleX(0.96);
        nodo.setScaleY(0.96);

        FadeTransition fundido = new FadeTransition(Duration.millis(220), nodo);
        fundido.setToValue(1);
        ScaleTransition zoom = new ScaleTransition(Duration.millis(220), nodo);
        zoom.setToX(1);
        zoom.setToY(1);
        zoom.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(fundido, zoom).play();
    }

    // No para cuando el login falla
    public static void sacudir(Node nodo) {
        TranslateTransition sacudida = new TranslateTransition(Duration.millis(55), nodo);
        sacudida.setFromX(0);
        sacudida.setByX(9);
        sacudida.setCycleCount(6);
        sacudida.setAutoReverse(true);
        sacudida.setOnFinished(e -> nodo.setTranslateX(0));
        sacudida.play();
    }

    // El numero sube desde el valor que tenia hasta el nuevo
    public static void contar(Label label, int hasta) {
        contar(label, hasta, "");
    }

    // sufijo
    public static void contar(Label label, int hasta, String sufijo) {
        int desde;
        try {
            // se quitan los caracteres que no son numeros por si ya llevaba el sufijo
            desde = Integer.parseInt(label.getText().replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            desde = 0;
        }
        DoubleProperty valor = new SimpleDoubleProperty(desde);
        valor.addListener((obs, antes, ahora) -> label.setText(Math.round(ahora.doubleValue()) + sufijo));
        new Timeline(new KeyFrame(Duration.millis(700), new KeyValue(valor, hasta, Interpolator.EASE_OUT))).play();
    }

    public static void animarHasta(DoubleProperty propiedad, double valor) {
        new Timeline(new KeyFrame(Duration.millis(800), new KeyValue(propiedad, valor, Interpolator.EASE_OUT))).play();
    }

    // Se levanta un poco al pasar el raton por encima
    public static void elevarAlPasar(Node nodo) {
        nodo.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> moverY(nodo, -3));
        nodo.addEventHandler(MouseEvent.MOUSE_EXITED, e -> moverY(nodo, 0));
    }

    private static void moverY(Node nodo, double y) {
        TranslateTransition t = new TranslateTransition(Duration.millis(150), nodo);
        t.setToY(y);
        t.play();
    }
}
