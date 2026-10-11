package com.gms.taskmanagement.reports;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import com.gms.taskmanagement.R;
import com.gms.taskmanagement.api.ApiClient;
import com.gms.taskmanagement.api.ApiResponse;
import com.gms.taskmanagement.api.ApiCallback;
import com.gms.taskmanagement.auth.SessionManager;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.util.Iterator;

public class ReportsActivity extends AppCompatActivity {
    private SessionManager sessionManager;
    private ImageView btnBackReports;
    private Button btnExportCsv;
    private TextView tvReportPending, tvReportInProgress, tvReportCompleted, tvReportTotal;
    private TextView tvReportByDepartment, tvReportByEmployee, tvReportByJobType;
    private ProgressBar reportsProgressBar;

    private JSONObject rawReportData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        sessionManager = new SessionManager(this);

        initViews();
        loadReports();
    }

    private void initViews() {
        btnBackReports = findViewById(R.id.btnBackReports);
        btnExportCsv = findViewById(R.id.btnExportCsv);
        tvReportPending = findViewById(R.id.tvReportPending);
        tvReportInProgress = findViewById(R.id.tvReportInProgress);
        tvReportCompleted = findViewById(R.id.tvReportCompleted);
        tvReportTotal = findViewById(R.id.tvReportTotal);
        tvReportByDepartment = findViewById(R.id.tvReportByDepartment);
        tvReportByEmployee = findViewById(R.id.tvReportByEmployee);
        tvReportByJobType = findViewById(R.id.tvReportByJobType);
        reportsProgressBar = findViewById(R.id.reportsProgressBar);

        btnBackReports.setOnClickListener(v -> finish());
        btnExportCsv.setOnClickListener(v -> exportCsv());
    }

    private void loadReports() {
        reportsProgressBar.setVisibility(View.VISIBLE);

        JSONObject params = new JSONObject();
        try {
            params.put("action", "get_reports");
            params.put("email", sessionManager.getUserEmail());
            params.put("role", sessionManager.getUserRole());
        } catch (Exception ignored) {}

        ApiClient.post(sessionManager.getBackendUrl(), params, new ApiCallback() {
            @Override
            public void onSuccess(ApiResponse response) {
                reportsProgressBar.setVisibility(View.GONE);
                if (response.isSuccess() && response.getData() != null) {
                    rawReportData = response.getData();
                    displayReport(response.getData());
                } else {
                    Toast.makeText(ReportsActivity.this, response.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                reportsProgressBar.setVisibility(View.GONE);
                Toast.makeText(ReportsActivity.this, "Failed to load reports: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayReport(JSONObject data) {
        int total = data.optInt("totalTasks", 0);
        tvReportTotal.setText("Total Recorded Tasks: " + total);

        JSONObject byStatus = data.optJSONObject("byStatus");
        if (byStatus != null) {
            tvReportPending.setText("Pending Tasks: " + byStatus.optInt("Pending", 0));
            tvReportInProgress.setText("In Progress Tasks: " + byStatus.optInt("In Progress", 0));
            tvReportCompleted.setText("Completed Tasks: " + byStatus.optInt("Completed", 0));
        }

        JSONObject byDept = data.optJSONObject("byDepartment");
        if (byDept != null) {
            StringBuilder sb = new StringBuilder();
            Iterator<String> keys = byDept.keys();
            while (keys.hasNext()) {
                String k = keys.next();
                sb.append("• ").append(k).append(": ").append(byDept.optInt(k, 0)).append(" tasks\n");
            }
            tvReportByDepartment.setText(sb.toString().trim());
        }

        JSONObject byEmp = data.optJSONObject("byEmployee");
        if (byEmp != null) {
            StringBuilder sb = new StringBuilder();
            Iterator<String> keys = byEmp.keys();
            while (keys.hasNext()) {
                String k = keys.next();
                sb.append("• ").append(k).append(": ").append(byEmp.optInt(k, 0)).append(" tasks\n");
            }
            tvReportByEmployee.setText(sb.toString().trim());
        }

        JSONObject byJob = data.optJSONObject("byJobType");
        if (byJob != null) {
            StringBuilder sb = new StringBuilder();
            Iterator<String> keys = byJob.keys();
            while (keys.hasNext()) {
                String k = keys.next();
                sb.append("• ").append(k).append(": ").append(byJob.optInt(k, 0)).append(" tasks\n");
            }
            tvReportByJobType.setText(sb.toString().trim());
        }
    }

    private void exportCsv() {
        if (rawReportData == null) {
            Toast.makeText(this, "No report data to export", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            File exportDir = new File(getExternalFilesDir(null), "reports");
            if (!exportDir.exists()) exportDir.mkdirs();
            File file = new File(exportDir, "GMS_Task_Report_" + System.currentTimeMillis() + ".csv");

            FileWriter writer = new FileWriter(file);
            writer.append("Category,Name,Count\n");

            JSONObject byStatus = rawReportData.optJSONObject("byStatus");
            if (byStatus != null) {
                Iterator<String> keys = byStatus.keys();
                while (keys.hasNext()) {
                    String k = keys.next();
                    writer.append("Status,").append(k).append(",").append(String.valueOf(byStatus.optInt(k, 0))).append("\n");
                }
            }

            JSONObject byDept = rawReportData.optJSONObject("byDepartment");
            if (byDept != null) {
                Iterator<String> keys = byDept.keys();
                while (keys.hasNext()) {
                    String k = keys.next();
                    writer.append("Department,\"").append(k).append("\",").append(String.valueOf(byDept.optInt(k, 0))).append("\n");
                }
            }

            JSONObject byEmp = rawReportData.optJSONObject("byEmployee");
            if (byEmp != null) {
                Iterator<String> keys = byEmp.keys();
                while (keys.hasNext()) {
                    String k = keys.next();
                    writer.append("Employee,\"").append(k).append("\",").append(String.valueOf(byEmp.optInt(k, 0))).append("\n");
                }
            }

            writer.flush();
            writer.close();

            Toast.makeText(this, "Report exported to: " + file.getName(), Toast.LENGTH_LONG).show();

            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.setType("text/csv");
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
            sendIntent.putExtra(Intent.EXTRA_STREAM, uri);
            sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(sendIntent, "Share GMS Task Report CSV"));

        } catch (Exception e) {
            Toast.makeText(this, "Export error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
