package com.example.bragbike;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.bragbike.api.ApiService;
import com.example.bragbike.api.RetrofitClient;
import com.example.bragbike.auth.LoginActivity;
import com.example.bragbike.model.User;
import com.example.bragbike.utils.TokenManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvUserName, tvUserPhone, tvTripCount, tvRating;
    private ImageView ivAvatar;
    private LinearLayout layoutDriverStats, btnLogout, btnBecomeDriver, btnEditProfile;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        initViews();
        setupListeners();
        setupBottomNavigation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserProfile();
    }

    private void initViews() {
        tvUserName = findViewById(R.id.tvUserName);
        tvUserPhone = findViewById(R.id.tvUserPhone);
        tvTripCount = findViewById(R.id.tvTripCount);
        tvRating = findViewById(R.id.tvRating);
        ivAvatar = findViewById(R.id.ivAvatar);
        layoutDriverStats = findViewById(R.id.layoutDriverStats);
        
        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnBecomeDriver = findViewById(R.id.btnBecomeDriver);
        btnLogout = findViewById(R.id.btnLogout);
        
        bottomNav = findViewById(R.id.bottom_navigation);
    }

    private void setupListeners() {
        btnLogout.setOnClickListener(v -> {
            TokenManager tokenManager = RetrofitClient.getInstance(this).getTokenManager();
            tokenManager.clearToken();
            
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        btnBecomeDriver.setOnClickListener(v -> {
            Toast.makeText(this, "Tính năng đang phát triển", Toast.LENGTH_SHORT).show();
        });

        btnEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, EditProfileActivity.class);
            startActivity(intent);
        });
    }

    private void setupBottomNavigation() {
        bottomNav.setSelectedItemId(R.id.nav_profile);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                // Tùy theo Activity chính của bạn là HomeActivity hay HomeUserActivity
                // Tôi sẽ dùng HomeActivity theo code cũ của bạn
                Intent intent = new Intent(this, HomeActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
                return true;
            } else if (id == R.id.nav_history) {
                startActivity(new Intent(this, ActivityHistoryActivity.class));
                finish();
                return true;
            }
            return id == R.id.nav_profile;
        });
    }

    private void loadUserProfile() {
        ApiService api = RetrofitClient.getInstance(this).getApiService();
        api.getMe().enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    updateUI(response.body());
                }
            }
            @Override
            public void onFailure(Call<User> call, Throwable t) {
                Log.e("PROFILE_ACT", "Error loading profile", t);
            }
        });
    }

    private void updateUI(User user) {
        tvUserName.setText(user.getFullName() != null ? user.getFullName() : "N/A");
        tvUserPhone.setText(user.getPhone() != null ? user.getPhone() : "N/A");

        // Tải ảnh từ URL bằng Glide và xử lý logic tương tự Home
        String avatarUrl = user.getAvatarUrl();
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            
            // Fix SVG DiceBear tương tự trang Home
            if (avatarUrl.contains("dicebear.com") && avatarUrl.contains("/svg")) {
                avatarUrl = avatarUrl.replace("/svg", "/png");
            }

            String fullUrl = avatarUrl.startsWith("http") ? avatarUrl : BuildConfig.BASE_URL + avatarUrl;

            Glide.with(this)
                    .load(fullUrl)
                    .placeholder(R.drawable.bg_badge_pro) // Dùng placeholder khác nếu cần
                    .error(R.drawable.bg_badge_pro)
                    .circleCrop()
                    .into(ivAvatar);
        }

        if ("DRIVER".equals(user.getRole())) {
            layoutDriverStats.setVisibility(View.VISIBLE);
            btnBecomeDriver.setVisibility(View.GONE);
            loadDriverStats();
        } else {
            layoutDriverStats.setVisibility(View.GONE);
            btnBecomeDriver.setVisibility(View.VISIBLE);
        }
    }

    private void loadDriverStats() {
        ApiService api = RetrofitClient.getInstance(this).getApiService();
        api.getDriverStats().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> stats = response.body();
                    try {
                        Object tripsObj = stats.get("total_trips");
                        if (tripsObj != null) {
                            double trips = Double.parseDouble(tripsObj.toString());
                            tvTripCount.setText(String.valueOf((int) trips));
                        }

                        Object ratingObj = stats.get("avg_rating");
                        if (ratingObj != null) {
                            double rating = Double.parseDouble(ratingObj.toString());
                            tvRating.setText(String.format("%.1f", rating));
                        }
                    } catch (Exception e) {
                        Log.e("PROFILE_ACT", "Error parsing stats", e);
                    }
                }
            }
            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Log.e("PROFILE_ACT", "Error loading driver stats", t);
            }
        });
    }
}
