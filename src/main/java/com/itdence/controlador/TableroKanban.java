package com.itdence.controlador;

import com.itdence.modelo.Ticket;
import com.itdence.modelo.Usuario;
import com.itdence.servicio.TicketServicio;
import com.itdence.servicio.ValidacionException;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Consumer;

//Tablero tipo Kanban
public class TableroKanban extends VBox {

    private static final String POR_ESTADO = "Estado";
    private static final String POR_PRIORIDAD = "Prioridad";
    private static final String POR_TECNICO = "Técnico";

    private final TicketServicio ticketServicio = new TicketServicio();
    private final Usuario usuario;
    private final Consumer<Ticket> abrirDetalle;
    private final Runnable alCambiar;

    private final ComboBox<String> agruparCombo = new ComboBox<>();
    private final TextField buscarField = new TextField();
    private final Label ayudaLabel = new Label();
    private final HBox columnas = new HBox(14);

    private List<Ticket> tickets = new ArrayList<>();

    public TableroKanban(Usuario usuario, Consumer<Ticket> abrirDetalle, Runnable alCambiar) {
        this.usuario = usuario;
        this.abrirDetalle = abrirDetalle;
        this.alCambiar = alCambiar;

        agruparCombo.getItems().addAll(POR_ESTADO, POR_PRIORIDAD, POR_TECNICO);
        agruparCombo.setValue(POR_ESTADO);
        agruparCombo.getStyleClass().add("filtro");
        agruparCombo.valueProperty().addListener((obs, antes, ahora) -> dibujar());

        buscarField.setPromptText("Buscar...");
        buscarField.getStyleClass().add("buscador");
        buscarField.setPrefWidth(220);
        buscarField.textProperty().addListener((obs, antes, ahora) -> dibujar());

        Label agruparLabel = Componentes.etiquetaCampo("Agrupar por");
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        ayudaLabel.getStyleClass().add("texto-tenue");
        HBox barra = new HBox(10, Iconos.crear("filtro", 18), buscarField, agruparLabel, agruparCombo, espacio, ayudaLabel);
        barra.setAlignment(Pos.CENTER_LEFT);

        ScrollPane scroll = new ScrollPane(columnas);
        scroll.setFitToHeight(true);
        scroll.setFitToWidth(true);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("scroll-transparente");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        setSpacing(14);
        getChildren().addAll(barra, scroll);
    }

    public void setTickets(List<Ticket> tickets) {
        this.tickets = new ArrayList<>(tickets);
        dibujar();
    }

    private boolean agrupadoPorEstado() {
        return POR_ESTADO.equals(agruparCombo.getValue());
    }

    // Columnas

    private void dibujar() {
        String texto = buscarField.getText() == null ? "" : buscarField.getText().trim().toLowerCase();
        List<Ticket> visibles = new ArrayList<>();
        for (Ticket t : tickets) {
            String descripcion = t.getDescripcion() == null ? "" : t.getDescripcion().toLowerCase();
            if (texto.isEmpty() || t.getTitulo().toLowerCase().contains(texto)
                    || descripcion.contains(texto) || t.getCodigo().contains(texto)) {
                visibles.add(t);
            }
        }
        visibles.sort(Comparator
                .comparingInt((Ticket t) -> -Componentes.pesoPrioridad(t.getPrioridad()))
                .thenComparing(Ticket::getFechaLimite, Comparator.nullsLast(Comparator.naturalOrder())));

        ayudaLabel.setText(agrupadoPorEstado()
                ? "Arrastra una tarjeta a otra columna para cambiar su estado"
                : "Vista de solo lectura: agrupa por estado para poder arrastrar");

        columnas.getChildren().clear();
        for (Map.Entry<String, String> columna : definirColumnas(visibles).entrySet()) {
            List<Ticket> deLaColumna = new ArrayList<>();
            for (Ticket t : visibles) {
                if (claveDe(t).equals(columna.getKey())) {
                    deLaColumna.add(t);
                }
            }
            columnas.getChildren().add(crearColumna(columna.getKey(), columna.getValue(), deLaColumna));
        }
    }

    private Map<String, String> definirColumnas(List<Ticket> visibles) {
        Map<String, String> definicion = new LinkedHashMap<>();
        switch (agruparCombo.getValue()) {
            case POR_PRIORIDAD:
                for (String p : List.of("alta", "media", "baja")) {
                    definicion.put(p, Componentes.capitalizar(p));
                }
                break;
            case POR_TECNICO:
                definicion.put("", "Sin asignar");
                for (String nombre : new TreeSet<>(nombresDeTecnicos())) {
                    definicion.put(nombre, nombre);
                }
                break;
            default:
                for (String e : TicketServicio.ESTADOS) {
                    definicion.put(e, Componentes.capitalizar(e));
                }
        }
        return definicion;
    }

    private List<String> nombresDeTecnicos() {
        List<String> nombres = new ArrayList<>();
        for (Ticket t : tickets) {
            if (t.getTecnicoNombre() != null && !nombres.contains(t.getTecnicoNombre())) {
                nombres.add(t.getTecnicoNombre());
            }
        }
        return nombres;
    }

    private String claveDe(Ticket t) {
        switch (agruparCombo.getValue()) {
            case POR_PRIORIDAD: return t.getPrioridad();
            case POR_TECNICO: return t.getTecnicoNombre() == null ? "" : t.getTecnicoNombre();
            default: return t.getEstado();
        }
    }

    private String claseColor(String clave) {
        switch (agruparCombo.getValue()) {
            case POR_PRIORIDAD:
                return clave.equals("alta") ? "color-alta" : clave.equals("media") ? "color-progreso" : "color-resuelto";
            case POR_TECNICO:
                return "color-acento";
            default:
                return "color-" + Componentes.claseEstado(clave);
        }
    }

    private VBox crearColumna(String clave, String titulo, List<Ticket> deLaColumna) {
        Label tituloLabel = new Label(titulo);
        tituloLabel.getStyleClass().add("kanban-titulo");
        Label contador = new Label(String.valueOf(deLaColumna.size()));
        contador.getStyleClass().add("kanban-contador");
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        HBox cabecera = new HBox(8, tituloLabel, espacio, contador);
        cabecera.setAlignment(Pos.CENTER_LEFT);
        cabecera.getStyleClass().add("kanban-cabecera");

        VBox tarjetas = new VBox(10);
        tarjetas.setPadding(new Insets(10));
        if (deLaColumna.isEmpty()) {
            Label vacio = Componentes.textoTenue(agrupadoPorEstado() ? "Sin tickets. Suelta una tarjeta aquí." : "Sin tickets");
            vacio.setPadding(new Insets(14, 6, 6, 6));
            tarjetas.getChildren().add(vacio);
        }
        for (Ticket t : deLaColumna) {
            tarjetas.getChildren().add(crearTarjeta(t));
        }

        ScrollPane scroll = new ScrollPane(tarjetas);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("scroll-transparente");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox columna = new VBox(cabecera, scroll);
        columna.getStyleClass().addAll("kanban-columna", claseColor(clave));
        columna.setMinWidth(210);
        columna.setPrefWidth(260);
        HBox.setHgrow(columna, Priority.ALWAYS);

        if (agrupadoPorEstado()) {
            prepararDestino(columna, clave);
        }
        return columna;
    }

    // Tarjeta

    private VBox crearTarjeta(Ticket t) {
        Label titulo = new Label(t.getTitulo());
        titulo.getStyleClass().add("kanban-tarjeta-titulo");
        titulo.setWrapText(true);
        FlowPane pills = new FlowPane(6, 6);
        if (!POR_PRIORIDAD.equals(agruparCombo.getValue())) {
            pills.getChildren().add(Componentes.pillPrioridad(t.getPrioridad()));
        }
        pills.getChildren().add(Componentes.pillSla(t));

        HBox pie = new HBox(8);
        pie.setAlignment(Pos.CENTER_LEFT);
        if (t.getTecnicoNombre() != null) {
            pie.getChildren().addAll(Componentes.crearAvatar(t.getTecnicoNombre(), 22), new Label(t.getTecnicoNombre()));
        } else {
            Label sinAsignar = Componentes.textoTenue("Sin asignar");
            pie.getChildren().addAll(Iconos.crear("usuario", 16), sinAsignar);
        }
        pie.getChildren().forEach(n -> {
            if (n instanceof Label && !((Label) n).getStyleClass().contains("avatar")) {
                ((Label) n).getStyleClass().add("kanban-tarjeta-pie");
            }
        });

        VBox tarjeta = new VBox(8, titulo, Componentes.codigoYCategoria(t), pills, pie);
        tarjeta.getStyleClass().add("kanban-tarjeta");

        tarjeta.setOnMouseClicked(e -> {
            if (e.isStillSincePress()) {
                abrirDetalle.accept(t);
            }
        });
        if (agrupadoPorEstado()) {
            prepararOrigen(tarjeta, t);
        }
        Animaciones.elevarAlPasar(tarjeta);
        return tarjeta;
    }

    //Arrastrar y soltar
    private void prepararOrigen(Node tarjeta, Ticket t) {
        tarjeta.setOnDragDetected(e -> {
            Dragboard db = tarjeta.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent contenido = new ClipboardContent();
            contenido.putString(String.valueOf(t.getId()));
            db.setContent(contenido);
            SnapshotParameters parametros = new SnapshotParameters();
            parametros.setFill(Color.TRANSPARENT);
            db.setDragView(tarjeta.snapshot(parametros, null));
            tarjeta.setOpacity(0.35);
            e.consume();
        });
        tarjeta.setOnDragDone(e -> tarjeta.setOpacity(1));
    }

    private void prepararDestino(VBox columna, String estadoDestino) {
        columna.setOnDragOver(e -> {
            if (e.getGestureSource() != columna && e.getDragboard().hasString()) {
                e.acceptTransferModes(TransferMode.MOVE);
                if (!columna.getStyleClass().contains("kanban-destino")) {
                    columna.getStyleClass().add("kanban-destino");
                }
            }
            e.consume();
        });
        columna.setOnDragExited(e -> columna.getStyleClass().remove("kanban-destino"));
        columna.setOnDragDropped(e -> {
            boolean hecho = false;
            columna.getStyleClass().remove("kanban-destino");
            if (e.getDragboard().hasString()) {
                try {
                    int id = Integer.parseInt(e.getDragboard().getString());
                    Ticket ticket = tickets.stream().filter(t -> t.getId() == id).findFirst().orElse(null);
                    if (ticket != null) {
                        String antes = ticket.getEstado();
                        ticketServicio.moverEstado(ticket, estadoDestino, usuario);
                        hecho = true;
                        if (!antes.equals(estadoDestino)) {
                            Notificaciones.exito(this, ticket.getCodigo() + " movido a \"" + Componentes.capitalizar(estadoDestino) + "\"");
                        }
                    }
                } catch (ValidacionException ex) {
                    Notificaciones.error(this, ex.getMessage());
                } catch (NumberFormatException ex) {
                }
            }
            e.setDropCompleted(hecho);
            e.consume();
            Platform.runLater(alCambiar);
        });
    }
}
