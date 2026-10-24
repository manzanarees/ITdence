package com.itdence.dao;
import com.itdence.modelo.Sla;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class ConexionBD {
    private static final String RUTA_POR_DEFECTO = "data/itdence.db";
    private static Connection conexion;

    private ConexionBD() {}

    public static Connection getConexion(){
        if (conexion == null){
            try {
                File fichero = new File(System.getProperty("itdence.bd", RUTA_POR_DEFECTO));
                if (fichero.getParentFile() != null) {
                    fichero.getParentFile().mkdirs();
                }
                conexion = DriverManager.getConnection("jdbc:sqlite:" + fichero.getPath());
                try (Statement stmt = conexion.createStatement()) {
                    stmt.execute("PRAGMA foreign_keys = ON");
                }
                inicializarEsquema(conexion);
                migrar(conexion);
            } catch (SQLException e){
                throw new RuntimeException("Error al conectar con la base de datos", e);
            }
        }
        return conexion;
    }

    public static void cerrar() {
        if (conexion != null) {
            try {
                conexion.close();
            } catch (SQLException e) {
                System.err.println("No se ha podido cerrar la conexion: " + e.getMessage());
            }
            conexion = null;
        }
    }

    public static void migrar(Connection con) throws SQLException {
        anadirColumna(con, "comentario", "interno", "INTEGER NOT NULL DEFAULT 0");
        anadirColumna(con, "ticket", "fecha_limite", "TEXT");
        anadirColumna(con, "ticket", "fecha_resolucion", "TEXT");
        anadirColumna(con, "ticket", "valoracion", "INTEGER");
        anadirColumna(con, "ticket", "reaperturas", "INTEGER NOT NULL DEFAULT 0");
        anadirColumna(con, "ticket", "cancelado", "INTEGER NOT NULL DEFAULT 0");

        try (Statement stmt = con.createStatement()) {
            stmt.execute("UPDATE ticket SET fecha_limite = datetime(fecha_creacion, '+' || CASE prioridad " +
                    "WHEN 'alta' THEN " + Sla.horas("alta") + " WHEN 'media' THEN " + Sla.horas("media") +
                    " ELSE " + Sla.horas("baja") + " END || ' hours') WHERE fecha_limite IS NULL");
            stmt.execute("UPDATE ticket SET fecha_resolucion = fecha_actualizacion " +
                    "WHERE fecha_resolucion IS NULL AND estado IN ('resuelto', 'cerrado')");
        }

        try (Statement stmt = con.createStatement()) {
            stmt.execute("INSERT INTO actividad (ticket_id, usuario_id, tipo, detalle, fecha) " +
                    "SELECT id, creador_id, 'creado', 'creó el ticket', fecha_creacion FROM ticket " +
                    "WHERE id NOT IN (SELECT ticket_id FROM actividad WHERE tipo = 'creado')");
        }
    }


    private static boolean anadirColumna(Connection con, String tabla, String columna, String definicion) throws SQLException {
        try (Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA table_info(" + tabla + ")")) {
            while (rs.next()) {
                if (columna.equals(rs.getString("name"))) {
                    return false;
                }
            }
        }
        try (Statement stmt = con.createStatement()) {
            stmt.execute("ALTER TABLE " + tabla + " ADD COLUMN " + columna + " " + definicion);
        }
        return true;
    }

    private static void inicializarEsquema (Connection conexion){
        try(InputStream in = ConexionBD.class.getResourceAsStream("/schema.sql")){
            if (in == null) {
                throw new RuntimeException("no se ha encontrado schema.sql en resources");
            }
            String script = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            try (Statement stmt = conexion.createStatement()) {
                for (String sentencia : script.split(";")) {
                    if (!sentencia.isBlank()){
                        stmt.execute(sentencia);
                    }
                }
            }
        } catch (IOException | SQLException e) {
            throw new RuntimeException("Error al iniciar el esquema de la base de datos", e);
        }
    }
}
