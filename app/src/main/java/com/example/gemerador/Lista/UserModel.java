package com.example.gemerador.Lista;

public class UserModel {
    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
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

    public String getCargo() {
        return cargo;
    }

    public void setCargo(String cargo) {
        this.cargo = cargo;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    private String uid;
    private String nombre;
    private String email;
    private String role;
    private String cargo;
    private String area;
    public UserModel() {
    }
    // Constructor con parámetros
    public UserModel(String uid, String nombre, String email, String role, String cargo, String area) {
        this.uid = uid != null ? uid : "";
        this.nombre = nombre != null ? nombre : "";
        this.email = email != null ? email : "";
        this.role = role != null ? role : "";
        this.cargo = cargo != null ? cargo : "";
        this.area = area != null ? area : "";
    }
}
