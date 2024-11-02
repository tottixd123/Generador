package com.example.gemerador.Notificaciones;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gemerador.Data_base.Ticket;
import com.example.gemerador.R;
import java.util.List;

public class TicketNotificationAdapter extends RecyclerView.Adapter<TicketNotificationAdapter.ViewHolder> {
    private List<TicketNotification> notifications;
    private Context context;
    private List<Ticket> tickets;

    public TicketNotificationAdapter(List<TicketNotification> notifications) {
        this.notifications = notifications;
    }

    public void setTickets(List<Ticket> tickets) {
        this.tickets = tickets;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.notification_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        TicketNotification notification = notifications.get(position);

        holder.ticketNumberText.setText("Ticket: " + notification.getTicketId());
        holder.statusText.setText("Estado: " + notification.getStatus());
        holder.messageText.setText(notification.getMessage());
        holder.dateText.setText(notification.getTimestamp());
        holder.priorityText.setText("Prioridad: " + notification.getPriority());

        // Configurar color del estado según el estado del ticket
        switch (notification.getStatus().toLowerCase()) {
            case "pendiente":
                holder.statusText.setTextColor(holder.itemView.getContext()
                        .getResources().getColor(android.R.color.holo_orange_dark));
                break;
            case "en proceso":
                holder.statusText.setTextColor(holder.itemView.getContext()
                        .getResources().getColor(android.R.color.holo_blue_dark));
                break;
            case "resuelto":
                holder.statusText.setTextColor(holder.itemView.getContext()
                        .getResources().getColor(android.R.color.holo_green_dark));
                break;
            default:
                holder.statusText.setTextColor(holder.itemView.getContext()
                        .getResources().getColor(android.R.color.darker_gray));
                break;
        }

        // Configurar el clic para mostrar detalles
        holder.itemView.setOnClickListener(v -> showTicketDetails(notification.getTicketId()));
    }

    private void showTicketDetails(String ticketId) {
        if (tickets == null) {
            Toast.makeText(context, "Detalles del ticket no disponibles", Toast.LENGTH_SHORT).show();
            return;
        }

        Ticket ticket = findTicketById(ticketId);
        if (ticket == null) {
            Toast.makeText(context, "Detalles del ticket no disponibles", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.activity_ticket_detail, null);
        // Configurar las vistas del diálogo
        TextView tvTicketNumber = dialogView.findViewById(R.id.tvTicketDetailNumber);
        TextView tvCreatedBy = dialogView.findViewById(R.id.tvTicketDetailCreator);
        TextView tvCreationDate = dialogView.findViewById(R.id.tvTicketDetailDate);
        TextView tvProblem = dialogView.findViewById(R.id.tvTicketDetailProblem);
        TextView tvArea = dialogView.findViewById(R.id.tvTicketDetailArea);
        TextView tvDetail = dialogView.findViewById(R.id.tvTicketDetailDescription);

        // Establecer los valores
        tvTicketNumber.setText("Ticket: " + ticket.getTicketNumber());
        tvCreatedBy.setText("Creado por: " + ticket.getCreatedBy());
        tvCreationDate.setText("Fecha: " + ticket.getCreationDate());
        tvProblem.setText("Problema: " + ticket.getProblemSpinner());
        tvArea.setText("Área: " + ticket.getArea_problema());
        tvDetail.setText("Detalle: " + ticket.getDetalle());

        builder.setView(dialogView)
                .setTitle("Detalles del Ticket")
                .setPositiveButton("Cerrar", null)
                .show();
    }

    private Ticket findTicketById(String ticketId) {
        for (Ticket ticket : tickets) {
            if (ticket.getTicketNumber().equals(ticketId)) {
                return ticket;
            }
        }
        return null;
    }

    private String findNotificationStatus(String ticketId) {
        for (TicketNotification notification : notifications) {
            if (notification.getTicketId().equals(ticketId)) {
                return notification.getStatus();
            }
        }
        return "Desconocido";
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView ticketNumberText;
        TextView statusText;
        TextView messageText;
        TextView dateText;
        TextView priorityText;

        public ViewHolder(View itemView) {
            super(itemView);
            ticketNumberText = itemView.findViewById(R.id.ticketNumberText);
            statusText = itemView.findViewById(R.id.statusText);
            messageText = itemView.findViewById(R.id.messageText);
            dateText = itemView.findViewById(R.id.dateText);
            priorityText = itemView.findViewById(R.id.priorityText);
        }
    }
}