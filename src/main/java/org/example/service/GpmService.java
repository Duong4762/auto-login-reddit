package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.config.AppConfig;
import org.example.model.GpmOpenResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class GpmService {
    private static final Logger log = LoggerFactory.getLogger(GpmService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String PREFIX = "reddit-";

    private final AppConfig config;
    private final HttpService httpService;

    public GpmService(AppConfig config) {
        this.config = config;
        this.httpService = new HttpService(20000);
    }

    public GpmOpenResult openOrCreateProfile(String profileName, String proxy) {
        GpmOpenResult openResult = openProfile(profileName, proxy);
        if (openResult.success()) {
            return openResult;
        }
        if (!"PROFILE_NOT_FOUND".equals(openResult.message())) {
            return openResult;
        }

        boolean created = createProfile(profileName);
        if (!created) {
            return new GpmOpenResult(false, "CREATE_PROFILE_FAILED", null);
        }
        sleep(3000);
        return openProfile(profileName, proxy);
    }

    public GpmOpenResult openProfile(String profileName, String proxy) {
        try {
            String profileId = getProfileIdByName(PREFIX + profileName);
            if (profileId == null) {
                return new GpmOpenResult(false, "PROFILE_NOT_FOUND", null);
            }
            if (proxy != null && !proxy.isBlank()) {
                updateProfile(profileName, proxy, profileId);
            }

            String url = config.getGpm().getAddress() + "/api/v3/profiles/start/" + profileId + "?win_size=1920,1080&win_scale=1";
            String raw = httpService.get(url, Map.of("Accept", "application/json"));
            JsonNode root = MAPPER.readTree(raw);
            boolean success = root.path("success").asBoolean(false);
            if (!success) {
                return new GpmOpenResult(false, root.path("message").asText("GPM_OPEN_FAILED"), null);
            }
            String remoteDebugAddress = root.path("data").path("remote_debugging_address").asText("");
            if (remoteDebugAddress.isBlank()) {
                return new GpmOpenResult(false, "EMPTY_REMOTE_DEBUG_ADDRESS", null);
            }
            return new GpmOpenResult(true, "SUCCESS", remoteDebugAddress);
        } catch (Exception ex) {
            log.error("Open GPM profile failed for {}", profileName, ex);
            return new GpmOpenResult(false, "GPM_OPEN_FAILED", null);
        }
    }

    public boolean closeProfile(String profileName) {
        try {
            String profileId = getProfileIdByName(PREFIX + profileName);
            if (profileId == null) {
                return false;
            }
            String url = config.getGpm().getAddress() + "/api/v3/profiles/close/" + profileId;
            String raw = httpService.get(url, Map.of("Accept", "application/json"));
            JsonNode root = MAPPER.readTree(raw);
            return root.path("success").asBoolean(false);
        } catch (Exception ex) {
            log.warn("Close GPM profile failed for {}", profileName, ex);
            return false;
        }
    }

    private boolean createProfile(String profileName) {
        try {
            String payload = MAPPER.writeValueAsString(Map.of(
                    "profile_name", PREFIX + profileName,
                    "browser_core", config.getGpm().getBrowserCore(),
                    "browser_version", config.getGpm().getBrowserVersion(),
                    "startup_urls", config.getGpm().getStartupUrl()
            ));
            String url = config.getGpm().getAddress() + "/api/v3/profiles/create";
            String raw = httpService.postJson(url, payload, Map.of("Content-Type", "application/json"));
            JsonNode root = MAPPER.readTree(raw);
            return root.path("success").asBoolean(false);
        } catch (Exception ex) {
            log.error("Create GPM profile failed for {}", profileName, ex);
            return false;
        }
    }

    private void updateProfile(String profileName, String proxy, String profileId) {
        try {
            String payload = MAPPER.writeValueAsString(Map.of(
                    "profile_name", PREFIX + profileName,
                    "browser_core", config.getGpm().getBrowserCore(),
                    "browser_version", config.getGpm().getBrowserVersion(),
                    "startup_urls", config.getGpm().getStartupUrl(),
                    "raw_proxy", proxy
            ));
            String url = config.getGpm().getAddress() + "/api/v3/profiles/update/" + profileId;
            httpService.postJson(url, payload, Map.of("Content-Type", "application/json"));
        } catch (Exception ex) {
            log.warn("Update profile proxy failed for {}", profileName, ex);
        }
    }

    private String getProfileIdByName(String profileName) {
        try {
            String encoded = URLEncoder.encode(profileName, StandardCharsets.UTF_8);
            String url = config.getGpm().getAddress() + "/api/v3/profiles?search=" + encoded + "&page=1&per_page=1";
            String raw = httpService.get(url, Map.of("Accept", "application/json"));
            JsonNode root = MAPPER.readTree(raw);
            if (!root.path("success").asBoolean(false)) {
                return null;
            }
            JsonNode data = root.path("data");
            if (!data.isArray() || data.isEmpty()) {
                return null;
            }
            return data.get(0).path("id").asText(null);
        } catch (Exception ex) {
            log.error("Get profile id by name failed: {}", profileName, ex);
            return null;
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
