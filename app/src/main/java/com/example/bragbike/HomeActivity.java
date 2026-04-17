package com.example.bragbike;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.bragbike.api.ApiService;
import com.example.bragbike.api.RetrofitClient;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity {

    private TextView tvUserName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        tvUserName = findViewById(R.id.tvUserName);

        loadUserProfile();
    }

    private void loadUserProfile() {
        ApiService api = RetrofitClient.getInstance(this).getApiService();

        // Dựa trên logic backend bạn cung cấp: Driver include User
        api.getMyDriverProfile().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> body = response.body();
                    Log.d("HOME_API", "Response JSON: " + body.toString());

                    // Bóc tách theo cấu trúc: body (driver) -> user -> full_name
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
                    
                    // Nếu login trả về name trực tiếp ở root
                    if (body.containsKey("full_name")) {
                        tvUserName.setText((String) body.get("full_name"));
                    } else {
                        tvUserName.setText("Admin BragBike");
                    }
                } else {
                    Log.e("HOME_API", "Lỗi lấy Profile (Code " + response.code() + ")");
                    // Nếu lỗi 404 (chưa có hồ sơ tài xế), hiện tên mặc định hoặc gọi API khác
                    tvUserName.setText("Admin BragBike");
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Log.e("HOME_API", "Failure: " + t.getMessage());
                tvUserName.setText("Lỗi kết nối");
            }
        });
    }
}