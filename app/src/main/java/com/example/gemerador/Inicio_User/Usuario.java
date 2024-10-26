package com.example.gemerador.Inicio_User;

public class Usuario {
    public Usuario(String nombre, String email, String role, String area, String cargo) {
        this.nombre = nombre;
        this.email = email;
        this.role = role;
        this.area = area;
        this.cargo = cargo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public String getCargo() {
        return cargo;
    }

    public void setCargo(String cargo) {
        this.cargo = cargo;
    }

    public String nombre;
    public String email;
    public String role;
    public String area;
    public String cargo;



}
