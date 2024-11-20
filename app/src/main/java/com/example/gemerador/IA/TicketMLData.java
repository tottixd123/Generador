package com.example.gemerador.IA;

import com.example.gemerador.Data_base.Ticket;

import java.util.HashMap;
import java.util.Map;

public class TicketMLData {
    private String area;
    private String priority;
    private long resolutionTime;
    private String assignedWorker;
    private boolean resolved;

    private static final Map<String, Integer> areaMap = new HashMap<>();
    private static final Map<String, Integer> priorityMap = new HashMap<>();
    private static final Map<String, Integer> workerMap = new HashMap<>();

    static {
        // Mapeos de área (actualizar según tus áreas reales)
        areaMap.put("Mantenimiento de impresora", 0);
        areaMap.put("Mantenimiento de ordenador", 1);
        areaMap.put("Cambio de cable Internet", 2);
        areaMap.put("Registro SIGA", 3);
        areaMap.put("Registro SIAF", 4);
        areaMap.put("Instalacion de aplicaciones", 5);

        // Mapeos de prioridad según tus constantes
        priorityMap.put(Ticket.PRIORITY_LOW, 0);    // "Baja"
        priorityMap.put(Ticket.PRIORITY_NORMAL, 1); // "Normal"
        priorityMap.put(Ticket.PRIORITY_HIGH, 2);   // "Alta"
    }

    public TicketMLData(String area, String priority, long resolutionTime, String assignedWorker, boolean resolved) {
        this.area = area;
        this.priority = priority;
        this.resolutionTime = resolutionTime;
        this.assignedWorker = assignedWorker;
        this.resolved = resolved;
    }

    // Getters
    public String getArea() { return area; }
    public String getPriority() { return priority; }
    public long getResolutionTime() { return resolutionTime; }
    public String getAssignedWorker() { return assignedWorker; }
    public boolean isResolved() { return resolved; }

    // Método para convertir a array de double (usado por OnlineTicketModel)
    public double[] toDoubleArray() {
        double[] features = new double[4];
        features[0] = areaMap.getOrDefault(area, -1);
        features[1] = priorityMap.getOrDefault(priority, -1);
        features[2] = (double) resolutionTime / 3600000; // Convertir a horas
        features[3] = workerMap.getOrDefault(assignedWorker, -1);
        return features;
    }
    // Método para convertir a array de float (usado por TFLite)
    public float[] toFloatArray() {
        float[] features = new float[3];

        // Convertir área a valor numérico
        Integer areaValue = areaMap.get(area);
        features[0] = areaValue != null ? areaValue : -1;

        // Convertir prioridad a valor numérico
        Integer priorityValue = priorityMap.get(priority);
        features[1] = priorityValue != null ? priorityValue : 1; // Valor por defecto: normal

        // Normalizar el tiempo de resolución
        features[2] = resolutionTime > 0 ? (float) resolutionTime / 3600000 : 0;

        return features;
    }
}
