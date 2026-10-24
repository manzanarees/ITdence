package com.itdence.dao;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Comprueba que una BD creada con una version anterior de la app se actualiza
// sola y no pierde los datos. Se crea a mano una BD con el esquema viejo.
class MigracionTest {

    private static List<String> columnas(Connection con, String tabla) throws Exception {
        List<String> nombres = new ArrayList<>();
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery("PRAGMA table_info(" + tabla + ")")) {
            while (rs.next()) {
                nombres.add(rs.getString("name"));
            }
        }
        return nombres;
    }

    @Test
    void unaBaseDeDatosAntiguaRecibeLasColumnasNuevasYConservaLosDatos() throws Exception {
        File fichero = new File("target/test-data/antigua-" + System.nanoTime() + ".db");
        fichero.getParentFile().mkdirs();
        try (Connection con = DriverManager.getConnection("jdbc:sqlite:" + fichero.getPath());
             Statement st = con.createStatement()) {
            // el esquema de la primera version, sin las columnas nuevas
            st.execute("CREATE TABLE usuario (id INTEGER PRIMARY KEY AUTOINCREMENT, nombre TEXT NOT NULL, email TEXT NOT NULL UNIQUE, contrasena_hash TEXT NOT NULL, rol TEXT NOT NULL)");
            st.execute("CREATE TABLE categoria (id INTEGER PRIMARY KEY AUTOINCREMENT, nombre TEXT NOT NULL UNIQUE)");
            st.execute("CREATE TABLE ticket (id INTEGER PRIMARY KEY AUTOINCREMENT, titulo TEXT NOT NULL, descripcion TEXT, prioridad TEXT NOT NULL, estado TEXT NOT NULL DEFAULT 'abierto', categoria_id INTEGER NOT NULL, creador_id INTEGER NOT NULL, tecnico_asignado_id INTEGER, fecha_creacion TEXT NOT NULL DEFAULT (datetime('now', 'localtime')), fecha_actualizacion TEXT NOT NULL DEFAULT (datetime('now', 'localtime')))");
            st.execute("CREATE TABLE comentario (id INTEGER PRIMARY KEY AUTOINCREMENT, ticket_id INTEGER NOT NULL, usuario_id INTEGER NOT NULL, texto TEXT NOT NULL, fecha TEXT NOT NULL DEFAULT (datetime('now', 'localtime')))");
            // la tabla nueva la crea schema.sql antes de migrar, aqui se imita
            st.execute("CREATE TABLE actividad (id INTEGER PRIMARY KEY AUTOINCREMENT, ticket_id INTEGER NOT NULL, usuario_id INTEGER NOT NULL, tipo TEXT NOT NULL, detalle TEXT, fecha TEXT NOT NULL DEFAULT (datetime('now', 'localtime')))");
            st.execute("INSERT INTO usuario (nombre, email, contrasena_hash, rol) VALUES ('Ana', 'ana@x.com', 'hash', 'cliente')");
            st.execute("INSERT INTO categoria (nombre) VALUES ('Red')");
            st.execute("INSERT INTO ticket (titulo, prioridad, categoria_id, creador_id, fecha_creacion, fecha_actualizacion) VALUES ('Antiguo', 'alta', 1, 1, '2026-09-01 10:00:00', '2026-09-01 10:00:00')");
            st.execute("INSERT INTO ticket (titulo, prioridad, estado, categoria_id, creador_id, fecha_creacion, fecha_actualizacion) VALUES ('Viejo resuelto', 'baja', 'resuelto', 1, 1, '2026-09-01 09:00:00', '2026-09-02 08:30:00')");
            st.execute("INSERT INTO comentario (ticket_id, usuario_id, texto) VALUES (1, 1, 'comentario viejo')");

            assertFalse(columnas(con, "comentario").contains("interno"));

            ConexionBD.migrar(con);

            assertTrue(columnas(con, "comentario").contains("interno"));
            // los datos de antes siguen ahi y el comentario viejo no es interno
            try (ResultSet rs = st.executeQuery("SELECT texto, interno FROM comentario")) {
                assertTrue(rs.next());
                assertEquals("comentario viejo", rs.getString("texto"));
                assertEquals(0, rs.getInt("interno"));
            }

            // cada ticket antiguo recibe su linea de historial con su propia fecha
            try (ResultSet rs = st.executeQuery("SELECT a.tipo, a.fecha, t.fecha_creacion FROM actividad a JOIN ticket t ON t.id = a.ticket_id")) {
                int filas = 0;
                while (rs.next()) {
                    assertEquals("creado", rs.getString("tipo"));
                    assertEquals(rs.getString("fecha_creacion"), rs.getString("fecha"));
                    filas++;
                }
                assertEquals(2, filas);
            }

            // SLA: columnas nuevas y limites calculados con las horas de la prioridad
            assertTrue(columnas(con, "ticket").containsAll(List.of("fecha_limite", "fecha_resolucion", "valoracion", "reaperturas", "cancelado")));
            try (ResultSet rs = st.executeQuery("SELECT titulo, fecha_limite, fecha_resolucion FROM ticket ORDER BY id")) {
                assertTrue(rs.next());
                // alta: creado a las 10:00 y 4 horas de limite
                assertEquals("2026-09-01 14:00:00", rs.getString("fecha_limite"));
                assertNull(rs.getString("fecha_resolucion"));
                assertTrue(rs.next());
                // baja: 72 horas desde las 09:00 del dia 1
                assertEquals("2026-09-04 09:00:00", rs.getString("fecha_limite"));
                // el ya resuelto usa su ultima actualizacion como fecha de resolucion
                assertEquals("2026-09-02 08:30:00", rs.getString("fecha_resolucion"));
            }
            // los tickets antiguos empiezan sin valoracion y con 0 reaperturas
            try (ResultSet rs = st.executeQuery("SELECT valoracion, reaperturas, cancelado FROM ticket")) {
                while (rs.next()) {
                    assertNull(rs.getObject("valoracion"));
                    assertEquals(0, rs.getInt("reaperturas"));
                    assertEquals(0, rs.getInt("cancelado"));
                }
            }

            // y migrar dos veces no da error ni duplica el historial (la app lo ejecuta en cada arranque)
            assertDoesNotThrow(() -> ConexionBD.migrar(con));
            try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM actividad")) {
                assertTrue(rs.next());
                assertEquals(2, rs.getInt(1));
            }
        }
    }
}
