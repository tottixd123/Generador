package com.example.gemerador.Data_base;

public class Ticket {
    // Constantes de estado
    public static final String STATUS_PENDING = "Pendiente";
    public static final String STATUS_IN_PROGRESS = "En Proceso";
    public static final String STATUS_COMPLETED = "Terminado";

    // Campos básicos
    private String ticketNumber;
    private String problemSpinner;
    private String area_problema;
    private String detalle;
    private String imagen;
    private String id;
    private String createdBy;
    private String creationDate;
    private String userId;

    // Campos de gestión
    private String status = STATUS_PENDING;
    private String priority;
    private String assignedWorkerId;
    private String assignedWorkerName;
    private String lastUpdated;
    private String comments;

    // Constructor principal
    public Ticket(String ticketNumber, String problemSpinner, String area_problema,
                  String detalle, String imagen, String id, String createdBy,
                  String creationDate, String userId) {
        this.ticketNumber = ticketNumber;
        this.problemSpinner = problemSpinner;
        this.area_problema = area_problema;
        this.detalle = detalle;
        this.imagen = imagen;
        this.id = id;
        this.createdBy = createdBy;
        this.creationDate = creationDate;
        this.userId = userId;
        this.status = STATUS_PENDING;
        this.lastUpdated = creationDate;
    }

    // Getters y setters para campos básicos
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

    // Getters y setters para campos de gestión
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getAssignedWorkerId() {
        return assignedWorkerId;
    }

    public void setAssignedWorkerId(String assignedWorkerId) {
        this.assignedWorkerId = assignedWorkerId;
    }

    public String getAssignedWorkerName() {
        return assignedWorkerName;
    }

    public void setAssignedWorkerName(String assignedWorkerName) {
        this.assignedWorkerName = assignedWorkerName;
    }

    public String getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(String lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }
}