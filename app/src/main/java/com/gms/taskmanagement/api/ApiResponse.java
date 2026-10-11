package com.gms.taskmanagement.api;

import org.json.JSONObject;

public class ApiResponse {
    private final boolean success;
    private final String message;
    private final JSONObject data;
    private final String rawJson;

    public ApiResponse(boolean success, String message, JSONObject data, String rawJson) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.rawJson = rawJson;
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public JSONObject getData() { return data; }
    public String getRawJson() { return rawJson; }
}
