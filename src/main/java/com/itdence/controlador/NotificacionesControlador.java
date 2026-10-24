package com.itdence.controlador;

import com.itdence.modelo.Notificacion;
import com.itdence.modelo.Usuario;
import com.itdence.servicio.NotificacionServicio;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;

// Ventana con los avisos del usuario
public class NotificacionesControlador {

    private final Stage owner;
    private final Usuario usuario;
    private final NotificacionServicio servicio = new NotificacionServicio();

    private final VBox lista = new VBox();
    private Integer ticketAbierto = null;
    private Stage ventana;

    public NotificacionesControlador(Stage owner, Usuario usuario) {
        this.owner = owner;
        this.usuario = usuario;
    }

    public Integer mostrar() {
        StackPane icono = new StackPane(Iconos.crear("campana", 22));
        icono.getStyleClass().add("modal-icono");
        Label titulo = new Label("Avisos");
        titulo.getStyleClass().add("titulo-modal");
        Label subtitulo = new Label("Lo que ha pasado en tus tickets");
        subtitulo.getStyleClass().add("texto-secundario");

        Button todasButton = new Button("Marcar todo como leído");
        todasButton.getStyleClass().addAll("boton-secundario", "boton-pequeno");
        todasButton.setOnAction(e -> {
            servicio.marcarTodasLeidas(usuario);
            rellenar();
        });

        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        HBox cabecera = new HBox(14, icono, new VBox(3, titulo, subtitulo), espacio, todasButton);
        cabecera.setAlignment(Pos.CENTER_LEFT);
        cabecera.setPadding(new Insets(24, 24, 18, 24));

        ScrollPane scroll = new ScrollPane(lista);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("scroll-transparente");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox contenido = new VBox(cabecera, scroll);
        contenido.getStyleClass().add("fondo-superficie");

        ventana = Componentes.crearVentanaModal(owner, "Avisos", contenido, 500, 580);
        rellenar();
        ventana.showAndWait();
        return ticketAbierto;
    }

    private void rellenar() {
        List<Notificacion> avisos = servicio.listar(usuario);
        lista.getChildren().clear();
        if (avisos.isEmpty()) {
            VBox vacio = Componentes.estadoVacio("campana", "No tienes avisos",
                    "Aquí aparecerá lo que pase en tus tickets: cuando alguien los tome, los responda o los cierre.");
            vacio.setPadding(new Insets(60, 30, 30, 30));
            lista.getChildren().add(vacio);
            return;
        }
        for (Notificacion n : avisos) {
            lista.getChildren().add(crearAviso(n));
        }
    }

    private HBox crearAviso(Notificacion n) {
        Region punto = new Region();
        punto.getStyleClass().add("aviso-punto");
        punto.setVisible(!n.isLeida());

        Label texto = new Label(n.getTexto());
        texto.setWrapText(true);
        texto.getStyleClass().add(n.isLeida() ? "aviso-texto-leido" : "aviso-texto");

        Label codigo = new Label(n.getTicketCodigo());
        codigo.getStyleClass().add("celda-codigo");
        Label ticket = new Label(n.getTicketTitulo());
        ticket.getStyleClass().add("texto-tenue");
        ticket.setMaxWidth(260);
        HBox ticketFila = new HBox(8, codigo, ticket);

        Label cuando = new Label(Componentes.haceCuanto(n.getFecha()));
        cuando.getStyleClass().add("comentario-fecha");
        cuando.setMinWidth(Region.USE_PREF_SIZE);

        VBox textos = new VBox(3, texto, ticketFila);
        HBox.setHgrow(textos, Priority.ALWAYS);
        HBox fila = new HBox(12, punto, textos, cuando);
        fila.setAlignment(Pos.TOP_LEFT);
        fila.getStyleClass().add("aviso-fila");
        fila.setOnMouseClicked(e -> {
            servicio.marcarLeida(n, usuario);
            ticketAbierto = n.getTicketId();
            ventana.close();
        });
        return fila;
    }
}
