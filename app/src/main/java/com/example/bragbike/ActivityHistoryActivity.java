package com.example.bragbike;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.example.bragbike.adapter.RideHistoryAdapter;
import com.example.bragbike.api.ApiService;
import com.example.bragbike.api.RetrofitClient;
import com.example.bragbike.model.Ride;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActivityHistoryActivity extends AppCompatActivity {

    private RecyclerView rvRideHistory;
    private RideHistoryAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_activity_history);

        initViews();
        setupRecyclerView();
        setupBottomNavigation();
        setupSwipeRefresh();
        
        loadRideHistory();
    }

    private void initViews() {
        rvRideHistory = findViewById(R.id.rvRideHistory);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        bottomNav = findViewById(R.id.bottom_navigation);
    }

    private void setupRecyclerView() {
        adapter = new RideHistoryAdapter();
        rvRideHistory.setLayoutManager(new LinearLayoutManager(this));
        rvRideHistory.setAdapter(adapter);
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener(this::loadRideHistory);
        swipeRefresh.setColorSchemeResources(R.color.primary);
    }

    private void setupBottomNavigation() {
        bottomNav.setSelectedItemId(R.id.nav_history);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
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

    private void loadRideHistory() {
        swipeRefresh.setRefreshing(true);
        ApiService api = RetrofitClient.getInstance(this).getApiService();
        api.getUserRideHistory().enqueue(new Callback<List<Ride>>() {
            @Override
            public void onResponse(Call<List<Ride>> call, Response<List<Ride>> response) {
                swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    adapter.setRides(response.body());
                } else {
                    Toast.makeText(ActivityHistoryActivity.this, "Không thể tải lịch sử chuyến đi", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Ride>> call, Throwable t) {
                swipeRefresh.setRefreshing(false);
                Log.e("HISTORY_ACT", "Error loading history", t);
                Toast.makeText(ActivityHistoryActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
