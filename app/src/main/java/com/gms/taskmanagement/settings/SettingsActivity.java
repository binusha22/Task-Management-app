package com.gms.taskmanagement.settings;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.gms.taskmanagement.R;
import com.gms.taskmanagement.api.ApiClient;
import com.gms.taskmanagement.api.ApiResponse;
import com.gms.taskmanagement.api.ApiCallback;
import com.gms.taskmanagement.auth.SessionManager;
import org.json.JSONObject;

public class SettingsActivity extends AppCompatActivity {
    private SessionManager sessionManager;
    private ImageView btnBackSettings;
    private LinearLayout layoutAdminSheetsConfig;
    private EditText etSettingsBackendUrl, etSettingsUsersSheet, etSettingsTasksSheet,
                     etSettingsFcmServerKey;
    private Button btnSettingsTestConnection, btnSettingsSaveConfig;
    private TextView tvSettingsConnectionStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        sessionManager = new SessionManager(this);

        initViews();
        loadSettings();
    }

    private void initViews() {
        btnBackSettings            = findViewById(R.id.btnBackSettings);
        layoutAdminSheetsConfig    = findViewById(R.id.layoutAdminSheetsConfig);
        etSettingsBackendUrl       = findViewById(R.id.etSettingsBackendUrl);
        etSettingsUsersSheet       = findViewById(R.id.etSettingsUsersSheet);
        etSettingsTasksSheet       = findViewById(R.id.etSettingsTasksSheet);
        etSettingsFcmServerKey     = findViewById(R.id.etSettingsFcmServerKey);
        btnSettingsTestConnection  = findViewById(R.id.btnSettingsTestConnection);
        btnSettingsSaveConfig      = findViewById(R.id.btnSettingsSaveConfig);
        tvSettingsConnectionStatus = findViewById(R.id.tvSettingsConnectionStatus);

        btnBackSettings.setOnClickListener(v -> finish());
        btnSettingsTestConnection.setOnClickListener(v -> testConnection());
        btnSettingsSaveConfig.setOnClickListener(v -> saveConfig());

        if (!sessionManager.isAdmin()) {
            etSettingsBackendUrl.setEnabled(false);
            etSettingsUsersSheet.setEnabled(false);
            etSettingsTasksSheet.setEnabled(false);
            etSettingsFcmServerKey.setEnabled(false);
            btnSettingsSaveConfig.setVisibility(View.GONE);
        }
    }

    private void loadSettings() {
        etSettingsBackendUrl.setText(sessionManager.getBackendUrl());
        etSettingsUsersSheet.setText(sessionManager.getUsersSheetName());
        etSettingsTasksSheet.setText(sessionManager.getTasksSheetName());
        etSettingsFcmServerKey.setText(sessionManager.getFcmServerKey());
    }

    private void testConnection() {
        String url = etSettingsBackendUrl.getText().toString().trim();
        if (url.isEmpty()) {
            etSettingsBackendUrl.setError("URL required");
            return;
        }

        tvSettingsConnectionStatus.setText("Testing connectivity...");
        tvSettingsConnectionStatus.setTextColor(Color.DKGRAY);
        btnSettingsTestConnection.setEnabled(false);

        JSONObject params = new JSONObject();
        try {
            params.put("action", "test_connection");
        } catch (Exception ignored) {}

        ApiClient.post(url, params, new ApiCallback() {
            @Override
            public void onSuccess(ApiResponse response) {
                btnSettingsTestConnection.setEnabled(true);
                if (response.isSuccess()) {
                    tvSettingsConnectionStatus.setText("✓ " + response.getMessage());
                    tvSettingsConnectionStatus.setTextColor(Color.parseColor("#10B981"));
                } else {
                    tvSettingsConnectionStatus.setText("✗ " + response.getMessage());
                    tvSettingsConnectionStatus.setTextColor(Color.parseColor("#EF4444"));
                }
            }

            @Override
            public void onError(String errorMessage) {
                btnSettingsTestConnection.setEnabled(true);
                tvSettingsConnectionStatus.setText("✗ " + errorMessage);
                tvSettingsConnectionStatus.setTextColor(Color.parseColor("#EF4444"));
            }
        });
    }

    private void saveConfig() {
        String url    = etSettingsBackendUrl.getText().toString().trim();
        String uSheet = etSettingsUsersSheet.getText().toString().trim();
        String tSheet = etSettingsTasksSheet.getText().toString().trim();
        String fcmKey = etSettingsFcmServerKey.getText().toString().trim();

        if (url.isEmpty()) {
            etSettingsBackendUrl.setError("Web App URL is required");
            return;
        }

        if (uSheet.isEmpty()) uSheet = "Users";
        if (tSheet.isEmpty()) tSheet = "Form Responses 1";

        sessionManager.saveBackendConfig(url, uSheet, tSheet);
        sessionManager.saveFcmServerKey(fcmKey);
        Toast.makeText(this, "Settings updated successfully", Toast.LENGTH_SHORT).show();
        finish();
    }
}

