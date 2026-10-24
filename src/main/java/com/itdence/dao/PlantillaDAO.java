package com.itdence.dao;

import com.itdence.modelo.Plantilla;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PlantillaDAO {

    public List<Plantilla> listarTodas() {
        String sql = "SELECT * FROM plantilla ORDER BY titulo";
        List<Plantilla> lista = new ArrayList<>();
        Connection con = ConexionBD.getConexion();
        try (PreparedStatement stmt = con.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                lista.add(new Plantilla(rs.getInt("id"), rs.getString("titulo"), rs.getString("texto")));
            }
            return lista;
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar las respuestas predefinidas", e);
        }
    }

    public void crear(Plantilla plantilla) {
        ejecutar("INSERT INTO plantilla (titulo, texto) VALUES (?, ?)", plantilla, false);
    }

    public void actualizar(Plantilla plantilla) {
        ejecutar("UPDATE plantilla SET titulo = ?, texto = ? WHERE id = ?", plantilla, true);
    }

    public void eliminar(int id) {
        Connection con = ConexionBD.getConexion();
        try (PreparedStatement stmt = con.prepareStatement("DELETE FROM plantilla WHERE id = ?")) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar la respuesta predefinida", e);
        }
    }

    private void ejecutar(String sql, Plantilla plantilla, boolean conId) {
        Connection con = ConexionBD.getConexion();
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, plantilla.getTitulo());
            stmt.setString(2, plantilla.getTexto());
            if (conId) {
                stmt.setInt(3, plantilla.getId());
            }
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar la respuesta predefinida", e);
        }
    }
}
