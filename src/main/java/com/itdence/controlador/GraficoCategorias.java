package com.itdence.controlador;

import javafx.scene.Group;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Grafico de categorias
public class GraficoCategorias extends VBox {

    private final XYChart.Series<Number, String> serie = new XYChart.Series<>();
    private final NumberAxis ejeX = new NumberAxis();
    private final List<Text> etiquetas = new ArrayList<>();

    public GraficoCategorias() {
        Label titulo = new Label("Tickets por categoría");
        titulo.getStyleClass().add("tarjeta-titulo");

        ejeX.setTickLabelsVisible(false);
        ejeX.setTickMarkVisible(false);
        ejeX.setMinorTickVisible(false);
        ejeX.setAnimated(false);
        ejeX.setAutoRanging(false);
        ejeX.setLowerBound(0);

        CategoryAxis ejeY = new CategoryAxis();
        ejeY.setTickMarkVisible(false);
        ejeY.setAnimated(false);

        BarChart<Number, String> grafico = new BarChart<>(ejeX, ejeY);
        grafico.setLegendVisible(false);
        grafico.setAnimated(false);
        grafico.setCategoryGap(16);
        grafico.setHorizontalGridLinesVisible(false);
        grafico.setVerticalGridLinesVisible(false);
        grafico.setMinHeight(210);
        grafico.getStyleClass().add("grafico-categorias");
        grafico.getData().add(serie);
        VBox.setVgrow(grafico, Priority.ALWAYS);

        getChildren().addAll(titulo, grafico);
        setSpacing(10);
        getStyleClass().addAll("tarjeta", "tarjeta-padding");
    }

    public void setDatos(Map<String, Integer> porCategoria) {
        for (Text t : etiquetas) {
            if (t.getParent() instanceof Group) {
                ((Group) t.getParent()).getChildren().remove(t);
            }
        }
        etiquetas.clear();

        int maximo = 1;
        for (int n : porCategoria.values()) {
            maximo = Math.max(maximo, n);
        }
        ejeX.setUpperBound(maximo * 1.2 + 0.5);

        List<Map.Entry<String, Integer>> entradas = new ArrayList<>(porCategoria.entrySet());
        java.util.Collections.reverse(entradas);
        List<XYChart.Data<Number, String>> datos = new ArrayList<>();
        for (Map.Entry<String, Integer> e : entradas) {
            XYChart.Data<Number, String> dato = new XYChart.Data<>(e.getValue(), e.getKey());
            ponerNumeroAlFinal(dato, e.getValue());
            datos.add(dato);
        }
        serie.getData().setAll(datos);
    }

    private void ponerNumeroAlFinal(XYChart.Data<Number, String> dato, int valor) {
        dato.nodeProperty().addListener((obs, antes, barra) -> {
            if (barra == null) {
                return;
            }
            barra.parentProperty().addListener((o, padreAntes, padre) -> {
                if (!(padre instanceof Group)) {
                    return;
                }
                Text numero = new Text(String.valueOf(valor));
                numero.getStyleClass().add("grafico-numero-barra");
                ((Group) padre).getChildren().add(numero);
                etiquetas.add(numero);
                barra.boundsInParentProperty().addListener((ob, viejo, b) -> {
                    numero.setLayoutX(b.getMaxX() + 8);
                    numero.setLayoutY(b.getMinY() + b.getHeight() / 2 + numero.getBoundsInLocal().getHeight() / 4);
                });
            });
        });
    }
}
