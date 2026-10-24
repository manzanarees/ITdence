package com.itdence.modelo;

// Un aviso para un usuario dentro de la app
public class Notificacion {
    private int id;
    private int usuarioId;
    private int ticketId;
    private String texto;
    private boolean leida;
    private String fecha;

    private String ticketTitulo;

    public Notificacion() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(int usuarioId) {
        this.usuarioId = usuarioId;
    }

    public int getTicketId() {
        return ticketId;
    }

    public void setTicketId(int ticketId) {
        this.ticketId = ticketId;
    }

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public boolean isLeida() {
        return leida;
    }

    public void setLeida(boolean leida) {
        this.leida = leida;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public String getTicketTitulo() {
        return ticketTitulo;
    }

    public void setTicketTitulo(String ticketTitulo) {
        this.ticketTitulo = ticketTitulo;
    }

    public String getTicketCodigo() {
        return String.format("#%04d", ticketId);
    }
}
