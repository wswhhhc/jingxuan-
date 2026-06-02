package com.jingxuan.modules.runtime.service.impl;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

@Component
class RuntimeHealthProbe {

    boolean waitForEndpoint(String url, int maxAttempts, long sleepMillis) {
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setConnectTimeout(1000);
                connection.setReadTimeout(1000);
                connection.setRequestMethod("GET");
                int code = connection.getResponseCode();
                if (code >= 200 && code < 500) {
                    return true;
                }
            } catch (IOException ignored) {
                // continue polling
            }

            try {
                Thread.sleep(sleepMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }
}
