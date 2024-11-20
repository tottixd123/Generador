package com.example.gemerador.IA;

public class WorkerPerformance {
    private String workerId;
    private String nombre;
    private int ticketsCompletados;
    private int ticketsPendientes;
    private double tiempoPromedioResolucion;
    private double tasaSatisfaccion;

    public WorkerPerformance() {
        // Constructor vacío requerido para Firebase
    }

    public WorkerPerformance(String workerId, String nombre) {
        this.workerId = workerId;
        this.nombre = nombre;
        this.ticketsCompletados = 0;
        this.ticketsPendientes = 0;
        this.tiempoPromedioResolucion = 0.0;
        this.tasaSatisfaccion = 0.0;
    }

    // Getters y Setters
    public String getWorkerId() { return workerId; }
    public void setWorkerId(String workerId) { this.workerId = workerId; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public int getTicketsCompletados() { return ticketsCompletados; }
    public void setTicketsCompletados(int ticketsCompletados) {
        this.ticketsCompletados = ticketsCompletados;
    }

    public int getTicketsPendientes() { return ticketsPendientes; }
    public void setTicketsPendientes(int ticketsPendientes) {
        this.ticketsPendientes = ticketsPendientes;
    }

    public double getTiempoPromedioResolucion() { return tiempoPromedioResolucion; }
    public void setTiempoPromedioResolucion(double tiempoPromedioResolucion) {
        this.tiempoPromedioResolucion = tiempoPromedioResolucion;
    }

    public double getTasaSatisfaccion() { return tasaSatisfaccion; }
    public void setTasaSatisfaccion(double tasaSatisfaccion) {
        this.tasaSatisfaccion = tasaSatisfaccion;
    }
}