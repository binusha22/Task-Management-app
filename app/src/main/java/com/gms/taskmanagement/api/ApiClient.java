package com.gms.taskmanagement.api;

import android.os.Handler;
import android.os.Looper;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ApiClient {
    private static final int TIMEOUT_MS = 25000;
    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static void post(String urlString, JSONObject jsonBody, ApiCallback callback) {
        executor.execute(() -> {
            try {
                String responseBody = executePostWithRedirects(urlString, jsonBody.toString());
                JSONObject jsonResponse = new JSONObject(responseBody);
                boolean success = jsonResponse.optBoolean("success", false);
                String message = jsonResponse.optString("message", "");
                JSONObject data = jsonResponse.optJSONObject("data");

                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onSuccess(new ApiResponse(success, message, data, responseBody));
                    }
                });
            } catch (Exception e) {
                final String errorMsg = (e.getMessage() != null && !e.getMessage().isEmpty()) 
                        ? e.getMessage() 
                        : "Network connection failed. Please check internet connection.";
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onError(errorMsg);
                    }
                });
            }
        });
    }

    public static void get(String urlString, ApiCallback callback) {
        executor.execute(() -> {
            try {
                String responseBody = executeGetWithRedirects(urlString);
                JSONObject jsonResponse = new JSONObject(responseBody);
                boolean success = jsonResponse.optBoolean("success", false);
                String message = jsonResponse.optString("message", "");
                JSONObject data = jsonResponse.optJSONObject("data");

                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onSuccess(new ApiResponse(success, message, data, responseBody));
                    }
                });
            } catch (Exception e) {
                final String errorMsg = (e.getMessage() != null && !e.getMessage().isEmpty()) 
                        ? e.getMessage() 
                        : "Connection failed";
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onError(errorMsg);
                    }
                });
            }
        });
    }

    private static String executePostWithRedirects(String targetUrl, String body) throws Exception {
        URL url = new URL(targetUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(TIMEOUT_MS);
        conn.setDoOutput(true);
        conn.setInstanceFollowRedirects(false);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = body.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int status = conn.getResponseCode();
        if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM 
                || status == HttpURLConnection.HTTP_SEE_OTHER || status == 307) {
            String redirectUrl = conn.getHeaderField("Location");
            conn.disconnect();
            if (redirectUrl != null) {
                return executeGetWithRedirects(redirectUrl);
            }
        }

        return readStream(conn.getInputStream());
    }

    private static String executeGetWithRedirects(String targetUrl) throws Exception {
        int redirects = 0;
        String currentUrl = targetUrl;

        while (redirects < 5) {
            URL url = new URL(currentUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setInstanceFollowRedirects(false);

            int status = conn.getResponseCode();
            if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM 
                    || status == HttpURLConnection.HTTP_SEE_OTHER || status == 307) {
                currentUrl = conn.getHeaderField("Location");
                conn.disconnect();
                redirects++;
                continue;
            }

            InputStream is = (status >= 400) ? conn.getErrorStream() : conn.getInputStream();
            return readStream(is);
        }

        throw new Exception("Too many redirects");
    }

    private static String readStream(InputStream is) throws Exception {
        if (is == null) return "";
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }
}
