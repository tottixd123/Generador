package com.example.gemerador.Trabajador;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.gemerador.R;
import java.util.List;
import java.util.Map;

public class TrabajadorManagementAdapter extends RecyclerView.Adapter<TrabajadorManagementAdapter.ViewHolder> {
    private final List<Map<String, String>> userList;
    private final OnUserActionListener listener;

    public interface OnUserActionListener {
        void onPromoteToWorker(Map<String, String> user);
    }

    public TrabajadorManagementAdapter(List<Map<String, String>> userList, OnUserActionListener listener) {
        this.userList = userList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_trabajador, parent, false); // Cambiado a item_trabajador.xml
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, String> user = userList.get(position);
        holder.textViewName.setText("Nombre: " + user.get("nombre"));
        holder.textViewEmail.setText("Email: " + user.get("email"));
        holder.textViewArea.setText("Área: " + user.get("area"));
        holder.textViewCargo.setText("Cargo: " + user.get("cargo"));

        holder.buttonPromote.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPromoteToWorker(user);
            }
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textViewName;
        TextView textViewEmail;
        TextView textViewArea;
        TextView textViewCargo;
        Button buttonPromote;

        ViewHolder(View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.textViewName);
            textViewEmail = itemView.findViewById(R.id.textViewEmail);
            textViewArea = itemView.findViewById(R.id.textViewArea);
            textViewCargo = itemView.findViewById(R.id.textViewCargo);
            buttonPromote = itemView.findViewById(R.id.buttonPromote);
        }
    }
}
