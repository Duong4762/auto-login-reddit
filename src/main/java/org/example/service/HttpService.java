package org.example.service;

import okhttp3.*;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

public class HttpService {
    private final OkHttpClient client;

    public HttpService(int timeoutMs) {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofMillis(timeoutMs))
                .readTimeout(Duration.ofMillis(timeoutMs))
                .writeTimeout(Duration.ofMillis(timeoutMs))
                .build();
    }

    public String get(String url, Map<String, String> headers) throws IOException {
        Request.Builder builder = new Request.Builder().url(url);
        if (headers != null) {
            headers.forEach(builder::addHeader);
        }
        try (Response response = client.newCall(builder.build()).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("GET " + url + " failed with status " + response.code());
            }
            ResponseBody body = response.body();
            return body == null ? "" : body.string();
        }
    }

    public String postJson(String url, String jsonBody, Map<String, String> headers) throws IOException {
        RequestBody body = RequestBody.create(
                jsonBody,
                MediaType.parse("application/json; charset=utf-8")
        );
        Request.Builder builder = new Request.Builder().url(url).post(body);
        if (headers != null) {
            headers.forEach(builder::addHeader);
        }
        try (Response response = client.newCall(builder.build()).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("POST " + url + " failed with status " + response.code());
            }
            ResponseBody responseBody = response.body();
            return responseBody == null ? "" : responseBody.string();
        }
    }
}
