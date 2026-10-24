package com.itdence.controlador;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public class PanelMetricas extends HBox {

    private final Map<String, VBox> items = new LinkedHashMap<>();
    private final Map<String, Label> numeros = new HashMap<>();
    private Consumer<String> alSeleccionar;

    public PanelMetricas() {
        setAlignment(Pos.CENTER_LEFT);
        getStyleClass().add("tarjeta");
    }

    public void agregar(String clave, String titulo, String claseColor) {
        if (!items.isEmpty()) {
            Region raya = new Region();
            raya.getStyleClass().add("metrica-raya");
            getChildren().add(raya);
        }

        Region punto = new Region();
        punto.getStyleClass().addAll("punto-leyenda", claseColor);
        Label nombre = new Label(titulo);
        nombre.getStyleClass().add("texto-secundario");
        HBox nombreFila = new HBox(7, punto, nombre);
        nombreFila.setAlignment(Pos.CENTER_LEFT);

        Label numero = new Label("0");
        numero.getStyleClass().add("leyenda-numero");
        numeros.put(clave, numero);

        VBox item = new VBox(2, nombreFila, numero);
        item.getStyleClass().addAll("metrica", claseColor);
        item.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(item, Priority.ALWAYS);
        item.setOnMouseClicked(e -> {
            if (alSeleccionar != null) {
                alSeleccionar.accept(clave);
            }
        });
        items.put(clave, item);
        getChildren().add(item);
    }

    public void alSeleccionar(Consumer<String> accion) {
        this.alSeleccionar = accion;
        for (VBox item : items.values()) {
            item.getStyleClass().add("metrica-clicable");
        }
    }

    public void setValor(String clave, int valor) {
        Label numero = numeros.get(clave);
        if (numero != null) {
            Animaciones.contar(numero, valor);
        }
    }

    public void marcar(String claveSeleccionada) {
        for (Map.Entry<String, VBox> e : items.entrySet()) {
            e.getValue().getStyleClass().remove("metrica-seleccionada");
            if (e.getKey().equals(claveSeleccionada)) {
                e.getValue().getStyleClass().add("metrica-seleccionada");
            }
        }
    }
}
