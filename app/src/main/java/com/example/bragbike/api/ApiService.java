package com.example.bragbike.api;

import com.example.bragbike.model.LoginRequest;
import com.example.bragbike.model.LoginResponse;
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
    Call<Map<String, Object>> getMe();

    @PUT("users/me")
    Call<Map<String, Object>> updateMe(@Body Map<String, Object> body);

    @PUT("users/me/password")
    Call<Map<String, Object>> changePassword(@Body Map<String, Object> body);

    @PUT("users/me/avatar")
    Call<Map<String, Object>> updateAvatar(@Body Map<String, Object> body);

    // ═══════════════════════════════════════
    //  3. DRIVERS
    // ═══════════════════════════════════════
    @POST("drivers/apply")
    Call<Map<String, Object>> applyDriver(@Body Map<String, Object> body);

    @GET("drivers/me")
    Call<Map<String, Object>> getMyDriverProfile();

    @GET("drivers/me/stats")
    Call<Map<String, Object>> getMyDriverStats();

    @PATCH("drivers/me/online")
    Call<Map<String, Object>> toggleOnline(@Body Map<String, Object> body);

    @PATCH("drivers/me/location")
    Call<Map<String, Object>> updateLocation(@Body Map<String, Object> body);

    @GET("drivers/available-rides")
    Call<List<Map<String, Object>>> getAvailableRides();

    @GET("drivers/me/history")
    Call<List<Map<String, Object>>> getMyRideHistory();

    @GET("drivers/{id}")
    Call<Map<String, Object>> getDriverPublic(@Path("id") int driverId);

    // ═══════════════════════════════════════
    //  4. RIDES
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
    Call<List<Map<String, Object>>> getUserRideHistory();

    @GET("rides/{id}")
    Call<Map<String, Object>> getRideById(@Path("id") int rideId);

    @GET("rides/{id}/status-history")
    Call<List<Map<String, Object>>> getRideStatusHistory(@Path("id") int rideId);

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

    // ═══════════════════════════════════════
    //  5. RATINGS
    // ═══════════════════════════════════════
    @POST("ratings")
    Call<Map<String, Object>> createRating(@Body Map<String, Object> body);

    @GET("ratings/ride/{rideId}")
    Call<Map<String, Object>> getRatingByRide(@Path("rideId") int rideId);

    @GET("ratings/driver/{driverId}")
    Call<List<Map<String, Object>>> getDriverRatings(@Path("driverId") int driverId);

    // ═══════════════════════════════════════
    //  6. REPORTS
    // ═══════════════════════════════════════
    @POST("reports")
    Call<Map<String, Object>> createReport(@Body Map<String, Object> body);

    @GET("reports/me")
    Call<List<Map<String, Object>>> getMyReports();

    @GET("reports/{id}")
    Call<Map<String, Object>> getReportById(@Path("id") int reportId);

    // ═══════════════════════════════════════
    //  7. NOTIFICATIONS
    // ═══════════════════════════════════════
    @GET("notifications")
    Call<List<Map<String, Object>>> getMyNotifications(
            @Query("page") int page,
            @Query("limit") int limit
    );

    @GET("notifications/unread-count")
    Call<Map<String, Object>> getUnreadCount();

    @PATCH("notifications/read-all")
    Call<Map<String, Object>> markAllAsRead();

    @PATCH("notifications/{id}/read")
    Call<Map<String, Object>> markAsRead(@Path("id") int notifId);
}
