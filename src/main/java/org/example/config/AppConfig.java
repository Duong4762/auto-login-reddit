package org.example.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class AppConfig {
    private DbConfig db = new DbConfig();
    private BotConfig bot = new BotConfig();
    private TmProxyConfig tmproxy = new TmProxyConfig();
    private GpmConfig gpm = new GpmConfig();
    private SecurityConfig security = new SecurityConfig();
    private ActionConfig action = new ActionConfig();

    public static AppConfig load() {
        try (InputStream inputStream = AppConfig.class.getClassLoader().getResourceAsStream("application.yml")) {
            if (inputStream == null) {
                throw new IllegalStateException("Missing application.yml in resources");
            }
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            return mapper.readValue(inputStream, AppConfig.class);
        } catch (Exception ex) {
            throw new RuntimeException("Cannot load application.yml", ex);
        }
    }

    public BotConfig getBot() {
        return bot;
    }

    public void setBot(BotConfig bot) {
        this.bot = bot;
    }

    public DbConfig getDb() {
        return db;
    }

    public void setDb(DbConfig db) {
        this.db = db;
    }

    public TmProxyConfig getTmproxy() {
        return tmproxy;
    }

    public void setTmproxy(TmProxyConfig tmproxy) {
        this.tmproxy = tmproxy;
    }

    public GpmConfig getGpm() {
        return gpm;
    }

    public void setGpm(GpmConfig gpm) {
        this.gpm = gpm;
    }

    public SecurityConfig getSecurity() {
        return security;
    }

    public void setSecurity(SecurityConfig security) {
        this.security = security;
    }

    public ActionConfig getAction() {
        return action;
    }

    public void setAction(ActionConfig action) {
        this.action = action;
    }

    public static class BotConfig {
        private long pollIntervalMs = 30000;
        private String groupCode = "";

        public long getPollIntervalMs() {
            return pollIntervalMs;
        }

        public void setPollIntervalMs(long pollIntervalMs) {
            this.pollIntervalMs = pollIntervalMs;
        }

        public String getGroupCode() {
            return groupCode;
        }

        public void setGroupCode(String groupCode) {
            this.groupCode = groupCode;
        }
    }

    public static class DbConfig {
        private String url = "jdbc:postgresql://localhost:5436/ga";
        private String username = "postgres";
        private String password = "postgres";
        private int timeoutSeconds = 15;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }
    }

    public static class TmProxyConfig {
        private List<String> apiKeys = new ArrayList<>();
        private int location = 2;
        private int isp = 1;

        public List<String> getApiKeys() {
            return apiKeys;
        }

        public void setApiKeys(List<String> apiKeys) {
            this.apiKeys = apiKeys;
        }

        public int getLocation() {
            return location;
        }

        public void setLocation(int location) {
            this.location = location;
        }

        public int getIsp() {
            return isp;
        }

        public void setIsp(int isp) {
            this.isp = isp;
        }
    }

    public static class GpmConfig {
        private String address = "http://127.0.0.1:19995";
        private String browserCore = "chromium";
        private String browserVersion = "";
        private String startupUrl = "https://www.reddit.com";

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public String getBrowserCore() {
            return browserCore;
        }

        public void setBrowserCore(String browserCore) {
            this.browserCore = browserCore;
        }

        public String getBrowserVersion() {
            return browserVersion;
        }

        public void setBrowserVersion(String browserVersion) {
            this.browserVersion = browserVersion;
        }

        public String getStartupUrl() {
            return startupUrl;
        }

        public void setStartupUrl(String startupUrl) {
            this.startupUrl = startupUrl;
        }
    }

    public static class SecurityConfig {
        private String aesDecryptKey = "";

        public String getAesDecryptKey() {
            return aesDecryptKey;
        }

        public void setAesDecryptKey(String aesDecryptKey) {
            this.aesDecryptKey = aesDecryptKey;
        }
    }

    public static class ActionConfig {
        private int viewPostMin = 2;
        private int viewPostMax = 5;

        public int getViewPostMin() {
            return viewPostMin;
        }

        public void setViewPostMin(int viewPostMin) {
            this.viewPostMin = viewPostMin;
        }

        public int getViewPostMax() {
            return viewPostMax;
        }

        public void setViewPostMax(int viewPostMax) {
            this.viewPostMax = viewPostMax;
        }
    }
}
