package com.example.gemerador.Notificaciones;

public class TicketNotification {
    private String ticketId;
    private String status;
    private String message;
    private String timestamp;
    private String priority;
    private String username;

    public TicketNotification(String ticketId, String status, String message,
                              String timestamp, String priority, String username) {
        this.ticketId = ticketId;
        this.status = status;
        this.message = message;
        this.timestamp = timestamp;
        this.priority = priority;
        this.username = username;
    }

    // Getters
    public String getTicketId() { return ticketId; }
    public String getStatus() { return status; }
    public String getMessage() { return message; }
    public String getTimestamp() { return timestamp; }
    public String getPriority() { return priority; }
    public String getUsername() { return username; }
}
