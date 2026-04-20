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

import java.util.ArrayList;
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
    
    private List<Integer> ignoredRideIds = new ArrayList<>();
    private List<Ride> currentAvailableRides = new ArrayList<>();
    private boolean isShowingRequest = false;
    private boolean isServerOnline = false;

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
            switchDriverOnline.setOnClickListener(v -> handleDriverToggleClick());
        }

        View.OnClickListener startBooking = v -> {
            Intent intent = new Intent(HomeActivity.this, BookingActivity.class);
            startActivity(intent);
        };
        if (cvSearch != null) cvSearch.setOnClickListener(startBooking);
        if (btnServiceCar != null) btnServiceCar.setOnClickListener(startBooking);
        if (btnServiceBike != null) btnServiceBike.setOnClickListener(startBooking);
    }

    private void handleDriverToggleClick() {
        boolean isChecked = switchDriverOnline.isChecked();
        if (isChecked) {
            startDriverMode();
        } else {
            stopDriverMode();
        }
    }

    private void startDriverMode() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1000);
            switchDriverOnline.setChecked(false);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                updateLocationAndStatus(location.getLatitude(), location.getLongitude(), true);
            } else {
                Toast.makeText(this, "Vị trí không xác định. Hãy mở bản đồ!", Toast.LENGTH_SHORT).show();
                switchDriverOnline.setChecked(false);
            }
        });
    }

    private void stopDriverMode() {
        isShowingRequest = false;
        stopAutoRefresh();
        updateLocationAndStatus(0, 0, false);
    }

    private void updateLocationAndStatus(double lat, double lng, boolean targetOnline) {
        ApiService api = RetrofitClient.getInstance(this).getApiService();
        if (targetOnline) {
            Map<String, Object> body = new HashMap<>();
            body.put("lat", lat);
            body.put("lng", lng);
            api.updateLocation(body).enqueue(new Callback<Map<String, Object>>() {
                @Override
                public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                    syncAndToggleOnline(true);
                }
                @Override
                public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                    switchDriverOnline.setChecked(false);
                }
            });
        } else {
            syncAndToggleOnline(false);
        }
    }

    private void syncAndToggleOnline(boolean targetOnline) {
        ApiService api = RetrofitClient.getInstance(this).getApiService();
        api.getDriverStats().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Object onlineObj = response.body().get("is_online");
                    isServerOnline = false;
                    if (onlineObj instanceof Boolean) isServerOnline = (Boolean) onlineObj;
                    else if (onlineObj instanceof Number) isServerOnline = ((Number) onlineObj).intValue() == 1;

                    if (isServerOnline != targetOnline) {
                        api.toggleOnline(new HashMap<>()).enqueue(new Callback<Map<String, Object>>() {
                            @Override
                            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                                if (response.isSuccessful() && response.body() != null) {
                                    updateDriverUI(targetOnline);
                                    if (targetOnline) {
                                        refreshHandler.postDelayed(() -> fetchAvailableRides(), 2000);
                                        startAutoRefresh();
                                    }
                                }
                            }
                            @Override
                            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                                switchDriverOnline.setChecked(isServerOnline);
                            }
                        });
                    } else {
                        updateDriverUI(targetOnline);
                        if (targetOnline) startAutoRefresh();
                    }
                }
            }
            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });
    }

    private void startAutoRefresh() {
        stopAutoRefresh();
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                if (switchDriverOnline.isChecked() && !isShowingRequest) {
                    fetchAvailableRides();
                    refreshHandler.postDelayed(this, 10000); 
                }
            }
        };
        refreshHandler.postDelayed(refreshRunnable, 10000);
    }

    private void stopAutoRefresh() {
        if (refreshRunnable != null) {
            refreshHandler.removeCallbacks(refreshRunnable);
            refreshRunnable = null;
        }
    }

    private void fetchAvailableRides() {
        if (isShowingRequest || !switchDriverOnline.isChecked()) return;

        ApiService api = RetrofitClient.getInstance(this).getApiService();
        api.getAvailableRides().enqueue(new Callback<List<Ride>>() {
            @Override
            public void onResponse(Call<List<Ride>> call, Response<List<Ride>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentAvailableRides = response.body();
                    showNextAvailableRide();
                }
            }
            @Override
            public void onFailure(Call<List<Ride>> call, Throwable t) {}
        });
    }

    private void showNextAvailableRide() {
        if (isShowingRequest || currentAvailableRides.isEmpty() || !switchDriverOnline.isChecked()) return;
        Ride nextRide = null;
        for (Ride r : currentAvailableRides) {
            if (!ignoredRideIds.contains(r.getId())) {
                nextRide = r;
                break;
            }
        }
        if (nextRide != null) openRideRequestPage(nextRide);
    }

    private void openRideRequestPage(Ride ride) {
        if (isShowingRequest) return;
        isShowingRequest = true;
        Intent intent = new Intent(this, RideRequestActivity.class);
        intent.putExtra(RideRequestActivity.EXTRA_RIDE_ID, ride.getId());
        intent.putExtra(RideRequestActivity.EXTRA_FARE, ride.getFare());
        intent.putExtra(RideRequestActivity.EXTRA_PICKUP, ride.getPickupAddress());
        intent.putExtra(RideRequestActivity.EXTRA_DROPOFF, ride.getDropoffAddress());
        intent.putExtra(RideRequestActivity.EXTRA_VEHICLE, ride.getVehicleType());
        startActivityForResult(intent, 2000);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        isShowingRequest = false;
        if (requestCode == 2000) {
            if (resultCode == RESULT_CANCELED && data != null) {
                int declinedId = data.getIntExtra("declined_ride_id", -1);
                if (declinedId != -1) {
                    ignoredRideIds.add(declinedId);
                    showNextAvailableRide(); 
                }
            }
            if (switchDriverOnline.isChecked()) startAutoRefresh();
        }
    }

    private void updateDriverUI(boolean online) {
        if (switchDriverOnline != null) switchDriverOnline.setChecked(online);
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
                    
                    if (!ignoredRideIds.contains(ride.getId()) && !isShowingRequest && switchDriverOnline.isChecked()) {
                        currentAvailableRides.add(0, ride);
                        showNextAvailableRide();
                    }
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
    protected void onResume() { 
        super.onResume(); 
        loadUserProfile(); 
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopAutoRefresh();
    }

    private void loadUserProfile() {
        ApiService api = RetrofitClient.getInstance(this).getApiService();
        api.getMe().enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    User user = response.body();
                    tvUserName.setText("Chào, " + user.getFullName() + "!");
                    
                    // Fix lỗi mất Avatar
                    String avatarUrl = user.getAvatarUrl();
                    if (avatarUrl != null && !avatarUrl.isEmpty()) {
                        if (avatarUrl.contains("dicebear.com") && avatarUrl.contains("/svg")) {
                            avatarUrl = avatarUrl.replace("/svg", "/png");
                        }
                        String fullUrl = avatarUrl.startsWith("http") ? avatarUrl : BuildConfig.BASE_URL + avatarUrl;
                        Glide.with(HomeActivity.this)
                                .load(fullUrl)
                                .placeholder(R.drawable.bg_badge_pro)
                                .circleCrop()
                                .into(ivAvatar);
                    }

                    if (cvDriverToggle != null) {
                        boolean isDriver = "DRIVER".equals(user.getRole());
                        cvDriverToggle.setVisibility(isDriver ? View.VISIBLE : View.GONE);
                        // Chỉ đồng bộ trạng thái nút, KHÔNG tự động bật quét chuyến
                        if (isDriver) syncDriverStatusUIOnly(); 
                    }
                }
            }
            @Override
            public void onFailure(Call<User> call, Throwable t) { }
        });
    }

    private void syncDriverStatusUIOnly() {
        ApiService api = RetrofitClient.getInstance(this).getApiService();
        api.getDriverStats().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Object onlineObj = response.body().get("is_online");
                    boolean isOnline = false;
                    if (onlineObj instanceof Boolean) isOnline = (Boolean) onlineObj;
                    else if (onlineObj instanceof Number) isOnline = ((Number) onlineObj).intValue() == 1;
                    
                    // Cập nhật UI nhưng không tự động bật loop nếu người dùng vừa vào trang Home
                    if (switchDriverOnline != null) switchDriverOnline.setChecked(isOnline);
                    updateDriverUI(isOnline);
                }
            }
            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });
    }
}
