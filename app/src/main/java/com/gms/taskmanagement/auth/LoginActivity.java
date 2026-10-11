package com.gms.taskmanagement.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.gms.taskmanagement.R;
import com.gms.taskmanagement.api.ApiClient;
import com.gms.taskmanagement.api.ApiResponse;
import com.gms.taskmanagement.api.ApiCallback;
import com.gms.taskmanagement.dashboard.AdminDashboardActivity;
import com.gms.taskmanagement.dashboard.UserDashboardActivity;
import com.gms.taskmanagement.settings.SetupActivity;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {
    private EditText etEmail, etPassword;
    private ImageView ivTogglePassword;
    private Button btnLogin;
    private ProgressBar loginProgressBar;
    private TextView tvConfigureBackend;
    private SessionManager sessionManager;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        sessionManager = new SessionManager(this);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        ivTogglePassword = findViewById(R.id.ivTogglePassword);
        btnLogin = findViewById(R.id.btnLogin);
        loginProgressBar = findViewById(R.id.loginProgressBar);
        tvConfigureBackend = findViewById(R.id.tvConfigureBackend);

        ivTogglePassword.setOnClickListener(v -> togglePasswordVisibility());
        btnLogin.setOnClickListener(v -> attemptLogin());
        tvConfigureBackend.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SetupActivity.class));
        });
    }

    private void togglePasswordVisibility() {
        if (isPasswordVisible) {
            etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            ivTogglePassword.setImageResource(R.drawable.ic_visibility);
            isPasswordVisible = false;
        } else {
            etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            ivTogglePassword.setImageResource(R.drawable.ic_visibility_off);
            isPasswordVisible = true;
        }
        etPassword.setSelection(etPassword.getText().length());
    }

    private void attemptLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty()) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return;
        }

        String backendUrl = sessionManager.getBackendUrl();
        if (backendUrl.isEmpty()) {
            Toast.makeText(this, "Please configure backend URL in Setup", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, SetupActivity.class));
            return;
        }

        loginProgressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);

        JSONObject params = new JSONObject();
        try {
            params.put("action", "login");
            params.put("email", email);
            params.put("password", password);
        } catch (Exception ignored) {}

        ApiClient.post(backendUrl, params, new ApiCallback() {
            @Override
            public void onSuccess(ApiResponse response) {
                loginProgressBar.setVisibility(View.GONE);
                btnLogin.setEnabled(true);

                if (response.isSuccess() && response.getData() != null) {
                    JSONObject userData = response.getData();
                    String userEmail = userData.optString("email", email);
                    String userRole = userData.optString("role", "User");
                    String fullName = userData.optString("fullName", "User");
                    String token = userData.optString("token", "");

                    sessionManager.createLoginSession(userEmail, userRole, fullName, token);

                    Toast.makeText(LoginActivity.this, "Welcome, " + fullName, Toast.LENGTH_SHORT).show();

                    if (sessionManager.isAdmin()) {
                        startActivity(new Intent(LoginActivity.this, AdminDashboardActivity.class));
                    } else {
                        startActivity(new Intent(LoginActivity.this, UserDashboardActivity.class));
                    }
                    finish();
                } else {
                    Toast.makeText(LoginActivity.this, response.getMessage(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                loginProgressBar.setVisibility(View.GONE);
                btnLogin.setEnabled(true);
                Toast.makeText(LoginActivity.this, "Login failed: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }
}
