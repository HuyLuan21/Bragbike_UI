package com.example.bragbike;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bragbike.api.ApiService;
import com.example.bragbike.api.RetrofitClient;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RideRequestActivity extends AppCompatActivity {

    public static final String EXTRA_RIDE_ID = "extra_ride_id";
    public static final String EXTRA_FARE = "extra_fare";
    public static final String EXTRA_PICKUP = "extra_pickup";
    public static final String EXTRA_DROPOFF = "extra_dropoff";
    public static final String EXTRA_VEHICLE = "extra_vehicle";

    private int rideId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ride_request);

        rideId = getIntent().getIntExtra(EXTRA_RIDE_ID, -1);
        double fare = getIntent().getDoubleExtra(EXTRA_FARE, 0);
        String pickup = getIntent().getStringExtra(EXTRA_PICKUP);
        String dropoff = getIntent().getStringExtra(EXTRA_DROPOFF);
        String vehicle = getIntent().getStringExtra(EXTRA_VEHICLE);

        TextView tvFare = findViewById(R.id.tvFare);
        TextView tvPickup = findViewById(R.id.tvPickup);
        TextView tvDropoff = findViewById(R.id.tvDropoff);
        ImageView ivVehicle = findViewById(R.id.ivVehicleType);

        tvFare.setText(String.format("%,.0fđ", fare));
        tvPickup.setText("Điểm đón: " + pickup);
        tvDropoff.setText("Điểm đến: " + dropoff);

        if (vehicle != null && vehicle.contains("CAR")) {
            ivVehicle.setImageResource(R.drawable.directions_car_24px);
        } else {
            ivVehicle.setImageResource(R.drawable.moped_24px);
        }

        findViewById(R.id.btnAccept).setOnClickListener(v -> acceptRide());
        
        findViewById(R.id.btnDecline).setOnClickListener(v -> {
            Intent result = new Intent();
            result.putExtra("declined_ride_id", rideId);
            setResult(RESULT_CANCELED, result);
            finish();
        });
    }

    private void acceptRide() {
        ApiService api = RetrofitClient.getInstance(this).getApiService();
        api.acceptRide(rideId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(RideRequestActivity.this, "Đã nhận chuyến!", Toast.LENGTH_SHORT).show();
                    
                    // CHUYỂN SANG MÀN HÌNH THỰC HIỆN HÀNH TRÌNH
                    Intent intent = new Intent(RideRequestActivity.this, DriverRideProgressActivity.class);
                    intent.putExtra(DriverRideProgressActivity.EXTRA_RIDE_ID, rideId);
                    startActivity(intent);

                    finish();
                } else {
                    Toast.makeText(RideRequestActivity.this, "Cuốc xe không còn khả dụng", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(RideRequestActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
