package com.itdence.dao;

import com.itdence.modelo.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


public class UsuarioDAO {

    public Usuario buscarPorEmail (String email) {
        String sql = "SELECT * FROM usuario WHERE email = ?";
        Connection con = ConexionBD.getConexion();
        try (PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setString(1,email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapearUsuario(rs);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar usuario por email", e);
        }
    }

    public List <Usuario> listarTodos () {
        String sql = "SELECT * FROM usuario ORDER BY nombre";
        List <Usuario> usuarios = new ArrayList<>();

        Connection con = ConexionBD.getConexion();
        try (PreparedStatement stmt = con.prepareStatement(sql);ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                usuarios.add(mapearUsuario(rs));
            }
            return usuarios;
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar usuarios", e);
        }
    }

    public void crear(Usuario usuario){
        String sql = "INSERT INTO usuario (nombre, email, contrasena_hash, rol) VALUES (?, ?, ?, ?)";
        Connection con = ConexionBD.getConexion();
        try (PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setString(1, usuario.getNombre());
            stmt.setString(2, usuario.getEmail());
            stmt.setString(3, usuario.getContrasenaHash());
            stmt.setString(4, usuario.getRol());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al crear usuario", e);
        }
    }

    public void actualizar(Usuario usuario) {
        String sql = "UPDATE usuario SET nombre = ?, email = ?, contrasena_hash = ?, rol = ? WHERE id = ?";
        Connection con = ConexionBD.getConexion();
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, usuario.getNombre());
            stmt.setString(2, usuario.getEmail());
            stmt.setString(3, usuario.getContrasenaHash());
            stmt.setString(4, usuario.getRol());
            stmt.setInt(5, usuario.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar usuario", e);
        }
    }

    public void eliminar(int id) {
        String sql = "DELETE FROM usuario WHERE id = ?";
        Connection con = ConexionBD.getConexion();
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar usuario", e);
        }
    }

    private Usuario mapearUsuario(ResultSet rs) throws SQLException{
        return new Usuario(
                rs.getInt("id"),
                rs.getString("nombre"),
                rs.getString("email"),
                rs.getString("contrasena_hash"),
                rs.getString("rol")
        );
    }
}
