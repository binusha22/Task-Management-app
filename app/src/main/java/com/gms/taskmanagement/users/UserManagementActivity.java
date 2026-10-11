package com.gms.taskmanagement.users;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.gms.taskmanagement.R;
import com.gms.taskmanagement.adapters.UserAdapter;
import com.gms.taskmanagement.api.ApiClient;
import com.gms.taskmanagement.api.ApiResponse;
import com.gms.taskmanagement.api.ApiCallback;
import com.gms.taskmanagement.auth.SessionManager;
import com.gms.taskmanagement.models.User;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class UserManagementActivity extends AppCompatActivity {
    private SessionManager sessionManager;
    private ImageView btnBackUsers;
    private EditText etSearchUsers;
    private SwipeRefreshLayout usersSwipeRefresh;
    private RecyclerView rvUsers;
    private ProgressBar usersProgressBar;
    private FloatingActionButton fabAddUser;

    private UserAdapter userAdapter;
    private final List<User> allUsers = new ArrayList<>();
    private final List<User> filteredUsers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_management);

        sessionManager = new SessionManager(this);
        if (!sessionManager.isAdmin()) {
            Toast.makeText(this, "Admin access required", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupRecyclerView();
        setupSearch();
        loadUsers();
    }

    private void initViews() {
        btnBackUsers = findViewById(R.id.btnBackUsers);
        etSearchUsers = findViewById(R.id.etSearchUsers);
        usersSwipeRefresh = findViewById(R.id.usersSwipeRefresh);
        rvUsers = findViewById(R.id.rvUsers);
        usersProgressBar = findViewById(R.id.usersProgressBar);
        fabAddUser = findViewById(R.id.fabAddUser);

        btnBackUsers.setOnClickListener(v -> finish());
        fabAddUser.setOnClickListener(v -> showCreateUserDialog());
        usersSwipeRefresh.setOnRefreshListener(this::loadUsers);
    }

    private void setupRecyclerView() {
        userAdapter = new UserAdapter(this, this::showUserOptionsDialog);
        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        rvUsers.setAdapter(userAdapter);
    }

    private void setupSearch() {
        etSearchUsers.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterUsers(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadUsers() {
        usersProgressBar.setVisibility(View.VISIBLE);

        JSONObject params = new JSONObject();
        try {
            params.put("action", "get_users");
            params.put("email", sessionManager.getUserEmail());
            params.put("role", "Admin");
        } catch (Exception ignored) {}

        ApiClient.post(sessionManager.getBackendUrl(), params, new ApiCallback() {
            @Override
            public void onSuccess(ApiResponse response) {
                usersProgressBar.setVisibility(View.GONE);
                usersSwipeRefresh.setRefreshing(false);

                if (response.isSuccess()) {
                    allUsers.clear();
                    try {
                        JSONObject json = new JSONObject(response.getRawJson());
                        JSONArray arr = json.optJSONArray("data");
                        if (arr != null) {
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject obj = arr.getJSONObject(i);
                                User u = new User();
                                u.setFirstName(obj.optString("firstName", ""));
                                u.setLastName(obj.optString("lastName", ""));
                                u.setRole(obj.optString("role", "User"));
                                u.setEmail(obj.optString("email", ""));
                                allUsers.add(u);
                            }
                        }
                    } catch (Exception ignored) {}

                    filterUsers(etSearchUsers.getText().toString());
                } else {
                    Toast.makeText(UserManagementActivity.this, response.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                usersProgressBar.setVisibility(View.GONE);
                usersSwipeRefresh.setRefreshing(false);
                Toast.makeText(UserManagementActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterUsers(String query) {
        filteredUsers.clear();
        String q = query.trim().toLowerCase();

        for (User u : allUsers) {
            if (q.isEmpty() ||
                    u.getFullName().toLowerCase().contains(q) ||
                    u.getEmail().toLowerCase().contains(q) ||
                    u.getRole().toLowerCase().contains(q)) {
                filteredUsers.add(u);
            }
        }
        userAdapter.setUsers(filteredUsers);
    }

    private void showCreateUserDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_user, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        EditText etFirst = dialogView.findViewById(R.id.etNewUserFirstName);
        EditText etLast = dialogView.findViewById(R.id.etNewUserLastName);
        EditText etEmail = dialogView.findViewById(R.id.etNewUserEmail);
        EditText etPass = dialogView.findViewById(R.id.etNewUserPassword);
        Spinner spRole = dialogView.findViewById(R.id.spNewUserRole);
        Button btnCancel = dialogView.findViewById(R.id.btnCancelCreateUser);
        Button btnConfirm = dialogView.findViewById(R.id.btnConfirmCreateUser);

        String[] roles = {"User", "Admin"};
        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, roles);
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spRole.setAdapter(roleAdapter);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String pass = etPass.getText().toString().trim();
            String fName = etFirst.getText().toString().trim();
            String lName = etLast.getText().toString().trim();
            String role = spRole.getSelectedItem().toString();

            if (email.isEmpty()) {
                etEmail.setError("Email required");
                return;
            }
            if (pass.isEmpty()) {
                etPass.setError("Password required");
                return;
            }

            dialog.dismiss();
            createUser(fName, lName, email, pass, role);
        });

        dialog.show();
    }

    private void createUser(String fName, String lName, String email, String pass, String role) {
        usersProgressBar.setVisibility(View.VISIBLE);

        JSONObject userObj = new JSONObject();
        try {
            userObj.put("firstName", fName);
            userObj.put("lastName", lName);
            userObj.put("email", email);
            userObj.put("password", pass);
            userObj.put("role", role);
        } catch (Exception ignored) {}

        JSONObject params = new JSONObject();
        try {
            params.put("action", "create_user");
            params.put("email", sessionManager.getUserEmail());
            params.put("role", "Admin");
            params.put("newUser", userObj);
        } catch (Exception ignored) {}

        ApiClient.post(sessionManager.getBackendUrl(), params, new ApiCallback() {
            @Override
            public void onSuccess(ApiResponse response) {
                usersProgressBar.setVisibility(View.GONE);
                Toast.makeText(UserManagementActivity.this, response.getMessage(), Toast.LENGTH_SHORT).show();
                if (response.isSuccess()) {
                    loadUsers();
                }
            }

            @Override
            public void onError(String errorMessage) {
                usersProgressBar.setVisibility(View.GONE);
                Toast.makeText(UserManagementActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showUserOptionsDialog(User user) {
        String[] options = {"Change Role (Admin/User)", "Reset Password"};
        new AlertDialog.Builder(this)
                .setTitle(user.getFullName() + " (" + user.getRole() + ")")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        toggleUserRole(user);
                    } else if (which == 1) {
                        showResetPasswordDialog(user);
                    }
                })
                .show();
    }

    private void toggleUserRole(User user) {
        if (user.getEmail().equalsIgnoreCase(sessionManager.getUserEmail())) {
            Toast.makeText(this, "Cannot change your own role", Toast.LENGTH_SHORT).show();
            return;
        }

        String newRole = user.isAdmin() ? "User" : "Admin";
        usersProgressBar.setVisibility(View.VISIBLE);

        JSONObject targetUser = new JSONObject();
        try {
            targetUser.put("email", user.getEmail());
            targetUser.put("role", newRole);
        } catch (Exception ignored) {}

        JSONObject params = new JSONObject();
        try {
            params.put("action", "update_user");
            params.put("email", sessionManager.getUserEmail());
            params.put("role", "Admin");
            params.put("targetUser", targetUser);
        } catch (Exception ignored) {}

        ApiClient.post(sessionManager.getBackendUrl(), params, new ApiCallback() {
            @Override
            public void onSuccess(ApiResponse response) {
                usersProgressBar.setVisibility(View.GONE);
                Toast.makeText(UserManagementActivity.this, "Role updated to " + newRole, Toast.LENGTH_SHORT).show();
                loadUsers();
            }

            @Override
            public void onError(String errorMessage) {
                usersProgressBar.setVisibility(View.GONE);
                Toast.makeText(UserManagementActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showResetPasswordDialog(User user) {
        EditText etPass = new EditText(this);
        etPass.setHint("Enter new password");
        etPass.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);

        new AlertDialog.Builder(this)
                .setTitle("Reset Password for " + user.getEmail())
                .setView(etPass)
                .setPositiveButton("Reset", (d, w) -> {
                    String newPass = etPass.getText().toString().trim();
                    if (!newPass.isEmpty()) {
                        executePasswordReset(user.getEmail(), newPass);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void executePasswordReset(String email, String newPass) {
        usersProgressBar.setVisibility(View.VISIBLE);

        JSONObject targetUser = new JSONObject();
        try {
            targetUser.put("email", email);
            targetUser.put("password", newPass);
        } catch (Exception ignored) {}

        JSONObject params = new JSONObject();
        try {
            params.put("action", "update_user");
            params.put("email", sessionManager.getUserEmail());
            params.put("role", "Admin");
            params.put("targetUser", targetUser);
        } catch (Exception ignored) {}

        ApiClient.post(sessionManager.getBackendUrl(), params, new ApiCallback() {
            @Override
            public void onSuccess(ApiResponse response) {
                usersProgressBar.setVisibility(View.GONE);
                Toast.makeText(UserManagementActivity.this, "Password reset successfully", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String errorMessage) {
                usersProgressBar.setVisibility(View.GONE);
                Toast.makeText(UserManagementActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
