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

        if (mAuth.getCurrentUser() != null) {
            cargarVehiculos();
            cargarRegistros();
        } else {
            Toast.makeText(requireContext(), "Inicia sesión para ver registros", Toast.LENGTH_SHORT).show();
        }

        btnDesde.setOnClickListener(vv -> seleccionarFecha(true));
        btnHasta.setOnClickListener(vv -> seleccionarFecha(false));
        btnFiltrar.setOnClickListener(vv -> aplicarFiltro());
        btnAgregar.setOnClickListener(viewBtn -> mostrarDialogoAgregar());

        return v;
    }

    private void mostrarDialogoAgregar() {
        // Inflar el layout personalizado del diálogo
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_fuel, null);

        // Crear el diálogo SIN setPositiveButton / setNegativeButton
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        // Referencias de los elementos del layout
        EditText etVehiculo = dialogView.findViewById(R.id.etVehiculo);
        EditText etFecha = dialogView.findViewById(R.id.etFecha);
        EditText etLitros = dialogView.findViewById(R.id.etLitros);
        EditText etKm = dialogView.findViewById(R.id.etKm);
        EditText etPrecio = dialogView.findViewById(R.id.etPrecio);
        Spinner spnTipo = dialogView.findViewById(R.id.spnTipo);
        Button btnGuardar = dialogView.findViewById(R.id.btnGuardar);

        // Configurar spinner de tipo de combustible
        ArrayAdapter<String> tiposAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item,
                new String[]{"Gasolina", "GLP", "GNV"});
        tiposAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnTipo.setAdapter(tiposAdapter);

        // 📅 Calendario para seleccionar la fecha
        final Calendar fechaSeleccionada = Calendar.getInstance();
        etFecha.setOnClickListener(view -> {
            new DatePickerDialog(requireContext(), (picker, year, month, day) -> {
                fechaSeleccionada.set(year, month, day);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                etFecha.setText(sdf.format(fechaSeleccionada.getTime()));
            }, fechaSeleccionada.get(Calendar.YEAR),
                    fechaSeleccionada.get(Calendar.MONTH),
                    fechaSeleccionada.get(Calendar.DAY_OF_MONTH)).show();
        });

        // Acción del botón Guardar
        btnGuardar.setOnClickListener(v -> {
            String vehiculo = etVehiculo.getText().toString().trim();
            String tipoCombustible = spnTipo.getSelectedItem().toString();

            if (vehiculo.isEmpty() ||
                    etLitros.getText().toString().isEmpty() ||
                    etKm.getText().toString().isEmpty() ||
                    etPrecio.getText().toString().isEmpty() ||
                    etFecha.getText().toString().isEmpty()) {
                Toast.makeText(requireContext(), "Completa todos los campos", Toast.LENGTH_SHORT).show();
                return;
            }

            double litros = Double.parseDouble(etLitros.getText().toString());
            double precioTotal = Double.parseDouble(etPrecio.getText().toString());
            double kilometraje = Double.parseDouble(etKm.getText().toString());
            String uid = mAuth.getCurrentUser().getUid();

            // Crear registro con fecha tipo Timestamp
            Map<String, Object> registro = new HashMap<>();
            registro.put("id", String.format("%05d", new Random().nextInt(100000)));
            registro.put("vehicleId", vehiculo);
            registro.put("litros", litros);
            registro.put("kilometraje", kilometraje);
            registro.put("precioTotal", precioTotal);
            registro.put("tipoCombustible", tipoCombustible);
            registro.put("userId", uid);
            registro.put("fecha", new Timestamp(fechaSeleccionada.getTime()));

            // Guardar en Firestore
            db.collection("fuel_records")
                    .add(registro)
                    .addOnSuccessListener(docRef -> {
                        Toast.makeText(requireContext(), "Registro agregado correctamente", Toast.LENGTH_SHORT).show();
                        cargarRegistros();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    });
        });

        dialog.show();
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
                        try {
                            FuelRecord r = new FuelRecord();
                            r.setId(doc.getString("id"));
                            r.setVehicleId(doc.getString("vehicleId"));
                            r.setLitros(doc.getDouble("litros") != null ? doc.getDouble("litros") : 0);
                            r.setKilometraje(doc.getDouble("kilometraje") != null ? doc.getDouble("kilometraje") : 0);
                            r.setPrecioTotal(doc.getDouble("precioTotal") != null ? doc.getDouble("precioTotal") : 0);
                            r.setTipoCombustible(doc.getString("tipoCombustible"));

                            Object fechaObj = doc.get("fecha");
                            if (fechaObj instanceof Timestamp) {
                                r.setFecha((Timestamp) fechaObj);
                            } else {
                                // convertir si era String antigua
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                                try {
                                    Date parsed = sdf.parse(String.valueOf(fechaObj));
                                    r.setFecha(new Timestamp(parsed));
                                } catch (Exception e) {
                                    r.setFecha(new Timestamp(new Date()));
                                }
                            }
                            lista.add(r);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Error al cargar: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void aplicarFiltro() {
        if (vehiculoSeleccionado == null) return;
        lista.clear();

        db.collection("fuel_records")
                .whereEqualTo("vehicleId", vehiculoSeleccionado)
                .get()
                .addOnSuccessListener(snapshot -> {
                    for (QueryDocumentSnapshot doc : snapshot) {
                        Object fechaObj = doc.get("fecha");
                        Timestamp fecha = null;
                        if (fechaObj instanceof Timestamp) fecha = (Timestamp) fechaObj;

                        if (fecha != null) {
                            Date fechaDate = fecha.toDate();
                            if (fechaDesde != null && fechaDate.before(fechaDesde)) continue;
                            if (fechaHasta != null && fechaDate.after(fechaHasta)) continue;
                        }

                        FuelRecord r = doc.toObject(FuelRecord.class);
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
