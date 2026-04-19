package com.example.bragbike.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.bragbike.R;
import com.example.bragbike.model.Ride;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class RideHistoryAdapter extends RecyclerView.Adapter<RideHistoryAdapter.ViewHolder> {

    private List<Ride> rides = new ArrayList<>();
    private boolean isDriverView = false;

    public void setRides(List<Ride> rides) {
        this.rides = rides;
        notifyDataSetChanged();
    }

    public void setDriverView(boolean driverView) {
        isDriverView = driverView;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ride_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Ride ride = rides.get(position);
        
        holder.tvTimeDate.setText(formatDate(ride.getCreatedAt()));
        holder.tvStatus.setText(formatStatus(ride.getStatus()));
        
        // Hiển thị địa chỉ
        holder.tvPickupAddress.setText(ride.getPickupAddress());
        holder.tvDropoffAddress.setText(ride.getDropoffAddress());
        
        // Hiển thị giá tiền
        double fare = ride.getFare();
        holder.tvFare.setText(String.format("%,.0fđ", fare));
        
        updateStatusStyle(holder.tvStatus, ride.getStatus());

        // Cập nhật icon dựa trên loại phương tiện
        String vehicleType = ride.getVehicleType();
        if (vehicleType != null && (vehicleType.contains("CAR") || vehicleType.contains("car"))) {
            holder.ivVehicleIcon.setImageResource(R.drawable.directions_car_24px);
        } else {
            holder.ivVehicleIcon.setImageResource(R.drawable.moped_24px);
        }

        // Xử lý hiển thị nút bấm: Tài xế không thấy nút "Đặt lại/Đặt quay về"
        if (isDriverView) {
            holder.layoutUserActions.setVisibility(View.GONE);
        } else {
            holder.layoutUserActions.setVisibility(View.VISIBLE);
        }
    }

    private String formatDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return "N/A";
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            inputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = inputFormat.parse(dateStr);

            SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm, dd/MM", Locale.getDefault());
            return outputFormat.format(date);
        } catch (ParseException e) {
            return dateStr;
        }
    }

    private String formatStatus(String status) {
        if (status == null) return "Không xác định";
        switch (status) {
            case "COMPLETED": return "Hoàn thành";
            case "CANCELLED": return "Đã hủy";
            case "SEARCHING": return "Đang tìm tài xế";
            case "ACCEPTED": return "Tài xế nhận";
            case "ARRIVED": return "Tài xế đã đến";
            case "IN_PROGRESS": return "Đang di chuyển";
            default: return status;
        }
    }

    private void updateStatusStyle(TextView tvStatus, String status) {
        if (status == null) return;
        
        int backgroundRes = R.drawable.bg_status_cancelled;

        switch (status) {
            case "COMPLETED":
                backgroundRes = R.drawable.bg_status_completed;
                break;
            case "CANCELLED":
                backgroundRes = R.drawable.bg_status_cancelled;
                break;
            case "IN_PROGRESS":
            case "ACCEPTED":
            case "ARRIVED":
                backgroundRes = R.drawable.bg_status_in_progress;
                break;
        }
        
        tvStatus.setBackgroundResource(backgroundRes);
    }

    @Override
    public int getItemCount() {
        return rides.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTimeDate, tvStatus, tvPickupAddress, tvDropoffAddress, tvFare;
        ImageView ivVehicleIcon;
        View layoutUserActions;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTimeDate = itemView.findViewById(R.id.tvTimeDate);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvPickupAddress = itemView.findViewById(R.id.tvPickupAddress);
            tvDropoffAddress = itemView.findViewById(R.id.tvDropoffAddress);
            tvFare = itemView.findViewById(R.id.tvFare);
            ivVehicleIcon = itemView.findViewById(R.id.ivVehicleIcon);
            layoutUserActions = itemView.findViewById(R.id.layoutUserActions);
        }
    }
}
