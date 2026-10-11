package com.gms.taskmanagement.settings;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.gms.taskmanagement.R;
import com.gms.taskmanagement.api.ApiClient;
import com.gms.taskmanagement.api.ApiResponse;
import com.gms.taskmanagement.api.ApiCallback;
import com.gms.taskmanagement.auth.LoginActivity;
import com.gms.taskmanagement.auth.SessionManager;
import org.json.JSONObject;

public class SetupActivity extends AppCompatActivity {
    private EditText etBackendUrl, etUsersSheet, etTasksSheet;
    private Button btnTestConnection, btnSaveConfig, btnProvisionRecovery;
    private TextView tvConnectionStatus;
    private ProgressBar setupProgressBar;
    private SessionManager sessionManager;
    private boolean isConnectionTested = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        sessionManager = new SessionManager(this);

        etBackendUrl = findViewById(R.id.etBackendUrl);
        etUsersSheet = findViewById(R.id.etUsersSheet);
        etTasksSheet = findViewById(R.id.etTasksSheet);
        btnTestConnection = findViewById(R.id.btnTestConnection);
        btnSaveConfig = findViewById(R.id.btnSaveConfig);
        btnProvisionRecovery = findViewById(R.id.btnProvisionRecovery);
        tvConnectionStatus = findViewById(R.id.tvConnectionStatus);
        setupProgressBar = findViewById(R.id.setupProgressBar);

        String currentUrl = sessionManager.getBackendUrl();
        if (!currentUrl.isEmpty()) {
            etBackendUrl.setText(currentUrl);
            etUsersSheet.setText(sessionManager.getUsersSheetName());
            etTasksSheet.setText(sessionManager.getTasksSheetName());
        }

        btnTestConnection.setOnClickListener(v -> testConnection(false));
        btnSaveConfig.setOnClickListener(v -> saveAndContinue());
        btnProvisionRecovery.setOnClickListener(v -> provisionEmergencyAdmin());
    }

    private void testConnection(boolean autoSave) {
        String url = etBackendUrl.getText().toString().trim();
        if (url.isEmpty()) {
            etBackendUrl.setError("Enter Google Apps Script URL");
            return;
        }

        setupProgressBar.setVisibility(View.VISIBLE);
        tvConnectionStatus.setText("Connecting to Google Sheets...");
        tvConnectionStatus.setTextColor(Color.DKGRAY);
        btnTestConnection.setEnabled(false);

        JSONObject params = new JSONObject();
        try {
            params.put("action", "test_connection");
        } catch (Exception ignored) {}

        ApiClient.post(url, params, new ApiCallback() {
            @Override
            public void onSuccess(ApiResponse response) {
                setupProgressBar.setVisibility(View.GONE);
                btnTestConnection.setEnabled(true);
                if (response.isSuccess()) {
                    isConnectionTested = true;
                    tvConnectionStatus.setText("✓ Connected successfully! " + response.getMessage());
                    tvConnectionStatus.setTextColor(Color.parseColor("#10B981"));
                    if (autoSave) {
                        commitAndLaunch();
                    }
                } else {
                    tvConnectionStatus.setText("✗ " + response.getMessage());
                    tvConnectionStatus.setTextColor(Color.parseColor("#EF4444"));
                }
            }

            @Override
            public void onError(String errorMessage) {
                setupProgressBar.setVisibility(View.GONE);
                btnTestConnection.setEnabled(true);
                tvConnectionStatus.setText("✗ Connection failed: " + errorMessage);
                tvConnectionStatus.setTextColor(Color.parseColor("#EF4444"));
            }
        });
    }

    private void saveAndContinue() {
        String url = etBackendUrl.getText().toString().trim();
        if (url.isEmpty()) {
            etBackendUrl.setError("Web App URL is required");
            return;
        }

        if (!isConnectionTested) {
            testConnection(true);
        } else {
            commitAndLaunch();
        }
    }

    private void commitAndLaunch() {
        String url = etBackendUrl.getText().toString().trim();
        String uSheet = etUsersSheet.getText().toString().trim();
        String tSheet = etTasksSheet.getText().toString().trim();

        if (uSheet.isEmpty()) uSheet = "Users";
        if (tSheet.isEmpty()) tSheet = "Form_Responses";

        sessionManager.saveBackendConfig(url, uSheet, tSheet);
        Toast.makeText(this, "Configuration saved successfully", Toast.LENGTH_SHORT).show();

        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    private void provisionEmergencyAdmin() {
        String url = etBackendUrl.getText().toString().trim();
        if (url.isEmpty()) {
            Toast.makeText(this, "Enter backend URL first", Toast.LENGTH_SHORT).show();
            return;
        }
        setupProgressBar.setVisibility(View.VISIBLE);
        JSONObject params = new JSONObject();
        try {
            params.put("action", "provision_recovery_admin");
        } catch (Exception ignored) {}

        ApiClient.post(url, params, new ApiCallback() {
            @Override
            public void onSuccess(ApiResponse response) {
                setupProgressBar.setVisibility(View.GONE);
                Toast.makeText(SetupActivity.this, response.getMessage(), Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(String errorMessage) {
                setupProgressBar.setVisibility(View.GONE);
                Toast.makeText(SetupActivity.this, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }
}
