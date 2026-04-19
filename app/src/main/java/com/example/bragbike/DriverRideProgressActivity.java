package com.example.bragbike;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.bragbike.api.ApiService;
import com.example.bragbike.api.MapboxService;
import com.example.bragbike.api.RetrofitClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;
import com.google.gson.JsonObject;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.maps.CameraOptions;
import com.mapbox.maps.EdgeInsets;
import com.mapbox.maps.MapView;
import com.mapbox.maps.Style;
import com.mapbox.maps.extension.style.layers.LayerUtils;
import com.mapbox.maps.extension.style.layers.generated.CircleLayer;
import com.mapbox.maps.extension.style.layers.generated.LineLayer;
import com.mapbox.maps.extension.style.sources.SourceUtils;
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource;
import com.mapbox.maps.plugin.animation.CameraAnimationsUtils;
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin;
import com.mapbox.maps.plugin.locationcomponent.LocationComponentUtils;
import com.mapbox.turf.TurfMeasurement;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class DriverRideProgressActivity extends AppCompatActivity {

    public static final String EXTRA_RIDE_ID = "extra_ride_id";
    private static final String ROUTE_SOURCE_ID = "route-source-id";
    private static final String ROUTE_LAYER_ID = "route-layer-id";
    private static final String PICKUP_SOURCE_ID = "pickup-source";
    private static final String DROPOFF_SOURCE_ID = "dropoff-source";
    private static final String DRIVER_SIM_SOURCE_ID = "driver-sim-source";

    private MapView mapView;
    private TextView tvPassengerName, tvRideStatus, tvTargetAddress;
    private MaterialButton btnMainAction;
    private int rideId;
    private String currentStatus = "ACCEPTED";
    
    private MapboxService mapboxService;
    private FusedLocationProviderClient fusedLocationClient;
    private Point pickupPoint, dropoffPoint;

    // Simulation Variables
    private Handler simHandler = new Handler(Looper.getMainLooper());
    private Runnable simRunnable;
    private List<Point> routePoints;
    private int currentSimIndex = 0;
    private boolean isSimulating = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_ride_progress);

        rideId = getIntent().getIntExtra(EXTRA_RIDE_ID, -1);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        initMapboxService();
        initViews();
        
        mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS, style -> {
            initRouteLayers(style);
            initMarkerLayers(style);
            initLocationComponent();
            loadRideDetails();
        });
    }

    private void initMapboxService() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.mapbox.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        mapboxService = retrofit.create(MapboxService.class);
    }

    private void initViews() {
        mapView = findViewById(R.id.mapView);
        tvPassengerName = findViewById(R.id.tvPassengerName);
        tvRideStatus = findViewById(R.id.tvRideStatus);
        tvTargetAddress = findViewById(R.id.tvTargetAddress);
        btnMainAction = findViewById(R.id.btnMainAction);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnMainAction.setOnClickListener(v -> handleMainAction());
    }

    private void initRouteLayers(Style style) {
        SourceUtils.addSource(style, new GeoJsonSource.Builder(ROUTE_SOURCE_ID).build());
        LineLayer routeLayer = new LineLayer(ROUTE_LAYER_ID, ROUTE_SOURCE_ID);
        routeLayer.lineColor(Color.parseColor("#4285F4"));
        routeLayer.lineWidth(6.0);
        LayerUtils.addLayer(style, routeLayer);
    }

    private void initMarkerLayers(Style style) {
        // Driver Simulated Puck (Dấu chấm màu hồng cho tài xế)
        SourceUtils.addSource(style, new GeoJsonSource.Builder(DRIVER_SIM_SOURCE_ID).build());
        CircleLayer driverLayer = new CircleLayer("driver-sim-layer", DRIVER_SIM_SOURCE_ID);
        driverLayer.circleColor(Color.parseColor("#FF4081"));
        driverLayer.circleRadius(10.0);
        driverLayer.circleStrokeColor(Color.WHITE);
        driverLayer.circleStrokeWidth(3.0);
        LayerUtils.addLayer(style, driverLayer);

        SourceUtils.addSource(style, new GeoJsonSource.Builder(PICKUP_SOURCE_ID).build());
        LayerUtils.addLayer(style, new CircleLayer("pickup-layer", PICKUP_SOURCE_ID).circleColor(Color.parseColor("#4285F4")).circleRadius(8.0));
        SourceUtils.addSource(style, new GeoJsonSource.Builder(DROPOFF_SOURCE_ID).build());
        LayerUtils.addLayer(style, new CircleLayer("dropoff-layer", DROPOFF_SOURCE_ID).circleColor(Color.parseColor("#F4B400")).circleRadius(8.0));
    }

    private void initLocationComponent() {
        LocationComponentUtils.getLocationComponent(mapView).setEnabled(true);
    }

    private void loadRideDetails() {
        ApiService api = RetrofitClient.getInstance(this).getApiService();
        api.getRideById(rideId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> data = response.body();
                    updateUI(data);
                    try {
                        pickupPoint = Point.fromLngLat(Double.parseDouble(data.get("pickup_lng").toString()), Double.parseDouble(data.get("pickup_lat").toString()));
                        dropoffPoint = Point.fromLngLat(Double.parseDouble(data.get("drop_lng").toString()), Double.parseDouble(data.get("drop_lat").toString()));
                        updateMarkers();
                        updateNavigation(); 
                    } catch (Exception e) { e.printStackTrace(); }
                }
            }
            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });
    }

    private void updateMarkers() {
        mapView.getMapboxMap().getStyle(style -> {
            GeoJsonSource p = (GeoJsonSource) SourceUtils.getSource(style, PICKUP_SOURCE_ID);
            if (p != null && pickupPoint != null) p.geometry(pickupPoint);
            GeoJsonSource d = (GeoJsonSource) SourceUtils.getSource(style, DROPOFF_SOURCE_ID);
            if (d != null && dropoffPoint != null) d.geometry(dropoffPoint);
        });
    }

    private void updateNavigation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                Point driverPoint = Point.fromLngLat(location.getLongitude(), location.getLatitude());
                Point target = (currentStatus.contains("ACCEPTED") || currentStatus.equals("ON_THE_WAY")) ? pickupPoint : dropoffPoint;
                if (target != null) drawRoute(driverPoint, target);
            }
        });
    }

    private void drawRoute(Point origin, Point destination) {
        String coordinates = String.format(Locale.US, "%f,%f;%f,%f", origin.longitude(), origin.latitude(), destination.longitude(), destination.latitude());
        mapboxService.getDirections("driving", coordinates, getString(R.string.mapbox_access_token), "polyline6", "full")
                .enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject route = response.body().getAsJsonArray("routes").get(0).getAsJsonObject();
                    String geometry = route.get("geometry").getAsString();
                    LineString lineString = LineString.fromPolyline(geometry, 6);
                    routePoints = lineString.coordinates(); 
                    
                    mapView.getMapboxMap().getStyle(style -> {
                        GeoJsonSource source = (GeoJsonSource) SourceUtils.getSource(style, ROUTE_SOURCE_ID);
                        if (source != null) {
                            source.geometry(lineString);
                            CameraOptions options = mapView.getMapboxMap().cameraForGeometry(lineString, new EdgeInsets(100.0, 100.0, 400.0, 100.0), null, null);
                            CameraAnimationsUtils.flyTo(mapView.getMapboxMap(), options, null, null);
                            
                            // TỰ ĐỘNG BẮT ĐẦU GIẢ LẬP
                            if (!isSimulating && routePoints != null && routePoints.size() > 1) {
                                startMovementSimulation();
                            }
                        }
                    });
                }
            }
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {}
        });
    }

    private void startMovementSimulation() {
        if (routePoints == null || routePoints.isEmpty()) return;
        isSimulating = true;
        currentSimIndex = 0;

        simRunnable = new Runnable() {
            @Override
            public void run() {
                if (currentSimIndex < routePoints.size()) {
                    Point nextPoint = routePoints.get(currentSimIndex);
                    updatePositionUI(nextPoint);
                    syncPositionWithServer(nextPoint);
                    
                    currentSimIndex += 1; // Nhích từng điểm cho mượt
                    simHandler.postDelayed(this, 800); // Tốc độ di chuyển
                } else {
                    isSimulating = false;
                    Toast.makeText(DriverRideProgressActivity.this, "Đã đến đích!", Toast.LENGTH_SHORT).show();
                }
            }
        };
        simHandler.post(simRunnable);
    }

    private void updatePositionUI(Point point) {
        mapView.getMapboxMap().getStyle(style -> {
            GeoJsonSource source = (GeoJsonSource) SourceUtils.getSource(style, DRIVER_SIM_SOURCE_ID);
            if (source != null) source.geometry(point);
        });

        // Tính toán góc xoay dựa trên điểm trước đó
        double bearing = 0.0;
        if (currentSimIndex > 0 && currentSimIndex < routePoints.size()) {
            bearing = TurfMeasurement.bearing(routePoints.get(currentSimIndex - 1), point);
        }

        CameraOptions cameraOptions = new CameraOptions.Builder()
                .center(point)
                .zoom(16.5)
                .bearing(bearing)
                .pitch(45.0) 
                .build();
        CameraAnimationsUtils.flyTo(mapView.getMapboxMap(), cameraOptions, null, null);
    }

    private void syncPositionWithServer(Point point) {
        Map<String, Object> body = new HashMap<>();
        body.put("lat", point.latitude());
        body.put("lng", point.longitude());
        RetrofitClient.getInstance(this).getApiService().updateLocation(body).enqueue(new Callback<Map<String, Object>>() {
            @Override public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {}
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });
    }

    private void updateUI(Map<String, Object> data) {
        Map<String, Object> user = (Map<String, Object>) data.get("user");
        if (user != null) tvPassengerName.setText((String) user.get("full_name"));
        currentStatus = (String) data.get("status");
        refreshActionStatus();
        if (currentStatus.contains("ACCEPTED") || currentStatus.equals("ON_THE_WAY") || currentStatus.equals("ARRIVED")) {
            tvTargetAddress.setText("Điểm đón: " + data.get("pickup_address"));
        } else {
            tvTargetAddress.setText("Điểm trả: " + data.get("drop_address"));
        }
    }

    private void refreshActionStatus() {
        switch (currentStatus) {
            case "DRIVER_ACCEPTED": case "ACCEPTED": case "ON_THE_WAY":
                tvRideStatus.setText("Đang đến điểm đón");
                btnMainAction.setText("TÔI ĐÃ ĐẾN NƠI");
                break;
            case "ARRIVED":
                tvRideStatus.setText("Khách chuẩn bị lên xe");
                btnMainAction.setText("BẮT ĐẦU HÀNH TRÌNH");
                break;
            case "IN_PROGRESS":
                tvRideStatus.setText("Đang di chuyển tới điểm trả");
                btnMainAction.setText("HOÀN THÀNH CHUYẾN ĐI");
                break;
            case "COMPLETED": finish(); break;
        }
    }

    private void handleMainAction() {
        if (isSimulating) {
            simHandler.removeCallbacks(simRunnable);
            isSimulating = false;
        }
        ApiService api = RetrofitClient.getInstance(this).getApiService();
        Call<Map<String, Object>> call = null;
        if (currentStatus.contains("ACCEPTED") || currentStatus.equals("ON_THE_WAY")) call = api.pickupRide(rideId);
        else if ("ARRIVED".equals(currentStatus)) call = api.startRide(rideId);
        else if ("IN_PROGRESS".equals(currentStatus)) call = api.completeRide(rideId);

        if (call != null) {
            call.enqueue(new Callback<Map<String, Object>>() {
                @Override
                public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                    if (response.isSuccessful()) loadRideDetails(); 
                }
                @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (simHandler != null && simRunnable != null) simHandler.removeCallbacks(simRunnable);
    }
}
