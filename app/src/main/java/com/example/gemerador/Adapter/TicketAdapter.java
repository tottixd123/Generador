package com.example.gemerador.Adapter;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.example.gemerador.Crear_Ti.TicketDetail;
import com.example.gemerador.Data_base.Ticket;
import com.example.gemerador.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

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
    private static final int STATUS_BACKGROUND_ALPHA = 230;
    private List<Ticket> tickets;
    private List<Ticket> filteredTickets;
    private Context context;
    private String userRole;
    private TicketActionListener listener;
    private DatabaseReference usersRef;

    public TicketAdapter(List<Ticket> tickets, String userRole, TicketActionListener listener) {
        this.tickets = new ArrayList<>(tickets);
        this.filteredTickets = new ArrayList<>(tickets);
        this.userRole = userRole;
        this.listener = listener;
        this.usersRef = FirebaseDatabase.getInstance().getReference().child("usuarios");
    }

    // Constructor simple para compatibilidad
    public TicketAdapter(List<Ticket> tickets) {

        this(tickets, "", null);
    }
    public void setTickets(List<Ticket> newTickets) {
        this.tickets = new ArrayList<>(newTickets);
        this.filteredTickets = new ArrayList<>(newTickets);
        notifyDataSetChanged();
    }
    // Método principal para aplicar todos los filtros
    public void applyFilters(String workerId, String status, String priority) {
        filteredTickets.clear();

        // Aplicar todos los filtros de una vez
        for (Ticket ticket : tickets) {
            boolean matchesWorker = workerId.isEmpty() ||
                    (ticket.getAssignedWorkerId() != null &&
                            ticket.getAssignedWorkerId().equals(workerId));
            boolean matchesStatus = status.isEmpty() ||
                    ticket.getStatus().equals(status);
            boolean matchesPriority = priority.isEmpty() ||
                    ticket.getPriority().equals(priority);

            if (matchesWorker && matchesStatus && matchesPriority) {
                filteredTickets.add(ticket);
            }
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_ticket, parent, false);
        return new ViewHolder(view);
    }
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Ticket ticket = filteredTickets.get(position);

        // Configurar el contenedor de estado con animación
        configureStatusContainer(holder, ticket);

        // Configurar información básica
        setupBasicInfo(holder, ticket);

        // Configurar información de estado y trabajador
        setupStatusAndWorkerInfo(holder, ticket);

        // Configurar botones según rol
        configureButtonsForRole(holder, ticket);

        // Manejar imagen del ticket
        handleTicketImage(holder, ticket);

        // Configurar click listener
        setupTicketClickListener(holder, ticket);
    }

    private void configureStatusContainer(@NonNull ViewHolder holder, Ticket ticket) {
        String status = safeString(ticket.getStatus(), "Pendiente");
        int backgroundColor = getStatusColor(status);

        // Aplicar color con animación
        holder.statusContainer.setBackgroundTintList(ColorStateList.valueOf(backgroundColor));
        holder.statusContainer.setAlpha(0f);
        holder.statusContainer.animate()
                .alpha(1f)
                .setDuration(300)
                .start();
    }
    public void searchTickets(String query) {
        filteredTickets.clear();
        if (query == null || query.isEmpty()) {
            filteredTickets.addAll(tickets);
        } else {
            String searchQuery = query.toLowerCase().trim();
            for (Ticket ticket : tickets) {
                if (matchesSearchCriteria(ticket, searchQuery)) {
                    filteredTickets.add(ticket);
                }
            }
        }
        notifyDataSetChanged();
    }

    private boolean matchesSearchCriteria(Ticket ticket, String query) {
        return (ticket.getTicketNumber() != null && ticket.getTicketNumber().toLowerCase().contains(query)) ||
                (ticket.getCreatedBy() != null && ticket.getCreatedBy().toLowerCase().contains(query)) ||
                (ticket.getProblemSpinner() != null && ticket.getProblemSpinner().toLowerCase().contains(query)) ||
                (ticket.getArea_problema() != null && ticket.getArea_problema().toLowerCase().contains(query)) ||
                (ticket.getDetalle() != null && ticket.getDetalle().toLowerCase().contains(query)) ||
                (ticket.getAssignedWorkerName() != null && ticket.getAssignedWorkerName().toLowerCase().contains(query));
    }
    private void setupBasicInfo(@NonNull ViewHolder holder, Ticket ticket) {
        holder.tvTicketNumber.setText("Ticket #" + safeString(ticket.getTicketNumber(), "Sin número"));
        // Obtener el nombre del usuario a partir del email
        String email = safeString(ticket.getCreatedBy(), "");
        getUserNameFromEmail(email, holder.tvCreator);
        holder.tvDate.setText("Fecha: " + safeString(ticket.getCreationDate(), "Fecha no disponible"));
        holder.tvProblemType.setText("Problema: " + safeString(ticket.getProblemSpinner(), "No especificado"));
        holder.tvArea.setText("Área: " + safeString(ticket.getArea_problema(), "No especificada"));
        holder.tvDetails.setText("Detalle: " + safeString(ticket.getDetalle(), "Sin detalles"));
        holder.tvPriority.setText("Prioridad: " + safeString(ticket.getPriority(), "Normal"));
    }
    private void getUserNameFromEmail(String email, final TextView textView) {
        if (email.isEmpty()) {
            textView.setText("Creado por: Usuario desconocido");
            return;
        }
        Query query = usersRef.orderByChild("email").equalTo(email);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                boolean foundUser = false;
                if (dataSnapshot.exists()) {
                    for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                        String nombre = userSnapshot.child("nombre").getValue(String.class);
                        if (nombre != null && !nombre.isEmpty()) {
                            textView.setText("Creado por: " + nombre);
                            foundUser = true;
                            break;
                        }
                    }
                }
                if (!foundUser) {
                    // Si no se encuentra el usuario o no tiene nombre, mostrar el email sin dominio
                    String nombrePorDefecto = email.split("@")[0];
                    textView.setText("Creado por: " + nombrePorDefecto);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // En caso de error, mostrar el email sin dominio
                String nombrePorDefecto = email.split("@")[0];
                textView.setText("Creado por: " + nombrePorDefecto);
            }
        });
    }
    private void setupStatusAndWorkerInfo(@NonNull ViewHolder holder, Ticket ticket) {
        // Configurar estado y color
        String status = safeString(ticket.getStatus(), "Pendiente");
        holder.tvStatus.setText("Estado: " + status);

        int backgroundColor;
        switch (status.toLowerCase()) {
            case "en proceso":
            case "en progreso":
                backgroundColor = ContextCompat.getColor(context, R.color.status_in_progress);
                holder.statusContainer.setBackgroundTintList(ColorStateList.valueOf(backgroundColor));
                break;
            case "terminado":
                backgroundColor = ContextCompat.getColor(context, R.color.status_completed);
                holder.statusContainer.setBackgroundTintList(ColorStateList.valueOf(backgroundColor));
                break;
            case "cancelado":
                backgroundColor = ContextCompat.getColor(context, R.color.status_cancelled);
                holder.statusContainer.setBackgroundTintList(ColorStateList.valueOf(backgroundColor));
                break;
            default:
                backgroundColor = ContextCompat.getColor(context, R.color.status_pending);
                holder.statusContainer.setBackgroundTintList(ColorStateList.valueOf(backgroundColor));
                break;
        }

        // Mostrar trabajador asignado de forma prominente
        String assignedWorkerName = ticket.getAssignedWorkerName();
        if (assignedWorkerName != null && !assignedWorkerName.isEmpty()) {
            holder.tvAssignedWorker.setVisibility(View.VISIBLE);
            holder.tvAssignedWorker.setText("Asignado a: " + assignedWorkerName);
            // Hacer el texto más visible
            holder.tvAssignedWorker.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            holder.tvAssignedWorker.setTextColor(ContextCompat.getColor(context, android.R.color.black));
        } else {
            holder.tvAssignedWorker.setVisibility(View.VISIBLE);
            holder.tvAssignedWorker.setText("Sin asignar");
        }

    }
    private int getStatusColor(String status) {
        switch (status.toLowerCase()) {
            case "en progreso":
                return ContextCompat.getColor(context, R.color.status_in_progress);
            case "terminado":
                return ContextCompat.getColor(context, R.color.status_completed);
            case "cancelado":
                return ContextCompat.getColor(context, R.color.status_cancelled);
            default:
                return ContextCompat.getColor(context, R.color.status_pending);
        }
    }

    // Método para actualizar un ticket específico
    public void updateTicket(Ticket updatedTicket) {
        if (updatedTicket == null) return;

        boolean updated = false;
        for (int i = 0; i < tickets.size(); i++) {
            if (tickets.get(i).getId().equals(updatedTicket.getId())) {
                tickets.set(i, updatedTicket);
                updated = true;
                break;
            }
        }

        for (int i = 0; i < filteredTickets.size(); i++) {
            if (filteredTickets.get(i).getId().equals(updatedTicket.getId())) {
                filteredTickets.set(i, updatedTicket);
                notifyItemChanged(i);
                updated = true;
                break;
            }
        }

        // Si el ticket no se encontró en ninguna lista, agregarlo
        if (!updated) {
            tickets.add(updatedTicket);
            filteredTickets.add(updatedTicket);
            notifyItemInserted(filteredTickets.size() - 1);
        }
    }
    // ViewHolder actualizado
    public static class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout statusContainer;
        TextView tvTicketNumber, tvCreator, tvDate, tvProblemType, tvArea,
                tvDetails, tvStatus, tvPriority, tvAssignedWorker, tvLastUpdated;
        ImageView ivTicketImage;
        Button btnUpdateStatus, btnUpdatePriority, btnAssignWorker;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            initializeViews(itemView);
        }

        private void initializeViews(View itemView) {
            // Contenedor de estado
            statusContainer = itemView.findViewById(R.id.statusContainer);

            // Información básica
            tvTicketNumber = itemView.findViewById(R.id.tvTicketNumber);
            tvCreator = itemView.findViewById(R.id.tvCreator);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvProblemType = itemView.findViewById(R.id.tvProblemType);
            tvArea = itemView.findViewById(R.id.tvArea);
            tvDetails = itemView.findViewById(R.id.tvDetails);

            // Información de estado
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvPriority = itemView.findViewById(R.id.tvPriority);
            tvAssignedWorker = itemView.findViewById(R.id.tvAssignedWorker);
            tvLastUpdated = itemView.findViewById(R.id.tvLastUpdated);

            // Imagen
            ivTicketImage = itemView.findViewById(R.id.ivTicketImage);

            // Botones
            btnUpdateStatus = itemView.findViewById(R.id.btnUpdateStatus);
            btnUpdatePriority = itemView.findViewById(R.id.btnUpdatePriority);
            btnAssignWorker = itemView.findViewById(R.id.btnAssignWorker);
        }
    }
    private void handleTicketImage(@NonNull ViewHolder holder, Ticket ticket) {
        String imageUrl = ticket.getImagen();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            holder.ivTicketImage.setVisibility(View.VISIBLE);
            try {
                // Agregar permisos de acceso al PhotoPicker
                Glide.with(context)
                        .load(imageUrl)
                        .apply(new RequestOptions()
                                .placeholder(R.drawable.bordes)
                                .error(R.drawable.bordes)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .skipMemoryCache(false))
                        .listener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                        Target<Drawable> target, boolean isFirstResource) {
                                // Si falla la carga, ocultar la imagen sin mostrar error
                                holder.ivTicketImage.setVisibility(View.GONE);
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Drawable resource, Object model,
                                                           Target<Drawable> target, DataSource dataSource,
                                                           boolean isFirstResource) {
                                holder.ivTicketImage.setVisibility(View.VISIBLE);
                                return false;
                            }
                        })
                        .into(new CustomTarget<Drawable>() {
                            @Override
                            public void onResourceReady(@NonNull Drawable resource,
                                                        @Nullable Transition<? super Drawable> transition) {
                                holder.ivTicketImage.setImageDrawable(resource);
                            }

                            @Override
                            public void onLoadCleared(@Nullable Drawable placeholder) {
                                holder.ivTicketImage.setImageDrawable(placeholder);
                            }
                        });
            } catch (Exception e) {
                holder.ivTicketImage.setVisibility(View.GONE);
            }
        } else {
            holder.ivTicketImage.setVisibility(View.GONE);
        }
    }

    private void setupTicketClickListener(@NonNull ViewHolder holder, Ticket ticket) {
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, TicketDetail.class);
            intent.putExtra("ticketNumber", safeString(ticket.getTicketNumber(), ""));
            intent.putExtra("creator", safeString(ticket.getCreatedBy(), ""));
            intent.putExtra("date", safeString(ticket.getCreationDate(), ""));
            intent.putExtra("problem", safeString(ticket.getProblemSpinner(), ""));
            intent.putExtra("area", safeString(ticket.getArea_problema(), ""));
            intent.putExtra("description", safeString(ticket.getDetalle(), ""));
            intent.putExtra("imagen", safeString(ticket.getImagen(), ""));
            context.startActivity(intent);
        });
    }

    private void configureButtonsForRole(ViewHolder holder, Ticket ticket) {
        if ("Administrador".equals(userRole)) {
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
            holder.btnUpdateStatus.setVisibility(View.GONE);
            holder.btnUpdatePriority.setVisibility(View.GONE);
            holder.btnAssignWorker.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return filteredTickets != null ? filteredTickets.size() : 0;
    }

    public void filterByUser(String userId) {
        // Limpiar la lista filtrada solo una vez
        filteredTickets.clear();

        // Si no hay userId para filtrar, mostrar todos los tickets
        if (userId == null || userId.isEmpty()) {
            filteredTickets.addAll(tickets);
        } else {
            // Filtrar tickets por userId
            for (Ticket ticket : tickets) {
                if (ticket.getUserId() != null && ticket.getUserId().equals(userId)) {
                    filteredTickets.add(ticket);
                }
            }
        }
        // Notificar cambios al RecyclerView
        notifyDataSetChanged();
    }
    public void clearFilters() {
        filteredTickets.clear();
        filteredTickets.addAll(tickets);
        notifyDataSetChanged();
    }
    private boolean meetsFilterCriteria(Ticket ticket) {
        // Aquí puedes agregar la lógica específica de tus filtros actuales
        return true; // Por defecto acepta todos los tickets
    }
}