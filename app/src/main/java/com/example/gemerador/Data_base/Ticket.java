package com.example.gemerador.Data_base;

import com.google.firebase.database.DatabaseReference;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Ticket {
    // Constantes de estado
    public static final String STATUS_PENDING = "Pendiente";
    public static final String STATUS_IN_PROGRESS = "En Proceso";
    public static final String STATUS_COMPLETED = "Terminado";

    // Constantes de prioridad
    public static final String PRIORITY_LOW = "Baja";
    public static final String PRIORITY_NORMAL = "Normal";
    public static final String PRIORITY_HIGH = "Alta";

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
    private String priority=PRIORITY_NORMAL;
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
        this.priority = PRIORITY_NORMAL;
        this.lastUpdated = creationDate;
    }
    // Métodos de actualización para Firebase
    public void updateStatus(String newStatus, String workerId, DatabaseReference dbRef) {
        if (!isValidStatus(newStatus)) {
            throw new IllegalArgumentException("Estado no válido: " + newStatus);
        }

        this.status = newStatus;
        this.lastUpdated = getCurrentTimestamp();

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", newStatus);
        updates.put("lastUpdated", lastUpdated);

        if (workerId != null) {
            updates.put("lastModifiedBy", workerId);
        }

        if (dbRef != null) {
            dbRef.child("tickets").child(id).updateChildren(updates);
        }
    }
    public void updatePriority(String newPriority, DatabaseReference dbRef) {
        if (!isValidPriority(newPriority)) {
            throw new IllegalArgumentException("Prioridad no válida: " + newPriority);
        }

        this.priority = newPriority;
        this.lastUpdated = getCurrentTimestamp();

        Map<String, Object> updates = new HashMap<>();
        updates.put("priority", newPriority);
        updates.put("lastUpdated", lastUpdated);

        if (dbRef != null) {
            dbRef.child("tickets").child(id).updateChildren(updates);
        }
    }
    public void assignWorker(String workerId, String workerName, DatabaseReference dbRef) {
        this.assignedWorkerId = workerId;
        this.assignedWorkerName = workerName;
        this.lastUpdated = getCurrentTimestamp();

        Map<String, Object> updates = new HashMap<>();
        updates.put("assignedWorkerId", workerId);
        updates.put("assignedWorkerName", workerName);
        updates.put("lastUpdated", lastUpdated);

        if (dbRef != null) {
            dbRef.child("tickets").child(id).updateChildren(updates);
        }
    }

    public void addComment(String comment, String userId, String userName, DatabaseReference dbRef) {
        String timestamp = getCurrentTimestamp();
        String newComment = String.format("[%s] %s: %s\n", timestamp, userName, comment);

        this.comments = (this.comments == null || this.comments.isEmpty())
                ? newComment
                : this.comments + newComment;

        Map<String, Object> updates = new HashMap<>();
        updates.put("comments", this.comments);
        updates.put("lastUpdated", timestamp);

        if (dbRef != null) {
            dbRef.child("tickets").child(id).updateChildren(updates);
        }
    }
    // Métodos de validación
    private boolean isValidStatus(String status) {
        return status != null && (
                status.equals(STATUS_PENDING) ||
                        status.equals(STATUS_IN_PROGRESS) ||
                        status.equals(STATUS_COMPLETED)
        );
    }

    private boolean isValidPriority(String priority) {
        return priority != null && (
                priority.equals(PRIORITY_LOW) ||
                        priority.equals(PRIORITY_NORMAL) ||
                        priority.equals(PRIORITY_HIGH)
        );
    }

    private String getCurrentTimestamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date());
    }

        // Getters y setters para campos básicos
    public String getTicketNumber() {
        return ticketNumber != null ? ticketNumber : "";
    }

    public void setTicketNumber(String ticketNumber) {
        this.ticketNumber = ticketNumber;
    }

    public String getProblemSpinner() {
        return problemSpinner != null ? problemSpinner : "";
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
    // Métodos de utilidad adicionales
    public boolean isAssignedTo(String workerId) {
        return this.assignedWorkerId != null && this.assignedWorkerId.equals(workerId);
    }

    public boolean canBeModifiedBy(String userId, String userRole) {
        return "Administrador".equals(userRole) ||
                (isAssignedTo(userId) && "Trabajador".equals(userRole));
    }
}