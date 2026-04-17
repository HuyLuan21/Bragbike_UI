package com.example.bragbike;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.example.bragbike.databinding.ActivityBookingBinding;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.mapbox.geojson.Point;
import com.mapbox.maps.CameraOptions;
import com.mapbox.maps.Style;
import com.mapbox.maps.plugin.gestures.GesturesUtils;
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin;
import com.mapbox.maps.plugin.locationcomponent.LocationComponentUtils;
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener;

public class BookingActivity extends AppCompatActivity {

    private ActivityBookingBinding binding;
    private static final int REQUEST_LOCATION_PERMISSION = 1;

    // Tọa độ trung tâm Việt Nam để khởi đầu
    private final Point VIETNAM_CENTER = Point.fromLngLat(108.2022, 16.0544);
    
    // Tọa độ Google HQ (Mặc định máy ảo)
    private final double GOOGLE_LAT = 37.422;
    private final double GOOGLE_LON = -122.084;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBookingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 1. Khởi động Camera tại Việt Nam
        binding.mapView.getMapboxMap().setCamera(new CameraOptions.Builder()
                .center(VIETNAM_CENTER)
                .zoom(6.0)
                .build());

        // 2. Load Style và thiết lập Vị trí
        binding.mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS, style -> {
            if (checkLocationPermission()) {
                initLocationComponent();
            } else {
                requestLocationPermission();
            }
        });

        setupBottomSheet();
        setupClickListeners();
    }

    private void setupBottomSheet() {
        BottomSheetBehavior<View> bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet);
        bottomSheetBehavior.setPeekHeight(800);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v -> finish());

        // Xử lý khi nhấn vào ô tìm kiếm "Bạn muốn đi đâu"
        binding.etDestination.setOnClickListener(v -> {
            Toast.makeText(this, "Tính năng tìm kiếm địa điểm đang được cập nhật...", Toast.LENGTH_SHORT).show();
        });

        binding.optionEconomy.setOnClickListener(v -> {
            binding.optionEconomy.setCardBackgroundColor(getResources().getColor(R.color.primary_light, getTheme()));
            binding.optionComfort.setCardBackgroundColor(getResources().getColor(R.color.white, getTheme()));
            binding.btnConfirm.setText("Xác nhận Đặt xe Tiết kiệm");
        });

        binding.optionComfort.setOnClickListener(v -> {
            binding.optionComfort.setCardBackgroundColor(getResources().getColor(R.color.primary_light, getTheme()));
            binding.optionEconomy.setCardBackgroundColor(getResources().getColor(R.color.white, getTheme()));
            binding.btnConfirm.setText("Xác nhận Đặt xe Thoải mái");
        });

        binding.btnConfirm.setOnClickListener(v -> {
            Toast.makeText(this, "Đang kết nối với tài xế BragBike gần nhất...", Toast.LENGTH_SHORT).show();
        });
    }

    private boolean checkLocationPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
    }

    private void initLocationComponent() {
        LocationComponentPlugin locationComponentPlugin = LocationComponentUtils.getLocationComponent(binding.mapView);

        // Kích hoạt hiển thị chấm xanh (Puck)
        locationComponentPlugin.setEnabled(true);
        locationComponentPlugin.setPuckBearingEnabled(true); // Hiển thị hướng nhìn
        
        // Lắng nghe thay đổi vị trí
        locationComponentPlugin.addOnIndicatorPositionChangedListener(new OnIndicatorPositionChangedListener() {
            @Override
            public void onIndicatorPositionChanged(@NonNull Point point) {
                // Bỏ qua nếu là vị trí mặc định tại Mỹ của máy ảo
                if (Math.abs(point.latitude() - GOOGLE_LAT) < 0.1 && Math.abs(point.longitude() - GOOGLE_LON) < 0.1) {
                    return;
                }

                // Khi đã có vị trí ở Việt Nam:
                // 1. Bay camera đến đó
                binding.mapView.getMapboxMap().setCamera(new CameraOptions.Builder()
                        .center(point)
                        .zoom(16.0)
                        .build());
                
                // 2. Gỡ lắng nghe để không làm phiền khi người dùng tự lướt bản đồ
                locationComponentPlugin.removeOnIndicatorPositionChangedListener(this);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initLocationComponent();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        binding.mapView.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        binding.mapView.onStop();
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