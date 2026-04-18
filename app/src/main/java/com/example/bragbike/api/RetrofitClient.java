package com.example.bragbike.api;

import android.content.Context;
import com.example.bragbike.BuildConfig;
import com.example.bragbike.utils.TokenManager;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.concurrent.TimeUnit;

public class RetrofitClient {
    private static RetrofitClient instance;
    private final Retrofit retrofit;
    private final TokenManager tokenManager;

    private RetrofitClient(Context context) {
        tokenManager = new TokenManager(context);

        // Khởi tạo HttpLoggingInterceptor để xem log API
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging) // Thêm logging vào đầu danh sách interceptor
                .addInterceptor(chain -> {
                    Request original = chain.request();
                    String token = tokenManager.getToken();
                    Request request = original.newBuilder()
                            .header("Authorization", token != null ? "Bearer " + token : "")
                            .build();
                    return chain.proceed(request);
                })
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        retrofit = new Retrofit.Builder()
                .baseUrl(BuildConfig.BASE_URL + "/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    public static synchronized RetrofitClient getInstance(Context context) {
        if (instance == null) {
            instance = new RetrofitClient(context.getApplicationContext());
        }
        return instance;
    }

    public ApiService getApiService() {
        return retrofit.create(ApiService.class);
    }

    public TokenManager getTokenManager() {
        return tokenManager;
    }
}
