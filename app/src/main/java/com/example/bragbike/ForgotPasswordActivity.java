package com.example.bragbike;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bragbike.api.ApiService;
import com.example.bragbike.api.RetrofitClient;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ForgotPasswordActivity extends AppCompatActivity {

    private LinearLayout layoutStep1, layoutStep2;
    private TextInputEditText etForgotPhone, etOtp, etNewPassword;
    private MaterialButton btnGetOtp, btnResetPassword;
    private TextView tvBackToLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        // Ánh xạ
        layoutStep1 = findViewById(R.id.layoutStep1);
        layoutStep2 = findViewById(R.id.layoutStep2);
        etForgotPhone = findViewById(R.id.etForgotPhone);
        etOtp = findViewById(R.id.etOtp);
        etNewPassword = findViewById(R.id.etNewPassword);
        btnGetOtp = findViewById(R.id.btnGetOtp);
        btnResetPassword = findViewById(R.id.btnResetPassword);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);

        // Bước 1: Yêu cầu OTP
        btnGetOtp.setOnClickListener(v -> {
            String phone = etForgotPhone.getText().toString().trim();
            if (phone.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập số điện thoại", Toast.LENGTH_SHORT).show();
            } else {
                requestOtp(phone);
            }
        });

        // Bước 2: Đổi mật khẩu
        btnResetPassword.setOnClickListener(v -> {
            String phone = etForgotPhone.getText().toString().trim();
            String otp = etOtp.getText().toString().trim();
            String newPass = etNewPassword.getText().toString().trim();

            if (otp.isEmpty() || newPass.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ OTP và mật khẩu mới", Toast.LENGTH_SHORT).show();
            } else {
                resetPassword(phone, otp, newPass);
            }
        });

        tvBackToLogin.setOnClickListener(v -> finish());
    }

    private void requestOtp(String phone) {
        btnGetOtp.setEnabled(false);
        btnGetOtp.setText("Đang gửi...");

        Map<String, Object> map = new HashMap<>();
        map.put("phone", phone);

        ApiService api = RetrofitClient.getInstance(this).getApiService();
        api.forgotPassword(map).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                btnGetOtp.setEnabled(true);
                btnGetOtp.setText("Gửi mã OTP");

                if (response.isSuccessful()) {
                    // Vì backend bạn đang trả OTP về luôn trong response để test
                    // Thông thường sẽ gửi qua SMS, ở đây mình lấy từ response để hiện cho dễ test
                    if (response.body() != null && response.body().containsKey("otp")) {
                        String otpFromServer = response.body().get("otp").toString();
                        Toast.makeText(ForgotPasswordActivity.this, "Mã OTP của bạn là: " + otpFromServer, Toast.LENGTH_LONG).show();
                    }

                    layoutStep1.setVisibility(View.GONE);
                    layoutStep2.setVisibility(View.VISIBLE);
                } else {
                    Toast.makeText(ForgotPasswordActivity.this, "Số điện thoại không tồn tại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                btnGetOtp.setEnabled(true);
                btnGetOtp.setText("Gửi mã OTP");
                Toast.makeText(ForgotPasswordActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void resetPassword(String phone, String otp, String newPassword) {
        btnResetPassword.setEnabled(false);
        btnResetPassword.setText("Đang xử lý...");

        Map<String, Object> map = new HashMap<>();
        map.put("phone", phone);
        map.put("otp", otp);
        map.put("new_password", newPassword);

        ApiService api = RetrofitClient.getInstance(this).getApiService();
        api.resetPassword(map).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                btnResetPassword.setEnabled(true);
                btnResetPassword.setText("Xác nhận đổi mật khẩu");

                if (response.isSuccessful()) {
                    Toast.makeText(ForgotPasswordActivity.this, "Đổi mật khẩu thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(ForgotPasswordActivity.this, "OTP không đúng hoặc đã hết hạn", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                btnResetPassword.setEnabled(true);
                btnResetPassword.setText("Xác nhận đổi mật khẩu");
                Toast.makeText(ForgotPasswordActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
