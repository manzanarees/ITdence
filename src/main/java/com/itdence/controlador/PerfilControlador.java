package com.itdence.controlador;

import com.itdence.modelo.Usuario;
import com.itdence.servicio.UsuarioServicio;
import com.itdence.servicio.ValidacionException;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

// Mi perfil
public class PerfilControlador {

    private final Stage owner;
    private final Usuario usuario;
    private final UsuarioServicio servicio = new UsuarioServicio();
    private Stage ventana;

    public PerfilControlador(Stage owner, Usuario usuario) {
        this.owner = owner;
        this.usuario = usuario;
    }

    public void mostrar() {
        Label avatar = Componentes.crearAvatar(usuario.getNombre(), 56);
        Label nombreGrande = new Label(usuario.getNombre());
        nombreGrande.getStyleClass().add("titulo-modal");
        Label detalle = new Label(usuario.getEmail() + "  ·  " + Componentes.capitalizar(Componentes.nombreRol(usuario.getRol())));
        detalle.getStyleClass().add("texto-secundario");
        HBox cabecera = new HBox(16, avatar, new VBox(3, nombreGrande, detalle));
        cabecera.setAlignment(Pos.CENTER_LEFT);

        // nombre
        TextField nombreField = new TextField(usuario.getNombre());
        Button guardarNombre = new Button("Guardar nombre");
        guardarNombre.getStyleClass().add("boton-secundario");
        guardarNombre.setOnAction(e -> {
            try {
                servicio.cambiarNombre(usuario, nombreField.getText());
                nombreGrande.setText(usuario.getNombre());
                Notificaciones.exito(nombreField, "Nombre actualizado");
            } catch (ValidacionException ex) {
                Notificaciones.error(nombreField, ex.getMessage());
            }
        });
        HBox.setHgrow(nombreField, Priority.ALWAYS);
        HBox filaNombre = new HBox(10, nombreField, guardarNombre);
        filaNombre.setAlignment(Pos.CENTER_LEFT);

        // contraseña
        PasswordField actualField = new PasswordField();
        PasswordField nuevaField = new PasswordField();
        PasswordField repetidaField = new PasswordField();
        nuevaField.setPromptText("Mínimo 6 caracteres");

        Button cambiarContrasena = new Button("Cambiar contraseña");
        cambiarContrasena.setGraphic(Iconos.crear("candado", 16));
        cambiarContrasena.setOnAction(e -> {
            try {
                servicio.cambiarContrasena(usuario, actualField.getText(), nuevaField.getText(), repetidaField.getText());
                actualField.clear();
                nuevaField.clear();
                repetidaField.clear();
                Notificaciones.exito(nombreField, "Contraseña cambiada");
            } catch (ValidacionException ex) {
                Notificaciones.error(nombreField, ex.getMessage());
            }
        });

        Label tituloContrasena = new Label("Contraseña");
        tituloContrasena.getStyleClass().add("subtitulo");
        Label tituloNombre = new Label("Nombre");
        tituloNombre.getStyleClass().add("subtitulo");

        Region raya = new Region();
        raya.getStyleClass().add("separador");

        Button cerrar = new Button("Cerrar");
        cerrar.getStyleClass().add("boton-secundario");
        cerrar.setCancelButton(true);
        HBox pie = new HBox(cerrar);
        pie.setAlignment(Pos.CENTER_RIGHT);

        VBox contenido = new VBox(14,
                cabecera,
                raya,
                tituloNombre, filaNombre,
                tituloContrasena,
                new VBox(7, Componentes.etiquetaCampo("Contraseña actual"), actualField),
                new VBox(7, Componentes.etiquetaCampo("Contraseña nueva"), nuevaField),
                new VBox(7, Componentes.etiquetaCampo("Repite la contraseña nueva"), repetidaField),
                cambiarContrasena,
                pie);
        contenido.setPadding(new Insets(28));
        contenido.getStyleClass().add("fondo-superficie");

        ventana = Componentes.crearVentanaModal(owner, "Mi perfil", contenido, 520, 560);
        cerrar.setOnAction(e -> ventana.close());
        ventana.showAndWait();
    }
}
