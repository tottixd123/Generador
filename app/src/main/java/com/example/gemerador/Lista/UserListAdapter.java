package com.example.gemerador.Lista;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gemerador.R;

import java.util.List;

public class UserListAdapter extends RecyclerView.Adapter<UserListAdapter.UserViewHolder> {
    private List<UserModel> userList;

    public UserListAdapter(List<UserModel> userList) {
        this.userList = userList;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        UserModel user = userList.get(position);

        holder.textNombre.setText("Nombre: " + (user.getNombre() != null ? user.getNombre() : "N/A"));
        holder.textEmail.setText("Email: " + (user.getEmail() != null ? user.getEmail() : "N/A"));
        holder.textRole.setText("Rol: " + (user.getRole() != null ? user.getRole() : "N/A"));
        holder.textCargo.setText("Cargo: " + (user.getCargo() != null ? user.getCargo() : "N/A"));
        holder.textArea.setText("Área: " + (user.getArea() != null ? user.getArea() : "N/A"));
    }
    @Override
    public int getItemCount() {
        return userList.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView textNombre, textEmail, textRole, textCargo, textArea;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            textNombre = itemView.findViewById(R.id.textNombre);
            textEmail = itemView.findViewById(R.id.textEmail);
            textRole = itemView.findViewById(R.id.textRole);
            textCargo = itemView.findViewById(R.id.textCargo);
            textArea = itemView.findViewById(R.id.textArea);
        }
    }
}
