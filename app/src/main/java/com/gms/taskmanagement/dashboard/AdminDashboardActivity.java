package com.gms.taskmanagement.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.gms.taskmanagement.R;
import com.gms.taskmanagement.adapters.TaskAdapter;
import com.gms.taskmanagement.api.ApiClient;
import com.gms.taskmanagement.api.ApiResponse;
import com.gms.taskmanagement.api.ApiCallback;
import com.gms.taskmanagement.auth.LoginActivity;
import com.gms.taskmanagement.auth.SessionManager;
import com.gms.taskmanagement.models.Task;
import com.gms.taskmanagement.notifications.NotificationActivity;
import com.gms.taskmanagement.profile.ProfileActivity;
import com.gms.taskmanagement.reports.ReportsActivity;
import com.gms.taskmanagement.settings.SettingsActivity;
import com.gms.taskmanagement.tasks.NewTaskActivity;
import com.gms.taskmanagement.tasks.TaskDetailActivity;
import com.gms.taskmanagement.users.UserManagementActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class AdminDashboardActivity extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener {
    private SessionManager sessionManager;
    private TextView tvHeaderUserName, tvHeaderUserRole, tvNotificationBadge;
    private TextView tvAdminTotalUsers, tvAdminTotalTasks, tvAdminPendingTasks, tvAdminInProgressTasks, tvAdminCompletedTasks;
    private FrameLayout btnNotification;
    private ImageView btnProfileMenu;
    private Button btnQuickUsers, btnQuickReports, btnQuickEditRequests, btnQuickSettings;
    private EditText etAdminSearchTasks;
    private Spinner spAdminFilterStatus;
    private SwipeRefreshLayout adminSwipeRefreshLayout;
    private RecyclerView rvAdminTasks;
    private LinearLayout layoutAdminEmptyState;
    private ProgressBar adminTasksProgressBar;
    private FloatingActionButton fabAdminAddTask;

    private TaskAdapter taskAdapter;
    private final List<Task> allTasks = new ArrayList<>();
    private final List<Task> filteredTasks = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn() || !sessionManager.isAdmin()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        initViews();
        setupHeader();
        setupRecyclerView();
        setupFilters();
        setupQuickActions();
        loadDashboardData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDashboardData();
    }

    private void initViews() {
        tvHeaderUserName = findViewById(R.id.tvHeaderUserName);
        tvHeaderUserRole = findViewById(R.id.tvHeaderUserRole);
        tvNotificationBadge = findViewById(R.id.tvNotificationBadge);
        btnNotification = findViewById(R.id.btnNotification);
        btnProfileMenu = findViewById(R.id.btnProfileMenu);

        tvAdminTotalUsers = findViewById(R.id.tvAdminTotalUsers);
        tvAdminTotalTasks = findViewById(R.id.tvAdminTotalTasks);
        tvAdminPendingTasks = findViewById(R.id.tvAdminPendingTasks);
        tvAdminInProgressTasks = findViewById(R.id.tvAdminInProgressTasks);
        tvAdminCompletedTasks = findViewById(R.id.tvAdminCompletedTasks);

        btnQuickUsers = findViewById(R.id.btnQuickUsers);
        btnQuickReports = findViewById(R.id.btnQuickReports);
        btnQuickEditRequests = findViewById(R.id.btnQuickEditRequests);
        btnQuickSettings = findViewById(R.id.btnQuickSettings);

        etAdminSearchTasks = findViewById(R.id.etAdminSearchTasks);
        spAdminFilterStatus = findViewById(R.id.spAdminFilterStatus);
        adminSwipeRefreshLayout = findViewById(R.id.adminSwipeRefreshLayout);
        rvAdminTasks = findViewById(R.id.rvAdminTasks);
        layoutAdminEmptyState = findViewById(R.id.layoutAdminEmptyState);
        adminTasksProgressBar = findViewById(R.id.adminTasksProgressBar);
        fabAdminAddTask = findViewById(R.id.fabAdminAddTask);

        btnNotification.setOnClickListener(v -> startActivity(new Intent(this, NotificationActivity.class)));
        btnProfileMenu.setOnClickListener(this::showProfileMenu);
        fabAdminAddTask.setOnClickListener(v -> startActivity(new Intent(this, NewTaskActivity.class)));
        adminSwipeRefreshLayout.setOnRefreshListener(this::loadDashboardData);
    }

    private void setupHeader() {
        tvHeaderUserName.setText(sessionManager.getUserFullName());
        tvHeaderUserRole.setText("ADMINISTRATOR");
    }

    private void setupQuickActions() {
        btnQuickUsers.setOnClickListener(v -> startActivity(new Intent(this, UserManagementActivity.class)));
        btnQuickReports.setOnClickListener(v -> startActivity(new Intent(this, ReportsActivity.class)));
        btnQuickSettings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        btnQuickEditRequests.setOnClickListener(v -> {
            Toast.makeText(this, "Showing Pending Edit Requests", Toast.LENGTH_SHORT).show();
            spAdminFilterStatus.setSelection(1); // Pending
        });
    }

    private void setupRecyclerView() {
        taskAdapter = new TaskAdapter(this, task -> {
            Intent intent = new Intent(AdminDashboardActivity.this, TaskDetailActivity.class);
            intent.putExtra("task", task);
            startActivity(intent);
        });
        rvAdminTasks.setLayoutManager(new LinearLayoutManager(this));
        rvAdminTasks.setAdapter(taskAdapter);
    }

    private void setupFilters() {
        String[] statusOptions = {"All Status", "Pending", "In Progress", "Completed", "Rejected"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, statusOptions);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spAdminFilterStatus.setAdapter(spinnerAdapter);

        spAdminFilterStatus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyFilters();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        etAdminSearchTasks.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilters();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadDashboardData() {
        String backendUrl = sessionManager.getBackendUrl();
        if (backendUrl.isEmpty()) return;

        adminTasksProgressBar.setVisibility(View.VISIBLE);

        JSONObject params = new JSONObject();
        try {
            params.put("action", "get_tasks");
            params.put("email", sessionManager.getUserEmail());
            params.put("role", "Admin");
        } catch (Exception ignored) {}

        ApiClient.post(backendUrl, params, new ApiCallback() {
            @Override
            public void onSuccess(ApiResponse response) {
                adminTasksProgressBar.setVisibility(View.GONE);
                adminSwipeRefreshLayout.setRefreshing(false);

                if (response.isSuccess()) {
                    allTasks.clear();
                    try {
                        JSONObject json = new JSONObject(response.getRawJson());
                        JSONArray arr = json.optJSONArray("data");
                        if (arr != null) {
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject obj = arr.getJSONObject(i);
                                Task t = new Task();
                                t.setRowIndex(obj.optInt("rowIndex", 0));
                                t.setTimestamp(obj.optString("timestamp", ""));
                                t.setTicketNumber(obj.optString("ticketNumber", ""));
                                t.setSelectEmployee(obj.optString("selectEmployee", ""));
                                t.setJobType(obj.optString("jobType", ""));
                                t.setDateOfPerform(obj.optString("dateOfPerform", ""));
                                t.setEmployeeName(obj.optString("employeeName", ""));
                                t.setDescription(obj.optString("description", ""));
                                t.setDepartment(obj.optString("department", ""));
                                t.setStatus(obj.optString("status", "Pending"));
                                allTasks.add(t);
                            }
                        }
                    } catch (Exception ignored) {}

                    calculateMetrics();
                    applyFilters();
                } else {
                    Toast.makeText(AdminDashboardActivity.this, response.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                adminTasksProgressBar.setVisibility(View.GONE);
                adminSwipeRefreshLayout.setRefreshing(false);
                Toast.makeText(AdminDashboardActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });

        // Also fetch total user count
        JSONObject userParams = new JSONObject();
        try {
            userParams.put("action", "get_users");
            userParams.put("email", sessionManager.getUserEmail());
            userParams.put("role", "Admin");
        } catch (Exception ignored) {}

        ApiClient.post(backendUrl, userParams, new ApiCallback() {
            @Override
            public void onSuccess(ApiResponse response) {
                if (response.isSuccess()) {
                    try {
                        JSONObject json = new JSONObject(response.getRawJson());
                        JSONArray arr = json.optJSONArray("data");
                        if (arr != null) {
                            tvAdminTotalUsers.setText(String.valueOf(arr.length()));
                        }
                    } catch (Exception ignored) {}
                }
            }
            @Override
            public void onError(String errorMessage) {}
        });
    }

    private void calculateMetrics() {
        int total = allTasks.size();
        int pending = 0;
        int inProgress = 0;
        int completed = 0;

        for (Task t : allTasks) {
            String s = t.getStatus() != null ? t.getStatus().toLowerCase() : "";
            if (s.contains("pending")) pending++;
            else if (s.contains("in progress")) inProgress++;
            else if (s.contains("completed")) completed++;
        }

        tvAdminTotalTasks.setText(String.valueOf(total));
        tvAdminPendingTasks.setText(String.valueOf(pending));
        tvAdminInProgressTasks.setText(String.valueOf(inProgress));
        tvAdminCompletedTasks.setText(String.valueOf(completed));
    }

    private void applyFilters() {
        String query = etAdminSearchTasks.getText().toString().trim().toLowerCase();
        String selectedStatus = spAdminFilterStatus.getSelectedItem() != null ? spAdminFilterStatus.getSelectedItem().toString() : "All Status";

        filteredTasks.clear();
        for (Task t : allTasks) {
            boolean matchesStatus = "All Status".equalsIgnoreCase(selectedStatus) ||
                    (t.getStatus() != null && t.getStatus().equalsIgnoreCase(selectedStatus));

            boolean matchesQuery = query.isEmpty() ||
                    (t.getTicketNumber() != null && t.getTicketNumber().toLowerCase().contains(query)) ||
                    (t.getSelectEmployee() != null && t.getSelectEmployee().toLowerCase().contains(query)) ||
                    (t.getEmployeeName() != null && t.getEmployeeName().toLowerCase().contains(query)) ||
                    (t.getJobType() != null && t.getJobType().toLowerCase().contains(query)) ||
                    (t.getDescription() != null && t.getDescription().toLowerCase().contains(query)) ||
                    (t.getDepartment() != null && t.getDepartment().toLowerCase().contains(query));

            if (matchesStatus && matchesQuery) {
                filteredTasks.add(t);
            }
        }

        taskAdapter.setTasks(filteredTasks);
        layoutAdminEmptyState.setVisibility(filteredTasks.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showProfileMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenu().add(0, 1, 0, "My Profile");
        popup.getMenu().add(0, 2, 1, "Settings");
        popup.getMenu().add(0, 3, 2, "Logout");
        popup.setOnMenuItemClickListener(this);
        popup.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case 1:
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            case 2:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case 3:
                sessionManager.logout();
                Toast.makeText(this, "Signed out", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                return true;
            default:
                return false;
        }
    }
}
