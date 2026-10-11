package com.gms.taskmanagement.api;

public interface ApiCallback {
    void onSuccess(ApiResponse response);
    void onError(String errorMessage);
}
