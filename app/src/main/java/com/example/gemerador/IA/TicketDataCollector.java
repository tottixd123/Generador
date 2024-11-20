package com.example.gemerador.IA;

import com.example.gemerador.Data_base.Ticket;

import java.util.ArrayList;
import java.util.List;

public class TicketDataCollector {
    private List<TicketMLData> trainingData;
    private static final int MAX_TRAINING_SAMPLES = 1000; // Ajusta según tus necesidades

    public TicketDataCollector() {
        trainingData = new ArrayList<>();
    }

    public void addTicketData(Ticket ticket) {
        TicketMLData mlData = new TicketMLData(
                ticket.getArea_problema(),
                ticket.getPriority(),
                calculateResolutionTime(ticket),
                ticket.getAssignedWorkerId(),
                ticket.getStatus().equals(Ticket.STATUS_COMPLETED)
        );
        trainingData.add(mlData);
        if (trainingData.size() > MAX_TRAINING_SAMPLES) {
            trainingData.remove(0); // Mantén un tamaño máximo de datos de entrenamiento
        }
    }
    public List<TicketMLData> getTrainingData() {
        return trainingData;
    }
    private long calculateResolutionTime(Ticket ticket) {
        // Implementa el cálculo del tiempo de resolución
        return 0;
    }
}