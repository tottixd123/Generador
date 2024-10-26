package com.example.gemerador.Models;

public class Solicitud {
    private String id;
    private String nombre;
    private String email;
    private String area;
    private String cargo;
    private String numeroContacto;
    private String estado;

    // Constructor vacío necesario para Firebase
    public Solicitud() {}

    public Solicitud(String id, String nombre, String email, String area, String cargo, String numeroContacto, String estado) {
        this.id = id;
        this.nombre = nombre;
        this.email = email;
        this.area = area;
        this.cargo = cargo;
        this.numeroContacto = numeroContacto;
        this.estado = estado;
    }

    // Getters y setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }

    public String getCargo() { return cargo; }
    public void setCargo(String cargo) { this.cargo = cargo; }

    public String getNumeroContacto() { return numeroContacto; }
    public void setNumeroContacto(String numeroContacto) { this.numeroContacto = numeroContacto; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}