package com.example.bragbike.users;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.bragbike.databinding.ActivityBookingBinding;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.Priority;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

import com.mapbox.geojson.Point;
import com.mapbox.maps.CameraOptions;
import com.mapbox.maps.Style;
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin;
import com.mapbox.maps.plugin.locationcomponent.LocationComponentUtils;

import java.util.Locale;

public class BookingActivity extends AppCompatActivity {

    private ActivityBookingBinding binding;
    private static final int REQUEST_LOCATION_PERMISSION = 1;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    private final Point VIETNAM_CENTER = Point.fromLngLat(108.2022, 16.0544);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBookingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        initMap();
        setupBottomSheet();
        setupClickListeners();
    }

    // ================= MAP =================
    private void initMap() {
        binding.mapView.getMapboxMap().setCamera(
                new CameraOptions.Builder()
                        .center(VIETNAM_CENTER)
                        .zoom(6.0)
                        .build()
        );

        binding.mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS, style -> {
            initLocationComponent();
            checkPermissionAndGetLocation();
        });
    }

    private void initLocationComponent() {
        LocationComponentPlugin locationComponentPlugin = LocationComponentUtils.getLocationComponent(binding.mapView);
        locationComponentPlugin.setEnabled(true);
    }

    // ================= LOCATION =================
    private void checkPermissionAndGetLocation() {
        if (hasLocationPermission()) {
            startLocationUpdates();
        } else {
            requestLocationPermission();
        }
    }

    private boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void startLocationUpdates() {
        // Sử dụng Builder mới cho LocationRequest (tránh Deprecated)
        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
                .setMinUpdateIntervalMillis(1000)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult result) {
                android.location.Location location = result.getLastLocation();
                if (location != null) {
                    updateMapLocation(location);
                    stopLocationUpdates(); // chỉ lấy 1 lần để tiết kiệm pin
                }
            }
        };

        if (hasLocationPermission()) {
            fusedLocationClient.requestLocationUpdates(
                    request,
                    locationCallback,
                    getMainLooper()
            );
        }
    }

    private void stopLocationUpdates() {
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    private void updateMapLocation(android.location.Location location) {
        Point point = Point.fromLngLat(
                location.getLongitude(),
                location.getLatitude()
        );

        binding.mapView.getMapboxMap().setCamera(
                new CameraOptions.Builder()
                        .center(point)
                        .zoom(15.0)
                        .build()
        );

        showLocationDialog(location.getLatitude(), location.getLongitude());

        Toast.makeText(this, "Đã lấy vị trí", Toast.LENGTH_SHORT).show();
    }

    // ================= UI =================
    private void showLocationDialog(double lat, double lon) {
        String coordinates = String.format(Locale.getDefault(), "%f, %f", lat, lon);

        new AlertDialog.Builder(this)
                .setTitle("Vị trí hiện tại")
                .setMessage(coordinates)
                .setPositiveButton("Copy", (dialog, which) -> {
                    ClipboardManager clipboard =
                            (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    if (clipboard != null) {
                        clipboard.setPrimaryClip(
                                ClipData.newPlainText("Coordinates", coordinates)
                        );
                        Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Đóng", null)
                .show();
    }

    private void setupBottomSheet() {
        BottomSheetBehavior<View> behavior =
                BottomSheetBehavior.from(binding.bottomSheet);
        behavior.setPeekHeight(800);
    }

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v -> finish());
    }

    // ================= PERMISSION =================
    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                REQUEST_LOCATION_PERMISSION
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            startLocationUpdates();
        }
    }

    // ================= LIFECYCLE =================
    @Override
    protected void onStart() {
        super.onStart();
        binding.mapView.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        binding.mapView.onStop();
        stopLocationUpdates(); // tránh leak
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        binding.mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding.mapView.onDestroy();
    }
}