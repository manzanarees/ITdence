package com.itdence.dao;

import com.itdence.modelo.Notificacion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class NotificacionDAO {

    public void crear(int usuarioId, int ticketId, String texto) {
        ejecutar("INSERT INTO notificacion (usuario_id, ticket_id, texto) VALUES (?, ?, ?)",
                new Object[]{usuarioId, ticketId, texto});
    }

    public List<Notificacion> listarPorUsuario(int usuarioId, int limite) {
        String sql = "SELECT n.*, t.titulo AS ticket_titulo FROM notificacion n " +
                "JOIN ticket t ON t.id = n.ticket_id WHERE n.usuario_id = ? " +
                "ORDER BY n.fecha DESC, n.id DESC LIMIT ?";
        List<Notificacion> lista = new ArrayList<>();
        Connection con = ConexionBD.getConexion();
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, usuarioId);
            stmt.setInt(2, limite);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Notificacion n = new Notificacion();
                    n.setId(rs.getInt("id"));
                    n.setUsuarioId(rs.getInt("usuario_id"));
                    n.setTicketId(rs.getInt("ticket_id"));
                    n.setTexto(rs.getString("texto"));
                    n.setLeida(rs.getInt("leida") == 1);
                    n.setFecha(rs.getString("fecha"));
                    n.setTicketTitulo(rs.getString("ticket_titulo"));
                    lista.add(n);
                }
                return lista;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar los avisos", e);
        }
    }

    public int contarNoLeidas(int usuarioId) {
        Connection con = ConexionBD.getConexion();
        try (PreparedStatement stmt = con.prepareStatement("SELECT COUNT(*) FROM notificacion WHERE usuario_id = ? AND leida = 0")) {
            stmt.setInt(1, usuarioId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al contar los avisos", e);
        }
    }

    public void marcarLeida(int notificacionId, int usuarioId) {
        ejecutar("UPDATE notificacion SET leida = 1 WHERE id = ? AND usuario_id = ?", new Object[]{notificacionId, usuarioId});
    }

    public void marcarTodasLeidas(int usuarioId) {
        ejecutar("UPDATE notificacion SET leida = 1 WHERE usuario_id = ?", new Object[]{usuarioId});
    }

    public void marcarTicketLeido(int usuarioId, int ticketId) {
        ejecutar("UPDATE notificacion SET leida = 1 WHERE usuario_id = ? AND ticket_id = ?", new Object[]{usuarioId, ticketId});
    }

    public void eliminarPorUsuario(int usuarioId) {
        ejecutar("DELETE FROM notificacion WHERE usuario_id = ?", new Object[]{usuarioId});
    }

    private void ejecutar(String sql, Object[] parametros) {
        Connection con = ConexionBD.getConexion();
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) {
                if (parametros[i] instanceof Integer) {
                    stmt.setInt(i + 1, (Integer) parametros[i]);
                } else {
                    stmt.setString(i + 1, (String) parametros[i]);
                }
            }
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar el aviso", e);
        }
    }
}
