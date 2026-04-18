package com.example.bragbike.users;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.bragbike.R;
import com.example.bragbike.databinding.ActivityBookingBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.mapbox.api.geocoding.v5.MapboxGeocoding;
import com.mapbox.api.geocoding.v5.models.CarmenFeature;
import com.mapbox.api.geocoding.v5.models.GeocodingResponse;
import com.mapbox.geojson.Point;
import com.mapbox.maps.CameraOptions;
import com.mapbox.maps.Style;
import com.mapbox.maps.plugin.animation.CameraAnimationsUtils;
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin;
import com.mapbox.maps.plugin.locationcomponent.LocationComponentUtils;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BookingActivity extends AppCompatActivity {

    private ActivityBookingBinding binding;
    private static final int REQUEST_LOCATION_PERMISSION = 1;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private BottomSheetBehavior<View> bottomSheetBehavior;

    private Point originPoint;
    private Point destinationPoint;
    private boolean isBikeSelected = true;

    // Giả định giá từ Database (Bạn sẽ thay thế bằng API call thực tế)
    private double pricePerKmBike = 5000; 
    private double pricePerKmCar = 12000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBookingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        initMap();
        setupBottomSheet();
        setupClickListeners();
        setupSearchInput();
    }

    private void initMap() {
        binding.mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS, style -> {
            initLocationComponent();
            checkPermissionAndGetLocation();
        });
    }

    private void initLocationComponent() {
        LocationComponentPlugin locationComponentPlugin = LocationComponentUtils.getLocationComponent(binding.mapView);
        locationComponentPlugin.setEnabled(true);
    }

    private void setupSearchInput() {
        binding.etOrigin.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_ACTION_DONE) {
                searchLocation(v.getText().toString(), true);
                return true;
            }
            return false;
        });

        binding.etDestination.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                searchLocation(v.getText().toString(), false);
                hideKeyboard(v);
                return true;
            }
            return false;
        });
    }

    private void searchLocation(String query, boolean isOrigin) {
        if (query.isEmpty()) return;

        String token = getString(R.string.mapbox_access_token);
        MapboxGeocoding mapboxGeocoding = MapboxGeocoding.builder()
                .accessToken(token)
                .query(query)
                .country("VN")
                .limit(1)
                .build();

        mapboxGeocoding.enqueueCall(new Callback<GeocodingResponse>() {
            @Override
            public void onResponse(Call<GeocodingResponse> call, Response<GeocodingResponse> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().features().isEmpty()) {
                    CarmenFeature feature = response.body().features().get(0);
                    Point point = (Point) feature.geometry();
                    if (point != null) {
                        if (isOrigin) originPoint = point;
                        else destinationPoint = point;
                        
                        moveCameraToPoint(point);
                        checkAndShowServices();
                    }
                }
            }

            @Override
            public void onFailure(Call<GeocodingResponse> call, Throwable t) {}
        });
    }

    private void checkAndShowServices() {
        if (originPoint != null && destinationPoint != null) {
            calculateDistanceAndPrice();
            binding.layoutServiceContent.setVisibility(View.VISIBLE);
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            updateServiceSelectionUI();
        }
    }

    private void calculateDistanceAndPrice() {
        // Tính khoảng cách cơ bản (đường chim bay) - Đơn vị: KM
        // Note: Để chính xác hơn nên dùng Mapbox Directions API
        double distance = calculateDistanceInKm(originPoint, destinationPoint);
        
        // Giả sử lấy từ DB hoặc cấu hình
        long bikePrice = Math.round(distance * pricePerKmBike);
        long carPrice = Math.round(distance * pricePerKmCar);

        // Đảm bảo giá tối thiểu (ví dụ 10.000đ)
        if (bikePrice < 10000) bikePrice = 10000;
        if (carPrice < 25000) carPrice = 25000;

        binding.tvBikePrice.setText(String.format("%,dđ", bikePrice));
        binding.tvCarPrice.setText(String.format("%,dđ", carPrice));
        binding.tvBikeInfo.setText(String.format("Tiết kiệm • %.1f km", distance));
        binding.tvCarInfo.setText(String.format("Thoải mái • %.1f km", distance));
    }

    private double calculateDistanceInKm(Point p1, Point p2) {
        double lat1 = p1.latitude();
        double lon1 = p1.longitude();
        double lat2 = p2.latitude();
        double lon2 = p2.longitude();
        
        double theta = lon1 - lon2;
        double dist = Math.sin(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2))
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.cos(Math.toRadians(theta));
        dist = Math.acos(dist);
        dist = Math.toDegrees(dist);
        dist = dist * 60 * 1.1515 * 1.609344;
        return dist;
    }

    private void setupBottomSheet() {
        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet);
        bottomSheetBehavior.setHideable(true);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnSwap.setOnClickListener(v -> {
            String temp = binding.etOrigin.getText().toString();
            binding.etOrigin.setText(binding.etDestination.getText().toString());
            binding.etDestination.setText(temp);
            Point tempPoint = originPoint;
            originPoint = destinationPoint;
            destinationPoint = tempPoint;
            if (originPoint != null && destinationPoint != null) calculateDistanceAndPrice();
        });

        binding.optionBike.setOnClickListener(v -> { isBikeSelected = true; updateServiceSelectionUI(); });
        binding.optionCar.setOnClickListener(v -> { isBikeSelected = false; updateServiceSelectionUI(); });
        binding.btnConfirm.setOnClickListener(v -> Toast.makeText(this, "Đang đặt " + (isBikeSelected ? "BragBike" : "BragCar"), Toast.LENGTH_SHORT).show());
    }

    private void updateServiceSelectionUI() {
        int activeColor = ContextCompat.getColor(this, R.color.primary_light);
        int inactiveColor = ContextCompat.getColor(this, R.color.white);
        
        binding.optionBike.setCardBackgroundColor(isBikeSelected ? activeColor : inactiveColor);
        binding.optionBike.setStrokeWidth(isBikeSelected ? 2 : 0);
        
        binding.optionCar.setCardBackgroundColor(!isBikeSelected ? activeColor : inactiveColor);
        binding.optionCar.setStrokeWidth(!isBikeSelected ? 2 : 0);
        
        binding.btnConfirm.setText("Đặt " + (isBikeSelected ? "BragBike" : "BragCar"));
    }

    private void moveCameraToPoint(Point point) {
        CameraOptions options = new CameraOptions.Builder().center(point).zoom(15.0).build();
        CameraAnimationsUtils.flyTo(binding.mapView.getMapboxMap(), options, null, null);
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void checkPermissionAndGetLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    originPoint = Point.fromLngLat(location.getLongitude(), location.getLatitude());
                    binding.etOrigin.setText("Vị trí hiện tại");
                    moveCameraToPoint(originPoint);
                }
            });
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        }
    }

    @Override protected void onStart() { super.onStart(); binding.mapView.onStart(); }
    @Override protected void onStop() { super.onStop(); binding.mapView.onStop(); }
    @Override protected void onDestroy() { super.onDestroy(); binding.mapView.onDestroy(); }
}
