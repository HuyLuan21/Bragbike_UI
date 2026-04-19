package com.example.bragbike;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.bragbike.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class ActivityHistoryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_activity_history);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        
        // Đặt mục Hoạt động là được chọn
        bottomNav.setSelectedItemId(R.id.nav_history);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                // Thay vì finish(), chúng ta khởi động lại HomeActivity 
                // với cờ CLEAR_TOP để reset trạng thái thanh điều hướng
                Intent intent = new Intent(this, HomeActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
                return true;
            } else if (id == R.id.nav_history) {
                return true;
            } else if (id == R.id.nav_profile) {
                Intent intent = new Intent(this, ProfileActivity.class);
                startActivity(intent);
                finish();
                return true;
            }
            return false;
        });
    }
}