package com.itdence.controlador;

import com.itdence.modelo.Ticket;
import com.itdence.modelo.Usuario;
import com.itdence.servicio.TicketServicio;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;

public class AdminControlador extends ControladorBase {

    private static final int DIAS_DE_ACTIVIDAD = 14;

    private final TicketServicio ticketServicio = new TicketServicio();

    private final ObservableList<Ticket> tickets = FXCollections.observableArrayList();
    private final ObservableList<Ticket> atencion = FXCollections.observableArrayList();

    private final PanelDonut panelDonut = new PanelDonut();
    private final PanelSla panelSla = new PanelSla();
    private final PanelCalidad panelCalidad = new PanelCalidad();
    private final GraficoActividad graficoActividad = new GraficoActividad(DIAS_DE_ACTIVIDAD);
    private final GraficoCategorias graficoCategorias = new GraficoCategorias();
    private final Label tituloAtencion = new Label("Requieren atención");

    private GestionUsuariosPanel panelUsuarios;
    private GestionCategoriasPanel panelCategorias;
    private GestionPlantillasPanel panelPlantillas;
    private Node vistaResumen;
    private VBox vistaTablero;
    private TableroKanban tablero;

    public AdminControlador(Stage stage, Usuario usuario) {
        super(stage, usuario);
    }

    @Override
    public Scene crearEscena() {
        panelUsuarios = new GestionUsuariosPanel(stage, usuario);
        panelCategorias = new GestionCategoriasPanel(stage);
        vistaResumen = crearResumen();
        tablero = new TableroKanban(usuario, this::abrirDetalle, this::refrescar);
        vistaTablero = new VBox(22, crearCabecera("Tablero",
                "Todos los tickets por estado. Arrastra una tarjeta para cambiar su estado o ábrela con un clic."), tablero);
        VBox.setVgrow(tablero, Priority.ALWAYS);
        Node vistaUsuarios = panelUsuarios.crearVista();
        Node vistaCategorias = panelCategorias.crearVista();
        panelPlantillas = new GestionPlantillasPanel(stage, usuario);
        Node vistaPlantillas = panelPlantillas.crearVista();

        agregarOpcionMenu("Resumen", "resumen", () -> {
            refrescar();
            mostrarVista(vistaResumen);
            // las tarjetas entran una detras de otra, no todas de golpe
            Animaciones.escalonada(List.of(panelDonut, panelSla, panelCalidad, graficoActividad, graficoCategorias), 70);
        });
        agregarOpcionMenu("Tablero", "tablero", () -> {
            refrescar();
            mostrarVista(vistaTablero);
        });
        agregarOpcionMenu("Usuarios", "usuarios", () -> mostrarVista(vistaUsuarios));
        agregarOpcionMenu("Categorías", "etiqueta", () -> {
            panelCategorias.refrescar();
            mostrarVista(vistaCategorias);
        });
        agregarOpcionMenu("Respuestas", "rayo", () -> {
            panelPlantillas.refrescar();
            mostrarVista(vistaPlantillas);
        });

        Scene scene = crearLayoutPrincipal();
        mostrarVista(vistaResumen);
        refrescar();
        return scene;
    }

    // Tres paneles con numeros, dos graficas y la lista de lo que hay que hacer hoy

    private Node crearResumen() {
        HBox cabecera = crearCabecera("Resumen", "Cómo va el soporte: los números, la evolución y lo que necesita atención ahora.");

        panelSla.setPrefWidth(230);
        panelSla.setMinWidth(190);
        panelCalidad.setPrefWidth(230);
        panelCalidad.setMinWidth(190);
        HBox.setHgrow(panelDonut, Priority.ALWAYS);
        HBox filaNumeros = new HBox(16, panelDonut, panelSla, panelCalidad);

        graficoCategorias.setPrefWidth(340);
        graficoCategorias.setMinWidth(340);
        HBox.setHgrow(graficoActividad, Priority.ALWAYS);
        HBox filaGraficas = new HBox(16, graficoActividad, graficoCategorias);

        VBox contenido = new VBox(22, cabecera, filaNumeros, filaGraficas, crearTablaDeAtencion());
        contenido.setPadding(new javafx.geometry.Insets(0, 14, 10, 0));

        ScrollPane scroll = new ScrollPane(contenido);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("scroll-transparente");
        return scroll;
    }

    private Node crearTablaDeAtencion() {
        TableView<Ticket> tabla = new TableView<>();
        SortedList<Ticket> ordenados = new SortedList<>(atencion);
        ordenados.comparatorProperty().bind(tabla.comparatorProperty());
        tabla.setItems(ordenados);
        tabla.getColumns().add(Componentes.columnaTicket());
        tabla.getColumns().add(Componentes.columnaPrioridad());
        tabla.getColumns().add(Componentes.columnaEstado());
        tabla.getColumns().add(Componentes.columnaSla());
        tabla.getColumns().add(Componentes.columnaPersona("Técnico", Ticket::getTecnicoNombre, "Sin asignar"));
        Componentes.configurarTabla(tabla, Componentes.estadoVacio("check-circulo", "Todo bajo control",
                "No hay tickets con el plazo vencido, a punto de vencer o sin técnico."));
        Componentes.alHacerDobleClic(tabla, this::abrirDetalle);
        tabla.setPrefHeight(300);
        tabla.setMinHeight(220);

        tituloAtencion.getStyleClass().add("tarjeta-titulo");
        HBox cabeceraTabla = new HBox(tituloAtencion);
        cabeceraTabla.getStyleClass().add("tarjeta-cabecera");
        VBox tarjeta = new VBox(cabeceraTabla, tabla);
        tarjeta.getStyleClass().add("tarjeta");
        return tarjeta;
    }

    @Override
    protected void refrescar() {
        panelDonut.setDatos(ticketServicio.contarPorEstado());
        panelSla.setDatos(ticketServicio.resumenSla());
        panelCalidad.setDatos(ticketServicio.resumenCalidad());
        graficoActividad.setDatos(ticketServicio.actividadPorDia(DIAS_DE_ACTIVIDAD));
        graficoCategorias.setDatos(ticketServicio.contarPorCategoria());

        atencion.setAll(ticketServicio.requierenAtencion());
        tituloAtencion.setText("Requieren atención (" + atencion.size() + ")");
        tickets.setAll(ticketServicio.listarTodos());
        tablero.setTickets(tickets);
    }
}
