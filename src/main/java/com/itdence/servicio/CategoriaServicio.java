package com.itdence.servicio;

import com.itdence.dao.CategoriaDAO;
import com.itdence.dao.TicketDAO;
import com.itdence.modelo.Categoria;

import java.util.List;

public class CategoriaServicio {

    private static final int MAX_NOMBRE = 40;

    private final CategoriaDAO categoriaDAO = new CategoriaDAO();
    private final TicketDAO ticketDAO = new TicketDAO();

    public List<Categoria> listarTodas() {
        return categoriaDAO.listarTodas();
    }

    public void crear(String nombre) {
        String limpio = validarNombre(nombre, -1);
        categoriaDAO.crear(new Categoria(0, limpio));
    }

    public void renombrar(Categoria categoria, String nuevoNombre) {
        String limpio = validarNombre(nuevoNombre, categoria.getId());
        categoriaDAO.actualizar(new Categoria(categoria.getId(), limpio));
    }

    public void eliminar(Categoria categoria) {
        int usos = ticketDAO.contarPorCategoria(categoria.getId());
        if (usos > 0) {
            throw new ValidacionException("No se puede eliminar: hay " + usos + " ticket(s) con esta categoría");
        }
        categoriaDAO.eliminar(categoria.getId());
    }

    private String validarNombre(String nombre, int idActual) {
        if (nombre == null || nombre.isBlank()) {
            throw new ValidacionException("El nombre de la categoría es obligatorio");
        }
        String limpio = nombre.trim();
        if (limpio.length() > MAX_NOMBRE) {
            throw new ValidacionException("El nombre no puede tener más de " + MAX_NOMBRE + " caracteres");
        }
        for (Categoria c : categoriaDAO.listarTodas()) {
            if (c.getNombre().equalsIgnoreCase(limpio) && c.getId() != idActual) {
                throw new ValidacionException("Ya existe una categoría con ese nombre");
            }
        }
        return limpio;
    }
}
