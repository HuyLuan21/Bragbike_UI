package com.example.bragbike.api;

import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface MapboxService {
    @GET("directions/v5/mapbox/{profile}/{coordinates}")
    Call<JsonObject> getDirections(
            @Path("profile") String profile,
            @Path("coordinates") String coordinates,
            @Query("access_token") String accessToken,
            @Query("geometries") String geometries,
            @Query("overview") String overview
    );
}
