package com.example.gemerador.IA;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class PredictionHistoryItem {
    private String ticketId;
    private String areaProblema;
    private String priority;
    private String recommendedWorker;
    private double confidence;
    private long timestamp;
    private boolean verified;
    private boolean wasSuccessful;

    // Constructor vacío requerido para Firebase
    public PredictionHistoryItem() {
    }

    // Constructor completo
    public PredictionHistoryItem(String ticketId, String areaProblema, String priority,
                                 String recommendedWorker, double confidence, long timestamp,
                                 boolean verified, boolean wasSuccessful) {
        this.ticketId = ticketId;
        this.areaProblema = areaProblema;
        this.priority = priority;
        this.recommendedWorker = recommendedWorker;
        this.confidence = confidence;
        this.timestamp = timestamp;
        this.verified = verified;
        this.wasSuccessful = wasSuccessful;
    }

    // Getters y Setters
    public String getTicketId() {
        return ticketId;
    }

    public void setTicketId(String ticketId) {
        this.ticketId = ticketId;
    }

    public String getAreaProblema() {
        return areaProblema;
    }

    public void setAreaProblema(String areaProblema) {
        this.areaProblema = areaProblema;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getRecommendedWorker() {
        return recommendedWorker;
    }

    public void setRecommendedWorker(String recommendedWorker) {
        this.recommendedWorker = recommendedWorker;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public boolean isWasSuccessful() {
        return wasSuccessful;
    }

    public void setWasSuccessful(boolean wasSuccessful) {
        this.wasSuccessful = wasSuccessful;
    }
}