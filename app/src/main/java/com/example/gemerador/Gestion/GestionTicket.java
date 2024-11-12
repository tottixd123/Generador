package com.example.gemerador.Gestion;

public class GestionTicket {
    public GestionTicket(String ticketId, String ticketNumber, String status, String assignedWorkerId, String assignedWorkerName, String priority, String lastUpdated, String comments) {
        this.ticketId = ticketId;
        this.ticketNumber = ticketNumber;
        this.status = status;
        this.assignedWorkerId = assignedWorkerId;
        this.assignedWorkerName = assignedWorkerName;
        this.priority = priority;
        this.lastUpdated = lastUpdated;
        this.comments = comments;
    }
    public String getTicketId() {
        return ticketId;
    }

    public void setTicketId(String ticketId) {
        this.ticketId = ticketId;
    }

    public String getTicketNumber() {
        return ticketNumber;
    }

    public void setTicketNumber(String ticketNumber) {
        this.ticketNumber = ticketNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
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
    private String ticketId;
    private String ticketNumber;
    private String status;
    private String assignedWorkerId;
    private String assignedWorkerName;
    private String priority;
    private String lastUpdated;
    private String comments;
}
