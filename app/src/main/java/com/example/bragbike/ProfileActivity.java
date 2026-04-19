package com.example.bragbike;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bragbike.api.ApiService;
import com.example.bragbike.api.RetrofitClient;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvUserName;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        tvUserName = findViewById(R.id.tvUserName);
        bottomNav = findViewById(R.id.bottom_navigation);

        // Highlight Profile Tab
        bottomNav.setSelectedItemId(R.id.nav_profile);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                Intent intent = new Intent(ProfileActivity.this, HomeActivity.class);
                startActivity(intent);
                finish();
                return true;
            } else if (id == R.id.nav_history) {
                Intent intent = new Intent(ProfileActivity.this, ActivityHistoryActivity.class);
                startActivity(intent);
                finish();
                return true;
            } else if (id == R.id.nav_profile) {
                return true;
            }
            return false;
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
                        tvUserName.setText(fullName);
                    }
                }
            }
            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Log.e("PROFILE_ACT", "Error", t);
            }
        });
    }

    private String extractFullName(Map<String, Object> body) {
        if (body.containsKey("full_name")) return (String) body.get("full_name");
        if (body.get("user") instanceof Map) return (String) ((Map<?, ?>) body.get("user")).get("full_name");
        return null;
    }
}