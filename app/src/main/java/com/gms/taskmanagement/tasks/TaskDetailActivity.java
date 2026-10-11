package com.gms.taskmanagement.tasks;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.gms.taskmanagement.R;
import com.gms.taskmanagement.api.ApiClient;
import com.gms.taskmanagement.api.ApiResponse;
import com.gms.taskmanagement.api.ApiCallback;
import com.gms.taskmanagement.auth.SessionManager;
import com.gms.taskmanagement.models.Task;
import org.json.JSONObject;

public class TaskDetailActivity extends AppCompatActivity {
    private SessionManager sessionManager;
    private Task task;

    private ImageView btnBackDetail;
    private TextView tvDetailTitleTicket, tvDetailStatusChip;
    private TextView tvDetailJobType, tvDetailSelectEmployee, tvDetailEmployeeName;
    private TextView tvDetailDepartment, tvDetailDateOfPerform, tvDetailTimestamp, tvDetailDescription;
    private LinearLayout layoutAdminStatusControl;
    private Spinner spDetailAdminStatus;
    private Button btnAdminUpdateStatus, btnRequestEdit, btnDeleteTask;
    private ProgressBar detailProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_detail);

        sessionManager = new SessionManager(this);
        task = (Task) getIntent().getSerializableExtra("task");

        if (task == null) {
            Toast.makeText(this, "Task details not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        populateDetails();
        configurePermissions();
    }

    private void initViews() {
        btnBackDetail = findViewById(R.id.btnBackDetail);
        tvDetailTitleTicket = findViewById(R.id.tvDetailTitleTicket);
        tvDetailStatusChip = findViewById(R.id.tvDetailStatusChip);

        tvDetailJobType = findViewById(R.id.tvDetailJobType);
        tvDetailSelectEmployee = findViewById(R.id.tvDetailSelectEmployee);
        tvDetailEmployeeName = findViewById(R.id.tvDetailEmployeeName);
        tvDetailDepartment = findViewById(R.id.tvDetailDepartment);
        tvDetailDateOfPerform = findViewById(R.id.tvDetailDateOfPerform);
        tvDetailTimestamp = findViewById(R.id.tvDetailTimestamp);
        tvDetailDescription = findViewById(R.id.tvDetailDescription);

        layoutAdminStatusControl = findViewById(R.id.layoutAdminStatusControl);
        spDetailAdminStatus = findViewById(R.id.spDetailAdminStatus);
        btnAdminUpdateStatus = findViewById(R.id.btnAdminUpdateStatus);
        btnRequestEdit = findViewById(R.id.btnRequestEdit);
        btnDeleteTask = findViewById(R.id.btnDeleteTask);
        detailProgressBar = findViewById(R.id.detailProgressBar);

        btnBackDetail.setOnClickListener(v -> finish());
        btnRequestEdit.setOnClickListener(v -> showEditRequestDialog());
        btnDeleteTask.setOnClickListener(v -> confirmDeleteTask());
        btnAdminUpdateStatus.setOnClickListener(v -> adminUpdateStatus());
    }

    private void populateDetails() {
        tvDetailTitleTicket.setText(task.getTicketNumber());
        tvDetailJobType.setText(task.getJobType());
        tvDetailSelectEmployee.setText(task.getSelectEmployee());
        tvDetailEmployeeName.setText(task.getEmployeeName());
        tvDetailDepartment.setText(task.getDepartment());
        tvDetailDateOfPerform.setText(task.getDateOfPerform());
        tvDetailTimestamp.setText(task.getTimestamp());
        tvDetailDescription.setText(task.getDescription());

        updateStatusBadge(task.getStatus());
    }

    private void updateStatusBadge(String status) {
        if (status == null) status = "Pending";
        tvDetailStatusChip.setText(status);

        if ("Completed".equalsIgnoreCase(status)) {
            tvDetailStatusChip.setTextColor(ContextCompat.getColor(this, R.color.status_completed));
            tvDetailStatusChip.setBackgroundColor(ContextCompat.getColor(this, R.color.status_completed_bg));
        } else if ("In Progress".equalsIgnoreCase(status)) {
            tvDetailStatusChip.setTextColor(ContextCompat.getColor(this, R.color.status_inprogress));
            tvDetailStatusChip.setBackgroundColor(ContextCompat.getColor(this, R.color.status_inprogress_bg));
        } else if ("Rejected".equalsIgnoreCase(status)) {
            tvDetailStatusChip.setTextColor(ContextCompat.getColor(this, R.color.status_rejected));
            tvDetailStatusChip.setBackgroundColor(ContextCompat.getColor(this, R.color.status_rejected_bg));
        } else {
            tvDetailStatusChip.setTextColor(ContextCompat.getColor(this, R.color.status_pending));
            tvDetailStatusChip.setBackgroundColor(ContextCompat.getColor(this, R.color.status_pending_bg));
        }
    }

    private void configurePermissions() {
        boolean isAdmin = sessionManager.isAdmin();
        boolean isCompleted = task.isCompleted();

        if (isAdmin) {
            layoutAdminStatusControl.setVisibility(View.VISIBLE);
            String[] statuses = {"Pending", "In Progress", "Completed", "Rejected"};
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, statuses);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spDetailAdminStatus.setAdapter(adapter);

            for (int i = 0; i < statuses.length; i++) {
                if (statuses[i].equalsIgnoreCase(task.getStatus())) {
                    spDetailAdminStatus.setSelection(i);
                    break;
                }
            }
            btnRequestEdit.setVisibility(View.GONE);
            btnDeleteTask.setVisibility(View.VISIBLE);
        } else {
            layoutAdminStatusControl.setVisibility(View.GONE);
            if (isCompleted) {
                btnRequestEdit.setVisibility(View.VISIBLE);
                btnDeleteTask.setVisibility(View.GONE);
            } else {
                btnRequestEdit.setVisibility(View.GONE);
                btnDeleteTask.setVisibility(View.VISIBLE);
            }
        }
    }

    private void showEditRequestDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_request, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        EditText etEditReason = dialogView.findViewById(R.id.etEditReason);
        Button btnCancel = dialogView.findViewById(R.id.btnCancelEditRequest);
        Button btnSubmit = dialogView.findViewById(R.id.btnSubmitEditRequest);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSubmit.setOnClickListener(v -> {
            String reason = etEditReason.getText().toString().trim();
            if (reason.isEmpty()) {
                etEditReason.setError("Reason is required");
                return;
            }
            dialog.dismiss();
            submitEditRequest(reason);
        });

        dialog.show();
    }

    private void submitEditRequest(String reason) {
        detailProgressBar.setVisibility(View.VISIBLE);

        JSONObject params = new JSONObject();
        try {
            params.put("action", "submit_edit_request");
            params.put("email", sessionManager.getUserEmail());
            params.put("name", sessionManager.getUserFullName());
            params.put("ticketNumber", task.getTicketNumber());
            params.put("reason", reason);
        } catch (Exception ignored) {}

        ApiClient.post(sessionManager.getBackendUrl(), params, new ApiCallback() {
            @Override
            public void onSuccess(ApiResponse response) {
                detailProgressBar.setVisibility(View.GONE);
                Toast.makeText(TaskDetailActivity.this, response.getMessage(), Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(String errorMessage) {
                detailProgressBar.setVisibility(View.GONE);
                Toast.makeText(TaskDetailActivity.this, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void adminUpdateStatus() {
        String newStatus = spDetailAdminStatus.getSelectedItem().toString();
        detailProgressBar.setVisibility(View.VISIBLE);
        btnAdminUpdateStatus.setEnabled(false);

        JSONObject taskObj = new JSONObject();
        try {
            taskObj.put("ticketNumber", task.getTicketNumber());
            taskObj.put("status", newStatus);
        } catch (Exception ignored) {}

        JSONObject params = new JSONObject();
        try {
            params.put("action", "update_task");
            params.put("email", sessionManager.getUserEmail());
            params.put("role", "Admin");
            params.put("task", taskObj);
        } catch (Exception ignored) {}

        ApiClient.post(sessionManager.getBackendUrl(), params, new ApiCallback() {
            @Override
            public void onSuccess(ApiResponse response) {
                detailProgressBar.setVisibility(View.GONE);
                btnAdminUpdateStatus.setEnabled(true);
                if (response.isSuccess()) {
                    task.setStatus(newStatus);
                    updateStatusBadge(newStatus);
                    Toast.makeText(TaskDetailActivity.this, "Status updated to " + newStatus, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(TaskDetailActivity.this, response.getMessage(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                detailProgressBar.setVisibility(View.GONE);
                btnAdminUpdateStatus.setEnabled(true);
                Toast.makeText(TaskDetailActivity.this, "Update error: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void confirmDeleteTask() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Task")
                .setMessage("Are you sure you want to delete task " + task.getTicketNumber() + "?")
                .setPositiveButton("Delete", (dialog, which) -> executeDelete())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void executeDelete() {
        detailProgressBar.setVisibility(View.VISIBLE);
        btnDeleteTask.setEnabled(false);

        JSONObject params = new JSONObject();
        try {
            params.put("action", "delete_task");
            params.put("email", sessionManager.getUserEmail());
            params.put("role", sessionManager.getUserRole());
            params.put("ticketNumber", task.getTicketNumber());
        } catch (Exception ignored) {}

        ApiClient.post(sessionManager.getBackendUrl(), params, new ApiCallback() {
            @Override
            public void onSuccess(ApiResponse response) {
                detailProgressBar.setVisibility(View.GONE);
                if (response.isSuccess()) {
                    Toast.makeText(TaskDetailActivity.this, "Task deleted", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    btnDeleteTask.setEnabled(true);
                    Toast.makeText(TaskDetailActivity.this, response.getMessage(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                detailProgressBar.setVisibility(View.GONE);
                btnDeleteTask.setEnabled(true);
                Toast.makeText(TaskDetailActivity.this, "Delete failed: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }
}
