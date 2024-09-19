package com.example.gemerador.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.GranularRoundedCorners;
import com.example.gemerador.Domain.TrendsDomain;
import com.example.gemerador.R;

import java.util.ArrayList;

public class TrendsAdapter extends RecyclerView.Adapter<TrendsAdapter.ViewHolder> {
    ArrayList<TrendsDomain> items;

    Context context;

    public TrendsAdapter(ArrayList<TrendsDomain> items) {
        this.items = items;
    }
    @NonNull
    @Override
    public TrendsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View inflator = LayoutInflater.from(parent.getContext()).inflate(R.layout.viewholder_trend, parent, false);
        context=parent.getContext();
        return new ViewHolder(inflator);
    }

    @Override
    public void onBindViewHolder(@NonNull TrendsAdapter.ViewHolder holder, int position) {
         holder.title.setText(items.get(position).getTitle());
         holder.fecha.setText(items.get(position).getFecha());
         holder.estado.setText(items.get(position).getEstado());
         holder.picAddress.setImageResource(items.get(position).getPicAddress());
         Glide.with(holder.itemView.getContext())
                .load(items.get(position).getPicAddress())  // Sin necesidad de drawableResourceId
                .transform(new GranularRoundedCorners(30, 30, 30, 30))
                .into(holder.picAddress);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, fecha, estado;
        ImageView  picAddress;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.problema);
            fecha = itemView.findViewById(R.id.fecha);
            estado = itemView.findViewById(R.id.Descripcion);
            picAddress = itemView.findViewById(R.id.fallos);
        }
    }
}
