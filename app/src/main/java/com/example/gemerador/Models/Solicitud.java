package com.example.gemerador.Models;

import androidx.annotation.NonNull;

import java.io.Serializable;

public class Solicitud implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String nombre;
    private String email;
    private String area;
    private String cargo;
    private String numeroContacto;
    private String estado;

    // Constructor vacío necesario para Firebase
    public Solicitud() {
        // Inicializar con valores por defecto
        this.estado = "pendiente";
    }

    // Constructor completo
    public Solicitud(String id, String nombre, String email, String area,
                     String cargo, String numeroContacto, String estado) {
        this.id = id;
        this.nombre = nombre;
        this.email = email;
        this.area = area;
        this.cargo = cargo;
        this.numeroContacto = numeroContacto;
        this.estado = estado != null ? estado : "pendiente";
    }

    // Getters y setters con validación
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id != null ? id.trim() : null;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre != null ? nombre.trim() : null;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email != null ? email.trim().toLowerCase() : null;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area != null ? area.trim() : null;
    }

    public String getCargo() {
        return cargo;
    }

    public void setCargo(String cargo) {
        this.cargo = cargo != null ? cargo.trim() : null;
    }

    public String getNumeroContacto() {
        return numeroContacto;
    }

    public void setNumeroContacto(String numeroContacto) {
        this.numeroContacto = numeroContacto != null ? numeroContacto.trim() : null;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado != null ? estado.trim().toLowerCase() : "pendiente";
    }

    // Método mejorado para verificar si la solicitud tiene todos los campos necesarios
    public boolean isValid() {
        return id != null && !id.trim().isEmpty() &&
                nombre != null && !nombre.trim().isEmpty() &&
                email != null && !email.trim().isEmpty() && email.contains("@") &&
                area != null && !area.trim().isEmpty() &&
                cargo != null && !cargo.trim().isEmpty() &&
                numeroContacto != null && !numeroContacto.trim().isEmpty() &&
                estado != null && !estado.trim().isEmpty();
    }

    @NonNull
    @Override
    public String toString() {
        return "Solicitud{" +
                "id='" + id + '\'' +
                ", nombre='" + nombre + '\'' +
                ", email='" + email + '\'' +
                ", area='" + area + '\'' +
                ", cargo='" + cargo + '\'' +
                ", numeroContacto='" + numeroContacto + '\'' +
                ", estado='" + estado + '\'' +
                '}';
    }
}