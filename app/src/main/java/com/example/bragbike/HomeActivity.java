package com.example.bragbike;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;

import com.bumptech.glide.Glide;
import com.example.bragbike.api.ApiService;
import com.example.bragbike.api.RetrofitClient;
import com.example.bragbike.model.Ride;
import com.example.bragbike.model.User;
import com.example.bragbike.socket.SocketManager;
import com.example.bragbike.users.BookingActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.materialswitch.MaterialSwitch;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.socket.client.Socket;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity {

    private TextView tvUserName;
    private ImageView ivAvatar;
    private CardView cvSearch;
    private View btnServiceCar, btnServiceBike;
    private BottomNavigationView bottomNav;

    // Driver Mode Views
    private CardView cvDriverToggle;
    private TextView tvDriverStatus;
    private MaterialSwitch switchDriverOnline;
    private View layoutUserContent, layoutDriverWaiting;
    
    private FusedLocationProviderClient fusedLocationClient;
    private Handler refreshHandler = new Handler();
    private Runnable refreshRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        initViews();
        setupBottomNavigation();
        
        String token = RetrofitClient.getInstance(this).getTokenManager().getToken();
        if (token != null) {
            SocketManager.connect(token);
            setupSocketListeners();
        }
    }

    private void initViews() {
        tvUserName = findViewById(R.id.tvUserName);
        ivAvatar = findViewById(R.id.ivAvatar);
        cvSearch = findViewById(R.id.cvSearch);
        btnServiceCar = findViewById(R.id.btnServiceCar);
        btnServiceBike = findViewById(R.id.btnServiceBike);
        bottomNav = findViewById(R.id.bottom_navigation);

        cvDriverToggle = findViewById(R.id.cvDriverToggle);
        tvDriverStatus = findViewById(R.id.tvDriverStatus);
        switchDriverOnline = findViewById(R.id.switchDriverOnline);
        layoutUserContent = findViewById(R.id.layoutUserContent);
        layoutDriverWaiting = findViewById(R.id.layoutDriverWaiting);

        if (switchDriverOnline != null) {
            switchDriverOnline.setOnClickListener(v -> {
                if (switchDriverOnline.isChecked()) startDriverMode();
                else stopDriverMode();
            });
        }

        View.OnClickListener startBooking = v -> startActivity(new Intent(HomeActivity.this, BookingActivity.class));
        if (cvSearch != null) cvSearch.setOnClickListener(startBooking);
        if (btnServiceCar != null) btnServiceCar.setOnClickListener(startBooking);
        if (btnServiceBike != null) btnServiceBike.setOnClickListener(startBooking);
    }

    private void startDriverMode() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1000);
            switchDriverOnline.setChecked(false);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                Log.d("DRIVER_LOG", "GPS hiện tại: " + location.getLatitude() + "," + location.getLongitude());
                updateLocationAndStatus(location.getLatitude(), location.getLongitude(), true);
            } else {
                Toast.makeText(this, "Vị trí không xác định. Hãy mở bản đồ!", Toast.LENGTH_SHORT).show();
                switchDriverOnline.setChecked(false);
            }
        });
    }

    private void stopDriverMode() {
        if (refreshHandler != null && refreshRunnable != null) refreshHandler.removeCallbacks(refreshRunnable);
        updateLocationAndStatus(0, 0, false);
    }

    private void updateLocationAndStatus(double lat, double lng, boolean online) {
        ApiService api = RetrofitClient.getInstance(this).getApiService();
        if (online) {
            Map<String, Object> body = new HashMap<>();
            body.put("lat", lat);
            body.put("lng", lng);
            body.put("latitude", lat);
            body.put("longitude", lng);

            api.updateLocation(body).enqueue(new Callback<Map<String, Object>>() {
                @Override
                public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                    Log.d("DRIVER_LOG", "Cập nhật GPS thành công.");
                    toggleOnlineStatusOnServer(true);
                }
                @Override
                public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                    switchDriverOnline.setChecked(false);
                }
            });
        } else {
            toggleOnlineStatusOnServer(false);
        }
    }

    private void toggleOnlineStatusOnServer(boolean online) {
        Map<String, Object> body = new HashMap<>();
        body.put("is_online", online);
        ApiService api = RetrofitClient.getInstance(this).getApiService();
        api.toggleOnline(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Log.d("DRIVER_LOG", "Driver Online: " + online);
                    updateDriverUI(online);
                    if (online) startAutoRefresh();
                } else {
                    switchDriverOnline.setChecked(!online);
                    Log.e("DRIVER_LOG", "Lỗi bật Online: " + response.code());
                }
            }
            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) { switchDriverOnline.setChecked(!online); }
        });
    }

    private void startAutoRefresh() {
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                fetchAvailableRides();
                refreshHandler.postDelayed(this, 7000); 
            }
        };
        refreshHandler.post(refreshRunnable);
    }

    private void fetchAvailableRides() {
        ApiService api = RetrofitClient.getInstance(this).getApiService();
        api.getAvailableRides().enqueue(new Callback<List<Ride>>() {
            @Override
            public void onResponse(Call<List<Ride>> call, Response<List<Ride>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Ride> rides = response.body();
                    Log.d("DRIVER_LOG", "Số cuốc tìm thấy: " + rides.size());
                    if (!rides.isEmpty()) {
                        openRideRequestPage(rides.get(0));
                    }
                } else if (response.code() == 403) {
                    Log.e("DRIVER_LOG", "LỖI 403: Bạn không có quyền lấy cuốc xe. Hãy đăng nhập lại!");
                }
            }
            @Override
            public void onFailure(Call<List<Ride>> call, Throwable t) {
                Log.e("DRIVER_LOG", "Lỗi kết nối khi quét chuyến", t);
            }
        });
    }

    private void openRideRequestPage(Ride ride) {
        Intent intent = new Intent(this, RideRequestActivity.class);
        intent.putExtra(RideRequestActivity.EXTRA_RIDE_ID, ride.getId());
        intent.putExtra(RideRequestActivity.EXTRA_FARE, ride.getFare());
        intent.putExtra(RideRequestActivity.EXTRA_PICKUP, ride.getPickupAddress());
        intent.putExtra(RideRequestActivity.EXTRA_DROPOFF, ride.getDropoffAddress());
        intent.putExtra(RideRequestActivity.EXTRA_VEHICLE, ride.getVehicleType());
        startActivity(intent);
        
        if (refreshHandler != null) refreshHandler.removeCallbacks(refreshRunnable);
    }

    private void updateDriverUI(boolean online) {
        if (online) {
            tvDriverStatus.setText("Đang trực tuyến");
            tvDriverStatus.setTextColor(getResources().getColor(R.color.primary));
            layoutUserContent.setVisibility(View.GONE);
            layoutDriverWaiting.setVisibility(View.VISIBLE);
        } else {
            tvDriverStatus.setText("Đang ngoại tuyến");
            tvDriverStatus.setTextColor(getResources().getColor(R.color.slate_500));
            layoutUserContent.setVisibility(View.VISIBLE);
            layoutDriverWaiting.setVisibility(View.GONE);
        }
    }

    private void setupSocketListeners() {
        Socket socket = SocketManager.getSocket(null);
        socket.on("new_ride_request", args -> {
            runOnUiThread(() -> {
                try {
                    JSONObject data = (JSONObject) args[0];
                    Ride ride = new Ride();
                    ride.setId(data.getInt("id"));
                    ride.setFare(data.optDouble("total_price", 0));
                    ride.setPickupAddress(data.optString("pickup_address", "N/A"));
                    ride.setDropoffAddress(data.optString("drop_address", "N/A"));
                    ride.setVehicleType(data.optString("vehicle_type", "MOTORBIKE"));
                    openRideRequestPage(ride);
                } catch (Exception e) { Log.e("SOCKET", "Error", e); }
            });
        });
    }

    private void setupBottomNavigation() {
        bottomNav.setSelectedItemId(R.id.nav_home);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) return true;
            if (id == R.id.nav_history) { startActivity(new Intent(this, ActivityHistoryActivity.class)); return true; }
            if (id == R.id.nav_profile) { startActivity(new Intent(this, ProfileActivity.class)); return true; }
            return false;
        });
    }

    @Override
    protected void onResume() { super.onResume(); loadUserProfile(); }

    @Override
    protected void onPause() {
        super.onPause();
        if (refreshHandler != null && refreshRunnable != null) refreshHandler.removeCallbacks(refreshRunnable);
    }

    private void loadUserProfile() {
        ApiService api = RetrofitClient.getInstance(this).getApiService();
        api.getMe().enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    User user = response.body();
                    tvUserName.setText("Chào, " + user.getFullName() + "!");
                    Log.d("DRIVER_LOG", "Quyền hiện tại trong máy: " + user.getRole());
                    
                    if (cvDriverToggle != null) {
                        cvDriverToggle.setVisibility("DRIVER".equals(user.getRole()) ? View.VISIBLE : View.GONE);
                    }

                    String avatarUrl = user.getAvatarUrl();
                    if (avatarUrl != null && !avatarUrl.isEmpty()) {
                        if (avatarUrl.contains("dicebear.com") && avatarUrl.contains("/svg")) avatarUrl = avatarUrl.replace("/svg", "/png");
                        String fullUrl = avatarUrl.startsWith("http") ? avatarUrl : BuildConfig.BASE_URL + avatarUrl;
                        Glide.with(HomeActivity.this).load(fullUrl).circleCrop().into(ivAvatar);
                    }
                }
            }
            @Override
            public void onFailure(Call<User> call, Throwable t) { }
        });
    }
}
