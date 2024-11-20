package com.example.gemerador.IA;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gemerador.R;

import java.util.List;

public class WorkerPerformanceAdapter extends RecyclerView.Adapter<WorkerPerformanceAdapter.ViewHolder> {
    private List<WorkerPerformance> workerPerformances;

    public WorkerPerformanceAdapter(List<WorkerPerformance> workerPerformances) {
        this.workerPerformances = workerPerformances;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.worker_performance_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WorkerPerformance performance = workerPerformances.get(position);
        holder.bind(performance);
    }

    @Override
    public int getItemCount() {
        return workerPerformances.size();
    }

    public void updateData(List<WorkerPerformance> newPerformances) {
        this.workerPerformances = newPerformances;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvWorkerName, tvTicketsCompletados, tvTicketsPendientes, tvTiempoPromedio;
        ProgressBar progressSatisfaccion;

        ViewHolder(View itemView) {
            super(itemView);
            tvWorkerName = itemView.findViewById(R.id.tvWorkerName);
            tvTicketsCompletados = itemView.findViewById(R.id.tvTicketsCompletados);
            tvTicketsPendientes = itemView.findViewById(R.id.tvTicketsPendientes);
            tvTiempoPromedio = itemView.findViewById(R.id.tvTiempoPromedio);
            progressSatisfaccion = itemView.findViewById(R.id.progressSatisfaccion);
        }

        void bind(WorkerPerformance performance) {
            tvWorkerName.setText(performance.getNombre());
            tvTicketsCompletados.setText(String.format("Completados: %d", performance.getTicketsCompletados()));
            tvTicketsPendientes.setText(String.format("Pendientes: %d", performance.getTicketsPendientes()));
            tvTiempoPromedio.setText(String.format("Tiempo promedio: %.1f hrs", performance.getTiempoPromedioResolucion()));
            progressSatisfaccion.setProgress((int) (performance.getTasaSatisfaccion() * 100));
        }
    }
}