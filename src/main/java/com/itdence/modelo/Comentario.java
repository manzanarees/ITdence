package com.itdence.modelo;

public class Comentario {
    private int id;
    private int ticketId;
    private int usuarioId;
    private String texto;
    private String fecha;
    private String autorNombre;
    private String autorRol;
    private boolean interno;

    public Comentario() {
    }

    public Comentario (int id, int ticketId, int usuarioId, String texto, String fecha){
        this.id = id;
        this.ticketId = ticketId;
        this.usuarioId = usuarioId;
        this.texto = texto;
        this.fecha = fecha;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getTicketId() {
        return ticketId;
    }

    public void setTicketId(int ticketId) {
        this.ticketId = ticketId;
    }

    public int getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(int usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public String getAutorNombre() {
        return autorNombre;
    }

    public void setAutorNombre(String autorNombre) {
        this.autorNombre = autorNombre;
    }

    public String getAutorRol() {
        return autorRol;
    }

    public void setAutorRol(String autorRol) {
        this.autorRol = autorRol;
    }

    public boolean isInterno() {
        return interno;
    }

    public void setInterno(boolean interno) {
        this.interno = interno;
    }
}
