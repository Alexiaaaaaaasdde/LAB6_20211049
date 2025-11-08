package com.example.fuelmonitorapp.entity;

import com.google.firebase.Timestamp;

public class FuelRecord {
    private String id;          // aleatorio de 5 dígitos
    private String vehicleId;   // referencia al ID del vehículo
    private Timestamp fecha;
    private double litros;
    private double kilometraje;
    private double precioTotal;
    private String tipoCombustible;
    private String userId;

    public FuelRecord() {}  // necesario para Firebase

    public FuelRecord(String id, String vehicleId, Timestamp fecha, double litros,
                      double kilometraje, double precioTotal, String tipoCombustible, String userId) {
        this.id = id;
        this.vehicleId = vehicleId;
        this.fecha = fecha;
        this.litros = litros;
        this.kilometraje = kilometraje;
        this.precioTotal = precioTotal;
        this.tipoCombustible = tipoCombustible;
        this.userId = userId;
    }

    public String getId() { return id; }
    public String getVehicleId() { return vehicleId; }
    public Timestamp getFecha() { return fecha; }
    public double getLitros() { return litros; }
    public double getKilometraje() { return kilometraje; }
    public double getPrecioTotal() { return precioTotal; }
    public String getTipoCombustible() { return tipoCombustible; }
    public String getUserId() { return userId; }

    public void setId(String id) { this.id = id; }
    public void setVehicleId(String vehicleId) { this.vehicleId = vehicleId; }
    public void setFecha(Timestamp fecha) { this.fecha = fecha; }
    public void setLitros(double litros) { this.litros = litros; }
    public void setKilometraje(double kilometraje) { this.kilometraje = kilometraje; }
    public void setPrecioTotal(double precioTotal) { this.precioTotal = precioTotal; }
    public void setTipoCombustible(String tipoCombustible) { this.tipoCombustible = tipoCombustible; }
    public void setUserId(String userId) { this.userId = userId; }
}
