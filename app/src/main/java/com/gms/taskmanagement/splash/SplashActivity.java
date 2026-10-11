package com.gms.taskmanagement.splash;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.gms.taskmanagement.R;
import com.gms.taskmanagement.auth.LoginActivity;
import com.gms.taskmanagement.auth.SessionManager;
import com.gms.taskmanagement.dashboard.AdminDashboardActivity;
import com.gms.taskmanagement.dashboard.UserDashboardActivity;

public class SplashActivity extends AppCompatActivity {
    private SessionManager sessionManager;
    private TextView tvSplashStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        sessionManager = new SessionManager(this);
        tvSplashStatus = findViewById(R.id.tvSplashStatus);

        new Handler(Looper.getMainLooper()).postDelayed(this::checkNavigation, 1200);
    }

    private void checkNavigation() {
        if (sessionManager.isLoggedIn()) {
            tvSplashStatus.setText("Restoring session...");
            if (sessionManager.isAdmin()) {
                startActivity(new Intent(this, AdminDashboardActivity.class));
            } else {
                startActivity(new Intent(this, UserDashboardActivity.class));
            }
        } else {
            tvSplashStatus.setText("Ready");
            startActivity(new Intent(this, LoginActivity.class));
        }
        finish();
    }
}

