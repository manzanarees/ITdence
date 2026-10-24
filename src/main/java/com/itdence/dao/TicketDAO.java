package com.itdence.dao;

import com.itdence.modelo.Sla;
import com.itdence.modelo.Ticket;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TicketDAO {

    private static final String SELECT_BASE =
            "SELECT t.*, c.nombre AS categoria_nombre, u.nombre AS creador_nombre, tec.nombre AS tecnico_nombre " +
            "FROM ticket t " +
            "JOIN categoria c ON c.id = t.categoria_id " +
            "JOIN usuario u ON u.id = t.creador_id " +
            "LEFT JOIN usuario tec ON tec.id = t.tecnico_asignado_id ";

    public int crear(Ticket ticket) {
        String sql = "INSERT INTO ticket (titulo, descripcion, prioridad, categoria_id, creador_id, fecha_limite) " +
                "VALUES (?, ?, ?, ?, ?, datetime('now', 'localtime', '+' || ? || ' hours'))";
        Connection con = ConexionBD.getConexion();
        try (PreparedStatement stmt = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, ticket.getTitulo());
            stmt.setString(2, ticket.getDescripcion());
            stmt.setString(3, ticket.getPrioridad());
            stmt.setInt(4, ticket.getCategoriaId());
            stmt.setInt(5, ticket.getCreadorId());
            stmt.setInt(6, Sla.horas(ticket.getPrioridad()));
            stmt.executeUpdate();

            try (ResultSet claves = stmt.getGeneratedKeys()) {
                return claves.next() ? claves.getInt(1) : -1;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al crear ticket", e);
        }
    }

    public List<Ticket> listarPorCreador(int creadorId) {
        String sql = SELECT_BASE + "WHERE t.creador_id = ? ORDER BY t.fecha_creacion DESC, t.id DESC";
        return listar(sql, creadorId);
    }

    public List<Ticket> listarSinAsignar() {
        String sql = SELECT_BASE + "WHERE t.tecnico_asignado_id IS NULL AND t.estado <> 'cerrado' ORDER BY t.fecha_creacion DESC, t.id DESC";
        return listar(sql);
    }

    public List<Ticket> listarPorTecnico(int tecnicoId) {
        String sql = SELECT_BASE + "WHERE t.tecnico_asignado_id = ? ORDER BY t.fecha_actualizacion DESC, t.id DESC";
        return listar(sql, tecnicoId);
    }

    public List<Ticket> listarTodos() {
        String sql = SELECT_BASE + "ORDER BY t.fecha_creacion DESC, t.id DESC";
        return listar(sql);
    }

    public Ticket buscarPorId(int id) {
        List<Ticket> resultado = listar(SELECT_BASE + "WHERE t.id = ?", id);
        return resultado.isEmpty() ? null : resultado.get(0);
    }

    public boolean asignarTecnico(int ticketId, int tecnicoId) {
        String sql = "UPDATE ticket SET tecnico_asignado_id = ?, " +
                "estado = CASE WHEN estado = 'abierto' THEN 'en progreso' ELSE estado END, " +
                "fecha_actualizacion = datetime('now', 'localtime') " +
                "WHERE id = ? AND tecnico_asignado_id IS NULL";
        Connection con = ConexionBD.getConexion();
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, tecnicoId);
            stmt.setInt(2, ticketId);
            return stmt.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException("Error al asignar el ticket", e);
        }
    }

    public void asignarForzado(int ticketId, int tecnicoId) {
        String sql = "UPDATE ticket SET tecnico_asignado_id = ?, " +
                "estado = CASE WHEN estado = 'abierto' THEN 'en progreso' ELSE estado END, " +
                "fecha_actualizacion = datetime('now', 'localtime') WHERE id = ?";
        Connection con = ConexionBD.getConexion();
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, tecnicoId);
            stmt.setInt(2, ticketId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al asignar el ticket", e);
        }
    }

    public void cancelar(int ticketId) {
        Connection con = ConexionBD.getConexion();
        String sql = "UPDATE ticket SET estado = 'cerrado', cancelado = 1, fecha_resolucion = NULL, " +
                "fecha_actualizacion = datetime('now', 'localtime') WHERE id = ?";
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, ticketId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al cancelar el ticket", e);
        }
    }

    public void guardarValoracion(int ticketId, int valoracion) {
        ejecutarUpdate("UPDATE ticket SET valoracion = ? WHERE id = ?", valoracion, ticketId);
    }


    public void reabrir(int ticketId, String nuevoEstado) {
        Connection con = ConexionBD.getConexion();
        String sql = "UPDATE ticket SET estado = ?, reaperturas = reaperturas + 1, fecha_resolucion = NULL, " +
                "fecha_actualizacion = datetime('now', 'localtime') WHERE id = ?";
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, nuevoEstado);
            stmt.setInt(2, ticketId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al reabrir el ticket", e);
        }
    }

    public void fijarTiempoDeResolucion(int ticketId, int horas) {
        ejecutarUpdate("UPDATE ticket SET fecha_resolucion = datetime(fecha_creacion, '+' || ? || ' hours'), " +
                "fecha_actualizacion = datetime(fecha_creacion, '+' || ? || ' hours') " +
                "WHERE id = ? AND fecha_resolucion IS NOT NULL", horas, horas, ticketId);
    }

    private void ejecutarUpdate(String sql, int... parametros) {
        Connection con = ConexionBD.getConexion();
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) {
                stmt.setInt(i + 1, parametros[i]);
            }
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar el ticket", e);
        }
    }

    public void cambiarEstado(int ticketId, String estado) {

        String sql = "UPDATE ticket SET estado = ?, fecha_actualizacion = datetime('now', 'localtime'), " +
                "fecha_resolucion = CASE WHEN ? IN ('resuelto', 'cerrado') " +
                "THEN COALESCE(fecha_resolucion, datetime('now', 'localtime')) ELSE NULL END WHERE id = ?";
        Connection con = ConexionBD.getConexion();
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, estado);
            stmt.setString(2, estado);
            stmt.setInt(3, ticketId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al cambiar el estado del ticket", e);
        }
    }

    public void ponerAntiguedad(int ticketId, int horas) {
        String desplazar = "'-' || ? || ' hours'";
        String[] sqls = {
                "UPDATE ticket SET fecha_creacion = datetime(fecha_creacion, " + desplazar + "), " +
                        "fecha_actualizacion = datetime(fecha_actualizacion, " + desplazar + "), " +
                        "fecha_limite = datetime(fecha_limite, " + desplazar + "), " +
                        "fecha_resolucion = datetime(fecha_resolucion, " + desplazar + ") WHERE id = ?",
                "UPDATE actividad SET fecha = datetime(fecha, " + desplazar + ") WHERE ticket_id = ?",
                "UPDATE comentario SET fecha = datetime(fecha, " + desplazar + ") WHERE ticket_id = ?"
        };
        int[] parametrosHoras = {4, 1, 1};
        Connection con = ConexionBD.getConexion();
        try {
            for (int i = 0; i < sqls.length; i++) {
                try (PreparedStatement stmt = con.prepareStatement(sqls[i])) {
                    for (int p = 1; p <= parametrosHoras[i]; p++) {
                        stmt.setInt(p, horas);
                    }
                    stmt.setInt(parametrosHoras[i] + 1, ticketId);
                    stmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al retrasar el ticket", e);
        }
    }

    public void actualizarFecha(int ticketId) {
        String sql = "UPDATE ticket SET fecha_actualizacion = datetime('now', 'localtime') WHERE id = ?";
        Connection con = ConexionBD.getConexion();
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, ticketId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar la fecha del ticket", e);
        }
    }

    public Map<String, Integer> contarPorEstado() {
        Map<String, Integer> conteo = new LinkedHashMap<>();
        conteo.put("abierto", 0);
        conteo.put("en progreso", 0);
        conteo.put("resuelto", 0);
        conteo.put("cerrado", 0);

        String sql = "SELECT estado, COUNT(*) AS total FROM ticket GROUP BY estado";
        Connection con = ConexionBD.getConexion();
        try (PreparedStatement stmt = con.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                conteo.put(rs.getString("estado"), rs.getInt("total"));
            }
            return conteo;
        } catch (SQLException e) {
            throw new RuntimeException("Error al contar tickets por estado", e);
        }
    }

    public Map<String, Integer> contarTicketsPorCategoria() {
        Map<String, Integer> conteo = new LinkedHashMap<>();
        String sql = "SELECT c.nombre, COUNT(t.id) AS total FROM categoria c " +
                "LEFT JOIN ticket t ON t.categoria_id = c.id " +
                "GROUP BY c.id ORDER BY total DESC, c.nombre";
        Connection con = ConexionBD.getConexion();
        try (PreparedStatement stmt = con.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                conteo.put(rs.getString("nombre"), rs.getInt("total"));
            }
            return conteo;
        } catch (SQLException e) {
            throw new RuntimeException("Error al contar tickets por categoría", e);
        }
    }

    public int contarPorCategoria(int categoriaId) {
        return contar("SELECT COUNT(*) FROM ticket WHERE categoria_id = ?", categoriaId);
    }

    public int contarPorUsuario(int usuarioId) {
        return contar("SELECT COUNT(*) FROM ticket WHERE creador_id = ? OR tecnico_asignado_id = ?", usuarioId, usuarioId);
    }

    private int contar(String sql, int... parametros) {
        Connection con = ConexionBD.getConexion();
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) {
                stmt.setInt(i + 1, parametros[i]);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al contar tickets", e);
        }
    }

    private List<Ticket> listar(String sql, int... parametros) {
        List<Ticket> tickets = new ArrayList<>();
        Connection con = ConexionBD.getConexion();
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) {
                stmt.setInt(i + 1, parametros[i]);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    tickets.add(mapearTicket(rs));
                }
                return tickets;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar tickets", e);
        }
    }

    private Ticket mapearTicket(ResultSet rs) throws SQLException {
        Ticket ticket = new Ticket();
        ticket.setId(rs.getInt("id"));
        ticket.setTitulo(rs.getString("titulo"));
        ticket.setDescripcion(rs.getString("descripcion"));
        ticket.setPrioridad(rs.getString("prioridad"));
        ticket.setEstado(rs.getString("estado"));
        ticket.setCategoriaId(rs.getInt("categoria_id"));
        ticket.setCreadorId(rs.getInt("creador_id"));
        int tecnicoId = rs.getInt("tecnico_asignado_id");
        ticket.setTecnicoAsignadoId(rs.wasNull() ? null : tecnicoId);
        ticket.setFechaCreacion(rs.getString("fecha_creacion"));
        ticket.setFechaActualizacion(rs.getString("fecha_actualizacion"));
        ticket.setCategoriaNombre(rs.getString("categoria_nombre"));
        ticket.setCreadorNombre(rs.getString("creador_nombre"));
        ticket.setTecnicoNombre(rs.getString("tecnico_nombre"));
        ticket.setFechaLimite(rs.getString("fecha_limite"));
        ticket.setFechaResolucion(rs.getString("fecha_resolucion"));
        int valoracion = rs.getInt("valoracion");
        ticket.setValoracion(rs.wasNull() ? null : valoracion);
        ticket.setReaperturas(rs.getInt("reaperturas"));
        ticket.setCancelado(rs.getInt("cancelado") == 1);
        return ticket;
    }
}
