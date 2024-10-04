package com.example.gemerador.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gemerador.Data_base.Ticket;
import com.example.gemerador.R;
import com.squareup.picasso.Picasso;

import java.util.List;

public class TicketAdapter extends RecyclerView.Adapter<TicketAdapter.ViewHolder> {

    private List<Ticket> tickets;

    public TicketAdapter(List<Ticket> tickets) {
        this.tickets = tickets;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ticket, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Ticket ticket = tickets.get(position);
        holder.ticketNumberTextView.setText(ticket.getTicketNumber());
        holder.problemSpinnerTextView.setText(ticket.getProblemSpinner());
        holder.areaProblemaTextView.setText(ticket.getArea_problema());
        holder.detalleTextView.setText(ticket.getDetalle());

        // Cargar imagen usando Picasso
        if (!ticket.getImagen().isEmpty()) {
            Picasso.get().load(ticket.getImagen()).into(holder.imagenImageView);
        }
    }

    @Override
    public int getItemCount() {
        return tickets.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView ticketNumberTextView;
        TextView problemSpinnerTextView;
        TextView areaProblemaTextView;
        TextView detalleTextView;
        ImageView imagenImageView;

        ViewHolder(View itemView) {
            super(itemView);
            ticketNumberTextView = itemView.findViewById(R.id.ticketNumberTextView);
            problemSpinnerTextView = itemView.findViewById(R.id.problemSpinnerTextView);
            areaProblemaTextView = itemView.findViewById(R.id.areaProblemaTextView);
            detalleTextView = itemView.findViewById(R.id.detalleTextView);
            imagenImageView = itemView.findViewById(R.id.imagenImageView);
        }
    }
}