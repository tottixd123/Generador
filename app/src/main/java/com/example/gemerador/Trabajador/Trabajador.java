package com.example.gemerador.Trabajador;

public class Trabajador {
    private String id;
    private String name;
    private String email;
    private String specialization;
    private boolean available;
    private int activeTickets;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public int getActiveTickets() {
        return activeTickets;
    }

    public void setActiveTickets(int activeTickets) {
        this.activeTickets = activeTickets;
    }
    public Trabajador(String id, String name, String email, String specialization, boolean available, int activeTickets) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.specialization = specialization;
        this.available = available;
        this.activeTickets = activeTickets;
    }

}
