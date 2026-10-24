package com.itdence.servicio;

import com.itdence.dao.ConexionBD;
import com.itdence.modelo.Categoria;
import com.itdence.modelo.Usuario;
import org.junit.jupiter.api.BeforeAll;

import java.io.File;

// Todos los tests usan la BD de target/test-data (se configura en el pom con itdence.bd).
// La primera vez se borra para empezar de cero y se cargan los datos de prueba.
public abstract class BaseTest {

    private static boolean preparada = false;

    protected static final AuthServicio auth = new AuthServicio();

    @BeforeAll
    static void prepararBD() {
        if (!preparada) {
            String ruta = System.getProperty("itdence.bd", "target/test-data/itdence-test.db");
            System.setProperty("itdence.bd", ruta);
            new File(ruta).delete();
            ConexionBD.getConexion();
            new DatosIniciales().cargar();
            preparada = true;
        }
    }

    protected Usuario cliente() {
        return auth.login("cliente@itdence.com", "cliente123");
    }

    protected Usuario tecnico() {
        return auth.login("tecnico@itdence.com", "tecnico123");
    }

    protected Usuario admin() {
        return auth.login("admin@itdence.com", "admin123");
    }

    protected Categoria primeraCategoria() {
        return new CategoriaServicio().listarTodas().get(0);
    }

    // email distinto en cada llamada para que los tests no choquen entre ellos
    protected String emailUnico() {
        return "prueba" + System.nanoTime() + "@itdence.com";
    }
}
