package com.itdence.dao;

import com.itdence.modelo.Comentario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ComentarioDAO {

    public List<Comentario> listarPorTicket(int ticketId) {
        String sql = "SELECT co.*, u.nombre AS autor_nombre, u.rol AS autor_rol " +
                "FROM comentario co JOIN usuario u ON u.id = co.usuario_id " +
                "WHERE co.ticket_id = ? ORDER BY co.fecha, co.id";
        List<Comentario> comentarios = new ArrayList<>();

        Connection con = ConexionBD.getConexion();
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, ticketId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Comentario comentario = new Comentario(
                            rs.getInt("id"),
                            rs.getInt("ticket_id"),
                            rs.getInt("usuario_id"),
                            rs.getString("texto"),
                            rs.getString("fecha"));
                    comentario.setAutorNombre(rs.getString("autor_nombre"));
                    comentario.setAutorRol(rs.getString("autor_rol"));
                    comentario.setInterno(rs.getInt("interno") == 1);
                    comentarios.add(comentario);
                }
                return comentarios;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar comentarios", e);
        }
    }

    public void crear(Comentario comentario) {
        String sql = "INSERT INTO comentario (ticket_id, usuario_id, texto, interno) VALUES (?, ?, ?, ?)";
        Connection con = ConexionBD.getConexion();
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, comentario.getTicketId());
            stmt.setInt(2, comentario.getUsuarioId());
            stmt.setString(3, comentario.getTexto());
            stmt.setInt(4, comentario.isInterno() ? 1 : 0);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al crear comentario", e);
        }
    }

    public int contarPorUsuario(int usuarioId) {
        String sql = "SELECT COUNT(*) FROM comentario WHERE usuario_id = ?";
        Connection con = ConexionBD.getConexion();
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, usuarioId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al contar comentarios", e);
        }
    }
}
