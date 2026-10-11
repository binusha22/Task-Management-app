package com.gms.taskmanagement.notifications;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.gms.taskmanagement.R;
import com.gms.taskmanagement.adapters.NotificationAdapter;
import com.gms.taskmanagement.api.ApiClient;
import com.gms.taskmanagement.api.ApiResponse;
import com.gms.taskmanagement.api.ApiCallback;
import com.gms.taskmanagement.auth.SessionManager;
import com.gms.taskmanagement.models.AppNotification;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class NotificationActivity extends AppCompatActivity {
    private SessionManager sessionManager;
    private ImageView btnBackNotif;
    private SwipeRefreshLayout notifSwipeRefresh;
    private RecyclerView rvNotifications;
    private TextView tvEmptyNotifs;
    private ProgressBar notifProgressBar;

    private NotificationAdapter adapter;
    private final List<AppNotification> notificationList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        sessionManager = new SessionManager(this);

        initViews();
        setupRecyclerView();
        loadNotifications();
    }

    private void initViews() {
        btnBackNotif = findViewById(R.id.btnBackNotif);
        notifSwipeRefresh = findViewById(R.id.notifSwipeRefresh);
        rvNotifications = findViewById(R.id.rvNotifications);
        tvEmptyNotifs = findViewById(R.id.tvEmptyNotifs);
        notifProgressBar = findViewById(R.id.notifProgressBar);

        btnBackNotif.setOnClickListener(v -> finish());
        notifSwipeRefresh.setOnRefreshListener(this::loadNotifications);
    }

    private void setupRecyclerView() {
        adapter = new NotificationAdapter(this, this::onNotificationClick);
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        rvNotifications.setAdapter(adapter);
    }

    private void loadNotifications() {
        notifProgressBar.setVisibility(View.VISIBLE);

        JSONObject params = new JSONObject();
        try {
            params.put("action", "get_notifications");
            params.put("email", sessionManager.getUserEmail());
        } catch (Exception ignored) {}

        ApiClient.post(sessionManager.getBackendUrl(), params, new ApiCallback() {
            @Override
            public void onSuccess(ApiResponse response) {
                notifProgressBar.setVisibility(View.GONE);
                notifSwipeRefresh.setRefreshing(false);

                if (response.isSuccess()) {
                    notificationList.clear();
                    try {
                        JSONObject json = new JSONObject(response.getRawJson());
                        JSONArray arr = json.optJSONArray("data");
                        if (arr != null) {
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject obj = arr.getJSONObject(i);
                                AppNotification n = new AppNotification();
                                n.setId(obj.optString("id", ""));
                                n.setRecipient(obj.optString("recipient", ""));
                                n.setTitle(obj.optString("title", ""));
                                n.setMessage(obj.optString("message", ""));
                                n.setTimestamp(obj.optString("timestamp", ""));
                                n.setRead(obj.optBoolean("isRead", false));
                                n.setTicketNumber(obj.optString("ticketNumber", ""));
                                notificationList.add(n);
                            }
                        }
                    } catch (Exception ignored) {}

                    adapter.setNotifications(notificationList);
                    tvEmptyNotifs.setVisibility(notificationList.isEmpty() ? View.VISIBLE : View.GONE);
                } else {
                    Toast.makeText(NotificationActivity.this, response.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                notifProgressBar.setVisibility(View.GONE);
                notifSwipeRefresh.setRefreshing(false);
                Toast.makeText(NotificationActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onNotificationClick(AppNotification notif) {
        if (!notif.isRead()) {
            JSONObject params = new JSONObject();
            try {
                params.put("action", "mark_notification_read");
                params.put("email", sessionManager.getUserEmail());
                params.put("notificationId", notif.getId());
            } catch (Exception ignored) {}

            ApiClient.post(sessionManager.getBackendUrl(), params, new ApiCallback() {
                @Override
                public void onSuccess(ApiResponse response) {
                    notif.setRead(true);
                    adapter.notifyDataSetChanged();
                }
                @Override
                public void onError(String errorMessage) {}
            });
        }

        if (notif.getTicketNumber() != null && !notif.getTicketNumber().isEmpty()) {
            Toast.makeText(this, "Task: " + notif.getTicketNumber(), Toast.LENGTH_SHORT).show();
        }
    }
}
