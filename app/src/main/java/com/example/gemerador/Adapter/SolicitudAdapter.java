package com.example.gemerador.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gemerador.R;
import com.example.gemerador.Models.Solicitud;

import java.util.List;

public class SolicitudAdapter extends RecyclerView.Adapter<SolicitudAdapter.SolicitudViewHolder>{
    private List<Solicitud> solicitudes;
    private OnSolicitudListener listener;

    public interface OnSolicitudListener {
        void onAprobarClick(Solicitud solicitud);
        void onRechazarClick(Solicitud solicitud);
    }

    public SolicitudAdapter(List<Solicitud> solicitudes, OnSolicitudListener listener) {
        this.solicitudes = solicitudes;
        this.listener = listener;
    }
    @NonNull
    @Override
    public SolicitudViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_solicitud, parent, false);
        return new SolicitudViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SolicitudViewHolder holder, int position) {
        Solicitud solicitud = solicitudes.get(position);
        holder.bind(solicitud);
    }

    @Override
    public int getItemCount() {

        return solicitudes.size();
    }

    class SolicitudViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombre, tvEmail, tvArea, tvCargo;
        Button btnAprobar, btnRechazar;

        SolicitudViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvNombre);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            tvArea = itemView.findViewById(R.id.tvArea);
            tvCargo = itemView.findViewById(R.id.tvCargo);
            btnAprobar = itemView.findViewById(R.id.btnAprobar);
            btnRechazar = itemView.findViewById(R.id.btnRechazar);
        }

        void bind(final Solicitud solicitud) {
            tvNombre.setText(solicitud.getNombre());
            tvEmail.setText(solicitud.getEmail());
            tvArea.setText(solicitud.getArea());
            tvCargo.setText(solicitud.getCargo());

            btnAprobar.setOnClickListener(v -> listener.onAprobarClick(solicitud));
            btnRechazar.setOnClickListener(v -> listener.onRechazarClick(solicitud));
        }
    }
}
