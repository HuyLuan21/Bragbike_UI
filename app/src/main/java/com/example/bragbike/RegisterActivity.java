package com.example.bragbike;

import android.os.Bundle;
import android.util.Log;
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

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etFullName, etPhone, etPassword;
    private MaterialButton btnRegister;
    private TextView tvLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Ánh xạ View
        etFullName = findViewById(R.id.etFullName);
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLogin);

        // Nút Đăng ký
        btnRegister.setOnClickListener(v -> {
            String name = etFullName.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (name.isEmpty() || phone.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            } else {
                registerUser(name, phone, password);
            }
        });


        // Quay lại màn hình Login
        tvLogin.setOnClickListener(v -> finish());
    }

    private void registerUser(String name, String phone, String password) {
        Map<String, Object> map = new HashMap<>();
        map.put("full_name", name);
        map.put("phone", phone);
        map.put("password", password);

        // Hiển thị trạng thái load
        btnRegister.setEnabled(false);
        btnRegister.setText("Đang xử lý...");

        ApiService api = RetrofitClient.getInstance(this).getApiService();
        api.register(map).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                btnRegister.setEnabled(true);
                btnRegister.setText("Đăng ký");

                if (response.isSuccessful()) {
                    Toast.makeText(RegisterActivity.this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
                    finish(); // Quay lại màn hình Login
                } else {
                    // Xử lý các lỗi từ backend (ví dụ số điện thoại đã tồn tại)
                    Toast.makeText(RegisterActivity.this, "Đăng ký thất bại. Có thể số điện thoại đã tồn tại.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                btnRegister.setEnabled(true);
                btnRegister.setText("Đăng ký");
                Log.e("REGISTER_ERR", "Lỗi: " + t.getMessage());
                Toast.makeText(RegisterActivity.this, "Lỗi kết nối server", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
