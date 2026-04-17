package com.example.bragbike;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.bragbike.api.ApiService;
import com.example.bragbike.api.RetrofitClient;
import com.example.bragbike.model.LoginRequest;
import com.example.bragbike.model.LoginResponse;
import com.example.bragbike.utils.TokenManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private TextInputEditText etEmailPhone, etPassword;
    private MaterialButton btnLogin;
    private TextView tvSignUp, tvForgotPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Ánh xạ View từ XML
        etEmailPhone = findViewById(R.id.etEmailPhone);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvSignUp = findViewById(R.id.tvSignUp);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        // Lắng nghe sự kiện nhấn nút Login
        btnLogin.setOnClickListener(v -> {
            String identifier = etEmailPhone.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (identifier.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            } else {
                login(identifier, password);
            }
        });

        // Chuyển sang màn hình Đăng ký
        tvSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        // Chuyển sang màn hình Quên mật khẩu
        tvForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });
    }

    private void login(String identifier, String password) {
        ApiService api = RetrofitClient.getInstance(this).getApiService();
        TokenManager tokenManager = RetrofitClient.getInstance(this).getTokenManager();

        // Hiển thị trạng thái đang load
        btnLogin.setEnabled(false);
        btnLogin.setText("Đang đăng nhập...");

        api.login(new LoginRequest(identifier, password)).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                btnLogin.setEnabled(true);
                btnLogin.setText("Đăng nhập");

                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse data = response.body();
                    tokenManager.saveToken(data.getToken());

                    String name = data.getUser().getFullName();
                    Toast.makeText(MainActivity.this, "Chào mừng " + name, Toast.LENGTH_LONG).show();
                    Log.d("LOGIN_TEST", "Thành công! Token: " + data.getToken());

                    // Đăng nhập thành công, chuyển sang HomeActivity
                    Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                    startActivity(intent);
                    finish(); // Đóng màn hình đăng nhập

                } else {
                    Toast.makeText(MainActivity.this, "Đăng nhập thất bại (Lỗi " + response.code() + ")", Toast.LENGTH_SHORT).show();
                    Log.e("LOGIN_TEST", "Lỗi: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                btnLogin.setEnabled(true);
                btnLogin.setText("Đăng nhập");

                Toast.makeText(MainActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("LOGIN_TEST", "Failure: " + t.getMessage());
            }
        });
    }
}
