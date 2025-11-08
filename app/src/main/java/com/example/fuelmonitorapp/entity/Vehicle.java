package com.example.fuelmonitorapp.entity;

import com.google.firebase.Timestamp;

public class Vehicle {
    private String id;
    private String placa;
    private String marca;
    private String modelo;
    private int anio;
    private Timestamp revision;
    private String userId;

    public Vehicle() {}

    public Vehicle(String id, String placa, String marca, String modelo, int anio, Timestamp revision, String userId) {
        this.id = id;
        this.placa = placa;
        this.marca = marca;
        this.modelo = modelo;
        this.anio = anio;
        this.revision = revision;
        this.userId = userId;
    }

    public String getId() { return id; }
    public String getPlaca() { return placa; }
    public String getMarca() { return marca; }
    public String getModelo() { return modelo; }
    public int getAnio() { return anio; }
    public Timestamp getRevision() { return revision; }
    public String getUserId() { return userId; }

    public void setId(String id) { this.id = id; }
    public void setPlaca(String placa) { this.placa = placa; }
    public void setMarca(String marca) { this.marca = marca; }
    public void setModelo(String modelo) { this.modelo = modelo; }
    public void setAnio(int anio) { this.anio = anio; }
    public void setRevision(Timestamp revision) { this.revision = revision; }
    public void setUserId(String userId) { this.userId = userId; }
}
