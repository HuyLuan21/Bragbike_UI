package com.example.bragbike;

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
        
        // Sửa lỗi: Đổi nav_activity thành nav_history (khớp với menu/bottom_nav_menu.xml)
        bottomNav.setSelectedItemId(R.id.nav_history);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                finish(); // Quay lại trang chủ
                return true;
            } else if (id == R.id.nav_history) {
                return true;
            }
            return false;
        });
    }
}