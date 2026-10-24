package com.itdence.controlador;

import com.itdence.servicio.TicketServicio;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.Map;

// Donut con el reparto de tickets por estado y, al lado, cada estado con su numero y su porcentaje.
public class PanelDonut extends VBox {

    private static final double TAMANO = 148;
    private static final double RADIO = 59;

    private final Map<String, Arc> arcos = new HashMap<>();
    private final Map<String, Label> numeros = new HashMap<>();
    private final Map<String, Label> porcentajes = new HashMap<>();
    private final Label totalLabel = new Label("0");

    public PanelDonut() {
        Label titulo = new Label("Estado de los tickets");
        titulo.getStyleClass().add("tarjeta-titulo");

        Pane anillo = new Pane();
        anillo.setMinSize(TAMANO, TAMANO);
        anillo.setPrefSize(TAMANO, TAMANO);
        anillo.setMaxSize(TAMANO, TAMANO);
        Circle pista = new Circle(TAMANO / 2, TAMANO / 2, RADIO);
        pista.getStyleClass().add("donut-pista");
        anillo.getChildren().add(pista);

        VBox leyenda = new VBox(14);
        leyenda.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(leyenda, Priority.ALWAYS);

        for (String estado : TicketServicio.ESTADOS) {
            String color = "color-" + Componentes.claseEstado(estado);

            Arc arco = new Arc(TAMANO / 2, TAMANO / 2, RADIO, RADIO, 90, 0);
            arco.setType(ArcType.OPEN);
            arco.getStyleClass().addAll("donut-arco", color);
            anillo.getChildren().add(arco);
            arcos.put(estado, arco);

            Region punto = new Region();
            punto.getStyleClass().addAll("punto-leyenda", color);
            Label nombre = new Label(Componentes.capitalizar(estado));
            nombre.getStyleClass().add("texto-secundario");
            nombre.setMinWidth(Region.USE_PREF_SIZE);
            Label numero = new Label("0");
            numero.getStyleClass().add("donut-numero");
            numero.setMinWidth(Region.USE_PREF_SIZE);
            Label porcentaje = new Label("0%");
            porcentaje.getStyleClass().add("texto-tenue");
            porcentaje.setMinWidth(36);
            porcentaje.setAlignment(Pos.CENTER_RIGHT);
            numeros.put(estado, numero);
            porcentajes.put(estado, porcentaje);

            Region espacio = new Region();
            HBox.setHgrow(espacio, Priority.ALWAYS);
            HBox fila = new HBox(8, punto, nombre, espacio, numero, porcentaje);
            fila.setAlignment(Pos.CENTER_LEFT);
            leyenda.getChildren().add(fila);
        }

        totalLabel.getStyleClass().add("donut-total");
        Label totalTexto = new Label("tickets");
        totalTexto.getStyleClass().add("texto-secundario");
        VBox centro = new VBox(0, totalLabel, totalTexto);
        centro.setAlignment(Pos.CENTER);
        centro.setMouseTransparent(true);
        StackPane donut = new StackPane(anillo, centro);

        HBox contenido = new HBox(20, donut, leyenda);
        contenido.setAlignment(Pos.CENTER_LEFT);
        VBox.setVgrow(contenido, Priority.ALWAYS);

        getChildren().addAll(titulo, contenido);
        setSpacing(14);
        getStyleClass().addAll("tarjeta", "tarjeta-padding");
    }

    public void setDatos(Map<String, Integer> conteo) {
        int total = 0;
        for (int n : conteo.values()) {
            total += n;
        }
        Animaciones.contar(totalLabel, total);
        double inicio = 90;
        for (String estado : TicketServicio.ESTADOS) {
            int n = conteo.getOrDefault(estado, 0);
            double longitud = total == 0 ? 0 : -360.0 * n / total;

            Arc arco = arcos.get(estado);
            arco.setStartAngle(inicio);
            arco.setVisible(n > 0);
            new Timeline(new KeyFrame(Duration.millis(700),
                    new KeyValue(arco.lengthProperty(), longitud, Interpolator.EASE_OUT))).play();
            inicio += longitud;

            Animaciones.contar(numeros.get(estado), n);
            porcentajes.get(estado).setText(total == 0 ? "0%" : Math.round(100.0 * n / total) + "%");
        }
    }
}
