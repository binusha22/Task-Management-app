package com.gms.taskmanagement.tasks;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.gms.taskmanagement.R;
import com.gms.taskmanagement.api.ApiClient;
import com.gms.taskmanagement.api.ApiResponse;
import com.gms.taskmanagement.api.ApiCallback;
import com.gms.taskmanagement.auth.SessionManager;
import com.gms.taskmanagement.utils.DateUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class NewTaskActivity extends AppCompatActivity {
    private SessionManager sessionManager;
    private ImageView btnBackNewTask;
    private EditText etTicketNumber, etDateOfPerform, etEmployeeName, etTaskDescription;
    private Spinner spSelectEmployee, spJobType, spDepartment, spTaskStatus;
    private Button btnSubmitTask;
    private ProgressBar submitProgressBar;

    private final List<String> employeeList = new ArrayList<>();
    private final List<String> jobTypeList = new ArrayList<>();
    private final List<String> departmentList = new ArrayList<>();

    private ArrayAdapter<String> employeeAdapter;
    private ArrayAdapter<String> jobTypeAdapter;
    private ArrayAdapter<String> departmentAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_task);

        sessionManager = new SessionManager(this);

        initViews();
        setupSpinners();
        fetchNextTicketNumber();
        fetchDynamicConfigLists();
    }

    private void initViews() {
        btnBackNewTask = findViewById(R.id.btnBackNewTask);
        etTicketNumber = findViewById(R.id.etTicketNumber);
        etDateOfPerform = findViewById(R.id.etDateOfPerform);
        etEmployeeName = findViewById(R.id.etEmployeeName);
        etTaskDescription = findViewById(R.id.etTaskDescription);

        spSelectEmployee = findViewById(R.id.spSelectEmployee);
        spJobType = findViewById(R.id.spJobType);
        spDepartment = findViewById(R.id.spDepartment);
        spTaskStatus = findViewById(R.id.spTaskStatus);

        btnSubmitTask = findViewById(R.id.btnSubmitTask);
        submitProgressBar = findViewById(R.id.submitProgressBar);

        btnBackNewTask.setOnClickListener(v -> finish());
        etDateOfPerform.setText(DateUtils.getTodayDate());
        etDateOfPerform.setOnClickListener(v -> showDatePicker());

        btnSubmitTask.setOnClickListener(v -> submitTask());
    }

    private void setupSpinners() {
        // Default Employees (current user is first)
        String currentUserName = sessionManager.getUserFullName();
        employeeList.add(currentUserName);
        if (!currentUserName.equalsIgnoreCase("Binusha Perera")) employeeList.add("Binusha Perera");
        if (!currentUserName.equalsIgnoreCase("Sahath Talip")) employeeList.add("Sahath Talip");
        if (!currentUserName.equalsIgnoreCase("Athif Shaheer")) employeeList.add("Athif Shaheer");
        if (!currentUserName.equalsIgnoreCase("Manoj Chathuranga")) employeeList.add("Manoj Chathuranga");

        employeeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, employeeList);
        employeeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spSelectEmployee.setAdapter(employeeAdapter);

        // Default Job Types
        jobTypeList.add("Network Cabling / Route");
        jobTypeList.add("Software Installation");
        jobTypeList.add("Hardware Maintenance");
        jobTypeList.add("Printer Troubleshooting");
        jobTypeList.add("Database Backup");
        jobTypeList.add("System Access Request");

        jobTypeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, jobTypeList);
        jobTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spJobType.setAdapter(jobTypeAdapter);

        // Default Departments
        departmentList.add("Accounts / Finance");
        departmentList.add("Information Technology");
        departmentList.add("Human Resources");
        departmentList.add("Production / Operations");
        departmentList.add("Procurement / Logistics");
        departmentList.add("Management");

        departmentAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, departmentList);
        departmentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spDepartment.setAdapter(departmentAdapter);

        // Status
        String[] statuses = {"Pending", "In Progress", "Completed"};
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, statuses);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spTaskStatus.setAdapter(statusAdapter);
    }

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        DatePickerDialog picker = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            String formatted = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth);
            etDateOfPerform.setText(formatted);
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        picker.show();
    }

    private void fetchNextTicketNumber() {
        String backendUrl = sessionManager.getBackendUrl();
        if (backendUrl.isEmpty()) return;

        JSONObject params = new JSONObject();
        try {
            params.put("action", "get_next_ticket");
        } catch (Exception ignored) {}

        ApiClient.post(backendUrl, params, new ApiCallback() {
            @Override
            public void onSuccess(ApiResponse response) {
                if (response.isSuccess() && response.getData() != null) {
                    String nextTicket = response.getData().optString("nextTicketNumber", "ITM 2026-16");
                    etTicketNumber.setText(nextTicket);
                }
            }
            @Override
            public void onError(String errorMessage) {}
        });
    }

    private void fetchDynamicConfigLists() {
        String backendUrl = sessionManager.getBackendUrl();
        if (backendUrl.isEmpty()) return;

        JSONObject params = new JSONObject();
        try {
            params.put("action", "get_config_lists");
        } catch (Exception ignored) {}

        ApiClient.post(backendUrl, params, new ApiCallback() {
            @Override
            public void onSuccess(ApiResponse response) {
                if (response.isSuccess() && response.getData() != null) {
                    JSONObject data = response.getData();
                    JSONArray jobs = data.optJSONArray("jobTypes");
                    if (jobs != null && jobs.length() > 0) {
                        jobTypeList.clear();
                        for (int i = 0; i < jobs.length(); i++) {
                            jobTypeList.add(jobs.optString(i));
                        }
                        jobTypeAdapter.notifyDataSetChanged();
                    }

                    JSONArray depts = data.optJSONArray("departments");
                    if (depts != null && depts.length() > 0) {
                        departmentList.clear();
                        for (int i = 0; i < depts.length(); i++) {
                            departmentList.add(depts.optString(i));
                        }
                        departmentAdapter.notifyDataSetChanged();
                    }
                }
            }
            @Override
            public void onError(String errorMessage) {}
        });
    }

    private void submitTask() {
        String selectEmp = spSelectEmployee.getSelectedItem() != null ? spSelectEmployee.getSelectedItem().toString() : "";
        String jobType = spJobType.getSelectedItem() != null ? spJobType.getSelectedItem().toString() : "";
        String dateOfPerform = etDateOfPerform.getText().toString().trim();
        String employeeName = etEmployeeName.getText().toString().trim();
        String description = etTaskDescription.getText().toString().trim();
        String department = spDepartment.getSelectedItem() != null ? spDepartment.getSelectedItem().toString() : "";
        String status = spTaskStatus.getSelectedItem() != null ? spTaskStatus.getSelectedItem().toString() : "Pending";

        if (description.isEmpty()) {
            etTaskDescription.setError("Description is required");
            etTaskDescription.requestFocus();
            return;
        }

        if (employeeName.isEmpty()) {
            employeeName = selectEmp;
        }

        btnSubmitTask.setEnabled(false);
        submitProgressBar.setVisibility(View.VISIBLE);

        JSONObject taskObj = new JSONObject();
        try {
            taskObj.put("ticketNumber", etTicketNumber.getText().toString());
            taskObj.put("selectEmployee", selectEmp);
            taskObj.put("jobType", jobType);
            taskObj.put("dateOfPerform", dateOfPerform);
            taskObj.put("employeeName", employeeName);
            taskObj.put("description", description);
            taskObj.put("department", department);
            taskObj.put("status", status);
        } catch (Exception ignored) {}

        JSONObject requestParams = new JSONObject();
        try {
            requestParams.put("action", "create_task");
            requestParams.put("email", sessionManager.getUserEmail());
            requestParams.put("role", sessionManager.getUserRole());
            requestParams.put("task", taskObj);
        } catch (Exception ignored) {}

        String backendUrl = sessionManager.getBackendUrl();
        ApiClient.post(backendUrl, requestParams, new ApiCallback() {
            @Override
            public void onSuccess(ApiResponse response) {
                submitProgressBar.setVisibility(View.GONE);
                btnSubmitTask.setEnabled(true);

                if (response.isSuccess()) {
                    Toast.makeText(NewTaskActivity.this, "Task created: " + response.getMessage(), Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    Toast.makeText(NewTaskActivity.this, "Failed: " + response.getMessage(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                submitProgressBar.setVisibility(View.GONE);
                btnSubmitTask.setEnabled(true);
                Toast.makeText(NewTaskActivity.this, "Error submitting task: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }
}
