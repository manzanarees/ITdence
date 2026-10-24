package com.itdence.controlador;

import com.itdence.modelo.Usuario;
import javafx.animation.FadeTransition;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;

public final class Navegacion {

    private Navegacion() {}

    public static void irALogin(Stage stage) {
        cambiarEscena(stage, new LoginControlador(stage).crearEscena());
    }

    public static void abrirPanel(Stage stage, Usuario usuario) {
        ControladorBase panel;
        switch (usuario.getRol()) {
            case "cliente":
                panel = new ClienteControlador(stage, usuario);
                break;
            case "tecnico":
                panel = new TecnicoControlador(stage, usuario);
                break;
            case "admin":
                panel = new AdminControlador(stage, usuario);
                break;
            default:
                throw new IllegalStateException("Rol desconocido: " + usuario.getRol());
        }
        cambiarEscena(stage, panel.crearEscena());
    }

    private static void cambiarEscena(Stage stage, Scene scene) {
        stage.setScene(scene);
        Rectangle2D pantalla = Screen.getPrimary().getVisualBounds();
        boolean noCabe = scene.getWidth() > pantalla.getWidth() - 40 || scene.getHeight() > pantalla.getHeight() - 60;
        if (noCabe) {
            stage.setMaximized(true);
        } else if (!stage.isMaximized()) {
            stage.sizeToScene();
            stage.centerOnScreen();
        }
        FadeTransition fundido = new FadeTransition(Duration.millis(250), scene.getRoot());
        fundido.setFromValue(0);
        fundido.setToValue(1);
        fundido.play();
    }
}
