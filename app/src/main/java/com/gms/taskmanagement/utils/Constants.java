package com.gms.taskmanagement.utils;

public class Constants {
    public static final String PREF_NAME = "gms_task_prefs";
    public static final String KEY_BACKEND_URL = "backend_url";
    public static final String KEY_USERS_SHEET = "users_sheet_name";
    public static final String KEY_TASKS_SHEET = "tasks_sheet_name";
    public static final String KEY_FCM_SERVER_KEY = "fcm_server_key";
    public static final String KEY_USER_EMAIL = "user_email";
    public static final String KEY_USER_ROLE = "user_role";
    public static final String KEY_USER_NAME = "user_full_name";
    public static final String KEY_USER_TOKEN = "user_token";
    public static final String KEY_IS_LOGGED_IN = "is_logged_in";
    public static final String KEY_FCM_TOKEN = "fcm_token";

    // Default values baked in — no setup screen required
    public static final String DEFAULT_BACKEND_URL =
            "https://script.google.com/macros/s/AKfycbzbPpfyyCUz94Hwu2SrTM3gz0qTcWrRGA69QSnMlcQ3V3BUhCkjO4srzepI732iHO6x/exec";
    public static final String DEFAULT_USERS_SHEET  = "Users";
    public static final String DEFAULT_TASKS_SHEET  = "Form Responses 1";
    public static final String DEFAULT_FCM_SERVER_KEY = "";   // fill in Settings if needed

    public static final String ROLE_ADMIN = "Admin";
    public static final String ROLE_USER  = "User";

    public static final String STATUS_PENDING     = "Pending";
    public static final String STATUS_IN_PROGRESS = "In Progress";
    public static final String STATUS_COMPLETED   = "Completed";
    public static final String STATUS_REJECTED    = "Rejected";
}
