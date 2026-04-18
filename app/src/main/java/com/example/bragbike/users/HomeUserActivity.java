package com.example.bragbike.users;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.bragbike.ActivityHistoryActivity;
import com.example.bragbike.R;
import com.example.bragbike.api.ApiService;
import com.example.bragbike.api.RetrofitClient;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeUserActivity extends AppCompatActivity {

    private TextView tvUserName;
    private CardView cvSearch;
    private View btnServiceCar, btnServiceBike;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        tvUserName = findViewById(R.id.tvUserName);
        cvSearch = findViewById(R.id.cvSearch);
        btnServiceCar = findViewById(R.id.btnServiceCar);
        btnServiceBike = findViewById(R.id.btnServiceBike);
        bottomNav = findViewById(R.id.bottom_navigation);

        // 1. Hiển thị tên từ bộ nhớ tạm ngay lập tức
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

        // 2. XỬ LÝ BOTTOM NAVIGATION ĐỂ MỞ TRANG HOẠT ĐỘNG
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_history) { // ID từ bottom_nav_menu.xml
                Intent intent = new Intent(HomeUserActivity.this, ActivityHistoryActivity.class);
                startActivity(intent);
                return true;
            }
            return id == R.id.nav_home;
        });

        loadUserProfile();
    }

    private void loadUserProfile() {
        ApiService api = RetrofitClient.getInstance(this).getApiService();
        api.getMe().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> body = response.body();
                    String fullName = extractFullName(body);
                    if (fullName != null) {
                        tvUserName.setText("Chào, " + fullName + "!");
                        getSharedPreferences("bragbike_prefs", MODE_PRIVATE)
                                .edit().putString("user_name", fullName).apply();
                    }
                }
            }
            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Log.e("HOME_USER_ACT", "Error", t);
            }
        });
    }

    private String extractFullName(Map<String, Object> body) {
        if (body.containsKey("full_name")) return (String) body.get("full_name");
        if (body.get("user") instanceof Map) {
            Map<?, ?> user = (Map<?, ?>) body.get("user");
            return (String) user.get("full_name");
        }
        return null;
    }
}