package com.example.fuelmonitorapp.fragments;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fuelmonitorapp.R;
import com.example.fuelmonitorapp.adapter.FuelRecordAdapter;
import com.example.fuelmonitorapp.entity.FuelRecord;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.*;

public class RegistrosFragment extends Fragment {

    private Spinner spnVehiculos;
    private Button btnDesde, btnHasta, btnFiltrar, btnAgregar;
    private RecyclerView rvRegistros;
    private FuelRecordAdapter adapter;
    private List<FuelRecord> lista = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private Date fechaDesde = null;
    private Date fechaHasta = null;
    private String vehiculoSeleccionado = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_registros, container, false);

        spnVehiculos = v.findViewById(R.id.spnVehiculos);
        btnDesde = v.findViewById(R.id.btnDesde);
        btnHasta = v.findViewById(R.id.btnHasta);
        btnFiltrar = v.findViewById(R.id.btnFiltrar);
        btnAgregar = v.findViewById(R.id.btnAgregarRegistro);
        rvRegistros = v.findViewById(R.id.rvRegistros);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        rvRegistros.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new FuelRecordAdapter(requireContext(), lista);
        rvRegistros.setAdapter(adapter);

        cargarVehiculos();
        cargarRegistros();

        btnDesde.setOnClickListener(vv -> seleccionarFecha(true));
        btnHasta.setOnClickListener(vv -> seleccionarFecha(false));
        btnFiltrar.setOnClickListener(vv -> aplicarFiltro());

        btnAgregar.setOnClickListener(viewBtn -> {
            View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_fuel, null);
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setView(dialogView);
            AlertDialog dialog = builder.create();

            EditText etVehiculo = dialogView.findViewById(R.id.etVehiculo);
            EditText etFecha = dialogView.findViewById(R.id.etFecha);
            EditText etLitros = dialogView.findViewById(R.id.etLitros);
            EditText etKm = dialogView.findViewById(R.id.etKm);
            EditText etPrecio = dialogView.findViewById(R.id.etPrecio);
            Spinner spnTipo = dialogView.findViewById(R.id.spnTipo);
            Button btnGuardar = dialogView.findViewById(R.id.btnGuardar);

            ArrayAdapter<String> tiposAdapter = new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_spinner_item,
                    new String[]{"Gasolina", "GLP", "GNV"});
            spnTipo.setAdapter(tiposAdapter);

            btnGuardar.setOnClickListener(vv -> {
                String vehiculo = etVehiculo.getText().toString().trim();
                String fechaStr = etFecha.getText().toString().trim();
                String tipo = spnTipo.getSelectedItem().toString();

                if (vehiculo.isEmpty() || fechaStr.isEmpty() ||
                        etLitros.getText().toString().isEmpty() ||
                        etKm.getText().toString().isEmpty() ||
                        etPrecio.getText().toString().isEmpty()) {
                    Toast.makeText(requireContext(), "Completa todos los campos", Toast.LENGTH_SHORT).show();
                    return;
                }

                double litros = Double.parseDouble(etLitros.getText().toString());
                double precio = Double.parseDouble(etPrecio.getText().toString());
                long km = Long.parseLong(etKm.getText().toString());

                String uid = mAuth.getCurrentUser().getUid();

                db.collection("fuel_records")
                        .whereEqualTo("vehiculo", vehiculo)
                        .orderBy("km", com.google.firebase.firestore.Query.Direction.DESCENDING)
                        .limit(1)
                        .get()
                        .addOnSuccessListener(snapshot -> {
                            long ultimoKm = 0;
                            if (!snapshot.isEmpty()) {
                                ultimoKm = snapshot.getDocuments().get(0).getLong("km");
                            }

                            if (km <= ultimoKm) {
                                Toast.makeText(requireContext(),
                                        "El kilometraje debe ser mayor al último registrado (" + ultimoKm + " km)",
                                        Toast.LENGTH_LONG).show();
                                return;
                            }

                            Map<String, Object> registro = new HashMap<>();
                            registro.put("id", String.format("%05d", new Random().nextInt(100000)));
                            registro.put("vehiculo", vehiculo);
                            registro.put("fecha", fechaStr);
                            registro.put("litros", litros);
                            registro.put("km", km);
                            registro.put("precio", precio);
                            registro.put("tipo", tipo);
                            registro.put("userId", uid);

                            db.collection("fuel_records")
                                    .add(registro)
                                    .addOnSuccessListener(docRef -> {
                                        Toast.makeText(requireContext(), "Registro agregado correctamente", Toast.LENGTH_SHORT).show();
                                        cargarRegistros();
                                        dialog.dismiss();
                                    })
                                    .addOnFailureListener(e ->
                                            Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        });
            });

            dialog.show();
        });

        return v;
    }

    private void cargarVehiculos() {
        String uid = mAuth.getCurrentUser().getUid();
        db.collection("vehicles")
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<String> vehiculos = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        vehiculos.add(doc.getString("id"));
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                            android.R.layout.simple_spinner_item, vehiculos);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spnVehiculos.setAdapter(adapter);

                    spnVehiculos.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            vehiculoSeleccionado = vehiculos.get(position);
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {}
                    });
                });
    }

    private void cargarRegistros() {
        lista.clear();
        String uid = mAuth.getCurrentUser().getUid();
        db.collection("fuel_records")
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    for (QueryDocumentSnapshot doc : snapshot) {
                        FuelRecord r = doc.toObject(FuelRecord.class);
                        lista.add(r);
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private void aplicarFiltro() {
        if (vehiculoSeleccionado == null) return;

        lista.clear();
        db.collection("fuel_records")
                .whereEqualTo("vehiculo", vehiculoSeleccionado)
                .get()
                .addOnSuccessListener(snapshot -> {
                    for (QueryDocumentSnapshot doc : snapshot) {
                        FuelRecord r = doc.toObject(FuelRecord.class);

                        // ✅ convertir Timestamp → Date antes de comparar
                        Timestamp ts = r.getFecha();
                        if (ts != null) {
                            Date fecha = ts.toDate();
                            if (fechaDesde != null && fecha.before(fechaDesde)) continue;
                            if (fechaHasta != null && fecha.after(fechaHasta)) continue;
                        }

                        lista.add(r);
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private void seleccionarFecha(boolean esDesde) {
        Calendar c = Calendar.getInstance();
        DatePickerDialog dp = new DatePickerDialog(requireContext(),
                (view, year, month, dayOfMonth) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(year, month, dayOfMonth);
                    if (esDesde) fechaDesde = selected.getTime();
                    else fechaHasta = selected.getTime();

                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    if (esDesde) btnDesde.setText("Desde: " + sdf.format(selected.getTime()));
                    else btnHasta.setText("Hasta: " + sdf.format(selected.getTime()));
                },
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        dp.show();
    }
}
