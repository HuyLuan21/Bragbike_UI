package com.example.bragbike.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.bragbike.R;
import com.example.bragbike.api.ApiService;
import com.example.bragbike.api.RetrofitClient;
import com.example.bragbike.model.LoginRequest;
import com.example.bragbike.model.LoginResponse;
import com.example.bragbike.users.HomeUserActivity;
import com.example.bragbike.utils.TokenManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmailPhone, etPassword;
    private MaterialButton btnLogin;
    private TextView tvSignUp, tvForgotPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etEmailPhone = findViewById(R.id.etEmailPhone);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvSignUp = findViewById(R.id.tvSignUp);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        btnLogin.setOnClickListener(v -> {
            String identifier = etEmailPhone.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (identifier.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            } else {
                login(identifier, password);
            }
        });

        tvSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        tvForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });
    }

    private void login(String identifier, String password) {
        ApiService api = RetrofitClient.getInstance(this).getApiService();
        TokenManager tokenManager = RetrofitClient.getInstance(this).getTokenManager();

        btnLogin.setEnabled(false);
        btnLogin.setText("Đang đăng nhập...");

        api.login(new LoginRequest(identifier, password)).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                btnLogin.setEnabled(true);
                btnLogin.setText("Đăng nhập");

                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse data = response.body();
                    
                    // 1. Lưu Token
                    tokenManager.saveToken(data.getToken());

                    // 2. LƯU TÊN FULL NAME VÀO PREFERENCES ĐỂ SỬ DỤNG
                    String fullName = data.getUser().getFullName();
                    getSharedPreferences("bragbike_prefs", MODE_PRIVATE)
                            .edit()
                            .putString("user_name", fullName)
                            .apply();

                    Toast.makeText(LoginActivity.this, "Chào mừng " + fullName, Toast.LENGTH_LONG).show();
                    
                    // 3. Chuyển màn hình
                    Intent intent = new Intent(LoginActivity.this, HomeUserActivity.class);
                    startActivity(intent);
                    finish();

                } else {
                    Toast.makeText(LoginActivity.this, "Sai tài khoản hoặc mật khẩu", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                btnLogin.setEnabled(true);
                btnLogin.setText("Đăng nhập");
                Toast.makeText(LoginActivity.this, "Lỗi kết nối mạng", Toast.LENGTH_SHORT).show();
            }
        });
    }
}