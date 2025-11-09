package com.example.fuelmonitorapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fuelmonitorapp.R;
import com.example.fuelmonitorapp.entity.Vehicle;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class VehicleAdapter extends RecyclerView.Adapter<VehicleAdapter.ViewHolder> {

    public interface OnVehicleActionListener {
        void onEdit(Vehicle v);
        void onDelete(Vehicle v);
        void onShowQR(Vehicle v);
    }

    private List<Vehicle> vehicles;
    private OnVehicleActionListener listener;

    public VehicleAdapter(List<Vehicle> vehicles, OnVehicleActionListener listener) {
        this.vehicles = vehicles;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_vehicle, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Vehicle v = vehicles.get(position);

        holder.tvPlaca.setText(v.getPlaca());
        holder.tvModelo.setText(v.getMarca() + " " + v.getModelo() + " (" + v.getAnio() + ")");

        if (v.getRevision() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            holder.tvRevision.setText("Última revisión: " + sdf.format(v.getRevision().toDate()));
        } else {
            holder.tvRevision.setText("Sin revisión registrada");
        }

        holder.btnEditar.setOnClickListener(view -> listener.onEdit(v));
        holder.btnEliminar.setOnClickListener(view -> listener.onDelete(v));

        holder.itemView.setOnClickListener(view -> listener.onShowQR(v));
    }

    @Override
    public int getItemCount() {
        return vehicles.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPlaca, tvModelo, tvRevision;
        Button btnEditar, btnEliminar;

        ViewHolder(View itemView) {
            super(itemView);
            tvPlaca = itemView.findViewById(R.id.tvPlaca);
            tvModelo = itemView.findViewById(R.id.tvModelo);
            tvRevision = itemView.findViewById(R.id.tvRevision);
            btnEditar = itemView.findViewById(R.id.btnEditar);
            btnEliminar = itemView.findViewById(R.id.btnEliminar);
        }
    }
}
