package com.example.gemerador.Adapter;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.gemerador.Crear_Ti.TicketDetail;
import com.example.gemerador.Data_base.Ticket;
import com.example.gemerador.R;
import com.google.firebase.auth.FirebaseAuth;
import java.util.ArrayList;
import java.util.List;

public class TicketAdapter extends RecyclerView.Adapter<TicketAdapter.ViewHolder> {
    public interface OnTicketAddedListener {
        void onTicketAdded(Ticket ticket);
    }

    private OnTicketAddedListener ticketAddedListener;

    public void setOnTicketAddedListener(OnTicketAddedListener listener) {
        this.ticketAddedListener = listener;
    }
    public interface TicketActionListener {
        void onTicketAction(Ticket ticket, String action);
    }
    private String safeString(String value, String defaultValue) {
        return value != null ? value : defaultValue;
    }
    private static final String TAG = "TicketAdapter";
    private List<Ticket> tickets;
    private List<Ticket> filteredTickets;
    private Context context;
    private String userRole;
    private TicketActionListener listener;

    public TicketAdapter(List<Ticket> tickets, String userRole, TicketActionListener listener) {
        this.tickets = tickets;
        this.filteredTickets = new ArrayList<>(tickets);
        this.userRole = userRole;
        this.listener = listener;
    }
    // Constructor simple para compatibilidad
    public TicketAdapter(List<Ticket> tickets) {

        this(tickets, "", null);
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

        // Configurar la información básica
        holder.tvTicketNumber.setText("Ticket #" + safeString(ticket.getTicketNumber(), "Sin número"));
        holder.tvCreator.setText("Creado por: " + safeString(ticket.getCreatedBy(), "Usuario desconocido"));
        holder.tvDate.setText("Fecha: " + safeString(ticket.getCreationDate(), "Fecha no disponible"));
        holder.tvProblemType.setText("Problema: " + safeString(ticket.getProblemSpinner(), "No especificado"));
        holder.tvArea.setText("Área: " + safeString(ticket.getArea_problema(), "No especificada"));
        holder.tvDetails.setText("Detalle: " + safeString(ticket.getDetalle(), "Sin detalles"));

        // Configurar estado y aplicar color
        String status = safeString(ticket.getStatus(), "Pendiente");
        holder.tvStatus.setText("Estado: " + status);
        applyStatusColor(holder.statusContainer, status);

        holder.tvPriority.setText("Prioridad: " + safeString(ticket.getPriority(), "Normal"));

        String assignedWorker = ticket.getAssignedWorkerName();
        holder.tvAssignedWorker.setText("Asignado a: " +
                (assignedWorker != null && !assignedWorker.isEmpty() ? assignedWorker : "Sin asignar"));

        // Configurar visibilidad y funcionalidad de los botones según el rol
        configureButtonsForRole(holder, ticket);

        // Manejar la imagen del ticket
        handleTicketImage(holder, ticket);

        // Click listener para ver detalles
        setupTicketClickListener(holder, ticket);
    }
    public void setTickets(List<Ticket> tickets) {
        this.tickets = new ArrayList<>(tickets);
        this.filteredTickets = new ArrayList<>(tickets);
        notifyDataSetChanged();
    }

    private void handleTicketImage(@NonNull ViewHolder holder, Ticket ticket) {
        String imageUrl = ticket.getImagen();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            holder.ivTicketImage.setVisibility(View.VISIBLE);

            // Intentar cargar la imagen con Glide
            try {
                Glide.with(context)
                        .load(imageUrl)
                        .placeholder(R.drawable.bordes)
                        .error(R.drawable.bordes)
                        .listener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                        Target<Drawable> target, boolean isFirstResource) {
                                holder.ivTicketImage.setVisibility(View.GONE);
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Drawable resource, Object model,
                                                           Target<Drawable> target, com.bumptech.glide.load.DataSource dataSource,
                                                           boolean isFirstResource) {
                                holder.ivTicketImage.setVisibility(View.VISIBLE);
                                return false;
                            }
                        })
                        .into(holder.ivTicketImage);
            } catch (Exception e) {
                holder.ivTicketImage.setVisibility(View.GONE);
            }
        } else {
            holder.ivTicketImage.setVisibility(View.GONE);
        }
    }

    private void applyStatusColor(View container, String status) {
        int colorResId;
        switch (status.toLowerCase()) {
            case "en progreso":
                colorResId = R.color.status_in_progress;
                break;
            case "completado":
                colorResId = R.color.status_completed;
                break;
            case "cancelado":
                colorResId = R.color.status_cancelled;
                break;
            default:
                colorResId = R.color.status_pending;
                break;
        }
        container.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(context, colorResId)));
    }
    private void setupTicketClickListener(@NonNull ViewHolder holder, Ticket ticket) {
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, TicketDetail.class);
            intent.putExtra("ticketNumber", safeString(ticket.getTicketNumber(), ""));
            intent.putExtra("creator", safeString(ticket.getCreatedBy(), ""));  // Cambiado de "creador" a "creator"
            intent.putExtra("date", safeString(ticket.getCreationDate(), ""));  // Cambiado de "fecha" a "date"
            intent.putExtra("problem", safeString(ticket.getProblemSpinner(), ""));  // Cambiado de "problema" a "problem"
            intent.putExtra("area", safeString(ticket.getArea_problema(), ""));
            intent.putExtra("description", safeString(ticket.getDetalle(), ""));  // Cambiado "descripcion" a "description"
            intent.putExtra("imagen", safeString(ticket.getImagen(), ""));
            context.startActivity(intent);
        });
    }
    private void configureButtonsForRole(ViewHolder holder, Ticket ticket) {
        if ("Administrador".equals(userRole)) {
            // Configuración para administradores
            holder.btnUpdateStatus.setVisibility(View.VISIBLE);
            holder.btnUpdatePriority.setVisibility(View.VISIBLE);
            holder.btnAssignWorker.setVisibility(View.VISIBLE);

            holder.btnUpdateStatus.setOnClickListener(v -> {
                if (listener != null) listener.onTicketAction(ticket, "UPDATE_STATUS");
            });
            holder.btnUpdatePriority.setOnClickListener(v -> {
                if (listener != null) listener.onTicketAction(ticket, "UPDATE_PRIORITY");
            });
            holder.btnAssignWorker.setOnClickListener(v -> {
                if (listener != null) listener.onTicketAction(ticket, "ASSIGN_WORKER");
            });
        } else if ("trabajadores".equals(userRole)) {
            // Configuración para trabajadores
            holder.btnUpdateStatus.setVisibility(View.VISIBLE);
            holder.btnUpdatePriority.setVisibility(View.GONE);
            holder.btnAssignWorker.setVisibility(View.GONE);

            String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            if (ticket.getAssignedWorkerId() != null &&
                    ticket.getAssignedWorkerId().equals(currentUserId)) {
                holder.btnUpdateStatus.setEnabled(true);
                holder.btnUpdateStatus.setOnClickListener(v -> {
                    if (listener != null) listener.onTicketAction(ticket, "UPDATE_STATUS");
                });
            } else {
                holder.btnUpdateStatus.setEnabled(false);
            }
        } else {
            // Usuario normal
            holder.btnUpdateStatus.setVisibility(View.GONE);
            holder.btnUpdatePriority.setVisibility(View.GONE);
            holder.btnAssignWorker.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return filteredTickets.size();
    }

    // Métodos de filtrado
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
        TextView tvTicketNumber, tvCreator, tvDate, tvProblemType, tvArea,
                tvDetails, tvStatus, tvPriority, tvAssignedWorker;
        ImageView ivTicketImage;
        Button btnUpdateStatus, btnUpdatePriority, btnAssignWorker;
        View statusContainer; // Añade esta línea

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Vistas básicas
            statusContainer = itemView.findViewById(R.id.statusContainer); // Añade esta línea
            tvTicketNumber = itemView.findViewById(R.id.tvTicketNumber);
            tvCreator = itemView.findViewById(R.id.tvCreator);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvProblemType = itemView.findViewById(R.id.tvProblemType);
            tvArea = itemView.findViewById(R.id.tvArea);
            tvDetails = itemView.findViewById(R.id.tvDetails);
            ivTicketImage = itemView.findViewById(R.id.ivTicketImage);

            // Vistas de gestión
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvPriority = itemView.findViewById(R.id.tvPriority);
            tvAssignedWorker = itemView.findViewById(R.id.tvAssignedWorker);

            // Botones de acción
            btnUpdateStatus = itemView.findViewById(R.id.btnUpdateStatus);
            btnUpdatePriority = itemView.findViewById(R.id.btnUpdatePriority);
            btnAssignWorker = itemView.findViewById(R.id.btnAssignWorker);
        }
    }
}