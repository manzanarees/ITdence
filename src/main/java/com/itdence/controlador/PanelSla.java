package com.itdence.controlador;

import com.itdence.servicio.TicketServicio;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

// el % de tickets resueltos

public class PanelSla extends VBox {

    private final Label porcentajeLabel = new Label("–");
    private final Label explicacionLabel = new Label();
    private final Label vencidosLabel = new Label();
    private final Label enRiesgoLabel = new Label();
    private final HBox vencidosFila;
    private final HBox enRiesgoFila;

    public PanelSla() {
        Label titulo = new Label("Cumplimiento SLA");
        titulo.getStyleClass().add("tarjeta-titulo");

        porcentajeLabel.getStyleClass().add("sla-porcentaje");
        explicacionLabel.getStyleClass().add("texto-secundario");
        explicacionLabel.setWrapText(true);

        vencidosFila = crearFila("bandera", vencidosLabel);
        enRiesgoFila = crearFila("reloj", enRiesgoLabel);

        getChildren().addAll(titulo, new VBox(2, porcentajeLabel, explicacionLabel), new VBox(8, vencidosFila, enRiesgoFila));
        setSpacing(14);
        getStyleClass().addAll("tarjeta", "tarjeta-padding");
    }

    private HBox crearFila(String icono, Label texto) {
        HBox fila = new HBox(8, Iconos.crear(icono, 16), texto);
        fila.setAlignment(Pos.CENTER_LEFT);
        return fila;
    }

    public void setDatos(TicketServicio.ResumenSla sla) {
        if (sla.porcentajeCumplido() < 0) {
            porcentajeLabel.setText("–");
            explicacionLabel.setText("Todavía no hay tickets resueltos con los que calcularlo.");
        } else {
            Animaciones.contar(porcentajeLabel, sla.porcentajeCumplido(), "%");
            explicacionLabel.setText(sla.resueltosEnPlazo() + " de " + sla.resueltos()
                    + (sla.resueltos() == 1 ? " ticket resuelto" : " tickets resueltos") + " dentro de plazo");
        }

        vencidosLabel.setText(sla.vencidos() == 1 ? "1 ticket vencido" : sla.vencidos() + " tickets vencidos");
        enRiesgoLabel.setText(sla.enRiesgo() == 1 ? "1 a punto de vencer" : sla.enRiesgo() + " a punto de vencer");
        marcar(vencidosFila, "sla-alerta-roja", sla.vencidos() > 0);
        marcar(enRiesgoFila, "sla-alerta-ambar", sla.enRiesgo() > 0);
    }

    private void marcar(HBox fila, String clase, boolean activo) {
        fila.getStyleClass().remove(clase);
        if (activo) {
            fila.getStyleClass().add(clase);
        }
    }
}
