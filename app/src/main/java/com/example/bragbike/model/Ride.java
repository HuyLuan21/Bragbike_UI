package com.example.bragbike.model;

import com.google.gson.annotations.SerializedName;

public class Ride {
    @SerializedName("id")
    private int id;
    
    @SerializedName("pickup_address")
    private String pickupAddress;
    
    // Đổi thành drop_address để khớp với API của bạn
    @SerializedName("drop_address")
    private String dropoffAddress;
    
    @SerializedName("status")
    private String status;
    
    // Đổi thành total_price để khớp với API của bạn
    @SerializedName("total_price")
    private Double fare;
    
    @SerializedName("created_at")
    private String createdAt;

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
}
