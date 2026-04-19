package com.example.bragbike.model;

import com.google.gson.annotations.SerializedName;

public class Ride {
    @SerializedName("id")
    private int id;
    
    @SerializedName("pickup_address")
    private String pickupAddress;
    
    @SerializedName("drop_address")
    private String dropoffAddress;
    
    @SerializedName("status")
    private String status;
    
    @SerializedName("total_price")
    private Double fare;
    
    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("vehicle_type")
    private String vehicleType;

    @SerializedName("user")
    private User customer;

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getPickupAddress() { return pickupAddress != null ? pickupAddress : "N/A"; }
    public void setPickupAddress(String pickupAddress) { this.pickupAddress = pickupAddress; }

    public String getDropoffAddress() { return dropoffAddress != null ? dropoffAddress : "N/A"; }
    public void setDropoffAddress(String dropoffAddress) { this.dropoffAddress = dropoffAddress; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public double getFare() { return fare != null ? fare : 0.0; }
    public void setFare(double fare) { this.fare = fare; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

    public User getCustomer() { return customer; }
    public void setCustomer(User customer) { this.customer = customer; }
}
