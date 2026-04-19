package com.example.bragbike.api;

import com.example.bragbike.model.LoginRequest;
import com.example.bragbike.model.LoginResponse;
import com.example.bragbike.model.PeakHour;
import com.example.bragbike.model.Ride;
import com.example.bragbike.model.User;
import com.example.bragbike.model.VehiclePricing;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.*;
import java.util.List;
import java.util.Map;

public interface ApiService {

    // ═══════════════════════════════════════
    //  1. AUTH
    // ═══════════════════════════════════════
    @POST("auth/register")
    Call<Map<String, Object>> register(@Body Map<String, Object> body);

    @POST("auth/login")
    Call<LoginResponse> login(@Body LoginRequest body);

    @POST("auth/logout")
    Call<Map<String, Object>> logout();

    @POST("auth/refresh-token")
    Call<Map<String, Object>> refreshToken(@Body Map<String, Object> body);

    @POST("auth/forgot-password")
    Call<Map<String, Object>> forgotPassword(@Body Map<String, Object> body);

    @POST("auth/reset-password")
    Call<Map<String, Object>> resetPassword(@Body Map<String, Object> body);

    // ═══════════════════════════════════════
    //  2. USERS
    // ═══════════════════════════════════════
    @GET("users/me")
    Call<User> getMe();

    @PUT("users/me")
    Call<User> updateMe(@Body Map<String, Object> body);

    @PUT("users/me/password")
    Call<Map<String, Object>> changePassword(@Body Map<String, Object> body);

    @Multipart
    @PUT("users/me/avatar")
    Call<Map<String, Object>> updateAvatar(@Part MultipartBody.Part avatar);

    // ═══════════════════════════════════════
    //  3. DRIVERS
    // ═══════════════════════════════════════
    @GET("drivers/me/stats")
    Call<Map<String, Object>> getDriverStats();

    @GET("drivers/me")
    Call<Map<String, Object>> getDriverProfile();

    @GET("drivers/me/history")
    Call<List<Ride>> getDriverRideHistory();

    @PATCH("drivers/me/online")
    Call<Map<String, Object>> toggleOnline(@Body Map<String, Object> body);

    @PATCH("drivers/me/location")
    Call<Map<String, Object>> updateLocation(@Body Map<String, Object> body);

    @GET("drivers/available-rides")
    Call<List<Ride>> getAvailableRides();

    // ═══════════════════════════════════════
    //  4. PRICING & ESTIMATION
    // ═══════════════════════════════════════
    @GET("pricing")
    Call<List<VehiclePricing>> getAllPricing();

    @GET("pricing/peak-hours")
    Call<List<PeakHour>> getPeakHours();

    @GET("pricing/{vehicleType}")
    Call<VehiclePricing> getPricingByType(@Path("vehicleType") String vehicleType);

    @GET("pricing/calculate")
    Call<Map<String, Object>> calculateFare(
            @Query("vehicleType") String vehicleType,
            @Query("distance") double distanceKm
    );

    // ═══════════════════════════════════════
    //  5. RIDES
    // ═══════════════════════════════════════
    @GET("rides/estimate")
    Call<Map<String, Object>> estimateFare(
            @Query("pickup_lat") double pickupLat,
            @Query("pickup_lng") double pickupLng,
            @Query("dropoff_lat") double dropoffLat,
            @Query("dropoff_lng") double dropoffLng,
            @Query("vehicle_type") String vehicleType
    );

    @POST("rides")
    Call<Map<String, Object>> createRide(@Body Map<String, Object> body);

    @GET("rides/history")
    Call<List<Ride>> getUserRideHistory();

    @GET("rides/{id}")
    Call<Map<String, Object>> getRideById(@Path("id") int rideId);

    @POST("rides/{id}/accept")
    Call<Map<String, Object>> acceptRide(@Path("id") int rideId);

    @POST("rides/{id}/pickup")
    Call<Map<String, Object>> pickupRide(@Path("id") int rideId);

    @POST("rides/{id}/start")
    Call<Map<String, Object>> startRide(@Path("id") int rideId);

    @POST("rides/{id}/complete")
    Call<Map<String, Object>> completeRide(@Path("id") int rideId);

    @POST("rides/{id}/cancel")
    Call<Map<String, Object>> cancelRide(@Path("id") int rideId, @Body Map<String, Object> body);
}
