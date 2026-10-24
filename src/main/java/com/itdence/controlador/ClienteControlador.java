package com.itdence.controlador;

import com.itdence.modelo.Ticket;
import com.itdence.modelo.Usuario;
import com.itdence.servicio.TicketServicio;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;


public class ClienteControlador extends ControladorBase {

    private final TicketServicio ticketServicio = new TicketServicio();

    private final ObservableList<Ticket> tickets = FXCollections.observableArrayList();
    private final FilteredList<Ticket> filtrados = new FilteredList<>(tickets);
    private final TableView<Ticket> tabla = new TableView<>();
    private final TextField buscarField = new TextField();
    private final PanelMetricas panelEstados = new PanelMetricas();
    // franja que avisa de los tickets resueltos que esperan la respuesta del cliente
    private HBox avisoConfirmar;
    private Label avisoTitulo;

    // estado por el que se esta filtrando al pulsar una tarjeta (null = todos)
    private String estadoFiltro = null;

    public ClienteControlador(Stage stage, Usuario usuario) {
        super(stage, usuario);
    }

    @Override
    public Scene crearEscena() {
        agregarOpcionMenu("Mis tickets", "ticket", () -> mostrarVista(crearVista()));
        Scene scene = crearLayoutPrincipal();
        mostrarVista(crearVista());
        return scene;
    }

    private VBox crearVista() {
        Button nuevoButton = new Button("Nuevo ticket");
        nuevoButton.setGraphic(Iconos.crear("mas", 18));
        nuevoButton.getStyleClass().add("boton-grande");
        nuevoButton.setOnAction(event -> abrirNuevoTicket());

        String primerNombre = usuario.getNombre().split(" ")[0];
        HBox cabecera = crearCabecera("Hola, " + primerNombre,
                "Aquí tienes el estado de tus incidencias. Pulsa un estado para filtrar la lista.", nuevoButton);

        VBox vista = new VBox(22, cabecera, crearAvisoDeConfirmacion(), crearPanelEstados(), crearTarjeta("Mis tickets", crearTabla(), crearBuscador()));
        VBox.setVgrow(vista.getChildren().get(3), Priority.ALWAYS);
        refrescar();
        return vista;
    }

    // Lo que el cliente tiene que hacer
    private HBox crearAvisoDeConfirmacion() {
        avisoTitulo = new Label();
        avisoTitulo.getStyleClass().add("aviso-accion-titulo");
        Label texto = Componentes.textoTenue("Confirma si se ha solucionado o reábrelo y cuéntanos qué sigue fallando.");

        Button ver = new Button("Verlos");
        ver.getStyleClass().addAll("boton-secundario", "boton-pequeno");
        ver.setOnAction(e -> {
            if (!"resuelto".equals(estadoFiltro)) {
                filtrarPorEstado("resuelto");
            }
        });

        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        StackPane icono = new StackPane(Iconos.crear("check-circulo", 22));
        avisoConfirmar = new HBox(14, icono, new VBox(2, avisoTitulo, texto), espacio, ver);
        avisoConfirmar.setAlignment(Pos.CENTER_LEFT);
        avisoConfirmar.getStyleClass().add("aviso-accion");
        avisoConfirmar.setVisible(false);
        avisoConfirmar.setManaged(false);
        return avisoConfirmar;
    }

    private PanelMetricas crearPanelEstados() {
        for (String estado : TicketServicio.ESTADOS) {
            panelEstados.agregar(estado, Componentes.capitalizar(estado), "color-" + Componentes.claseEstado(estado));
        }
        panelEstados.alSeleccionar(this::filtrarPorEstado);
        return panelEstados;
    }

    private TextField crearBuscador() {
        buscarField.setPromptText("Buscar...");
        buscarField.getStyleClass().add("buscador");
        buscarField.setPrefWidth(240);
        buscarField.textProperty().addListener((obs, antes, ahora) -> aplicarFiltro());
        return buscarField;
    }

    private TableView<Ticket> crearTabla() {
        tabla.getColumns().clear();
        SortedList<Ticket> ordenados = new SortedList<>(filtrados);
        ordenados.comparatorProperty().bind(tabla.comparatorProperty());
        tabla.setItems(ordenados);

        tabla.getColumns().add(Componentes.columnaTicket());
        tabla.getColumns().add(Componentes.columnaPrioridad());
        tabla.getColumns().add(Componentes.columnaEstado());
        tabla.getColumns().add(Componentes.columnaPersona("Técnico", Ticket::getTecnicoNombre, "Sin asignar"));
        tabla.getColumns().add(Componentes.columnaFecha("Creado", Ticket::getFechaCreacion));
        Componentes.configurarTabla(tabla, Componentes.estadoVacio("ticket", "No hay tickets que enseñar",
                "Cuando tengas un problema con tu equipo, pulsa \"Nuevo ticket\" y un técnico se pondrá con ello."));
        Componentes.alHacerDobleClic(tabla, this::abrirDetalle);
        return tabla;
    }

    private void filtrarPorEstado(String estado) {
        estadoFiltro = estado.equals(estadoFiltro) ? null : estado;
        panelEstados.marcar(estadoFiltro);
        aplicarFiltro();
    }

    private void aplicarFiltro() {
        String texto = buscarField.getText() == null ? "" : buscarField.getText().trim().toLowerCase();
        filtrados.setPredicate(t -> (estadoFiltro == null || estadoFiltro.equals(t.getEstado()))
                && (texto.isEmpty() || t.getTitulo().toLowerCase().contains(texto) || t.getCodigo().contains(texto)));
    }

    @Override
    protected void refrescar() {
        tickets.setAll(ticketServicio.listarMisTickets(usuario));
        for (String estado : TicketServicio.ESTADOS) {
            panelEstados.setValor(estado, (int) tickets.stream().filter(t -> t.getEstado().equals(estado)).count());
        }
        actualizarAvisoDeConfirmacion();
    }

    private void actualizarAvisoDeConfirmacion() {
        long pendientes = tickets.stream().filter(t -> "resuelto".equals(t.getEstado())).count();
        boolean mostrar = pendientes > 0;
        avisoTitulo.setText(pendientes == 1 ? "Tienes 1 ticket esperando tu confirmación"
                : "Tienes " + pendientes + " tickets esperando tu confirmación");
        if (mostrar && !avisoConfirmar.isVisible()) {
            Animaciones.entrada(avisoConfirmar);
        }
        avisoConfirmar.setVisible(mostrar);
        avisoConfirmar.setManaged(mostrar);
    }

    private void abrirNuevoTicket() {
        NuevoTicketControlador formulario = new NuevoTicketControlador(stage, usuario);
        int nuevoId = formulario.mostrar();
        if (nuevoId > 0) {
            refrescar();
            Notificaciones.exito(tabla, String.format("Ticket #%04d creado. Un técnico lo revisará pronto.", nuevoId));
            // el ticket se crea aunque una captura falle; se dice cual y por que
            for (String problema : formulario.getProblemasAdjuntos()) {
                Notificaciones.error(tabla, "No se ha podido adjuntar: " + problema);
            }
        }
    }
}
