package com.example.gemerador.Adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.gemerador.Crear_Ti.TicketDetail;
import com.example.gemerador.Data_base.Ticket;
import com.example.gemerador.R;

import java.util.ArrayList;
import java.util.List;

public class TicketAdapter extends RecyclerView.Adapter<TicketAdapter.ViewHolder> {
    private List<Ticket> tickets;
    private List<Ticket> filteredTickets;
    private Context context;

    public TicketAdapter(List<Ticket> tickets) {
        this.tickets = tickets;
        this.filteredTickets = new ArrayList<>(tickets);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_ticket, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Ticket ticket = filteredTickets.get(position);

        // Información básica del ticket
        holder.tvTicketNumber.setText("Ticket #" + ticket.getTicketNumber());
        holder.tvCreator.setText("Creado por: " + ticket.getCreatedBy());
        holder.tvDate.setText("Fecha: " + ticket.getCreationDate());
        holder.tvProblemType.setText("Problema: " + ticket.getProblemSpinner());
        holder.tvArea.setText("Área: " + ticket.getArea_problema());
        holder.tvDetails.setText("Detalle: " + ticket.getDetalle());

        // Estado del ticket
        if (holder.tvStatus != null) {
            holder.tvStatus.setText("Estado: " + ticket.getStatus());

            // Cambiar color según estado
            int statusColor;
            switch (ticket.getStatus()) {
                case Ticket.STATUS_IN_PROGRESS:
                    statusColor = context.getResources().getColor(R.color.status_in_progress);
                    break;
                case Ticket.STATUS_COMPLETED:
                    statusColor = context.getResources().getColor(R.color.status_completed);
                    break;
                default:
                    statusColor = context.getResources().getColor(R.color.status_pending);
            }
            holder.tvStatus.setTextColor(statusColor);
        }

        // Prioridad
        if (holder.tvPriority != null) {
            holder.tvPriority.setText("Prioridad: " + ticket.getPriority());
        }

        // Trabajador asignado
        if (holder.tvAssignedWorker != null) {
            if (ticket.getAssignedWorkerName() != null && !ticket.getAssignedWorkerName().isEmpty()) {
                holder.tvAssignedWorker.setText("Asignado a: " + ticket.getAssignedWorkerName());
                holder.tvAssignedWorker.setVisibility(View.VISIBLE);
            } else {
                holder.tvAssignedWorker.setVisibility(View.GONE);
            }
        }

        // Cargar imagen si existe
        if (ticket.getImagen() != null && !ticket.getImagen().isEmpty()) {
            Glide.with(context)
                    .load(ticket.getImagen())
                    .placeholder(R.drawable.bordes)
                    .error(R.drawable.bordes)
                    .into(holder.ivTicketImage);
        }

        // Click listener para ver detalles
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, TicketDetail.class);
            intent.putExtra("ticketNumber", ticket.getTicketNumber());
            intent.putExtra("creador", ticket.getCreatedBy());
            intent.putExtra("fecha", ticket.getCreationDate());
            intent.putExtra("problema", ticket.getProblemSpinner());
            intent.putExtra("area", ticket.getArea_problema());
            intent.putExtra("descripcion", ticket.getDetalle());
            intent.putExtra("imagen", ticket.getImagen());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return filteredTickets.size();
    }

    // Filtrar por usuario
    public void filterByUser(String userId) {
        filteredTickets.clear();
        if (userId == null || userId.isEmpty()) {
            filteredTickets.addAll(tickets);
        } else {
            for (Ticket ticket : tickets) {
                if (ticket.getUserId().equals(userId)) {
                    filteredTickets.add(ticket);
                }
            }
        }
        notifyDataSetChanged();
    }

    // Filtrar por estado
    public void filterByStatus(String status) {
        filteredTickets.clear();
        if (status == null || status.isEmpty()) {
            filteredTickets.addAll(tickets);
        } else {
            for (Ticket ticket : tickets) {
                if (ticket.getStatus().equals(status)) {
                    filteredTickets.add(ticket);
                }
            }
        }
        notifyDataSetChanged();
    }

    // Buscar tickets
    public void searchTickets(String query) {
        filteredTickets.clear();
        if (query.isEmpty()) {
            filteredTickets.addAll(tickets);
        } else {
            query = query.toLowerCase();
            for (Ticket ticket : tickets) {
                if (ticket.getTicketNumber().toLowerCase().contains(query) ||
                        ticket.getCreatedBy().toLowerCase().contains(query) ||
                        ticket.getProblemSpinner().toLowerCase().contains(query) ||
                        ticket.getArea_problema().toLowerCase().contains(query)) {
                    filteredTickets.add(ticket);
                }
            }
        }
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTicketNumber, tvCreator, tvDate, tvProblemType, tvArea, tvDetails;
        TextView tvStatus, tvPriority, tvAssignedWorker;
        ImageView ivTicketImage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Vistas básicas
            tvTicketNumber = itemView.findViewById(R.id.tvTicketNumber);
            tvCreator = itemView.findViewById(R.id.tvCreator);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvProblemType = itemView.findViewById(R.id.tvProblemType);
            tvArea = itemView.findViewById(R.id.tvArea);
            tvDetails = itemView.findViewById(R.id.tvDetails);
            ivTicketImage = itemView.findViewById(R.id.ivTicketImage);

            // Vistas adicionales para gestión
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvPriority = itemView.findViewById(R.id.tvPriority);
            tvAssignedWorker = itemView.findViewById(R.id.tvAssignedWorker);
        }
    }
}