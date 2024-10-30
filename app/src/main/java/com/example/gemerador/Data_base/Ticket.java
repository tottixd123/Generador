package com.example.gemerador.Data_base;

public class Ticket {
    private String ticketNumber;

    public Ticket(String ticketNumber, String problemSpinner, String area_problema, String detalle, String imagen, String id, String createdBy, String creationDate, String userId) {
        this.ticketNumber = ticketNumber;
        this.problemSpinner = problemSpinner;
        this.area_problema = area_problema;
        this.detalle = detalle;
        this.imagen = imagen;
        this.id = id;
        this.createdBy=createdBy;
        this.creationDate = creationDate;
        this.userId = userId;
    }
    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
    private String problemSpinner;

    public String getArea_problema() {
        return area_problema;
    }

    public void setArea_problema(String area_problema) {
        this.area_problema = area_problema;
    }

    public String getDetalle() {
        return detalle;
    }

    public void setDetalle(String detalle) {
        this.detalle = detalle;
    }

    public String getImagen() {
        return imagen;
    }

    public void setImagen(String imagen) {
        this.imagen = imagen;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTicketNumber() {
        return ticketNumber;
    }

    public void setTicketNumber(String ticketNumber) {
        this.ticketNumber = ticketNumber;
    }

    public String getProblemSpinner() {
        return problemSpinner;
    }

    public void setProblemSpinner(String problemSpinner) {
        this.problemSpinner = problemSpinner;
    }

    private String area_problema;
    private String detalle;
    private String imagen;
    private String id;
    private String createdBy; // Usuario que creó el ticket
    private String creationDate; // Fecha de creación
    private String userId; // ID del usuario que creó el ticket
}
