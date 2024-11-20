package com.example.gemerador.Data_base;

import com.google.firebase.database.DatabaseReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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
    private List<String> commentsList;
    //constructor vacio requerido por Firebase
    public Ticket() {
    }
    // Constructor principal
    public Ticket(String ticketNumber, String problemSpinner, String area_problema,
                  String detalle, String imagen, String id, String createdBy,
                  String creationDate, String userId){
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
        this.comments="";
        this.commentsList = new ArrayList<>();
    }
    public List<String> getCommentsList() {
        if (commentsList == null) {
            commentsList = new ArrayList<>();
            // Convertir el string de comentarios a lista si existe
            if (comments != null && !comments.isEmpty()) {
                String[] commentArray = comments.split("\n");
                for (String comment : commentArray) {
                    if (!comment.trim().isEmpty()) {
                        commentsList.add(comment.trim());
                    }
                }
            }
        }
        return commentsList;
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
        String formattedComment = String.format("[%s] %s: %s", timestamp, userName, comment);
        // Actualizar el string de comentarios
        this.comments = (this.comments == null || this.comments.isEmpty())
                ? formattedComment
                : this.comments + "\n" + formattedComment;
        // Actualizar la lista de comentarios
        if (commentsList == null) {
            commentsList = new ArrayList<>();
        }
        commentsList.add(formattedComment);
        // Preparar actualizaciones para Firebase
        Map<String, Object> updates = new HashMap<>();
        updates.put("comments", this.comments);
        updates.put("lastUpdated", timestamp);
        updates.put("lastModifiedBy", userId);
        // Actualizar Firebase si hay una referencia válida
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
        // Actualizar también la lista de comentarios
        if (comments != null && !comments.isEmpty()) {
            commentsList = new ArrayList<>();
            String[] commentArray = comments.split("\n");
            for (String comment : commentArray) {
                if (!comment.trim().isEmpty()) {
                    commentsList.add(comment.trim());
                }
            }
        } else {
            commentsList = new ArrayList<>();
        }
    }
    // Método para verificar si un ticket está asignado a un trabajador
    public boolean isAssignedTo(String workerId) {
        return assignedWorkerId != null && assignedWorkerId.equals(workerId);
    }
    // Método para verificar si un usuario puede modificar el ticket
    public boolean canBeModifiedBy(String userId, String userRole) {
        return "Administrador".equals(userRole) ||
                (isAssignedTo(userId) && "Trabajador".equals(userRole));
    }
    public boolean hasPrediction() {
        // Retorna true si este ticket tiene una predicción asociada
        return getPredictedTime() > 0;
    }

    public boolean isPredictionSuccessful() {
        // Implementa la lógica para determinar si la predicción fue exitosa
        // Por ejemplo, comparando el tiempo predicho con el tiempo real
        if (!hasPrediction() || getStatus().equals("Pending")) {
            return false;
        }

        long predictedTime = getPredictedTime();
        long actualTime = getActualResolutionTime();

        // Considera exitosa si la diferencia es menor al 20%
        return Math.abs(predictedTime - actualTime) <= (predictedTime * 0.2);
    }

    public long getPredictedTime() {
        // Retorna el tiempo predicho para este ticket
        // Implementa según tu modelo de datos
        return 0;
    }

    public long getActualResolutionTime() {
        // Retorna el tiempo real que tomó resolver el ticket
        // Implementa según tu modelo de datos
        return 0;
    }
}