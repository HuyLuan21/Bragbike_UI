package com.example.bragbike.users;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;
import com.example.bragbike.ActivityHistoryActivity;
import com.example.bragbike.BuildConfig;
import com.example.bragbike.ProfileActivity;
import com.example.bragbike.R;
import com.example.bragbike.api.ApiService;
import com.example.bragbike.api.RetrofitClient;
import com.example.bragbike.model.User;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeUserActivity extends AppCompatActivity {

    private TextView tvUserName;
    private ImageView ivAvatar;
    private CardView cvSearch;
    private View btnServiceCar, btnServiceBike;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        tvUserName = findViewById(R.id.tvUserName);
        ivAvatar = findViewById(R.id.ivAvatar);
        cvSearch = findViewById(R.id.cvSearch);
        btnServiceCar = findViewById(R.id.btnServiceCar);
        btnServiceBike = findViewById(R.id.btnServiceBike);
        bottomNav = findViewById(R.id.bottom_navigation);

        // Hiển thị tên từ bộ nhớ tạm
        String savedName = getSharedPreferences("bragbike_prefs", MODE_PRIVATE)
                .getString("user_name", "Người dùng");
        tvUserName.setText("Chào, " + savedName + "!");

        // Điều hướng đặt xe
        View.OnClickListener startBooking = v -> {
            Intent intent = new Intent(HomeUserActivity.this, BookingActivity.class);
            startActivity(intent);
        };
        cvSearch.setOnClickListener(startBooking);
        btnServiceCar.setOnClickListener(startBooking);
        btnServiceBike.setOnClickListener(startBooking);

        // Xử lý Bottom Navigation
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_history) { 
                Intent intent = new Intent(HomeUserActivity.this, ActivityHistoryActivity.class);
                startActivity(intent);
                return true;
            } else if (id == R.id.nav_profile) {
                Intent intent = new Intent(HomeUserActivity.this, ProfileActivity.class);
                startActivity(intent);
                return true;
            }
            return id == R.id.nav_home;
        });

        loadUserProfile();
    }

    private void loadUserProfile() {
        ApiService api = RetrofitClient.getInstance(this).getApiService();
        api.getMe().enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    User user = response.body();
                    String fullName = user.getFullName();
                    if (fullName != null) {
                        tvUserName.setText("Chào, " + fullName + "!");
                        getSharedPreferences("bragbike_prefs", MODE_PRIVATE)
                                .edit().putString("user_name", fullName).apply();
                    }

                    // Tải ảnh avatar
                    String avatarUrl = user.getAvatarUrl();
                    if (avatarUrl != null && !avatarUrl.isEmpty()) {
                        
                        // FIX: Chuyển SVG sang PNG cho DiceBear vì Glide không hỗ trợ SVG mặc định
                        if (avatarUrl.contains("dicebear.com") && avatarUrl.contains("/svg")) {
                            avatarUrl = avatarUrl.replace("/svg", "/png");
                        }

                        String fullUrl = avatarUrl.startsWith("http") ? avatarUrl : BuildConfig.BASE_URL + avatarUrl;
                        
                        Glide.with(HomeUserActivity.this)
                                .load(fullUrl)
                                .placeholder(R.drawable.ic_launcher_foreground)
                                .error(R.drawable.ic_launcher_foreground)
                                .circleCrop()
                                .into(ivAvatar);
                    }
                }
            }
            @Override
            public void onFailure(Call<User> call, Throwable t) {
                Log.e("HOME_USER_ACT", "Error", t);
            }
        });
    }
}
