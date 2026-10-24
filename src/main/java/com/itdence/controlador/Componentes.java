package com.itdence.controlador;

import com.itdence.modelo.Sla;
import com.itdence.modelo.Ticket;
import com.itdence.servicio.TicketServicio;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.function.Consumer;
import java.util.function.Function;

// Metodos estaticos que se repiten en varias pantallas
public final class Componentes {


    private static final DateTimeFormatter FORMATO_BD = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy · HH:mm");

    private static final int[] TAMANOS_ICONO = {16, 32, 48, 64, 128, 256};

    private Componentes() {}

    public static void aplicarEstilos(Scene scene) {
        Tema.aplicar(scene);
    }

    // Marca

    public static ImageView crearLogo(double tamano) {
        ImageView logo = new ImageView(new Image(Componentes.class.getResourceAsStream("/img/icon-256.png")));
        logo.setFitWidth(tamano);
        logo.setFitHeight(tamano);
        logo.setSmooth(true);
        return logo;
    }

    // Logo + ITdence
    public static HBox crearMarca(double tamanoLogo, String claseTexto) {
        Label it = new Label("IT");
        it.getStyleClass().addAll(claseTexto, "marca-it");
        Label dence = new Label("dence");
        dence.getStyleClass().addAll(claseTexto, "marca-dence");
        HBox palabra = new HBox(0, it, dence);
        palabra.setAlignment(Pos.CENTER_LEFT);

        HBox marca = new HBox(tamanoLogo * 0.3, crearLogo(tamanoLogo), palabra);
        marca.setAlignment(Pos.CENTER_LEFT);
        return marca;
    }

    // Icono de la ventana
    public static void ponerIconos(Stage stage) {
        for (int tamano : TAMANOS_ICONO) {
            stage.getIcons().add(new Image(Componentes.class.getResourceAsStream("/img/icon-" + tamano + ".png")));
        }
    }

    // Ventana modal con el icono, el tema y la animacion de entrada
    public static Stage crearVentanaModal(Window owner, String titulo, Parent contenido, double ancho, double alto) {
        Stage ventana = new Stage();
        ventana.initOwner(owner);
        ventana.initModality(Modality.WINDOW_MODAL);
        ventana.setTitle(titulo + " - ITdence");
        ponerIconos(ventana);

        StackPane raiz = new StackPane(contenido);
        Rectangle2D pantalla = Screen.getPrimary().getVisualBounds();
        Scene scene = new Scene(raiz, Math.min(ancho, pantalla.getWidth() - 20), Math.min(alto, pantalla.getHeight() - 40));
        raiz.getChildren().add(Notificaciones.crearCapa(scene));
        aplicarEstilos(scene);
        ventana.setScene(scene);
        ventana.setOnShown(e -> Animaciones.aparecer(contenido));
        return ventana;
    }

    // Etiquetas pill

    public static Label pillEstado(String estado) {
        return pillConPunto(capitalizar(estado), "pill-" + claseEstado(estado));
    }

    public static Label pillPrioridad(String prioridad) {
        return pillConPunto(capitalizar(prioridad), "pill-prio-" + prioridad);
    }

    // Etiqueta del SLA
    public static Label pillSla(Ticket ticket) {
        LocalDateTime ahora = LocalDateTime.now();
        String clase;
        switch (Sla.situacion(ticket, ahora)) {
            case VENCIDO:
            case INCUMPLIDO:
                clase = "pill-prio-alta";
                break;
            case POR_VENCER:
                clase = "pill-prio-media";
                break;
            case CUMPLIDO:
                clase = "pill-prio-baja";
                break;
            default:
                clase = "pill-neutra";
        }
        return pillConPunto(Sla.texto(ticket, ahora), clase);
    }

    public static Label pillRol(String rol) {
        return pill(capitalizar(nombreRol(rol)), "pill-rol-" + rol);
    }

    public static Label pillNeutra(String texto) {
        return pill(texto, "pill-neutra");
    }

    private static Label pill(String texto, String clase) {
        Label label = new Label(texto);
        label.getStyleClass().addAll("pill", clase);
        label.setMinWidth(Label.USE_PREF_SIZE);
        return label;
    }

    // Pill con un circulito
    private static Label pillConPunto(String texto, String clase) {
        Label label = pill(texto, clase);
        Circle punto = new Circle(3);
        punto.getStyleClass().add("punto");
        label.setGraphic(punto);
        label.setGraphicTextGap(6);
        return label;
    }

    public static String claseEstado(String estado) {
        switch (estado) {
            case "abierto": return "abierto";
            case "en progreso": return "progreso";
            case "resuelto": return "resuelto";
            default: return "cerrado";
        }
    }

    //Avatares

    public static Label crearAvatar(String nombre, double tamano) {
        Label avatar = new Label(iniciales(nombre));
        avatar.getStyleClass().addAll("avatar", "avatar-" + Math.abs(nombre == null ? 0 : nombre.hashCode()) % 5);
        avatar.setMinSize(tamano, tamano);
        avatar.setPrefSize(tamano, tamano);
        avatar.setMaxSize(tamano, tamano);
        avatar.setStyle("-fx-font-size: " + Math.round(tamano * 0.38) + "px;");
        return avatar;
    }

    // Técnico de prueba  "TP"
    public static String iniciales(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return "?";
        }
        String[] partes = nombre.trim().split("\\s+");
        String resultado = partes[0].substring(0, 1);
        if (partes.length > 1) {
            resultado += partes[partes.length - 1].substring(0, 1);
        }
        return resultado.toUpperCase();
    }

    //Textos

    public static Label etiquetaCampo(String texto) {
        Label label = new Label(texto);
        label.getStyleClass().add("etiqueta-campo");
        return label;
    }

    public static Label fechaDeHoy() {
        String hoy = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", new Locale("es", "ES")));
        Label fecha = new Label(capitalizar(hoy));
        fecha.getStyleClass().add("texto-tenue");
        return fecha;
    }

    public static HBox estrellas(int n, double tamano) {
        HBox fila = new HBox(2);
        fila.setAlignment(Pos.CENTER_LEFT);
        for (int i = 1; i <= 5; i++) {
            StackPane estrella = Iconos.crear("estrella", tamano);
            estrella.getStyleClass().add(i <= n ? "estrella-llena" : "estrella-vacia");
            fila.getChildren().add(estrella);
        }
        return fila;
    }

    public static Label textoTenue(String texto) {
        Label label = new Label(texto);
        label.getStyleClass().add("texto-tenue");
        label.setWrapText(true);
        return label;
    }

    // Lo que se ve cuando una tabla o lista esta vacia
    public static VBox estadoVacio(String icono, String titulo, String texto) {
        StackPane circulo = new StackPane(Iconos.crear(icono, 26));
        circulo.getStyleClass().add("vacio-icono");
        Label tituloLabel = new Label(titulo);
        tituloLabel.getStyleClass().add("vacio-titulo");
        Label textoLabel = textoTenue(texto);
        textoLabel.setMaxWidth(320);
        textoLabel.setStyle("-fx-text-alignment: center;");

        VBox vacio = new VBox(8, circulo, tituloLabel, textoLabel);
        vacio.setAlignment(Pos.CENTER);
        return vacio;
    }

    public static String capitalizar(String texto) {
        if (texto == null || texto.isEmpty()) {
            return "";
        }
        return texto.substring(0, 1).toUpperCase() + texto.substring(1);
    }

    public static String nombreRol(String rol) {
        switch (rol) {
            case "tecnico": return "técnico";
            case "admin": return "admin";
            default: return "cliente";
        }
    }

    public static String formatearFecha(String fechaBD) {
        return formatear(fechaBD, FORMATO_FECHA);
    }

    public static String formatearFechaHora(String fechaBD) {
        return formatear(fechaBD, FORMATO_FECHA_HORA);
    }

    private static String formatear(String fechaBD, DateTimeFormatter formato) {
        if (fechaBD == null) {
            return "-";
        }
        try {
            return LocalDateTime.parse(fechaBD, FORMATO_BD).format(formato);
        } catch (DateTimeParseException e) {
            return fechaBD;
        }
    }

    // hace 5 min etc para la lista de avisos
    public static String haceCuanto(String fechaBD) {
        if (fechaBD == null) {
            return "";
        }
        try {
            LocalDateTime fecha = LocalDateTime.parse(fechaBD, FORMATO_BD);
            long minutos = java.time.Duration.between(fecha, LocalDateTime.now()).toMinutes();
            if (minutos < 1) {
                return "ahora";
            }
            if (minutos < 60) {
                return "hace " + minutos + " min";
            }
            long horas = minutos / 60;
            if (horas < 24) {
                return "hace " + horas + " h";
            }
            long dias = horas / 24;
            return dias == 1 ? "ayer" : "hace " + dias + " d";
        } catch (DateTimeParseException e) {
            return fechaBD;
        }
    }

    public static int pesoPrioridad(String prioridad) {
        switch (prioridad) {
            case "alta": return 3;
            case "media": return 2;
            default: return 1;
        }
    }

    // En progreso
    public static StringConverter<String> convertidorCapitalizado() {
        return new StringConverter<>() {
            @Override
            public String toString(String valor) {
                return valor == null ? "" : capitalizar(valor);
            }

            @Override
            public String fromString(String texto) {
                return texto == null ? null : texto.toLowerCase();
            }
        };
    }

    //  Tablas

    public static void configurarTabla(TableView<?> tabla, Node vacio) {
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        tabla.setPlaceholder(vacio);
    }

    public static <T> void alHacerDobleClic(TableView<T> tabla, Consumer<T> accion) {
        tabla.setRowFactory(tv -> {
            TableRow<T> fila = new TableRow<>();
            fila.setOnMouseClicked(evento -> {
                if (evento.getClickCount() == 2 && !fila.isEmpty()) {
                    accion.accept(fila.getItem());
                }
            });
            return fila;
        });
    }

    public static VBox centrado(VBox caja) {
        caja.setAlignment(Pos.CENTER_LEFT);
        return caja;
    }

    // Columna Ticket
    public static TableColumn<Ticket, Ticket> columnaTicket() {
        TableColumn<Ticket, Ticket> columna = new TableColumn<>("Ticket");
        columna.setCellValueFactory(datos -> new ReadOnlyObjectWrapper<>(datos.getValue()));
        columna.setComparator(Comparator.comparingInt(Ticket::getId));
        columna.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Ticket ticket, boolean vacio) {
                super.updateItem(ticket, vacio);
                setText(null);
                if (vacio || ticket == null) {
                    setGraphic(null);
                    return;
                }
                Label titulo = new Label(ticket.getTitulo());
                titulo.getStyleClass().add("celda-titulo");
                setGraphic(centrado(new VBox(3, titulo, codigoYCategoria(ticket))));
            }
        });
        columna.setPrefWidth(290);
        return columna;
    }

    // #0001 en mono (es un id) y al lado la categoria en texto normal
    public static HBox codigoYCategoria(Ticket ticket) {
        Label codigo = new Label(ticket.getCodigo());
        codigo.getStyleClass().add("celda-codigo");
        Label categoria = new Label(ticket.getCategoriaNombre());
        categoria.getStyleClass().add("texto-tenue");
        return new HBox(8, codigo, categoria);
    }

    public static <T> TableColumn<T, String> columnaTexto(String titulo, Function<T, String> valor, double ancho) {
        TableColumn<T, String> columna = new TableColumn<>(titulo);
        columna.setCellValueFactory(datos -> new ReadOnlyStringWrapper(valor.apply(datos.getValue())));
        columna.setPrefWidth(ancho);
        return columna;
    }

    // Nombre de una persona con su avatar delante
    public static <T> TableColumn<T, String> columnaPersona(String titulo, Function<T, String> nombre, String textoSiVacio) {
        TableColumn<T, String> columna = new TableColumn<>(titulo);
        columna.setCellValueFactory(datos -> new ReadOnlyStringWrapper(nombre.apply(datos.getValue())));
        columna.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String persona, boolean vacio) {
                super.updateItem(persona, vacio);
                setText(null);
                if (vacio) {
                    setGraphic(null);
                } else if (persona == null) {
                    Label nadie = new Label(textoSiVacio);
                    nadie.getStyleClass().add("texto-tenue");
                    setGraphic(nadie);
                } else {
                    HBox caja = new HBox(8, crearAvatar(persona, 24), new Label(persona));
                    caja.setAlignment(Pos.CENTER_LEFT);
                    setGraphic(caja);
                }
            }
        });
        columna.setPrefWidth(205);
        return columna;
    }

    // Columna del SLA
    public static TableColumn<Ticket, Ticket> columnaSla() {
        TableColumn<Ticket, Ticket> columna = new TableColumn<>("SLA");
        columna.setCellValueFactory(datos -> new ReadOnlyObjectWrapper<>(datos.getValue()));
        columna.setComparator(Comparator.comparing(Ticket::getFechaLimite, Comparator.nullsLast(Comparator.naturalOrder())));
        columna.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Ticket ticket, boolean vacio) {
                super.updateItem(ticket, vacio);
                setText(null);
                setGraphic(vacio || ticket == null ? null : pillSla(ticket));
            }
        });
        columna.setPrefWidth(190);
        return columna;
    }

    public static TableColumn<Ticket, String> columnaEstado() {
        TableColumn<Ticket, String> columna = columnaPill("Estado", Ticket::getEstado, Componentes::pillEstado);
        columna.setComparator(Comparator.comparingInt(TicketServicio.ESTADOS::indexOf));
        return columna;
    }

    public static TableColumn<Ticket, String> columnaPrioridad() {
        TableColumn<Ticket, String> columna = columnaPill("Prioridad", Ticket::getPrioridad, Componentes::pillPrioridad);
        columna.setComparator(Comparator.comparingInt(Componentes::pesoPrioridad));
        return columna;
    }

    // Columna con fecha en formato dd/MM/yyyy
    public static TableColumn<Ticket, String> columnaFecha(String titulo, Function<Ticket, String> fechaBD) {
        TableColumn<Ticket, String> columna = new TableColumn<>(titulo);
        columna.setCellValueFactory(datos -> new ReadOnlyStringWrapper(fechaBD.apply(datos.getValue())));
        columna.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String fecha, boolean vacio) {
                super.updateItem(fecha, vacio);
                setText(vacio || fecha == null ? null : formatearFecha(fecha));
            }
        });
        columna.setPrefWidth(110);
        return columna;
    }

    public static <T> TableColumn<T, String> columnaPill(String titulo, Function<T, String> valor, Function<String, Label> crearPill) {
        TableColumn<T, String> columna = new TableColumn<>(titulo);
        columna.setCellValueFactory(datos -> new ReadOnlyStringWrapper(valor.apply(datos.getValue())));
        columna.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean vacio) {
                super.updateItem(item, vacio);
                setText(null);
                setGraphic(vacio || item == null ? null : crearPill.apply(item));
            }
        });
        columna.setPrefWidth(112);
        return columna;
    }
}
