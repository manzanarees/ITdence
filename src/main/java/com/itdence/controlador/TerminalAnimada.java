package com.itdence.controlador;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;

import java.util.ArrayDeque;
import java.util.Deque;

// Ventana de terminal del login
public class TerminalAnimada {

    private static final String[] GUION = {
            "$ itdence login cliente@empresa.com",
            "[OK] Sesión iniciada · rol: cliente",
            "$ itdence nuevo \"No enciende el PC\" -p alta",
            "[OK] Ticket #0042 creado · abierto",
            "$ itdence tomar #0042 --tecnico dortiz",
            " ->  Asignado a David Ortiz · en progreso",
            "$ itdence estado #0042 resuelto",
            "[OK] Ticket #0042 resuelto en 1h 12m",
    };

    private static final Duration POR_LETRA = Duration.millis(38);

    private final VBox lineas = new VBox(5);
    private final Region cursor = new Region();
    private final VBox ventana;
    private final Deque<String> pendientes = new ArrayDeque<>();

    public TerminalAnimada() {
        cursor.getStyleClass().add("term-cursor");
        cursor.setMinSize(8, 15);
        cursor.setMaxSize(8, 15);
        FadeTransition parpadeo = new FadeTransition(Duration.millis(530), cursor);
        parpadeo.setFromValue(1);
        parpadeo.setToValue(0);
        parpadeo.setAutoReverse(true);
        parpadeo.setCycleCount(Animation.INDEFINITE);
        parpadeo.play();

        ventana = new VBox(crearBarraTitulo(), crearCuerpo());
        ventana.getStyleClass().add("term-ventana");
    }

    public Node getVista() {
        return ventana;
    }

    public void empezar() {
        esperar(700, this::reiniciar);
    }

    private StackPane crearBarraTitulo() {
        Circle rojo = new Circle(5.5);
        rojo.getStyleClass().add("term-rojo");
        Circle amarillo = new Circle(5.5);
        amarillo.getStyleClass().add("term-amarillo");
        Circle verde = new Circle(5.5);
        verde.getStyleClass().add("term-verde");
        HBox circulos = new HBox(7, rojo, amarillo, verde);
        circulos.setAlignment(Pos.CENTER_LEFT);

        Label titulo = new Label("itdence@helpdesk: ~");
        titulo.getStyleClass().add("term-titulo");

        StackPane barra = new StackPane(circulos, titulo);
        barra.getStyleClass().add("term-barra");
        return barra;
    }

    private VBox crearCuerpo() {
        VBox cuerpo = new VBox(lineas);
        cuerpo.getStyleClass().add("term-cuerpo");
        cuerpo.setMinHeight(236);
        cuerpo.setPrefHeight(236);
        return cuerpo;
    }

    // Animacion

    private void reiniciar() {
        lineas.getChildren().clear();
        pendientes.clear();
        for (String linea : GUION) {
            pendientes.add(linea);
        }
        siguienteLinea();
    }

    private boolean sigueVisible() {
        return ventana.getScene() != null
                && ventana.getScene().getWindow() != null
                && ventana.getScene().getWindow().isShowing();
    }

    private void siguienteLinea() {
        if (!sigueVisible()) {
            return;
        }
        if (pendientes.isEmpty()) {
            TextFlow prompt = nuevaLinea();
            prompt.getChildren().addAll(texto("$ ", "term-prompt"), cursor);
            esperar(4500, this::reiniciar);
            return;
        }
        String linea = pendientes.poll();
        if (linea.startsWith("$ ")) {
            escribirComando(linea.substring(2));
        } else {
            mostrarRespuesta(linea);
            esperar(700, this::siguienteLinea);
        }
    }

    private void escribirComando(String comando) {
        TextFlow flujo = nuevaLinea();
        Text escrito = texto("", "term-comando");
        flujo.getChildren().addAll(texto("$ ", "term-prompt"), escrito, cursor);

        Timeline tecleo = new Timeline();
        for (int i = 1; i <= comando.length(); i++) {
            String parcial = comando.substring(0, i);
            tecleo.getKeyFrames().add(new KeyFrame(POR_LETRA.multiply(i), e -> escrito.setText(parcial)));
        }
        tecleo.setOnFinished(e -> esperar(350, () -> {
            flujo.getChildren().setAll(texto("$ ", "term-prompt"));
            flujo.getChildren().addAll(colorear(comando));
            siguienteLinea();
        }));
        tecleo.play();
    }

    private void mostrarRespuesta(String linea) {
        TextFlow flujo = nuevaLinea();
        if (linea.startsWith("[OK]")) {
            flujo.getChildren().addAll(texto("[OK]", "term-ok"), texto(linea.substring(4), "term-salida"));
        } else if (linea.startsWith(" ->")) {
            flujo.getChildren().addAll(texto(" -> ", "term-flecha"), texto(linea.substring(4), "term-salida"));
        } else {
            flujo.getChildren().add(texto(linea, "term-salida"));
        }
        flujo.setOpacity(0);
        FadeTransition aparecer = new FadeTransition(Duration.millis(200), flujo);
        aparecer.setToValue(1);
        aparecer.play();
    }

    private Text[] colorear(String comando) {
        String[] partes = comando.split("(?<= )");
        Text[] textos = new Text[partes.length];
        boolean dentroDeComillas = false;
        for (int i = 0; i < partes.length; i++) {
            String parte = partes[i];
            String clase;
            if (dentroDeComillas || parte.startsWith("\"")) {
                clase = "term-cadena";
                // si la palabra abre comillas y no las cierra, las siguientes siguen siendo cadena
                if (parte.startsWith("\"") && !parte.trim().endsWith("\"")) {
                    dentroDeComillas = true;
                } else if (dentroDeComillas && parte.trim().endsWith("\"")) {
                    dentroDeComillas = false;
                }
            } else if (i == 0) {
                clase = "term-programa";
            } else if (parte.startsWith("-")) {
                clase = "term-opcion";
            } else if (parte.startsWith("#")) {
                clase = "term-id";
            } else {
                clase = "term-comando";
            }
            textos[i] = texto(parte, clase);
        }
        return textos;
    }

    private TextFlow nuevaLinea() {
        TextFlow flujo = new TextFlow();
        lineas.getChildren().add(flujo);
        return flujo;
    }

    private Text texto(String contenido, String clase) {
        Text t = new Text(contenido);
        t.getStyleClass().addAll("term-texto", clase);
        return t;
    }

    private void esperar(double ms, Runnable despues) {
        PauseTransition pausa = new PauseTransition(Duration.millis(ms));
        pausa.setOnFinished(e -> despues.run());
        pausa.play();
    }
}
