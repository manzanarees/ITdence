package com.itdence;

import com.itdence.controlador.Componentes;
import com.itdence.controlador.Navegacion;
import com.itdence.controlador.PantallaBienvenida;
import com.itdence.controlador.Tema;
import com.itdence.dao.ConexionBD;
import com.itdence.servicio.DatosIniciales;

import javafx.application.Application;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) {
        ConexionBD.getConexion();
        new DatosIniciales().cargar();
        Tema.cargarFuentes();

        stage.setTitle("ITdence");
        Componentes.ponerIconos(stage);
        stage.setMinWidth(1180);
        stage.setMinHeight(700);
        stage.setScene(PantallaBienvenida.crear(() -> Navegacion.irALogin(stage)));
        stage.centerOnScreen();
        stage.show();
    }

    @Override
    public void stop() {
        ConexionBD.cerrar();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
