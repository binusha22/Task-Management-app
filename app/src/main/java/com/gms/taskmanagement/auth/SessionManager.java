package com.gms.taskmanagement.auth;

import android.content.Context;
import android.content.SharedPreferences;
import com.gms.taskmanagement.utils.Constants;

public class SessionManager {
    private final SharedPreferences prefs;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
        seedDefaults();
    }

    /**
     * Seeds hardcoded defaults into SharedPreferences the first time the app runs.
     * If a value is already stored (user edited it via Settings), it is kept.
     */
    private void seedDefaults() {
        if (!prefs.contains(Constants.KEY_BACKEND_URL)) {
            editor.putString(Constants.KEY_BACKEND_URL, Constants.DEFAULT_BACKEND_URL);
        }
        if (!prefs.contains(Constants.KEY_USERS_SHEET)) {
            editor.putString(Constants.KEY_USERS_SHEET, Constants.DEFAULT_USERS_SHEET);
        }
        if (!prefs.contains(Constants.KEY_TASKS_SHEET)) {
            editor.putString(Constants.KEY_TASKS_SHEET, Constants.DEFAULT_TASKS_SHEET);
        }
        if (!prefs.contains(Constants.KEY_FCM_SERVER_KEY)) {
            editor.putString(Constants.KEY_FCM_SERVER_KEY, Constants.DEFAULT_FCM_SERVER_KEY);
        }
        editor.apply();
    }

    // ── Backend / Sheet config ────────────────────────────────────────────────

    public void saveBackendConfig(String backendUrl, String usersSheet, String tasksSheet) {
        editor.putString(Constants.KEY_BACKEND_URL, backendUrl);
        editor.putString(Constants.KEY_USERS_SHEET, usersSheet);
        editor.putString(Constants.KEY_TASKS_SHEET, tasksSheet);
        editor.apply();
    }

    /** Always true after seedDefaults(); kept for backward compatibility. */
    public boolean isConfigured() {
        String url = prefs.getString(Constants.KEY_BACKEND_URL, null);
        return url != null && !url.trim().isEmpty();
    }

    public String getBackendUrl() {
        return prefs.getString(Constants.KEY_BACKEND_URL, Constants.DEFAULT_BACKEND_URL);
    }

    public String getUsersSheetName() {
        return prefs.getString(Constants.KEY_USERS_SHEET, Constants.DEFAULT_USERS_SHEET);
    }

    public String getTasksSheetName() {
        return prefs.getString(Constants.KEY_TASKS_SHEET, Constants.DEFAULT_TASKS_SHEET);
    }

    // ── Firebase credentials ──────────────────────────────────────────────────

    public String getFcmServerKey() {
        return prefs.getString(Constants.KEY_FCM_SERVER_KEY, Constants.DEFAULT_FCM_SERVER_KEY);
    }

    public void saveFcmServerKey(String key) {
        editor.putString(Constants.KEY_FCM_SERVER_KEY, key);
        editor.apply();
    }

    // ── Session ───────────────────────────────────────────────────────────────

    public void createLoginSession(String email, String role, String fullName, String token) {
        editor.putBoolean(Constants.KEY_IS_LOGGED_IN, true);
        editor.putString(Constants.KEY_USER_EMAIL, email);
        editor.putString(Constants.KEY_USER_ROLE, role);
        editor.putString(Constants.KEY_USER_NAME, fullName);
        editor.putString(Constants.KEY_USER_TOKEN, token);
        editor.apply();
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(Constants.KEY_IS_LOGGED_IN, false);
    }

    public String getUserEmail() {
        return prefs.getString(Constants.KEY_USER_EMAIL, "");
    }

    public String getUserRole() {
        return prefs.getString(Constants.KEY_USER_ROLE, Constants.ROLE_USER);
    }

    public boolean isAdmin() {
        return Constants.ROLE_ADMIN.equalsIgnoreCase(getUserRole());
    }

    public String getUserFullName() {
        return prefs.getString(Constants.KEY_USER_NAME, "User");
    }

    public String getUserToken() {
        return prefs.getString(Constants.KEY_USER_TOKEN, "");
    }

    public void updateFullName(String fullName) {
        editor.putString(Constants.KEY_USER_NAME, fullName);
        editor.apply();
    }

    public void logout() {
        editor.remove(Constants.KEY_IS_LOGGED_IN);
        editor.remove(Constants.KEY_USER_EMAIL);
        editor.remove(Constants.KEY_USER_ROLE);
        editor.remove(Constants.KEY_USER_NAME);
        editor.remove(Constants.KEY_USER_TOKEN);
        editor.apply();
    }
}

