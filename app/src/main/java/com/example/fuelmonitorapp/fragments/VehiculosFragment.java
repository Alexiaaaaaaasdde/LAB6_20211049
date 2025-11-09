package com.example.fuelmonitorapp.fragments;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fuelmonitorapp.R;
import com.example.fuelmonitorapp.adapter.VehicleAdapter;
import com.example.fuelmonitorapp.entity.Vehicle;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.Writer;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;

import java.text.SimpleDateFormat;
import java.util.*;

public class VehiculosFragment extends Fragment {

    private RecyclerView recyclerVehiculos;
    private Button btnAgregarVehiculo;
    private VehicleAdapter adapter;
    private List<Vehicle> vehicleList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_vehiculos, container, false);

        recyclerVehiculos = view.findViewById(R.id.recyclerVehiculos);
        btnAgregarVehiculo = view.findViewById(R.id.btnAgregarVehiculo);
        recyclerVehiculos.setLayoutManager(new LinearLayoutManager(getContext()));

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        vehicleList = new ArrayList<>();

        adapter = new VehicleAdapter(vehicleList, new VehicleAdapter.OnVehicleActionListener() {
            @Override
            public void onEdit(Vehicle v) {
                mostrarDialogoVehiculo(v, true);
            }

            @Override
            public void onDelete(Vehicle v) {
                eliminarVehiculo(v);
            }

            @Override
            public void onShowQR(Vehicle v) {
                mostrarDialogoQR(v);
            }
        });

        recyclerVehiculos.setAdapter(adapter);

        cargarVehiculos();
        btnAgregarVehiculo.setOnClickListener(v -> mostrarDialogoVehiculo(null, false));

        return view;
    }

    private void cargarVehiculos() {
        String userId = mAuth.getCurrentUser().getUid();

        db.collection("vehicles")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(query -> {
                    vehicleList.clear();
                    for (QueryDocumentSnapshot doc : query) {
                        Vehicle vehicle = doc.toObject(Vehicle.class);
                        vehicleList.add(vehicle);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void mostrarDialogoVehiculo(@Nullable Vehicle v, boolean editar) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_vehicle, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        EditText etId = dialogView.findViewById(R.id.etId);
        EditText etPlaca = dialogView.findViewById(R.id.etPlaca);
        EditText etMarca = dialogView.findViewById(R.id.etMarca);
        EditText etModelo = dialogView.findViewById(R.id.etModelo);
        EditText etAnio = dialogView.findViewById(R.id.etAnio);
        EditText etRevision = dialogView.findViewById(R.id.etRevision);
        Button btnGuardar = dialogView.findViewById(R.id.btnGuardarVehiculo);

        final Timestamp[] fechaRevision = {null};

        etRevision.setOnClickListener(v1 -> {
            Calendar calendar = Calendar.getInstance();
            DatePickerDialog datePicker = new DatePickerDialog(
                    getContext(),
                    (view, year, month, dayOfMonth) -> {
                        calendar.set(year, month, dayOfMonth);
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        etRevision.setText(sdf.format(calendar.getTime()));
                        fechaRevision[0] = new Timestamp(calendar.getTime());
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePicker.show();
        });

        if (editar && v != null) {
            etId.setText(v.getId());
            etPlaca.setText(v.getPlaca());
            etMarca.setText(v.getMarca());
            etModelo.setText(v.getModelo());
            etAnio.setText(String.valueOf(v.getAnio()));

            if (v.getRevision() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                etRevision.setText(sdf.format(v.getRevision().toDate()));
                fechaRevision[0] = v.getRevision();
            }
        }

        btnGuardar.setOnClickListener(e -> {
            String id = etId.getText().toString().trim();
            String placa = etPlaca.getText().toString().trim();
            String marca = etMarca.getText().toString().trim();
            String modelo = etModelo.getText().toString().trim();
            String anioText = etAnio.getText().toString().trim();
            String userId = mAuth.getCurrentUser().getUid();

            if (id.isEmpty() || placa.isEmpty() || anioText.isEmpty()) {
                Toast.makeText(getContext(), "Completa todos los campos", Toast.LENGTH_SHORT).show();
                return;
            }

            int anio = Integer.parseInt(anioText);
            Timestamp revisionDate = fechaRevision[0] != null ? fechaRevision[0] : Timestamp.now();

            Vehicle vehicle = new Vehicle(id, placa, marca, modelo, anio, revisionDate, userId);

            if (editar) {
                db.collection("vehicles")
                        .whereEqualTo("id", v.getId())
                        .whereEqualTo("userId", userId)
                        .get()
                        .addOnSuccessListener(query -> {
                            for (QueryDocumentSnapshot doc : query) {
                                doc.getReference().set(vehicle);
                            }
                            Toast.makeText(getContext(), "Vehículo actualizado", Toast.LENGTH_SHORT).show();
                            cargarVehiculos();
                            dialog.dismiss();
                        });
            } else {
                db.collection("vehicles")
                        .add(vehicle)
                        .addOnSuccessListener(docRef -> {
                            Toast.makeText(getContext(), "Vehículo registrado", Toast.LENGTH_SHORT).show();
                            cargarVehiculos();
                            dialog.dismiss();
                        });
            }
        });

        dialog.show();
    }

    private void eliminarVehiculo(Vehicle v) {
        db.collection("vehicles")
                .whereEqualTo("id", v.getId())
                .whereEqualTo("userId", mAuth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(query -> {
                    for (QueryDocumentSnapshot doc : query) {
                        doc.getReference().delete();
                    }
                    Toast.makeText(getContext(), "Vehículo eliminado", Toast.LENGTH_SHORT).show();
                    cargarVehiculos();
                });
    }

    private void mostrarDialogoQR(Vehicle vehicle) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View qrView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_qr, null);
        builder.setView(qrView);
        AlertDialog dialog = builder.create();

        // ✅ Configurar fondo transparente (opcional pero recomendado)
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        ImageView imgQr = qrView.findViewById(R.id.imgQr);
        Button btnCerrar = qrView.findViewById(R.id.btnCerrar);

        final String placa = vehicle.getPlaca();
        final String idVehiculo = vehicle.getId();
        final String fechaRevision;
        if (vehicle.getRevision() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            fechaRevision = sdf.format(vehicle.getRevision().toDate());
        } else {
            fechaRevision = "No registrada";
        }

        imgQr.setVisibility(View.GONE); // Ocultar mientras carga

        db.collection("fuel_records")
                .whereEqualTo("vehicleId", idVehiculo)
                .orderBy("kilometraje", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    double ultimoKm = 0;
                    if (!snapshot.isEmpty()) {
                        Double km = snapshot.getDocuments().get(0).getDouble("kilometraje");
                        if (km != null) ultimoKm = km;
                    }

                    String contenidoQR = "📋 Placa: " + placa +
                            "\n📏 Último KM: " + ultimoKm +
                            "\n🗓 Revisión: " + fechaRevision;

                    try {
                        QRCodeWriter writer = new QRCodeWriter();
                        BitMatrix bitMatrix = writer.encode(contenidoQR, BarcodeFormat.QR_CODE, 600, 600);
                        int width = bitMatrix.getWidth();
                        int height = bitMatrix.getHeight();
                        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888); // ✅ Cambiar a ARGB_8888

                        for (int x = 0; x < width; x++) {
                            for (int y = 0; y < height; y++) {
                                bmp.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                            }
                        }

                        // ✅ Actualizar UI - Ya estamos en el hilo principal
                        imgQr.setImageBitmap(bmp);
                        imgQr.setVisibility(View.VISIBLE);

                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(getContext(), "Error al generar QR: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error al consultar datos: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });

        btnCerrar.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

}
