package com.itdence.servicio;

import com.itdence.modelo.Categoria;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CategoriaServicioTest extends BaseTest {

    private final CategoriaServicio servicio = new CategoriaServicio();

    private Categoria buscar(String nombre) {
        return servicio.listarTodas().stream().filter(c -> c.getNombre().equals(nombre)).findFirst().orElse(null);
    }

    @Test
    void existenLasCuatroCategoriasIniciales() {
        for (String nombre : new String[]{"Hardware", "Software", "Red", "Cuenta"}) {
            assertNotNull(buscar(nombre), "Falta la categoría " + nombre);
        }
    }

    @Test
    void crearRenombrarYEliminar() {
        String nombre = "Temporal" + System.nanoTime();
        servicio.crear(nombre);
        Categoria c = buscar(nombre);
        assertNotNull(c);

        servicio.renombrar(c, nombre + "B");
        assertNotNull(buscar(nombre + "B"));

        servicio.eliminar(buscar(nombre + "B"));
        assertNull(buscar(nombre + "B"));
    }

    @Test
    void noSePuedeRepetirNombreAunqueCambienMayusculas() {
        assertThrows(ValidacionException.class, () -> servicio.crear("hardware"));
    }

    @Test
    void noSePuedeCrearVacia() {
        assertThrows(ValidacionException.class, () -> servicio.crear("  "));
    }

    @Test
    void noSePuedeEliminarUnaCategoriaConTickets() {
        // Hardware tiene tickets de los datos de ejemplo
        assertThrows(ValidacionException.class, () -> servicio.eliminar(buscar("Hardware")));
    }
}
