package com.example.fuelmonitorapp.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.fuelmonitorapp.R;
import com.example.fuelmonitorapp.entity.FuelRecord;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.*;

public class FuelRecordAdapter extends RecyclerView.Adapter<FuelRecordAdapter.ViewHolder> {

    private List<FuelRecord> lista;
    private Context context;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    public FuelRecordAdapter(Context context, List<FuelRecord> lista) {
        this.context = context;
        this.lista = lista;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_fuel_record, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FuelRecord record = lista.get(position);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        // 🔹 Corregido para evitar crash si la fecha no es Timestamp
        Object fechaObj = record.getFecha();
        String fechaTexto;

        if (fechaObj instanceof com.google.firebase.Timestamp) {
            fechaTexto = sdf.format(((com.google.firebase.Timestamp) fechaObj).toDate());
        } else if (fechaObj instanceof String) {
            fechaTexto = (String) fechaObj;
        } else {
            fechaTexto = "Sin fecha";
        }

        holder.tvFecha.setText("Fecha: " + fechaTexto);
        holder.tvVehiculo.setText("Vehículo: " + record.getVehicleId());
        holder.tvKm.setText("Km: " + record.getKilometraje());
        holder.tvLitros.setText("Litros: " + record.getLitros());
        holder.tvPrecio.setText("Precio: S/ " + record.getPrecioTotal());
        holder.tvTipo.setText("Combustible: " + record.getTipoCombustible());

        holder.btnEliminar.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Eliminar registro")
                    .setMessage("¿Deseas eliminar este registro?")
                    .setPositiveButton("Sí", (dialog, which) -> {
                        db.collection("fuel_records")
                                .whereEqualTo("id", record.getId())
                                .get()
                                .addOnSuccessListener(snapshot -> {
                                    for (var doc : snapshot.getDocuments()) {
                                        db.collection("fuel_records").document(doc.getId()).delete();
                                    }
                                    lista.remove(position);
                                    notifyItemRemoved(position);
                                    Toast.makeText(context, "Registro eliminado", Toast.LENGTH_SHORT).show();
                                });
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });

        // Editar registro (solo permite editar precio y litros para ejemplo)
        holder.btnEditar.setOnClickListener(v -> {
            View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_fuel, null);
            EditText etLitros = dialogView.findViewById(R.id.etLitros);
            EditText etPrecio = dialogView.findViewById(R.id.etPrecio);
            Spinner spnTipo = dialogView.findViewById(R.id.spnTipo);

            etLitros.setText(String.valueOf(record.getLitros()));
            etPrecio.setText(String.valueOf(record.getPrecioTotal()));

            ArrayAdapter<String> tipoAdapter = new ArrayAdapter<>(context,
                    android.R.layout.simple_spinner_item,
                    Arrays.asList("Gasolina", "GLP", "GNV"));
            tipoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spnTipo.setAdapter(tipoAdapter);
            spnTipo.setSelection(tipoAdapter.getPosition(record.getTipoCombustible()));

            new AlertDialog.Builder(context)
                    .setTitle("Editar registro")
                    .setView(dialogView)
                    .setPositiveButton("Guardar", (d, which) -> {
                        double nuevosLitros = Double.parseDouble(etLitros.getText().toString());
                        double nuevoPrecio = Double.parseDouble(etPrecio.getText().toString());
                        String nuevoTipo = spnTipo.getSelectedItem().toString();

                        db.collection("fuel_records")
                                .whereEqualTo("id", record.getId())
                                .get()
                                .addOnSuccessListener(snapshot -> {
                                    for (var doc : snapshot.getDocuments()) {
                                        db.collection("fuel_records").document(doc.getId())
                                                .update("litros", nuevosLitros,
                                                        "precioTotal", nuevoPrecio,
                                                        "tipoCombustible", nuevoTipo);
                                    }
                                    record.setLitros(nuevosLitros);
                                    record.setPrecioTotal(nuevoPrecio);
                                    record.setTipoCombustible(nuevoTipo);
                                    notifyItemChanged(position);
                                    Toast.makeText(context, "Registro actualizado", Toast.LENGTH_SHORT).show();
                                });
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvFecha, tvVehiculo, tvKm, tvLitros, tvPrecio, tvTipo;
        Button btnEditar, btnEliminar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFecha = itemView.findViewById(R.id.tvFecha);
            tvVehiculo = itemView.findViewById(R.id.tvVehiculo);
            tvKm = itemView.findViewById(R.id.tvKm);
            tvLitros = itemView.findViewById(R.id.tvLitros);
            tvPrecio = itemView.findViewById(R.id.tvPrecio);
            tvTipo = itemView.findViewById(R.id.tvTipo);
            btnEditar = itemView.findViewById(R.id.btnEditar);
            btnEliminar = itemView.findViewById(R.id.btnEliminar);
        }
    }
}
