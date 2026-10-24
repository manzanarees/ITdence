package com.itdence.controlador;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Font;
import javafx.stage.Window;

import java.io.InputStream;

// Fuentes y tema claro/oscuro
public final class Tema {

    private static final String[] FUENTES = {
            "Archivo-Bold.ttf", "Archivo-ExtraBold.ttf",
            "PublicSans-Regular.ttf", "PublicSans-SemiBold.ttf", "PublicSans-Bold.ttf",
            "IBMPlexMono-Regular.ttf", "IBMPlexMono-Medium.ttf"
    };

    private static boolean oscuro = false;

    private Tema() {}

    public static void cargarFuentes() {
        for (String fuente : FUENTES) {
            try (InputStream in = Tema.class.getResourceAsStream("/fonts/" + fuente)) {
                if (in != null) {
                    Font.loadFont(in, 13);
                }
            } catch (Exception e) {
                System.err.println("No se ha podido cargar la fuente " + fuente);
            }
        }
    }

    public static void aplicar(Scene scene) {
        scene.getStylesheets().add(Tema.class.getResource("/styles.css").toExternalForm());
        aplicarClase(scene.getRoot());
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.TAB && !scene.getRoot().getStyleClass().contains("teclado")) {
                scene.getRoot().getStyleClass().add("teclado");
            }
        });
        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> scene.getRoot().getStyleClass().remove("teclado"));
    }

    public static void aplicarClase(Parent raiz) {
        raiz.getStyleClass().remove("oscuro");
        if (oscuro) {
            raiz.getStyleClass().add("oscuro");
        }
    }

    public static boolean esOscuro() {
        return oscuro;
    }

    public static void alternar() {
        oscuro = !oscuro;
        for (Window ventana : Window.getWindows()) {
            if (ventana.getScene() != null) {
                aplicarClase(ventana.getScene().getRoot());
            }
        }
    }
}
