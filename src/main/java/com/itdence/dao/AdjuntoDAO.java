package com.itdence.dao;

import com.itdence.modelo.Adjunto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AdjuntoDAO {

    public void crear(Adjunto adjunto) {
        String sql = "INSERT INTO adjunto (ticket_id, usuario_id, nombre, archivo, tamano) VALUES (?, ?, ?, ?, ?)";
        Connection con = ConexionBD.getConexion();
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, adjunto.getTicketId());
            stmt.setInt(2, adjunto.getUsuarioId());
            stmt.setString(3, adjunto.getNombre());
            stmt.setString(4, adjunto.getArchivo());
            stmt.setLong(5, adjunto.getTamano());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar el adjunto", e);
        }
    }

    public List<Adjunto> listarPorTicket(int ticketId) {
        String sql = "SELECT a.*, u.nombre AS usuario_nombre FROM adjunto a " +
                "JOIN usuario u ON u.id = a.usuario_id WHERE a.ticket_id = ? ORDER BY a.fecha, a.id";
        List<Adjunto> lista = new ArrayList<>();
        Connection con = ConexionBD.getConexion();
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, ticketId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Adjunto a = new Adjunto();
                    a.setId(rs.getInt("id"));
                    a.setTicketId(rs.getInt("ticket_id"));
                    a.setUsuarioId(rs.getInt("usuario_id"));
                    a.setNombre(rs.getString("nombre"));
                    a.setArchivo(rs.getString("archivo"));
                    a.setTamano(rs.getLong("tamano"));
                    a.setFecha(rs.getString("fecha"));
                    a.setUsuarioNombre(rs.getString("usuario_nombre"));
                    lista.add(a);
                }
                return lista;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar los adjuntos", e);
        }
    }

    public int contarPorUsuario(int usuarioId) {
        Connection con = ConexionBD.getConexion();
        try (PreparedStatement stmt = con.prepareStatement("SELECT COUNT(*) FROM adjunto WHERE usuario_id = ?")) {
            stmt.setInt(1, usuarioId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al contar los adjuntos", e);
        }
    }
}
