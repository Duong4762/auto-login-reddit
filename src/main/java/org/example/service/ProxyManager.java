package org.example.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.config.AppConfig;
import org.example.model.TmProxyResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProxyManager {
    private static final Logger log = LoggerFactory.getLogger(ProxyManager.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern RETRY_PATTERN = Pattern.compile("retry after (\\d+) second");

    private final AppConfig config;
    private final HttpService httpService;
    private final AtomicInteger keyCursor = new AtomicInteger(0);

    public ProxyManager(AppConfig config) {
        this.config = config;
        this.httpService = new HttpService(20000);
    }

    public String nextProxy() {
        List<String> apiKeys = config.getTmproxy().getApiKeys();
        if (apiKeys == null || apiKeys.isEmpty()) {
            log.warn("tmproxy.api-keys is empty");
            return null;
        }

        int startIndex = keyCursor.getAndIncrement();
        for (int attempt = 0; attempt < apiKeys.size(); attempt++) {
            String apiKey = apiKeys.get((startIndex + attempt) % apiKeys.size());
            String proxy = getProxyFromKey(apiKey);
            if (proxy != null) {
                return proxy;
            }
        }
        return null;
    }

    private String getProxyFromKey(String apiKey) {
        int location = config.getTmproxy().getLocation();
        if (location == 5) {
            location = 2;
        }
        String payload = String.format(
                "{\"api_key\":\"%s\",\"id_location\":%d,\"id_isp\":%d}",
                apiKey, location, config.getTmproxy().getIsp()
        );

        try {
            String raw = httpService.postJson(
                    "https://tmproxy.com/api/proxy/get-new-proxy",
                    payload,
                    Map.of("Content-Type", "application/json", "Accept", "application/json")
            );
            TmProxyResponse response = MAPPER.readValue(raw, TmProxyResponse.class);
            if (response.getCode() == 0 && response.getData() != null) {
                return response.getData().getHttps() + ":" + response.getData().getUsername() + ":" + response.getData().getPassword();
            }

            if (response.getCode() == 5) {
                long waitMs = (extractRetrySeconds(response.getMessage()) + 10L) * 1000L;
                log.warn("Proxy key {} rate-limited, wait {}ms", apiKey, waitMs);
                Thread.sleep(waitMs);
                return getProxyFromKey(apiKey);
            }
            log.warn("Get proxy failed for key {}: {}", apiKey, response.getMessage());
            return null;
        } catch (Exception ex) {
            log.error("Cannot get proxy from TMProxy key {}", apiKey, ex);
            return null;
        }
    }

    private int extractRetrySeconds(String message) {
        if (message == null) {
            return 0;
        }
        Matcher matcher = RETRY_PATTERN.matcher(message);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return 0;
    }
}
