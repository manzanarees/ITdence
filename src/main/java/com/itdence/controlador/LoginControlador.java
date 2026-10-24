package com.itdence.controlador;

import com.itdence.modelo.Usuario;
import com.itdence.servicio.AuthServicio;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;

// Pantalla de inicio de sesion
public class LoginControlador {

    private final Stage stage;
    private final AuthServicio authServicio = new AuthServicio();

    public LoginControlador(Stage stage) {
        this.stage = stage;
    }

    public Scene crearEscena() {
        StackPane panelMarca = crearPanelMarca();

        VBox formulario = crearFormulario();
        Label pie = new Label("© 2026 ITdence");
        pie.getStyleClass().add("texto-tenue");
        StackPane.setAlignment(pie, Pos.BOTTOM_CENTER);

        StackPane ladoFormulario = new StackPane(formulario, pie);
        ladoFormulario.getStyleClass().add("login-lado-formulario");
        HBox.setHgrow(ladoFormulario, Priority.ALWAYS);

        HBox layout = new HBox(panelMarca, ladoFormulario);

        Button temaButton = new Button();
        temaButton.setGraphic(Iconos.crear(Tema.esOscuro() ? "sol" : "luna", 18));
        temaButton.getStyleClass().add("boton-icono");
        temaButton.setTooltip(new Tooltip("Cambiar tema"));
        temaButton.setOnAction(e -> {
            Tema.alternar();
            temaButton.setGraphic(Iconos.crear(Tema.esOscuro() ? "sol" : "luna", 18));
        });
        StackPane.setAlignment(temaButton, Pos.TOP_RIGHT);
        StackPane.setMargin(temaButton, new Insets(18));

        StackPane raiz = new StackPane(layout, temaButton);
        Scene scene = new Scene(raiz, 1000, 680);
        Componentes.aplicarEstilos(scene);

        Animaciones.entrada(formulario, 150);
        return scene;
    }

    //  Panel izqdo

    private StackPane crearPanelMarca() {
        HBox marca = Componentes.crearMarca(40, "marca-texto");

        TerminalAnimada terminal = new TerminalAnimada();
        terminal.empezar();

        Label comentario = new Label("// helpdesk IT para tu empresa");
        comentario.getStyleClass().add("login-comentario");
        Label titular = new Label("Las incidencias de tu empresa, bajo control.");
        titular.getStyleClass().add("login-titular");
        titular.setWrapText(true);
        Label texto = new Label("Abre, asigna y resuelve tickets desde una sola aplicación, sin papeles ni correos perdidos.");
        texto.getStyleClass().add("login-texto");
        texto.setWrapText(true);
        // que no se encojan si falta sitio (antes el titular salia cortado con "...")
        titular.setMinHeight(Region.USE_PREF_SIZE);
        texto.setMinHeight(Region.USE_PREF_SIZE);
        VBox textos = new VBox(10, comentario, titular, texto);

        Label pie = new Label("Soporte técnico e incidencias");
        pie.getStyleClass().add("login-pie");

        Region espacio1 = new Region();
        Region espacio2 = new Region();
        VBox.setVgrow(espacio1, Priority.ALWAYS);
        VBox.setVgrow(espacio2, Priority.ALWAYS);

        VBox contenido = new VBox(marca, espacio1, terminal.getVista(), espacio2, textos, pie);
        contenido.setSpacing(18);
        contenido.setPadding(new Insets(36, 40, 30, 40));

        StackPane panel = new StackPane(contenido);
        panel.getStyleClass().add("login-panel-marca");
        panel.setPrefWidth(460);
        panel.setMinWidth(460);

        Animaciones.escalonada(List.of(marca, terminal.getVista(), textos, pie), 120);
        return panel;
    }

    //Formulario

    private VBox crearFormulario() {
        Label tituloLabel = new Label("Bienvenido de nuevo");
        tituloLabel.getStyleClass().add("titulo");
        Label subtituloLabel = new Label("Inicia sesión con tu cuenta de la empresa.");
        subtituloLabel.getStyleClass().add("texto-secundario");

        TextField emailField = new TextField();
        emailField.setPromptText("nombre@empresa.com");
        PasswordField contrasenaField = new PasswordField();
        contrasenaField.setPromptText("Tu contraseña");

        // mensaje de error
        Label errorTexto = new Label();
        errorTexto.setWrapText(true);
        HBox errorCaja = new HBox(10, Iconos.crear("error", 18), errorTexto);
        errorCaja.setAlignment(Pos.CENTER_LEFT);
        errorCaja.getStyleClass().add("alerta-error");
        errorCaja.setVisible(false);
        errorCaja.setManaged(false);

        Button loginButton = new Button("Iniciar sesión");
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.getStyleClass().add("boton-grande");
        loginButton.setDefaultButton(true);

        VBox formulario = new VBox(16,
                new VBox(6, tituloLabel, subtituloLabel),
                new VBox(7, Componentes.etiquetaCampo("Correo electrónico"), conIcono(emailField, "correo")),
                new VBox(7, Componentes.etiquetaCampo("Contraseña"), conIcono(contrasenaField, "candado")),
                errorCaja,
                loginButton,
                Componentes.textoTenue("¿No tienes cuenta? Pídesela al administrador de tu empresa."));
        formulario.setMaxWidth(360);
        formulario.setMaxHeight(Region.USE_PREF_SIZE);

        loginButton.setOnAction(event -> {
            String email = emailField.getText().trim();
            String contrasena = contrasenaField.getText();

            String error = null;
            Usuario usuario = null;
            if (email.isEmpty() || contrasena.isEmpty()) {
                error = "Escribe tu email y tu contraseña.";
            } else {
                usuario = authServicio.login(email, contrasena);
                if (usuario == null) {
                    error = "Email o contraseña incorrectos.";
                }
            }

            if (error != null) {
                errorTexto.setText(error);
                if (!errorCaja.isVisible()) {
                    errorCaja.setVisible(true);
                    errorCaja.setManaged(true);
                    Animaciones.entrada(errorCaja);
                }
                Animaciones.sacudir(formulario);
                contrasenaField.clear();
                contrasenaField.requestFocus();
            } else {
                Navegacion.abrirPanel(stage, usuario);
            }
        });

        Platform.runLater(emailField::requestFocus);
        return formulario;
    }

    private StackPane conIcono(TextField campo, String icono) {
        campo.getStyleClass().add("campo-con-icono");
        Node icon = Iconos.crear(icono, 18);
        icon.setMouseTransparent(true);
        StackPane.setAlignment(icon, Pos.CENTER_LEFT);
        StackPane.setMargin(icon, new Insets(0, 0, 0, 13));
        return new StackPane(campo, icon);
    }
}
