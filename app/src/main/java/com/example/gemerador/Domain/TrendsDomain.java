package com.example.gemerador.Domain;

public class TrendsDomain {
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public int getPicAddress() {
        return picAddress;
    }

    public void setPicAddress(int picAddress) {
        this.picAddress = picAddress;
    }

    public TrendsDomain(String title, String fecha, String estado, int picAddress) {
        this.title = title;
        this.fecha = fecha;
        this.estado = estado;
        this.picAddress = picAddress;
    }

    private String title;
    private String fecha;
    private String estado;
    private int picAddress;

}
