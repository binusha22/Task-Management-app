package com.gms.taskmanagement.profile;

import android.os.Bundle;
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
import com.gms.taskmanagement.auth.SessionManager;
import org.json.JSONObject;

public class ProfileActivity extends AppCompatActivity {
    private SessionManager sessionManager;
    private ImageView btnBackProfile;
    private TextView tvProfileFullName, tvProfileRole, tvProfileEmail;
    private EditText etCurrentPassword, etNewPassword;
    private Button btnSavePassword;
    private ProgressBar profileProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        sessionManager = new SessionManager(this);

        initViews();
        displayProfile();
    }

    private void initViews() {
        btnBackProfile = findViewById(R.id.btnBackProfile);
        tvProfileFullName = findViewById(R.id.tvProfileFullName);
        tvProfileRole = findViewById(R.id.tvProfileRole);
        tvProfileEmail = findViewById(R.id.tvProfileEmail);
        etCurrentPassword = findViewById(R.id.etCurrentPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        btnSavePassword = findViewById(R.id.btnSavePassword);
        profileProgressBar = findViewById(R.id.profileProgressBar);

        btnBackProfile.setOnClickListener(v -> finish());
        btnSavePassword.setOnClickListener(v -> changePassword());
    }

    private void displayProfile() {
        tvProfileFullName.setText(sessionManager.getUserFullName());
        tvProfileRole.setText(sessionManager.getUserRole().toUpperCase());
        tvProfileEmail.setText(sessionManager.getUserEmail());
    }

    private void changePassword() {
        String curr = etCurrentPassword.getText().toString().trim();
        String next = etNewPassword.getText().toString().trim();

        if (curr.isEmpty()) {
            etCurrentPassword.setError("Current password required");
            return;
        }
        if (next.isEmpty()) {
            etNewPassword.setError("New password required");
            return;
        }

        profileProgressBar.setVisibility(View.VISIBLE);
        btnSavePassword.setEnabled(false);

        JSONObject params = new JSONObject();
        try {
            params.put("action", "change_password");
            params.put("email", sessionManager.getUserEmail());
            params.put("currentPassword", curr);
            params.put("newPassword", next);
        } catch (Exception ignored) {}

        ApiClient.post(sessionManager.getBackendUrl(), params, new ApiCallback() {
            @Override
            public void onSuccess(ApiResponse response) {
                profileProgressBar.setVisibility(View.GONE);
                btnSavePassword.setEnabled(true);
                Toast.makeText(ProfileActivity.this, response.getMessage(), Toast.LENGTH_LONG).show();
                if (response.isSuccess()) {
                    etCurrentPassword.setText("");
                    etNewPassword.setText("");
                }
            }

            @Override
            public void onError(String errorMessage) {
                profileProgressBar.setVisibility(View.GONE);
                btnSavePassword.setEnabled(true);
                Toast.makeText(ProfileActivity.this, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }
}
