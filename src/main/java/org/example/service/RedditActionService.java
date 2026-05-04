package org.example.service;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitUntilState;
import org.example.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.regex.Pattern;

public class RedditActionService {
    private static final Logger log = LoggerFactory.getLogger(RedditActionService.class);
    private static final int WAIT_AFTER_LOAD = 8000;

    private final AppConfig config;
    private final Random random = new Random();

    public RedditActionService(AppConfig config) {
        this.config = config;
    }

    public LoginResult loginAndViewHome(String username, String password, String remoteDebugAddress) {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().connectOverCDP("http://" + remoteDebugAddress);
            if (browser.contexts().isEmpty()) {
                return new LoginResult(false, "THAT_BAI", "Không có browser context từ GPM");
            }
            BrowserContext context = browser.contexts().getFirst();
            Page page = context.pages().isEmpty() ? context.newPage() : context.pages().getFirst();

            page.navigate(config.getGpm().getStartupUrl(), new Page.NavigateOptions()
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                    .setTimeout(30000));
            page.waitForTimeout(WAIT_AFTER_LOAD);

            if (!isLoggedIn(page, 5000)) {
                boolean logged = login(page, username, password);
                if (!logged) {
                    String reason = detectBanReason(page);
                    if (reason != null) {
                        return new LoginResult(false, "THAT_BAI", reason);
                    }
                    log.warn("Login reddit failed for {}", username);
                    return new LoginResult(false, "THAT_BAI", "Đăng nhập Reddit thất bại");
                }
            } else {
                log.info("Account {} already logged in", username);
            }

            String banReason = detectBanReason(page);
            if (banReason != null) {
                return new LoginResult(false, "THAT_BAI", banReason);
            }

            viewPostAtHome(page);
            log.info("Complete login + view flow for {}", username);
            return new LoginResult(true, "THANH_CONG", "Đăng nhập và view home thành công");
        } catch (Exception ex) {
            log.error("Login and view flow error for {}", username, ex);
            return new LoginResult(false, "THAT_BAI", "Lỗi runtime: " + ex.getMessage());
        }
    }

    private boolean login(Page page, String username, String password) {
        try {
            Locator loginEntry = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions()
                    .setName(Pattern.compile("Đăng nhập|Log In")));
            if (loginEntry.count() > 0 && loginEntry.first().isVisible()) {
                loginEntry.first().click();
            } else {
                return false;
            }

            page.waitForTimeout(2000);
            Locator usernameInput = page.locator("faceplate-text-input#login-username");
            Locator passwordInput = page.locator("faceplate-text-input#login-password");
            usernameInput.waitFor(new Locator.WaitForOptions().setTimeout(15000));
            passwordInput.waitFor(new Locator.WaitForOptions().setTimeout(15000));

            usernameInput.fill(username);
            page.waitForTimeout(500 + random.nextInt(900));
            passwordInput.fill(password);
            page.waitForTimeout(500 + random.nextInt(900));

            Locator loginBtn = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions()
                    .setName(Pattern.compile("Đăng nhập|Log In")));
            loginBtn.last().click();

            page.waitForTimeout(5000);
            return isLoggedIn(page, 25000);
        } catch (Exception ex) {
            log.error("Login action error", ex);
            return false;
        }
    }

    private boolean isLoggedIn(Page page, int timeoutMs) {
        try {
            page.waitForSelector(
                    "faceplate-partial#user-drawer-avatar-logged-in",
                    new Page.WaitForSelectorOptions().setTimeout(timeoutMs)
            );
            return true;
        } catch (Exception ignore) {
            return false;
        }
    }

    private void viewPostAtHome(Page page) {
        int min = Math.max(1, config.getAction().getViewPostMin());
        int max = Math.max(min, config.getAction().getViewPostMax());
        int viewCount = random.nextInt(min, max + 1);

        page.navigate("https://www.reddit.com/", new Page.NavigateOptions()
                .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                .setTimeout(30000));
        page.waitForTimeout(3000);

        for (int i = 0; i < viewCount; i++) {
            int wheel = random.nextInt(700, 1600);
            page.mouse().wheel(0, wheel);
            page.waitForTimeout(random.nextInt(3000, 8000));
            if (random.nextInt(6) == 2) {
                page.mouse().wheel(0, -random.nextInt(300, 800));
                page.waitForTimeout(random.nextInt(1000, 3000));
            }
        }
    }

    private String detectBanReason(Page page) {
        try {
            page.waitForTimeout(3000);
            Locator banner = page.locator("#banner-text")
                    .getByText("Tài khoản này đã bị cấm vĩnh viễn. Hãy kiểm tra hộp thư đến để biết thêm thông tin.");
            if (banner.isVisible()) {
                return "Tài khoản bị ban/suspended trên Reddit";
            }
            return null;
        } catch (Exception ex) {
            return null;
        }
    }

    public record LoginResult(boolean success, String status, String detail) {
    }
}
