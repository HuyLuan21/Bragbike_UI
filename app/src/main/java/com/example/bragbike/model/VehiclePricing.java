package com.example.bragbike.model;

import com.google.gson.annotations.SerializedName;

public class VehiclePricing {
    private int id;
    
    @SerializedName("vehicle_type")
    private String vehicleType;
    
    private String label;
    
    @SerializedName("base_fare")
    private double baseFare;
    
    @SerializedName("price_per_km")
    private double pricePerKm;
    
    @SerializedName("peak_hour_multiplier")
    private double peakHourMultiplier;
    
    @SerializedName("min_fare")
    private double minFare;

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public double getBaseFare() { return baseFare; }
    public void setBaseFare(double baseFare) { this.baseFare = baseFare; }
    public double getPricePerKm() { return pricePerKm; }
    public void setPricePerKm(double pricePerKm) { this.pricePerKm = pricePerKm; }
    public double getPeakHourMultiplier() { return peakHourMultiplier; }
    public void setPeakHourMultiplier(double peakHourMultiplier) { this.peakHourMultiplier = peakHourMultiplier; }
    public double getMinFare() { return minFare; }
    public void setMinFare(double minFare) { this.minFare = minFare; }
}
