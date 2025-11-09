package com.example.fuelmonitorapp.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.fuelmonitorapp.R;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ResumenFragment extends Fragment {

    private BarChart barChart;
    private PieChart pieChart;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_resumen, container, false);

        barChart = v.findViewById(R.id.barChart);
        pieChart = v.findViewById(R.id.pieChart);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        cargarDatos();

        return v;
    }

    private void cargarDatos() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        db.collection("fuel_records")
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    Map<Integer, Float> litrosPorMes = new HashMap<>();
                    Map<String, Float> litrosPorTipo = new HashMap<>();

                    for (QueryDocumentSnapshot doc : snapshot) {
                        try {
                            Double litros = doc.getDouble("litros");
                            Object fechaObj = doc.get("fecha");
                            String tipo = doc.getString("tipoCombustible");

                            if (litros == null || fechaObj == null) continue;

                            java.util.Date fecha;
                            if (fechaObj instanceof com.google.firebase.Timestamp) {
                                fecha = ((com.google.firebase.Timestamp) fechaObj).toDate();
                            } else continue;

                            java.util.Calendar cal = java.util.Calendar.getInstance();
                            cal.setTime(fecha);
                            int mes = cal.get(java.util.Calendar.MONTH); // 0=enero

                            litrosPorMes.put(mes, litrosPorMes.getOrDefault(mes, 0f) + litros.floatValue());
                            litrosPorTipo.put(tipo, litrosPorTipo.getOrDefault(tipo, 0f) + litros.floatValue());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    mostrarGraficoBarras(litrosPorMes);
                    mostrarGraficoTorta(litrosPorTipo);
                });
    }

    private void mostrarGraficoBarras(Map<Integer, Float> litrosPorMes) {
        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();

        for (int i = 0; i < 12; i++) {
            float litros = litrosPorMes.getOrDefault(i, 0f);
            entries.add(new BarEntry(i, litros));
            labels.add(new DateFormatSymbols(new Locale("es")).getMonths()[i].substring(0, 3));
        }

        BarDataSet dataSet = new BarDataSet(entries, "Litros por mes");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextSize(10f);
        BarData data = new BarData(dataSet);

        barChart.setData(data);
        barChart.getXAxis().setDrawGridLines(false);
        barChart.getXAxis().setTextSize(9f);
        barChart.getAxisRight().setEnabled(false);
        barChart.getDescription().setEnabled(false);
        barChart.animateY(1000);
        barChart.invalidate();
    }

    private void mostrarGraficoTorta(Map<String, Float> litrosPorTipo) {
        ArrayList<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Float> entry : litrosPorTipo.entrySet()) {
            entries.add(new PieEntry(entry.getValue(), entry.getKey()));
        }

        PieDataSet dataSet = new PieDataSet(entries, "Consumo por tipo");
        dataSet.setColors(ColorTemplate.COLORFUL_COLORS);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(12f);

        PieData data = new PieData(dataSet);
        pieChart.setData(data);

        Description desc = new Description();
        desc.setText("Proporción de consumo por tipo de combustible");
        pieChart.setDescription(desc);

        pieChart.setCenterText("Combustible");
        pieChart.setHoleRadius(40f);
        pieChart.animateY(1000);
        pieChart.invalidate();
    }
}
