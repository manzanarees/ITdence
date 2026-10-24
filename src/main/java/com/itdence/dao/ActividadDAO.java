package com.itdence.dao;

import com.itdence.modelo.Actividad;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ActividadDAO {

    public void crear(int ticketId, int usuarioId, String tipo, String detalle) {
        String sql = "INSERT INTO actividad (ticket_id, usuario_id, tipo, detalle) VALUES (?, ?, ?, ?)";
        Connection con = ConexionBD.getConexion();
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, ticketId);
            stmt.setInt(2, usuarioId);
            stmt.setString(3, tipo);
            stmt.setString(4, detalle);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar la actividad del ticket", e);
        }
    }

    public List<Actividad> listarPorTicket(int ticketId) {
        String sql = "SELECT a.*, u.nombre AS usuario_nombre FROM actividad a " +
                "JOIN usuario u ON u.id = a.usuario_id WHERE a.ticket_id = ? ORDER BY a.fecha, a.id";
        List<Actividad> lista = new ArrayList<>();
        Connection con = ConexionBD.getConexion();
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, ticketId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Actividad a = new Actividad();
                    a.setId(rs.getInt("id"));
                    a.setTicketId(rs.getInt("ticket_id"));
                    a.setUsuarioId(rs.getInt("usuario_id"));
                    a.setTipo(rs.getString("tipo"));
                    a.setDetalle(rs.getString("detalle"));
                    a.setFecha(rs.getString("fecha"));
                    a.setUsuarioNombre(rs.getString("usuario_nombre"));
                    lista.add(a);
                }
                return lista;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar la actividad del ticket", e);
        }
    }

    public int contarPorUsuario(int usuarioId) {
        String sql = "SELECT COUNT(*) FROM actividad WHERE usuario_id = ?";
        Connection con = ConexionBD.getConexion();
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, usuarioId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al contar la actividad", e);
        }
    }
}
