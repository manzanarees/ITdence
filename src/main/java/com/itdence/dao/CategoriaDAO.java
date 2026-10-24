package com.itdence.dao;

import com.itdence.modelo.Categoria;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CategoriaDAO {
    public List<Categoria> listarTodas() {
        String sql = "SELECT * FROM categoria ORDER BY nombre";
        List<Categoria> categorias = new ArrayList<>();

        Connection con = ConexionBD.getConexion();
        try (PreparedStatement stmt = con.prepareStatement(sql);
        ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                categorias.add(new Categoria(rs.getInt("id"), rs.getString("nombre")));
            }
            return categorias;
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar categorías", e);
        }
    }

    public void crear(Categoria categoria) {
        String sql ="INSERT INTO categoria (nombre) VALUES (?)";
        Connection con = ConexionBD.getConexion();
        try(PreparedStatement stmt = con.prepareStatement(sql)){
            stmt.setString(1, categoria.getNombre());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al crear categoría", e);
        }
    }

    public void actualizar(Categoria categoria) {
        String sql = "UPDATE categoria SET nombre = ? WHERE id = ?";
        Connection con = ConexionBD.getConexion();
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, categoria.getNombre());
            stmt.setInt(2, categoria.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar categoría", e);
        }
    }

    public void eliminar(int id) {
        String sql = "DELETE FROM categoria WHERE id = ?";
        Connection con = ConexionBD.getConexion();
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar categoría", e);
        }
    }
}
