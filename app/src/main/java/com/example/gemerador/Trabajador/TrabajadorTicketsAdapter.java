package com.example.gemerador.Trabajador;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import com.example.gemerador.Data_base.Ticket;
import com.example.gemerador.R;

import java.util.List;


public class TrabajadorTicketsAdapter extends RecyclerView.Adapter<TrabajadorTicketsAdapter.TicketViewHolder> {
    private List<Ticket> tickets;
    private OnTicketClickListener listener;

    public TrabajadorTicketsAdapter(List<Ticket> tickets, OnTicketClickListener listener) {
        this.tickets = tickets;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TicketViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ticket_trabajador, parent, false);
        return new TicketViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TicketViewHolder holder, int position) {
        Ticket ticket = tickets.get(position);
        holder.bind(ticket, listener);
    }

    @Override
    public int getItemCount() {
        return tickets.size();
    }

    static class TicketViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvStatus, tvDescription;

        TicketViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTicketTitle);
            tvStatus = itemView.findViewById(R.id.tvTicketStatus);
            tvDescription = itemView.findViewById(R.id.tvTicketDescription);
        }

        void bind(Ticket ticket, OnTicketClickListener listener) {
            // Use ticketNumber and problemSpinner for the title
            tvTitle.setText(String.format("%s - %s",
                    ticket.getTicketNumber(),
                    ticket.getProblemSpinner()));

            tvStatus.setText(ticket.getStatus());
            // Use area_problema and detalle for the description
            tvDescription.setText(String.format("%s\n%s",
                    ticket.getArea_problema(),
                    ticket.getDetalle()));

            itemView.setOnClickListener(v -> listener.onTicketClick(ticket));
        }
    }

    public interface OnTicketClickListener {
        void onTicketClick(Ticket ticket);
    }
}

