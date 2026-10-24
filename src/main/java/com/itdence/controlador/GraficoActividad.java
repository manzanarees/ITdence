package com.itdence.controlador;

import com.itdence.servicio.TicketServicio;
import javafx.geometry.Pos;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// Grafico de lineas con los tickets creados y resueltos cada dia. Es el LineChart de JavaFX
// (con ejes, cuadricula y puntos) con los colores de la app puestos desde el CSS.
public class GraficoActividad extends VBox {

    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("d/M");
    private static final DateTimeFormatter MES = DateTimeFormatter.ofPattern("MMM", new Locale("es", "ES"));

    private final XYChart.Series<String, Number> creados = new XYChart.Series<>();
    private final XYChart.Series<String, Number> resueltos = new XYChart.Series<>();
    private final Label resumenLabel = new Label();

    public GraficoActividad(int dias) {
        Label titulo = new Label("Últimos " + dias + " días");
        titulo.getStyleClass().add("tarjeta-titulo");
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        resumenLabel.getStyleClass().add("texto-secundario");
        HBox cabecera = new HBox(12, titulo, espacio, leyenda("color-acento", "Creados"), leyenda("color-resuelto", "Resueltos"));
        cabecera.setAlignment(Pos.CENTER_LEFT);

        CategoryAxis ejeX = new CategoryAxis();
        ejeX.setAnimated(false);
        ejeX.setTickMarkVisible(false);
        ejeX.setTickLabelRotation(0);

        NumberAxis ejeY = new NumberAxis();
        ejeY.setAnimated(false);
        ejeY.setForceZeroInRange(true);
        ejeY.setMinorTickVisible(false);
        ejeY.setTickMarkVisible(false);
        // solo numeros enteros: no existen "0,5 tickets"
        ejeY.setTickLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Number n) {
                return n.doubleValue() == Math.rint(n.doubleValue()) ? String.valueOf(n.intValue()) : "";
            }

            @Override
            public Number fromString(String texto) {
                return null;
            }
        });

        LineChart<String, Number> grafico = new LineChart<>(ejeX, ejeY);
        grafico.setLegendVisible(false);
        grafico.setAnimated(false);
        grafico.setVerticalGridLinesVisible(false);
        grafico.setMinHeight(210);
        grafico.getStyleClass().add("grafico-actividad");
        grafico.getData().add(creados);
        grafico.getData().add(resueltos);
        VBox.setVgrow(grafico, Priority.ALWAYS);

        getChildren().addAll(cabecera, resumenLabel, grafico);
        setSpacing(6);
        getStyleClass().addAll("tarjeta", "tarjeta-padding");
    }

    private HBox leyenda(String claseColor, String texto) {
        Region punto = new Region();
        punto.getStyleClass().addAll("punto-leyenda", claseColor);
        Label label = new Label(texto);
        label.setMinWidth(Region.USE_PREF_SIZE);
        label.getStyleClass().add("texto-secundario");
        HBox caja = new HBox(6, punto, label);
        caja.setAlignment(Pos.CENTER_LEFT);
        return caja;
    }

    public void setDatos(List<TicketServicio.ActividadDia> datos) {
        List<XYChart.Data<String, Number>> puntosCreados = new ArrayList<>();
        List<XYChart.Data<String, Number>> puntosResueltos = new ArrayList<>();
        int totalCreados = 0;
        int totalResueltos = 0;
        for (int i = 0; i < datos.size(); i++) {
            TicketServicio.ActividadDia d = datos.get(i);
            String dia = etiquetaDelEje(d.dia(), i == 0);
            puntosCreados.add(new XYChart.Data<>(dia, d.creados()));
            puntosResueltos.add(new XYChart.Data<>(dia, d.resueltos()));
            totalCreados += d.creados();
            totalResueltos += d.resueltos();
        }
        creados.getData().setAll(puntosCreados);
        resueltos.getData().setAll(puntosResueltos);

        // al pasar el raton por un punto sale el dia y el numero
        for (int i = 0; i < datos.size(); i++) {
            String dia = DIA.format(datos.get(i).dia());
            ponerTooltip(puntosCreados.get(i), datos.get(i).creados() + (datos.get(i).creados() == 1 ? " creado" : " creados") + " el " + dia);
            ponerTooltip(puntosResueltos.get(i), datos.get(i).resueltos() + (datos.get(i).resueltos() == 1 ? " resuelto" : " resueltos") + " el " + dia);
        }
        resumenLabel.setText(totalCreados + " creados y " + totalResueltos + " resueltos en este periodo");
    }

    // En el eje solo el numero del dia (con 14 fechas completas el eje las giraba 90 grados y no se leian);
    // el primer dia y el 1 de cada mes llevan tambien el mes
    private String etiquetaDelEje(LocalDate dia, boolean primero) {
        if (primero || dia.getDayOfMonth() == 1) {
            return dia.getDayOfMonth() + " " + MES.format(dia).replace(".", "");
        }
        return String.valueOf(dia.getDayOfMonth());
    }

    // El nodo del punto no existe hasta que el grafico se dibuja, por eso el listener
    private void ponerTooltip(XYChart.Data<String, Number> punto, String texto) {
        if (punto.getNode() != null) {
            Tooltip.install(punto.getNode(), new Tooltip(texto));
        }
        punto.nodeProperty().addListener((obs, antes, nodo) -> {
            if (nodo != null) {
                Tooltip.install(nodo, new Tooltip(texto));
            }
        });
    }
}
