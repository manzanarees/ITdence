package com.itdence.modelo;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

// SLA (acuerdo de nivel de servicio): tiempo maximo para resolver un ticket segun su prioridad
public final class Sla {

    public enum Situacion { EN_PLAZO, POR_VENCER, VENCIDO, CUMPLIDO, INCUMPLIDO, CANCELADO }

    private static final DateTimeFormatter FORMATO_BD = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final double UMBRAL_POR_VENCER = 0.25;

    private Sla() {}

    public static int horas(String prioridad) {
        switch (prioridad) {
            case "alta": return 4;
            case "media": return 24;
            default: return 72;
        }
    }

    public static LocalDateTime parsear(String fechaBD) {
        if (fechaBD == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(fechaBD, FORMATO_BD);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static boolean estaTerminado(Ticket t) {
        return "resuelto".equals(t.getEstado()) || "cerrado".equals(t.getEstado());
    }

    public static Situacion situacion(Ticket t, LocalDateTime ahora) {
        if (t.isCancelado()) {
            return Situacion.CANCELADO;
        }
        LocalDateTime limite = parsear(t.getFechaLimite());
        if (limite == null) {
            return Situacion.EN_PLAZO;
        }
        if (estaTerminado(t)) {
            LocalDateTime resolucion = parsear(t.getFechaResolucion());
            if (resolucion == null) {
                resolucion = parsear(t.getFechaActualizacion());
            }
            boolean aTiempo = resolucion == null || !resolucion.isAfter(limite);
            return aTiempo ? Situacion.CUMPLIDO : Situacion.INCUMPLIDO;
        }
        if (ahora.isAfter(limite)) {
            return Situacion.VENCIDO;
        }
        long minutosRestantes = Duration.between(ahora, limite).toMinutes();
        long minutosTotales = horas(t.getPrioridad()) * 60L;
        return minutosRestantes <= minutosTotales * UMBRAL_POR_VENCER ? Situacion.POR_VENCER : Situacion.EN_PLAZO;
    }

    public static String texto(Ticket t, LocalDateTime ahora) {
        LocalDateTime limite = parsear(t.getFechaLimite());
        switch (situacion(t, ahora)) {
            case CANCELADO: return "Cancelado";
            case CUMPLIDO: return "Cumplido";
            case INCUMPLIDO: return "Fuera de plazo";
            case VENCIDO: return "Vencido hace " + duracion(Duration.between(limite, ahora));
            default:
                return limite == null ? "Sin límite" : "Vence en " + duracion(Duration.between(ahora, limite));
        }
    }

    public static String duracion(Duration d) {
        long minutos = Math.max(0, d.toMinutes());
        if (minutos < 60) {
            return minutos + " min";
        }
        long horas = minutos / 60;
        long resto = minutos % 60;
        if (horas < 24) {
            // a partir de 10 h los minutos sobran (y no caben en las etiquetas de las tablas)
            return resto == 0 || horas >= 10 ? horas + " h" : horas + " h " + resto + " min";
        }
        long dias = horas / 24;
        long horasResto = horas % 24;
        return horasResto == 0 ? dias + " d" : dias + " d " + horasResto + " h";
    }
}
