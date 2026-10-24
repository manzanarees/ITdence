package com.itdence.controlador;

import com.itdence.modelo.Usuario;
import com.itdence.servicio.UsuarioServicio;
import com.itdence.servicio.ValidacionException;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;

// Pantalla "Usuarios" del admin
public class GestionUsuariosPanel {

    private final Stage stage;
    private final Usuario usuarioActual;
    private final UsuarioServicio usuarioServicio = new UsuarioServicio();

    private final ObservableList<Usuario> usuarios = FXCollections.observableArrayList();
    private final FilteredList<Usuario> filtrados = new FilteredList<>(usuarios);
    private final TableView<Usuario> tabla = new TableView<>();
    private final Label tituloFormulario = new Label();
    private final StackPane iconoFormulario = new StackPane();
    private final TextField nombreField = new TextField();
    private final TextField emailField = new TextField();
    private final ComboBox<String> rolCombo = new ComboBox<>();
    private final PasswordField contrasenaField = new PasswordField();
    private final Label ayudaContrasena = new Label();
    private final Button eliminarButton = new Button("Eliminar");

    private Usuario seleccionado;

    public GestionUsuariosPanel(Stage stage, Usuario usuarioActual) {
        this.stage = stage;
        this.usuarioActual = usuarioActual;
    }

    public Node crearVista() {
        tabla.setItems(filtrados);
        tabla.getColumns().add(crearColumnaUsuario());
        tabla.getColumns().add(Componentes.columnaPill("Rol", Usuario::getRol, Componentes::pillRol));
        Componentes.configurarTabla(tabla, Componentes.estadoVacio("usuarios", "No hay usuarios", "Crea uno con el formulario."));
        tabla.getSelectionModel().selectedItemProperty().addListener((obs, antes, ahora) -> cargarEnFormulario(ahora));

        TextField buscar = new TextField();
        buscar.setPromptText("Buscar usuario...");
        buscar.getStyleClass().add("buscador");
        buscar.textProperty().addListener((obs, antes, ahora) -> {
            String texto = ahora.trim().toLowerCase();
            filtrados.setPredicate(u -> texto.isEmpty() || u.getNombre().toLowerCase().contains(texto)
                    || u.getEmail().contains(texto));
        });

        Label tituloTabla = new Label("Usuarios de la aplicación");
        tituloTabla.getStyleClass().add("tarjeta-titulo");
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        HBox cabeceraTabla = new HBox(10, tituloTabla, espacio, buscar);
        cabeceraTabla.setAlignment(Pos.CENTER_LEFT);
        cabeceraTabla.getStyleClass().add("tarjeta-cabecera");

        VBox tarjetaTabla = new VBox(cabeceraTabla, tabla);
        VBox.setVgrow(tabla, Priority.ALWAYS);
        tarjetaTabla.getStyleClass().add("tarjeta");
        HBox.setHgrow(tarjetaTabla, Priority.ALWAYS);

        HBox contenido = new HBox(18, tarjetaTabla, crearFormulario());
        VBox.setVgrow(contenido, Priority.ALWAYS);

        Label titulo = new Label("Usuarios");
        titulo.getStyleClass().add("titulo");
        Label subtitulo = new Label("Da de alta a clientes, técnicos y administradores o cambia sus datos.");
        subtitulo.getStyleClass().add("texto-secundario");

        refrescar();
        cargarEnFormulario(null);
        return new VBox(22, new VBox(4, Componentes.fechaDeHoy(), titulo, subtitulo), contenido);
    }

    // Avatar + nombre + email debajo
    private TableColumn<Usuario, Usuario> crearColumnaUsuario() {
        TableColumn<Usuario, Usuario> columna = new TableColumn<>("Usuario");
        columna.setCellValueFactory(datos -> new ReadOnlyObjectWrapper<>(datos.getValue()));
        columna.setComparator((a, b) -> a.getNombre().compareToIgnoreCase(b.getNombre()));
        columna.setPrefWidth(340);
        columna.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Usuario u, boolean vacio) {
                super.updateItem(u, vacio);
                setText(null);
                if (vacio || u == null) {
                    setGraphic(null);
                    return;
                }
                Label nombre = new Label(u.getNombre());
                nombre.getStyleClass().add("celda-titulo");
                Label email = new Label(u.getEmail());
                email.getStyleClass().add("celda-codigo");
                HBox caja = new HBox(10, Componentes.crearAvatar(u.getNombre(), 32), Componentes.centrado(new VBox(2, nombre, email)));
                caja.setAlignment(Pos.CENTER_LEFT);
                setGraphic(caja);
            }
        });
        return columna;
    }

    private VBox crearFormulario() {
        tituloFormulario.getStyleClass().add("subtitulo");
        iconoFormulario.getStyleClass().add("modal-icono");

        rolCombo.getItems().addAll(UsuarioServicio.ROLES);
        rolCombo.setMaxWidth(Double.MAX_VALUE);
        rolCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(String rol) {
                return rol == null ? "" : Componentes.capitalizar(Componentes.nombreRol(rol));
            }

            @Override
            public String fromString(String texto) {
                return texto;
            }
        });

        ayudaContrasena.getStyleClass().add("texto-tenue");
        ayudaContrasena.setWrapText(true);

        Button guardarButton = new Button("Guardar");
        guardarButton.setGraphic(Iconos.crear("check", 16));
        guardarButton.setOnAction(event -> guardar());

        Button nuevoButton = new Button("Nuevo");
        nuevoButton.getStyleClass().add("boton-secundario");
        nuevoButton.setOnAction(event -> tabla.getSelectionModel().clearSelection());

        eliminarButton.setGraphic(Iconos.crear("borrar", 16));
        eliminarButton.getStyleClass().add("boton-peligro");
        eliminarButton.setOnAction(event -> eliminar());

        guardarButton.setMaxWidth(Double.MAX_VALUE);
        nuevoButton.setMaxWidth(Double.MAX_VALUE);
        eliminarButton.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(nuevoButton, Priority.ALWAYS);
        HBox.setHgrow(eliminarButton, Priority.ALWAYS);
        VBox botones = new VBox(8, guardarButton, new HBox(8, nuevoButton, eliminarButton));
        botones.setPadding(new Insets(6, 0, 0, 0));

        HBox cabecera = new HBox(12, iconoFormulario, tituloFormulario);
        cabecera.setAlignment(Pos.CENTER_LEFT);

        VBox formulario = new VBox(14,
                cabecera,
                new VBox(7, Componentes.etiquetaCampo("Nombre"), nombreField),
                new VBox(7, Componentes.etiquetaCampo("Email"), emailField),
                new VBox(7, Componentes.etiquetaCampo("Rol"), rolCombo),
                new VBox(7, Componentes.etiquetaCampo("Contraseña"), contrasenaField, ayudaContrasena),
                botones);
        formulario.getStyleClass().add("tarjeta");
        formulario.setPadding(new Insets(22));
        formulario.setPrefWidth(330);
        formulario.setMinWidth(330);
        formulario.setMaxHeight(Region.USE_PREF_SIZE);
        return formulario;
    }

    private void cargarEnFormulario(Usuario usuario) {
        seleccionado = usuario;
        contrasenaField.clear();
        if (usuario == null) {
            tituloFormulario.setText("Nuevo usuario");
            iconoFormulario.getChildren().setAll(Iconos.crear("mas", 20));
            nombreField.clear();
            emailField.clear();
            rolCombo.setValue("cliente");
            ayudaContrasena.setText("Mínimo 6 caracteres.");
            eliminarButton.setDisable(true);
        } else {
            tituloFormulario.setText("Editar usuario");
            iconoFormulario.getChildren().setAll(Iconos.crear("editar", 20));
            nombreField.setText(usuario.getNombre());
            emailField.setText(usuario.getEmail());
            rolCombo.setValue(usuario.getRol());
            ayudaContrasena.setText("Déjala vacía para no cambiarla.");
            eliminarButton.setDisable(false);
        }
    }

    private void guardar() {
        try {
            if (seleccionado == null) {
                usuarioServicio.crear(nombreField.getText(), emailField.getText(), contrasenaField.getText(), rolCombo.getValue());
                Notificaciones.exito(tabla, "Usuario creado");
            } else {
                usuarioServicio.actualizar(seleccionado, nombreField.getText(), emailField.getText(),
                        rolCombo.getValue(), contrasenaField.getText(), usuarioActual);
                Notificaciones.exito(tabla, "Cambios guardados");
            }
            refrescar();
        } catch (ValidacionException e) {
            Notificaciones.error(tabla, e.getMessage());
        }
    }

    private void eliminar() {
        if (seleccionado == null) {
            return;
        }
        boolean confirmado = Dialogos.confirmar(stage, "Eliminar usuario",
                "¿Seguro que quieres eliminar a " + seleccionado.getNombre() + "? No se puede deshacer.", "Eliminar");
        if (!confirmado) {
            return;
        }
        try {
            usuarioServicio.eliminar(seleccionado, usuarioActual);
            refrescar();
            Notificaciones.exito(tabla, "Usuario eliminado");
        } catch (ValidacionException e) {
            Notificaciones.error(tabla, e.getMessage());
        }
    }

    public void refrescar() {
        usuarios.setAll(usuarioServicio.listarTodos());
    }
}
