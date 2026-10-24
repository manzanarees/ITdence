package com.itdence.controlador;

import com.itdence.modelo.Sla;
import com.itdence.servicio.TicketServicio;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.Duration;
import java.util.Locale;

// Panel del resumen del admin con la calidad
public class PanelCalidad extends VBox {

    private final Label notaLabel = new Label("–");
    private final HBox estrellasHueco = new HBox();
    private final Label valoracionesLabel = new Label();
    private final Label tiempoLabel = new Label();
    private final Label reaperturasLabel = new Label();

    public PanelCalidad() {
        Label titulo = new Label("Calidad del servicio");
        titulo.getStyleClass().add("tarjeta-titulo");

        notaLabel.getStyleClass().add("sla-porcentaje");
        Label sobreCinco = new Label("/ 5");
        sobreCinco.getStyleClass().add("texto-secundario");
        HBox nota = new HBox(6, notaLabel, sobreCinco);
        nota.setAlignment(Pos.BASELINE_LEFT);
        valoracionesLabel.getStyleClass().add("texto-secundario");

        getChildren().addAll(titulo, new VBox(4, nota, estrellasHueco, valoracionesLabel),
                new VBox(8, fila("reloj", tiempoLabel), fila("recargar", reaperturasLabel)));
        setSpacing(14);
        getStyleClass().addAll("tarjeta", "tarjeta-padding");
    }

    private HBox fila(String icono, Label texto) {
        HBox fila = new HBox(8, Iconos.crear(icono, 16), texto);
        fila.setAlignment(Pos.CENTER_LEFT);
        return fila;
    }

    public void setDatos(TicketServicio.ResumenCalidad calidad) {
        if (calidad.valoraciones() == 0) {
            notaLabel.setText("–");
            estrellasHueco.getChildren().setAll(Componentes.estrellas(0, 16));
            valoracionesLabel.setText("Todavía no hay valoraciones");
        } else {
            notaLabel.setText(String.format(new Locale("es", "ES"), "%.1f", calidad.mediaValoracion()));
            estrellasHueco.getChildren().setAll(Componentes.estrellas((int) Math.round(calidad.mediaValoracion()), 16));
            valoracionesLabel.setText(calidad.valoraciones() == 1 ? "1 valoración" : calidad.valoraciones() + " valoraciones");
        }
        tiempoLabel.setText(calidad.minutosMedios() < 0 ? "Sin tickets resueltos todavía"
                : Sla.duracion(Duration.ofMinutes(calidad.minutosMedios())) + " de media");
        reaperturasLabel.setText(calidad.reaperturas() == 1 ? "1 ticket reabierto" : calidad.reaperturas() + " tickets reabiertos");
    }
}
