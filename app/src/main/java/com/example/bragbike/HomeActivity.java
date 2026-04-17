package com.example.bragbike;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import android.view.View;

import com.example.bragbike.api.ApiService;
import com.example.bragbike.api.RetrofitClient;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity {

    private TextView tvUserName;
    private CardView cvSearch;
    private View btnServiceCar, btnServiceBike;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        tvUserName = findViewById(R.id.tvUserName);
        cvSearch = findViewById(R.id.cvSearch);
        btnServiceCar = findViewById(R.id.btnServiceCar);
        btnServiceBike = findViewById(R.id.btnServiceBike);

        // Chuyển sang màn hình đặt xe khi nhấn vào tìm kiếm hoặc dịch vụ
        View.OnClickListener startBooking = v -> {
            Intent intent = new Intent(HomeActivity.this, BookingActivity.class);
            startActivity(intent);
        };

        cvSearch.setOnClickListener(startBooking);
        btnServiceCar.setOnClickListener(startBooking);
        btnServiceBike.setOnClickListener(startBooking);

        loadUserProfile();
    }

    private void loadUserProfile() {
        ApiService api = RetrofitClient.getInstance(this).getApiService();

        api.getMyDriverProfile().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> body = response.body();
                    if (body.containsKey("user")) {
                        Object userObj = body.get("user");
                        if (userObj instanceof Map) {
                            Map<String, Object> userMap = (Map<String, Object>) userObj;
                            String fullName = (String) userMap.get("full_name");
                            if (fullName != null) {
                                tvUserName.setText(fullName);
                                return;
                            }
                        }
                    }
                    if (body.containsKey("full_name")) {
                        tvUserName.setText((String) body.get("full_name"));
                    } else {
                        tvUserName.setText("Admin BragBike");
                    }
                } else {
                    tvUserName.setText("Admin BragBike");
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                tvUserName.setText("Lỗi kết nối");
            }
        });
    }
}