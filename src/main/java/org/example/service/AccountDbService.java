package org.example.service;

import org.example.config.AppConfig;
import org.example.model.AccountInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class AccountDbService {
    private static final Logger log = LoggerFactory.getLogger(AccountDbService.class);

    private static final String SQL_GET_REDDIT_ACCOUNTS_BY_GROUP = """
            SELECT ga.code,
                   rd.username,
                   rd.email,
                   rd.gender,
                   rd.password AS encrypted_password
            FROM global_account ga
            JOIN reddit_account rd ON rd.ga_code = ga.code
            WHERE ga.group_code = ?
              AND ga.platform = 4
              AND ga.active = true
              AND (ga.hidden IS NULL OR ga.hidden = false)
            ORDER BY ga.updated_at DESC NULLS LAST
            """;

    private final AppConfig config;

    public AccountDbService(AppConfig config) {
        this.config = config;
    }

    public List<AccountInfo> fetchAccountsByGroupCode(String groupCode) {
        if (groupCode == null || groupCode.isBlank()) {
            log.warn("groupCode is empty");
            return List.of();
        }

        List<AccountInfo> accounts = new ArrayList<>();
        try {
            DriverManager.setLoginTimeout(config.getDb().getTimeoutSeconds());
            try (Connection connection = DriverManager.getConnection(
                    config.getDb().getUrl(),
                    config.getDb().getUsername(),
                    config.getDb().getPassword());
                 PreparedStatement statement = connection.prepareStatement(SQL_GET_REDDIT_ACCOUNTS_BY_GROUP)) {
                statement.setString(1, groupCode);
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        AccountInfo account = new AccountInfo();
                        account.setCode(rs.getString("code"));
                        account.setUsername(rs.getString("username"));
                        account.setEmail(rs.getString("email"));
                        account.setGender(rs.getString("gender"));
                        account.setEncryptedPassword(rs.getString("encrypted_password"));
                        accounts.add(account);
                    }
                }
            }
        } catch (Exception ex) {
            log.error("Query DB accounts failed for groupCode {}", groupCode, ex);
            return List.of();
        }
        return accounts;
    }
}
