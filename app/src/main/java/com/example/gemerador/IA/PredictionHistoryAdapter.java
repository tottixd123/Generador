package com.example.gemerador.IA;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.gemerador.R;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PredictionHistoryAdapter extends RecyclerView.Adapter<PredictionHistoryAdapter.ViewHolder> {
    private List<PredictionHistoryItem> historyItems;
    private final SimpleDateFormat dateFormat;

    public PredictionHistoryAdapter(List<PredictionHistoryItem> historyItems) {
        this.historyItems = historyItems;
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_prediction_history_adapter, parent, false);
        return new ViewHolder(view);
    }
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PredictionHistoryItem item = historyItems.get(position);

        // Área del problema
        holder.tvAreaProblema.setText(item.getAreaProblema());

        // Prioridad
        String prioridad = "Prioridad: " + item.getPriority();
        holder.tvPrioridad.setText(prioridad);

        // Trabajador recomendado
        String trabajador = "Trabajador: " + item.getRecommendedWorker();
        holder.tvTrabajador.setText(trabajador);

        // Confianza
        String confianza = String.format(Locale.getDefault(), "Confianza: %.1f%%",
                item.getConfidence() * 100);
        holder.tvConfianza.setText(confianza);

        // Fecha
        String fecha = dateFormat.format(new Date(item.getTimestamp()));
        holder.tvFecha.setText(fecha);
    }

    @Override
    public int getItemCount() {
        return historyItems.size();
    }

    public void updateData(List<PredictionHistoryItem> newItems) {
        this.historyItems = newItems;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAreaProblema;
        TextView tvPrioridad;
        TextView tvTrabajador;
        TextView tvConfianza;
        TextView tvFecha;

        ViewHolder(View view) {
            super(view);
            tvAreaProblema = view.findViewById(R.id.tvAreaProblema);
            tvPrioridad = view.findViewById(R.id.tvPrioridad);
            tvTrabajador = view.findViewById(R.id.tvTrabajador);
            tvConfianza = view.findViewById(R.id.tvConfianza);
            tvFecha = view.findViewById(R.id.tvFecha);
        }
    }
}