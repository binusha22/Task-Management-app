package com.gms.taskmanagement.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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
import com.gms.taskmanagement.settings.SettingsActivity;
import com.gms.taskmanagement.tasks.NewTaskActivity;
import com.gms.taskmanagement.tasks.TaskDetailActivity;
import com.gms.taskmanagement.utils.Constants;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class UserDashboardActivity extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener {
    private SessionManager sessionManager;
    private TextView tvHeaderUserName, tvHeaderUserRole, tvNotificationBadge;
    private TextView tvStatPending, tvStatInProgress, tvStatCompleted;
    private FrameLayout btnNotification;
    private ImageView btnProfileMenu;
    private EditText etSearchTasks;
    private Spinner spFilterStatus;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView rvTasks;
    private LinearLayout layoutEmptyState;
    private ProgressBar tasksProgressBar;
    private FloatingActionButton fabAddTask;

    private TaskAdapter taskAdapter;
    private final List<Task> allTasks = new ArrayList<>();
    private final List<Task> filteredTasks = new ArrayList<>();
    private final Handler refreshHandler = new Handler(Looper.getMainLooper());
    private boolean tasksRequestInFlight;
    private boolean notificationsRequestInFlight;
    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            loadTasks(false);
            loadNotificationBadge();
            refreshHandler.postDelayed(this, Constants.REFRESH_INTERVAL_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_dashboard);

        sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        initViews();
        setupHeader();
        setupRecyclerView();
        setupFilters();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTasks(true);
        loadNotificationBadge();
        refreshHandler.postDelayed(refreshRunnable, Constants.REFRESH_INTERVAL_MS);
    }

    @Override
    protected void onPause() {
        refreshHandler.removeCallbacks(refreshRunnable);
        super.onPause();
    }

    private void initViews() {
        tvHeaderUserName = findViewById(R.id.tvHeaderUserName);
        tvHeaderUserRole = findViewById(R.id.tvHeaderUserRole);
        tvNotificationBadge = findViewById(R.id.tvNotificationBadge);
        btnNotification = findViewById(R.id.btnNotification);
        btnProfileMenu = findViewById(R.id.btnProfileMenu);

        tvStatPending = findViewById(R.id.tvStatPending);
        tvStatInProgress = findViewById(R.id.tvStatInProgress);
        tvStatCompleted = findViewById(R.id.tvStatCompleted);

        etSearchTasks = findViewById(R.id.etSearchTasks);
        spFilterStatus = findViewById(R.id.spFilterStatus);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        rvTasks = findViewById(R.id.rvTasks);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        tasksProgressBar = findViewById(R.id.tasksProgressBar);
        fabAddTask = findViewById(R.id.fabAddTask);

        btnNotification.setOnClickListener(v -> startActivity(new Intent(this, NotificationActivity.class)));
        btnProfileMenu.setOnClickListener(this::showProfileMenu);
        fabAddTask.setOnClickListener(v -> startActivity(new Intent(this, NewTaskActivity.class)));
        swipeRefreshLayout.setOnRefreshListener(this::loadTasks);
    }

    private void setupHeader() {
        tvHeaderUserName.setText(sessionManager.getUserFullName());
        tvHeaderUserRole.setText(sessionManager.getUserRole().toUpperCase());
    }

    private void setupRecyclerView() {
        taskAdapter = new TaskAdapter(this, task -> {
            Intent intent = new Intent(UserDashboardActivity.this, TaskDetailActivity.class);
            intent.putExtra("task", task);
            startActivity(intent);
        });
        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        rvTasks.setAdapter(taskAdapter);
    }

    private void setupFilters() {
        String[] statusOptions = {"All Status", "Pending", "In Progress", "Completed", "Rejected"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, statusOptions);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spFilterStatus.setAdapter(spinnerAdapter);

        spFilterStatus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyFilters();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        etSearchTasks.addTextChangedListener(new TextWatcher() {
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

    private void loadTasks() {
        loadTasks(true);
    }

    private void loadTasks(boolean showProgress) {
        if (tasksRequestInFlight) return;

        String backendUrl = sessionManager.getBackendUrl();
        if (backendUrl.isEmpty()) {
            swipeRefreshLayout.setRefreshing(false);
            return;
        }

        tasksRequestInFlight = true;
        if (showProgress) tasksProgressBar.setVisibility(View.VISIBLE);

        JSONObject params = new JSONObject();
        try {
            params.put("action", "get_tasks");
            params.put("email", sessionManager.getUserEmail());
            params.put("role", sessionManager.getUserRole());
        } catch (Exception ignored) {}

        ApiClient.post(backendUrl, params, new ApiCallback() {
            @Override
            public void onSuccess(ApiResponse response) {
                tasksRequestInFlight = false;
                tasksProgressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);

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

                    updateStats();
                    applyFilters();
                } else {
                    if (showProgress) {
                        Toast.makeText(UserDashboardActivity.this, response.getMessage(), Toast.LENGTH_SHORT).show();
                    } else {
                        Log.w("UserDashboard", "Task refresh failed: " + response.getMessage());
                    }
                }
            }

            @Override
            public void onError(String errorMessage) {
                tasksRequestInFlight = false;
                tasksProgressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
                if (showProgress) {
                    Toast.makeText(UserDashboardActivity.this, "Sync error: " + errorMessage, Toast.LENGTH_SHORT).show();
                } else {
                    Log.w("UserDashboard", "Task refresh failed: " + errorMessage);
                }
            }
        });
    }

    private void loadNotificationBadge() {
        String backendUrl = sessionManager.getBackendUrl();
        if (backendUrl.isEmpty() || notificationsRequestInFlight) return;

        notificationsRequestInFlight = true;
        JSONObject params = new JSONObject();
        try {
            params.put("action", "get_notifications");
            params.put("email", sessionManager.getUserEmail());
        } catch (Exception ignored) {}

        ApiClient.post(backendUrl, params, new ApiCallback() {
            @Override
            public void onSuccess(ApiResponse response) {
                notificationsRequestInFlight = false;
                if (!response.isSuccess()) return;

                int unreadCount = 0;
                try {
                    JSONArray notifications = new JSONObject(response.getRawJson()).optJSONArray("data");
                    if (notifications != null) {
                        for (int i = 0; i < notifications.length(); i++) {
                            if (!notifications.getJSONObject(i).optBoolean("isRead", false)) {
                                unreadCount++;
                            }
                        }
                    }
                } catch (Exception ignored) {}

                tvNotificationBadge.setText(unreadCount > 9 ? "9+" : String.valueOf(unreadCount));
                tvNotificationBadge.setVisibility(unreadCount > 0 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(String errorMessage) {
                notificationsRequestInFlight = false;
                Log.w("UserDashboard", "Notification refresh failed: " + errorMessage);
            }
        });
    }

    private void updateStats() {
        int pending = 0;
        int inProgress = 0;
        int completed = 0;

        for (Task t : allTasks) {
            String s = t.getStatus() != null ? t.getStatus().toLowerCase() : "";
            if (s.contains("pending")) pending++;
            else if (s.contains("in progress")) inProgress++;
            else if (s.contains("completed")) completed++;
        }

        tvStatPending.setText(String.valueOf(pending));
        tvStatInProgress.setText(String.valueOf(inProgress));
        tvStatCompleted.setText(String.valueOf(completed));
    }

    private void applyFilters() {
        String query = etSearchTasks.getText().toString().trim().toLowerCase();
        String selectedStatus = spFilterStatus.getSelectedItem() != null ? spFilterStatus.getSelectedItem().toString() : "All Status";

        filteredTasks.clear();
        for (Task t : allTasks) {
            boolean matchesStatus = "All Status".equalsIgnoreCase(selectedStatus) ||
                    (t.getStatus() != null && t.getStatus().equalsIgnoreCase(selectedStatus));

            boolean matchesQuery = query.isEmpty() ||
                    (t.getTicketNumber() != null && t.getTicketNumber().toLowerCase().contains(query)) ||
                    (t.getJobType() != null && t.getJobType().toLowerCase().contains(query)) ||
                    (t.getDescription() != null && t.getDescription().toLowerCase().contains(query)) ||
                    (t.getDepartment() != null && t.getDepartment().toLowerCase().contains(query));

            if (matchesStatus && matchesQuery) {
                filteredTasks.add(t);
            }
        }

        taskAdapter.setTasks(filteredTasks);
        layoutEmptyState.setVisibility(filteredTasks.isEmpty() ? View.VISIBLE : View.GONE);
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
