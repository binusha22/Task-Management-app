package com.gms.taskmanagement.settings;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.gms.taskmanagement.R;
import com.gms.taskmanagement.api.ApiCallback;
import com.gms.taskmanagement.api.ApiClient;
import com.gms.taskmanagement.api.ApiResponse;
import com.gms.taskmanagement.auth.SessionManager;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ConfigListActivity extends AppCompatActivity {
    private static final String CATEGORY_JOB_TYPE = "JobType";
    private static final String CATEGORY_DEPARTMENT = "Department";

    private SessionManager sessionManager;
    private Button btnJobTypes, btnDepartments, btnAddConfigItem;
    private EditText etConfigItem;
    private LinearLayout layoutConfigItems;
    private TextView tvConfigEmpty;
    private ProgressBar configProgressBar;
    private String selectedCategory = CATEGORY_JOB_TYPE;
    private List<String> jobTypes = new ArrayList<>();
    private List<String> departments = new ArrayList<>();
    private boolean requestInFlight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config_lists);

        sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn() || !sessionManager.isAdmin()) {
            Toast.makeText(this, "Admin access required", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ImageView btnBack = findViewById(R.id.btnBackConfigLists);
        btnJobTypes = findViewById(R.id.btnConfigJobTypes);
        btnDepartments = findViewById(R.id.btnConfigDepartments);
        etConfigItem = findViewById(R.id.etConfigItem);
        btnAddConfigItem = findViewById(R.id.btnAddConfigItem);
        layoutConfigItems = findViewById(R.id.layoutConfigItems);
        tvConfigEmpty = findViewById(R.id.tvConfigEmpty);
        configProgressBar = findViewById(R.id.configProgressBar);

        btnBack.setOnClickListener(v -> finish());
        btnJobTypes.setOnClickListener(v -> selectCategory(CATEGORY_JOB_TYPE));
        btnDepartments.setOnClickListener(v -> selectCategory(CATEGORY_DEPARTMENT));
        btnAddConfigItem.setOnClickListener(v -> addItem());
        renderItems();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadConfigLists();
    }

    private void selectCategory(String category) {
        selectedCategory = category;
        btnJobTypes.setSelected(CATEGORY_JOB_TYPE.equals(category));
        btnDepartments.setSelected(CATEGORY_DEPARTMENT.equals(category));
        btnJobTypes.setAlpha(CATEGORY_JOB_TYPE.equals(category) ? 1f : 0.65f);
        btnDepartments.setAlpha(CATEGORY_DEPARTMENT.equals(category) ? 1f : 0.65f);
        etConfigItem.setHint(CATEGORY_JOB_TYPE.equals(category) ? "Enter a job type" : "Enter a department");
        renderItems();
    }

    private void loadConfigLists() {
        if (requestInFlight) return;
        requestInFlight = true;
        configProgressBar.setVisibility(View.VISIBLE);

        JSONObject params = new JSONObject();
        try {
            params.put("action", "get_config_lists");
        } catch (Exception e) {
            requestInFlight = false;
            configProgressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Unable to load configuration lists", Toast.LENGTH_SHORT).show();
            return;
        }

        ApiClient.post(sessionManager.getBackendUrl(), params, new ApiCallback() {
            @Override
            public void onSuccess(ApiResponse response) {
                requestInFlight = false;
                configProgressBar.setVisibility(View.GONE);
                if (!response.isSuccess() || response.getData() == null) {
                    Toast.makeText(ConfigListActivity.this, response.getMessage(), Toast.LENGTH_SHORT).show();
                    return;
                }

                jobTypes = readValues(response.getData().optJSONArray("jobTypes"));
                departments = readValues(response.getData().optJSONArray("departments"));
                renderItems();
            }

            @Override
            public void onError(String errorMessage) {
                requestInFlight = false;
                configProgressBar.setVisibility(View.GONE);
                Toast.makeText(ConfigListActivity.this, "Unable to load lists: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private List<String> readValues(JSONArray values) {
        List<String> result = new ArrayList<>();
        if (values == null) return result;
        for (int i = 0; i < values.length(); i++) {
            String value = values.optString(i).trim();
            if (!value.isEmpty()) result.add(value);
        }
        return result;
    }

    private void renderItems() {
        if (layoutConfigItems == null) return;
        List<String> values = CATEGORY_JOB_TYPE.equals(selectedCategory) ? jobTypes : departments;
        layoutConfigItems.removeAllViews();
        for (String value : values) {
            layoutConfigItems.addView(createItemRow(value));
        }
        tvConfigEmpty.setVisibility(values.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private View createItemRow(String value) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(10), dp(8), dp(10));
        row.setBackgroundResource(R.drawable.bg_card);

        TextView label = new TextView(this);
        label.setText(value);
        label.setTextColor(getColor(R.color.text_primary));
        label.setTextSize(15);
        label.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        row.addView(label);

        Button removeButton = new Button(this);
        removeButton.setText("Remove");
        removeButton.setTextSize(12);
        removeButton.setTextColor(getColor(R.color.status_rejected));
        removeButton.setOnClickListener(v -> confirmRemove(value));
        row.addView(removeButton);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(8);
        row.setLayoutParams(params);
        return row;
    }

    private void addItem() {
        String value = etConfigItem.getText().toString().trim();
        if (value.isEmpty()) {
            etConfigItem.setError("A name is required");
            return;
        }
        submitChange("add", value);
    }

    private void confirmRemove(String value) {
        new AlertDialog.Builder(this)
                .setTitle("Remove item")
                .setMessage("Remove \"" + value + "\" from the active list?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Remove", (dialog, which) -> submitChange("delete", value))
                .show();
    }

    private void submitChange(String operation, String value) {
        if (requestInFlight) return;
        requestInFlight = true;
        configProgressBar.setVisibility(View.VISIBLE);
        btnAddConfigItem.setEnabled(false);

        JSONObject params = new JSONObject();
        try {
            params.put("action", "manage_config_list");
            params.put("email", sessionManager.getUserEmail());
            params.put("role", sessionManager.getUserRole());
            params.put("category", selectedCategory);
            params.put("value", value);
            params.put("operation", operation);
        } catch (Exception e) {
            requestInFlight = false;
            configProgressBar.setVisibility(View.GONE);
            btnAddConfigItem.setEnabled(true);
            Toast.makeText(this, "Unable to prepare list update", Toast.LENGTH_SHORT).show();
            return;
        }

        ApiClient.post(sessionManager.getBackendUrl(), params, new ApiCallback() {
            @Override
            public void onSuccess(ApiResponse response) {
                requestInFlight = false;
                configProgressBar.setVisibility(View.GONE);
                btnAddConfigItem.setEnabled(true);
                if (!response.isSuccess()) {
                    Toast.makeText(ConfigListActivity.this, response.getMessage(), Toast.LENGTH_SHORT).show();
                    return;
                }

                if ("add".equals(operation)) etConfigItem.setText("");
                Toast.makeText(ConfigListActivity.this, response.getMessage(), Toast.LENGTH_SHORT).show();
                loadConfigLists();
            }

            @Override
            public void onError(String errorMessage) {
                requestInFlight = false;
                configProgressBar.setVisibility(View.GONE);
                btnAddConfigItem.setEnabled(true);
                Toast.makeText(ConfigListActivity.this, "Unable to update list: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
