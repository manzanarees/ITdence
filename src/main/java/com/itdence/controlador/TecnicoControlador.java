package com.itdence.controlador;

import com.itdence.modelo.Categoria;
import com.itdence.modelo.Ticket;
import com.itdence.modelo.Usuario;
import com.itdence.servicio.CategoriaServicio;
import com.itdence.servicio.TicketServicio;
import com.itdence.servicio.ValidacionException;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class TecnicoControlador extends ControladorBase {

    private final TicketServicio ticketServicio = new TicketServicio();

    // Las listas
    private final ObservableList<Ticket> sinAsignar = FXCollections.observableArrayList();
    private final ObservableList<Ticket> asignados = FXCollections.observableArrayList();
    private final FilteredList<Ticket> sinAsignarFiltrados = new FilteredList<>(sinAsignar);
    private final FilteredList<Ticket> asignadosFiltrados = new FilteredList<>(asignados);

    private final TextField buscarField = new TextField();
    private final ComboBox<String> estadoFiltro = new ComboBox<>();
    private final ComboBox<String> prioridadFiltro = new ComboBox<>();
    private final ComboBox<String> categoriaFiltro = new ComboBox<>();

    private final PanelMetricas metricas = new PanelMetricas();

    private TableroKanban tablero;
    private Node vistaLista;
    private VBox vistaTablero;
    private Node vistaActual;

    private Label contadorSinAsignar;
    private Label contadorAsignados;
    private TableView<Ticket> tablaSinAsignar;
    private TableView<Ticket> tablaAsignados;
    private final StackPane huecoTabla = new StackPane();

    public TecnicoControlador(Stage stage, Usuario usuario) {
        super(stage, usuario);
    }

    @Override
    public Scene crearEscena() {
        tablaSinAsignar = crearTabla(sinAsignarFiltrados, true);
        tablaAsignados = crearTabla(asignadosFiltrados, false);

        tablero = new TableroKanban(usuario, this::abrirDetalle, this::refrescar);
        vistaLista = crearVista();
        vistaTablero = new VBox(22, crearCabecera("Tablero",
                "Todos tus tickets y los que están sin asignar, por estado. Haz clic en una tarjeta para abrirla."), tablero);
        VBox.setVgrow(tablero, Priority.ALWAYS);

        contadorSinAsignar = agregarOpcionMenu("Sin asignar", "bandeja", () -> irALista(true));
        contadorAsignados = agregarOpcionMenu("Asignados a mí", "usuario", () -> irALista(false));
        agregarOpcionMenu("Tablero", "tablero", () -> {
            mostrarVista(vistaTablero);
            vistaActual = vistaTablero;
        });

        Scene scene = crearLayoutPrincipal();
        mostrarVista(vistaLista);
        vistaActual = vistaLista;
        cambiarTabla(true);
        refrescar();
        return scene;
    }

    private VBox crearVista() {
        HBox cabecera = crearCabecera("Bandeja de tickets",
                "Coge los tickets nuevos y ve actualizando los tuyos. Doble clic para ver el detalle.");

        metricas.agregar("sinAsignar", "Sin asignar", "color-abierto");
        metricas.agregar("urgentes", "Urgentes sin asignar", "color-alta");
        metricas.agregar("enProgreso", "Mis tickets en progreso", "color-progreso");
        metricas.agregar("resueltos", "Mis tickets resueltos", "color-resuelto");

        VBox tablaTarjeta = new VBox(crearFiltros(), huecoTabla);
        tablaTarjeta.getStyleClass().add("tarjeta");
        VBox.setVgrow(huecoTabla, Priority.ALWAYS);

        VBox vista = new VBox(22, cabecera, metricas, tablaTarjeta);
        VBox.setVgrow(tablaTarjeta, Priority.ALWAYS);
        return vista;
    }

    private void irALista(boolean mostrarSinAsignar) {
        if (vistaActual != vistaLista) {
            mostrarVista(vistaLista);
            vistaActual = vistaLista;
        }
        cambiarTabla(mostrarSinAsignar);
    }

    private void cambiarTabla(boolean mostrarSinAsignar) {
        Node tabla = mostrarSinAsignar ? tablaSinAsignar : tablaAsignados;
        huecoTabla.getChildren().setAll(tabla);
        Animaciones.entrada(tabla);
    }

    private HBox crearFiltros() {
        buscarField.setPromptText("Buscar...");
        buscarField.getStyleClass().add("buscador");
        buscarField.setPrefWidth(200);
        HBox.setHgrow(buscarField, Priority.ALWAYS);

        estadoFiltro.getItems().setAll("Estado");
        estadoFiltro.getItems().addAll(TicketServicio.ESTADOS);
        estadoFiltro.setConverter(Componentes.convertidorCapitalizado());

        prioridadFiltro.getItems().setAll("Prioridad");
        prioridadFiltro.getItems().addAll(TicketServicio.PRIORIDADES);
        prioridadFiltro.setConverter(Componentes.convertidorCapitalizado());

        categoriaFiltro.getItems().setAll("Categoría");
        for (Categoria c : new CategoriaServicio().listarTodas()) {
            categoriaFiltro.getItems().add(c.getNombre());
        }

        for (ComboBox<String> combo : List.of(estadoFiltro, prioridadFiltro, categoriaFiltro)) {
            combo.getSelectionModel().selectFirst();
            combo.getStyleClass().add("filtro");
            combo.valueProperty().addListener((obs, antes, ahora) -> aplicarFiltros());
        }
        buscarField.textProperty().addListener((obs, antes, ahora) -> aplicarFiltros());

        Button limpiarButton = new Button();
        limpiarButton.setGraphic(Iconos.crear("cerrar", 16));
        limpiarButton.getStyleClass().add("boton-icono");
        limpiarButton.setTooltip(new Tooltip("Quitar filtros"));
        limpiarButton.setOnAction(event -> {
            buscarField.clear();
            estadoFiltro.getSelectionModel().selectFirst();
            prioridadFiltro.getSelectionModel().selectFirst();
            categoriaFiltro.getSelectionModel().selectFirst();
        });

        for (ComboBox<String> combo : List.of(estadoFiltro, prioridadFiltro, categoriaFiltro)) {
            combo.setPrefWidth(150);
            combo.setMinWidth(130);
        }
        HBox filtros = new HBox(10, Iconos.crear("filtro", 18), buscarField, estadoFiltro, prioridadFiltro, categoriaFiltro, limpiarButton);
        filtros.setAlignment(Pos.CENTER_LEFT);
        filtros.getStyleClass().add("tarjeta-cabecera");
        return filtros;
    }

    private TableView<Ticket> crearTabla(FilteredList<Ticket> datos, boolean esSinAsignar) {
        TableView<Ticket> tabla = new TableView<>();
        SortedList<Ticket> ordenados = new SortedList<>(datos);
        ordenados.comparatorProperty().bind(tabla.comparatorProperty());
        tabla.setItems(ordenados);

        tabla.getColumns().add(Componentes.columnaTicket());
        tabla.getColumns().add(Componentes.columnaPrioridad());
        if (esSinAsignar) {
            tabla.getColumns().add(Componentes.columnaPersona("Creado por", Ticket::getCreadorNombre, "-"));
            tabla.getColumns().add(Componentes.columnaSla());
            tabla.getColumns().add(crearColumnaTomar());
            Componentes.configurarTabla(tabla, Componentes.estadoVacio("check-circulo", "¡Todo al día!",
                    "No hay tickets esperando a un técnico."));
        } else {
            tabla.getColumns().add(Componentes.columnaEstado());
            tabla.getColumns().add(Componentes.columnaPersona("Creado por", Ticket::getCreadorNombre, "-"));
            tabla.getColumns().add(Componentes.columnaFecha("Actualizado", Ticket::getFechaActualizacion));
            tabla.getColumns().add(Componentes.columnaSla());
            Componentes.configurarTabla(tabla, Componentes.estadoVacio("bandeja", "No tienes tickets asignados",
                    "Coge alguno de la bandeja \"Sin asignar\"."));
        }
        Componentes.alHacerDobleClic(tabla, this::abrirDetalle);
        return tabla;
    }

    private TableColumn<Ticket, Ticket> crearColumnaTomar() {
        TableColumn<Ticket, Ticket> columna = new TableColumn<>("");
        columna.setCellValueFactory(datos -> new ReadOnlyObjectWrapper<>(datos.getValue()));
        columna.setSortable(false);
        columna.setPrefWidth(140);
        columna.setCellFactory(c -> new TableCell<>() {
            private final Button tomarButton = new Button("Tomar ticket");

            {
                tomarButton.getStyleClass().addAll("boton-secundario", "boton-pequeno");
                tomarButton.setOnAction(event -> tomarTicket(getItem()));
            }

            @Override
            protected void updateItem(Ticket ticket, boolean vacio) {
                super.updateItem(ticket, vacio);
                setText(null);
                setGraphic(vacio || ticket == null ? null : tomarButton);
            }
        });
        return columna;
    }

    private void tomarTicket(Ticket ticket) {
        try {
            ticketServicio.tomarTicket(ticket, usuario);
            Notificaciones.exito(huecoTabla, "Te has asignado el ticket " + ticket.getCodigo() + ". Ahora está en progreso.");
        } catch (ValidacionException e) {
            Notificaciones.error(huecoTabla, e.getMessage());
        }
        refrescar();
    }

    private void aplicarFiltros() {
        Predicate<Ticket> filtro = ticket -> coincideTexto(ticket)
                && coincide(estadoFiltro, ticket.getEstado())
                && coincide(prioridadFiltro, ticket.getPrioridad())
                && coincide(categoriaFiltro, ticket.getCategoriaNombre());
        sinAsignarFiltrados.setPredicate(filtro);
        asignadosFiltrados.setPredicate(filtro);
        actualizarContadores();
    }

    private boolean coincide(ComboBox<String> combo, String valor) {
        return combo.getSelectionModel().getSelectedIndex() <= 0 || combo.getValue().equals(valor);
    }

    private boolean coincideTexto(Ticket ticket) {
        String texto = buscarField.getText();
        if (texto == null || texto.isBlank()) {
            return true;
        }
        String buscado = texto.trim().toLowerCase();
        String descripcion = ticket.getDescripcion() == null ? "" : ticket.getDescripcion().toLowerCase();
        return ticket.getTitulo().toLowerCase().contains(buscado)
                || descripcion.contains(buscado)
                || ticket.getCodigo().contains(buscado);
    }

    private void actualizarContadores() {
        contadorSinAsignar.setText(String.valueOf(sinAsignarFiltrados.size()));
        contadorAsignados.setText(String.valueOf(asignadosFiltrados.size()));
    }

    @Override
    protected void refrescar() {
        sinAsignar.setAll(ticketServicio.listarSinAsignar());
        asignados.setAll(ticketServicio.listarAsignadosA(usuario));
        actualizarContadores();

        List<Ticket> paraElTablero = new ArrayList<>(sinAsignar);
        paraElTablero.addAll(asignados);
        tablero.setTickets(paraElTablero);

        int urgentes = (int) sinAsignar.stream().filter(t -> "alta".equals(t.getPrioridad())).count();
        int enProgreso = (int) asignados.stream().filter(t -> "en progreso".equals(t.getEstado())).count();
        int resueltos = (int) asignados.stream().filter(t -> "resuelto".equals(t.getEstado()) || "cerrado".equals(t.getEstado())).count();

        metricas.setValor("sinAsignar", sinAsignar.size());
        metricas.setValor("urgentes", urgentes);
        metricas.setValor("enProgreso", enProgreso);
        metricas.setValor("resueltos", resueltos);
    }
}
