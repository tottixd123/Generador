package com.example.gemerador.IA;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.ArrayRealVector;

import java.util.List;
import java.util.Random;

public class OnlineTicketModel {
    private RealVector weights;
    private double learningRate;

    public OnlineTicketModel(int featureCount) {
        weights = new ArrayRealVector(featureCount);
        learningRate = 0.01; // Ajusta según sea necesario
    }

    public double predict(TicketMLData ticket) {
        RealVector features = new ArrayRealVector(ticket.toDoubleArray());
        return sigmoid(weights.dotProduct(features));
    }

    public void update(TicketMLData ticket, boolean actualOutcome) {
        RealVector features = new ArrayRealVector(ticket.toDoubleArray());
        double prediction = predict(ticket);
        double error = actualOutcome ? 1 - prediction : -prediction;

        weights = weights.add(features.mapMultiply(learningRate * error));
    }

    private double sigmoid(double x) {
        return 1 / (1 + Math.exp(-x));
    }

    // Nuevo método para recomendar un trabajador
    public String recommendWorker(List<String> availableWorkers) {
        if (availableWorkers.isEmpty()) {
            return null;
        }
        // Por ahora, simplemente seleccionamos un trabajador al azar
        // En una implementación más avanzada, podrías usar el modelo para hacer una recomendación más inteligente
        Random random = new Random();
        int index = random.nextInt(availableWorkers.size());
        return availableWorkers.get(index);
    }
}
