package com.itdence.controlador;

import com.itdence.modelo.Ticket;
import com.itdence.modelo.Usuario;
import com.itdence.servicio.NotificacionServicio;
import com.itdence.servicio.TicketServicio;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

// Lo comun a los paneles de cliente, tecnico y admin: la barra lateral con el logo, el menu, el usuario, el modo oscuro y cerrar sesion.
public abstract class ControladorBase {

    protected static final double ANCHO = 1300;
    protected static final double ALTO = 740;

    protected final Stage stage;
    protected final Usuario usuario;

    private final VBox menu = new VBox(4);
    private final ToggleGroup grupoMenu = new ToggleGroup();
    private final StackPane contenedor = new StackPane();

    // campana de avisos
    private final NotificacionServicio avisos = new NotificacionServicio();
    private final Label badge = new Label();
    private final Button campana = new Button();

    protected ControladorBase(Stage stage, Usuario usuario) {
        this.stage = stage;
        this.usuario = usuario;grupoMenu.selectedToggleProperty().addListener((obs, antes, ahora) -> {
            if (ahora == null && antes != null) {
                grupoMenu.selectToggle(antes);
            }
        });
    }

    public abstract Scene crearEscena();

    // Vuelve a cargar los datos de la pantalla
    protected abstract void refrescar();

    protected Scene crearLayoutPrincipal() {
        contenedor.setPadding(new Insets(28, 34, 28, 34));
        contenedor.setAlignment(Pos.TOP_LEFT);

        BorderPane layout = new BorderPane();
        layout.setLeft(crearBarraLateral());
        layout.setCenter(contenedor);

        StackPane raiz = new StackPane(layout);
        Scene scene = new Scene(raiz, ANCHO, ALTO);
        raiz.getChildren().add(Notificaciones.crearCapa(scene));
        Componentes.aplicarEstilos(scene);
        actualizarAvisos();
        iniciarSondeoDeAvisos();
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.K, KeyCombination.SHORTCUT_DOWN), this::abrirBuscador);
        return scene;
    }

    // Cambia lo que se ve en la parte derecha con una animacion
    protected void mostrarVista(Node vista) {
        contenedor.getChildren().setAll(vista);
        Animaciones.entrada(vista);
    }

    protected Label agregarOpcionMenu(String texto, String icono, Runnable accion) {
        Label textoLabel = new Label(texto);
        textoLabel.getStyleClass().add("opcion-texto");
        Label contador = new Label();
        contador.getStyleClass().add("contador");
        contador.setVisible(false);
        contador.textProperty().addListener((obs, antes, ahora) -> contador.setVisible(ahora != null && !ahora.isEmpty()));

        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        HBox contenido = new HBox(12, Iconos.crear(icono, 18), textoLabel, espacio, contador);
        contenido.setAlignment(Pos.CENTER_LEFT);

        ToggleButton opcion = new ToggleButton();
        opcion.setGraphic(contenido);
        opcion.setToggleGroup(grupoMenu);
        opcion.getStyleClass().add("opcion-menu");
        opcion.setMaxWidth(Double.MAX_VALUE);
        opcion.setAlignment(Pos.CENTER_LEFT);
        contenido.prefWidthProperty().bind(opcion.widthProperty().subtract(28));
        opcion.setOnAction(e -> {
            opcion.setSelected(true);
            accion.run();
        });

        if (menu.getChildren().isEmpty()) {
            opcion.setSelected(true);
        }
        menu.getChildren().add(opcion);
        return contador;
    }

    protected void abrirDetalle(Ticket ticket) {
        new DetalleTicketControlador(stage, usuario, ticket.getId()).mostrar();
        refrescar();
        actualizarAvisos();
    }

    //  Avisos

    protected void actualizarAvisos() {
        int sinLeer = avisos.contarNoLeidas(usuario);
        badge.setText(sinLeer > 9 ? "9+" : String.valueOf(sinLeer));
        badge.setVisible(sinLeer > 0);
        campana.setAccessibleText(sinLeer == 0 ? "Avisos" : "Avisos: " + sinLeer + " sin leer");
    }

    private void iniciarSondeoDeAvisos() {
        Timeline sondeo = new Timeline();
        sondeo.getKeyFrames().add(new KeyFrame(Duration.seconds(8), e -> {
            if (campana.getScene() == null || campana.getScene().getWindow() == null) {
                sondeo.stop();
                return;
            }
            actualizarAvisos();
        }));
        sondeo.setCycleCount(Animation.INDEFINITE);
        sondeo.play();
    }

    private void abrirBuscador() {
        Integer ticketId = new BuscadorControlador(stage, usuario).mostrar();
        if (ticketId != null) {
            Ticket ticket = new TicketServicio().buscarPorId(ticketId);
            if (ticket != null) {
                abrirDetalle(ticket);
            }
        }
    }

    // Boton de la barra lateral que abre el buscador
    private Button crearBotonBuscar() {
        Label texto = new Label("Buscar tickets");
        texto.getStyleClass().add("texto-tenue");
        Region hueco = new Region();
        HBox.setHgrow(hueco, Priority.ALWAYS);
        Label atajo = new Label("Ctrl K");
        atajo.getStyleClass().add("atajo-teclado");
        HBox contenido = new HBox(10, Iconos.crear("buscar", 16), texto, hueco, atajo);
        contenido.setAlignment(Pos.CENTER_LEFT);

        Button boton = new Button();
        boton.setGraphic(contenido);
        boton.getStyleClass().add("boton-buscar");
        boton.setMaxWidth(Double.MAX_VALUE);
        boton.setAlignment(Pos.CENTER_LEFT);
        boton.widthProperty().addListener((obs, antes, ahora) -> contenido.setPrefWidth(ahora.doubleValue() - 28));
        boton.setOnAction(e -> abrirBuscador());
        return boton;
    }

    private void abrirAvisos() {
        Integer ticketId = new NotificacionesControlador(stage, usuario).mostrar();
        actualizarAvisos();
        if (ticketId != null) {
            Ticket ticket = new TicketServicio().buscarPorId(ticketId);
            if (ticket != null) {
                abrirDetalle(ticket);
            }
        }
    }

    private StackPane crearCampana() {
        campana.setGraphic(Iconos.crear("campana", 20));
        campana.getStyleClass().add("boton-icono");
        campana.setTooltip(new Tooltip("Avisos"));
        campana.setOnAction(e -> abrirAvisos());

        badge.getStyleClass().add("badge-aviso");
        badge.setMouseTransparent(true);
        badge.setVisible(false);
        StackPane.setAlignment(badge, Pos.TOP_RIGHT);
        StackPane.setMargin(badge, new Insets(0, 0, 0, 0));
        return new StackPane(campana, badge);
    }

    protected HBox crearCabecera(String titulo, String subtitulo, Node... derecha) {
        Label tituloLabel = new Label(titulo);
        tituloLabel.getStyleClass().add("titulo");
        Label subtituloLabel = new Label(subtitulo);
        subtituloLabel.getStyleClass().add("texto-secundario");
        VBox textos = new VBox(4, Componentes.fechaDeHoy(), tituloLabel, subtituloLabel);

        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);

        HBox cabecera = new HBox(12);
        cabecera.getChildren().addAll(textos, espacio);
        cabecera.getChildren().addAll(derecha);
        cabecera.setAlignment(Pos.CENTER_LEFT);
        return cabecera;
    }

    // Tarjeta blanca con titulo, para meter tablas y demas cosas
    protected VBox crearTarjeta(String titulo, Node contenido, Node... derechaTitulo) {
        Label tituloLabel = new Label(titulo);
        tituloLabel.getStyleClass().add("tarjeta-titulo");
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        HBox cabecera = new HBox(10);
        cabecera.getChildren().addAll(tituloLabel, espacio);
        cabecera.getChildren().addAll(derechaTitulo);
        cabecera.setAlignment(Pos.CENTER_LEFT);
        cabecera.getStyleClass().add("tarjeta-cabecera");

        VBox tarjeta = new VBox(cabecera, contenido);
        VBox.setVgrow(contenido, Priority.ALWAYS);
        tarjeta.getStyleClass().add("tarjeta");
        return tarjeta;
    }

    private VBox crearBarraLateral() {
        HBox marca = Componentes.crearMarca(34, "marca-texto");
        Region huecoMarca = new Region();
        HBox.setHgrow(huecoMarca, Priority.ALWAYS);
        HBox marcaYCampana = new HBox(marca, huecoMarca, crearCampana());
        marcaYCampana.setAlignment(Pos.CENTER_LEFT);
        marcaYCampana.setPadding(new Insets(0, 0, 18, 6));

        Label seccion = Componentes.etiquetaCampo("Menú");
        seccion.setPadding(new Insets(12, 0, 6, 10));

        Region espacio = new Region();
        VBox.setVgrow(espacio, Priority.ALWAYS);

        VBox lateral = new VBox(marcaYCampana, crearBotonBuscar(), seccion, menu, espacio, crearBotonTema(), crearTarjetaUsuario());
        lateral.setSpacing(6);
        lateral.getStyleClass().add("barra-lateral");
        lateral.setPrefWidth(264);
        lateral.setMinWidth(264);
        return lateral;
    }

    private Button crearBotonTema() {
        Button tema = new Button();
        tema.getStyleClass().add("opcion-menu");
        tema.setMaxWidth(Double.MAX_VALUE);
        tema.setAlignment(Pos.CENTER_LEFT);
        actualizarBotonTema(tema);
        tema.setOnAction(e -> {
            Tema.alternar();
            actualizarBotonTema(tema);
        });
        return tema;
    }

    private void actualizarBotonTema(Button tema) {
        Label texto = new Label(Tema.esOscuro() ? "Modo claro" : "Modo oscuro");
        texto.getStyleClass().add("opcion-texto");
        HBox contenido = new HBox(12, Iconos.crear(Tema.esOscuro() ? "sol" : "luna", 18), texto);
        contenido.setAlignment(Pos.CENTER_LEFT);
        tema.setGraphic(contenido);
    }

    private HBox crearTarjetaUsuario() {
        Label nombre = new Label(usuario.getNombre());
        nombre.getStyleClass().add("usuario-nombre");
        Label rol = new Label(Componentes.capitalizar(Componentes.nombreRol(usuario.getRol())));
        rol.getStyleClass().add("usuario-rol");
        VBox textos = new VBox(1, nombre, rol);
        HBox.setHgrow(textos, Priority.ALWAYS);

        Button salir = new Button();
        salir.setGraphic(Iconos.crear("salir", 18));
        salir.getStyleClass().add("boton-icono");
        salir.setTooltip(new Tooltip("Cerrar sesión"));
        salir.setOnAction(e -> Navegacion.irALogin(stage));
        salir.addEventHandler(MouseEvent.MOUSE_CLICKED, MouseEvent::consume);

        HBox tarjeta = new HBox(10, Componentes.crearAvatar(usuario.getNombre(), 36), textos, salir);
        tarjeta.setAlignment(Pos.CENTER_LEFT);
        tarjeta.getStyleClass().addAll("tarjeta-usuario", "tarjeta-usuario-clicable");
        Tooltip.install(tarjeta, new Tooltip("Mi perfil"));
        tarjeta.setOnMouseClicked(e -> {
            new PerfilControlador(stage, usuario).mostrar();
            nombre.setText(usuario.getNombre());
            tarjeta.getChildren().set(0, Componentes.crearAvatar(usuario.getNombre(), 36));
        });
        return tarjeta;
    }
}
