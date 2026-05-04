package org.example.ui;

import org.example.config.AppConfig;
import org.example.model.AccountInfo;
import org.example.model.GpmOpenResult;
import org.example.service.AccountDbService;
import org.example.service.GpmService;
import org.example.service.ProxyManager;
import org.example.service.RedditActionService;
import org.example.util.CryptoUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Arrays;
import java.util.List;

public class LoginBotFrame extends JFrame {
    private final JTextField dbUrlField = new JTextField("jdbc:postgresql://localhost:5436/ga");
    private final JTextField dbUserField = new JTextField("postgres");
    private final JPasswordField dbPasswordField = new JPasswordField("postgres");
    private final JTextField groupCodeField = new JTextField();
    private final JTextArea proxyKeysArea = new JTextArea(3, 30);
    private final JTextField gpmAddressField = new JTextField("http://127.0.0.1:19995");
    private final JTextField gpmCoreField = new JTextField("chromium");
    private final JTextField gpmVersionField = new JTextField("139.0.7258.139");
    private final JTextField gpmStartupUrlField = new JTextField("https://www.reddit.com");
    private final JPasswordField aesKeyField = new JPasswordField();
    private final JButton runButton = new JButton("Chạy");
    private final JLabel infoLabel = new JLabel("Sẵn sàng");

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"Code", "Username", "Email", "Trạng thái", "Chi tiết"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    public LoginBotFrame() {
        setTitle("Login Bot Reddit");
        setSize(1200, 720);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBorder(new EmptyBorder(12, 12, 12, 12));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.gridx = 0;
        gbc.gridy = 0;

        addField(inputPanel, gbc, "DB URL", dbUrlField);
        addField(inputPanel, gbc, "DB Username", dbUserField);
        addField(inputPanel, gbc, "DB Password", dbPasswordField);
        addField(inputPanel, gbc, "Group Code", groupCodeField);
        addAreaField(inputPanel, gbc, "Proxy Keys (mỗi dòng 1 key)", proxyKeysArea);
        addField(inputPanel, gbc, "GPM Address", gpmAddressField);
        addField(inputPanel, gbc, "GPM Browser Core", gpmCoreField);
        addField(inputPanel, gbc, "GPM Browser Version", gpmVersionField);
        addField(inputPanel, gbc, "GPM Startup URL", gpmStartupUrlField);
        addField(inputPanel, gbc, "AES Key", aesKeyField);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(inputPanel, BorderLayout.CENTER);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actionPanel.add(runButton);
        actionPanel.add(infoLabel);
        topPanel.add(actionPanel, BorderLayout.SOUTH);

        JTable statusTable = new JTable(tableModel);
        JScrollPane tableScroll = new JScrollPane(statusTable);

        add(topPanel, BorderLayout.NORTH);
        add(tableScroll, BorderLayout.CENTER);

        runButton.addActionListener(e -> startRun());
    }

    private void startRun() {
        String groupCode = groupCodeField.getText().trim();
        if (groupCode.isBlank()) {
            JOptionPane.showMessageDialog(this, "Group code không được để trống", "Thiếu dữ liệu", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String aesKey = new String(aesKeyField.getPassword()).trim();
        if (aesKey.isBlank()) {
            JOptionPane.showMessageDialog(this, "AES key không được để trống", "Thiếu dữ liệu", JOptionPane.WARNING_MESSAGE);
            return;
        }

        AppConfig config = buildConfigFromForm(groupCode, aesKey);
        runButton.setEnabled(false);
        tableModel.setRowCount(0);
        infoLabel.setText("Đang tải danh sách tài khoản...");

        SwingWorker<Void, RowUpdate> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                AccountDbService accountDbService = new AccountDbService(config);
                ProxyManager proxyManager = new ProxyManager(config);
                GpmService gpmService = new GpmService(config);
                RedditActionService redditActionService = new RedditActionService(config);

                List<AccountInfo> accounts = accountDbService.fetchAccountsByGroupCode(groupCode);
                if (accounts.isEmpty()) {
                    publish(new RowUpdate(-1, "", "", "THẤT BẠI", "Không có tài khoản nào trong group"));
                    return null;
                }

                for (AccountInfo account : accounts) {
                    int row = addInitialRow(account);
                    publish(new RowUpdate(row, account.getCode(), account.getUsername(), "ĐANG LOGIN", "Đang chuẩn bị proxy"));

                    String proxy = proxyManager.nextProxy();
                    if (proxy == null || proxy.isBlank()) {
                        publish(new RowUpdate(row, account.getCode(), account.getUsername(), "THẤT BẠI", "Không lấy được proxy"));
                        continue;
                    }

                    GpmOpenResult gpmOpenResult = gpmService.openOrCreateProfile(account.getUsername(), proxy);
                    if (!gpmOpenResult.success()) {
                        publish(new RowUpdate(row, account.getCode(), account.getUsername(), "THẤT BẠI",
                                "GPM lỗi: " + gpmOpenResult.message()));
                        continue;
                    }

                    try {
                        String password = CryptoUtil.aesDecrypt(account.getEncryptedPassword(), aesKey);
                        RedditActionService.LoginResult result = redditActionService.loginAndViewHome(
                                account.getUsername(), password, gpmOpenResult.remoteDebugAddress()
                        );
                        if (result.success()) {
                            publish(new RowUpdate(row, account.getCode(), account.getUsername(), "THÀNH CÔNG", result.detail()));
                        } else {
                            publish(new RowUpdate(row, account.getCode(), account.getUsername(), "THẤT BẠI", result.detail()));
                        }
                    } catch (Exception ex) {
                        publish(new RowUpdate(row, account.getCode(), account.getUsername(), "THẤT BẠI",
                                "Lỗi decrypt/login: " + ex.getMessage()));
                    } finally {
                        gpmService.closeProfile(account.getUsername());
                    }
                }
                return null;
            }

            @Override
            protected void process(List<RowUpdate> chunks) {
                for (RowUpdate update : chunks) {
                    if (update.rowIndex < 0) {
                        infoLabel.setText(update.detail);
                        continue;
                    }
                    tableModel.setValueAt(update.status, update.rowIndex, 3);
                    tableModel.setValueAt(update.detail, update.rowIndex, 4);
                }
            }

            @Override
            protected void done() {
                runButton.setEnabled(true);
                infoLabel.setText("Đã chạy xong");
            }
        };
        worker.execute();
    }

    private int addInitialRow(AccountInfo account) {
        Object[] row = new Object[]{
                account.getCode(),
                account.getUsername(),
                account.getEmail(),
                "CHỜ",
                ""
        };
        tableModel.addRow(row);
        return tableModel.getRowCount() - 1;
    }

    private AppConfig buildConfigFromForm(String groupCode, String aesKey) {
        AppConfig config = AppConfig.load();
        config.getDb().setUrl(dbUrlField.getText().trim());
        config.getDb().setUsername(dbUserField.getText().trim());
        config.getDb().setPassword(new String(dbPasswordField.getPassword()));
        config.getBot().setGroupCode(groupCode);
        config.getSecurity().setAesDecryptKey(aesKey);
        config.getGpm().setAddress(gpmAddressField.getText().trim());
        config.getGpm().setBrowserCore(gpmCoreField.getText().trim());
        config.getGpm().setBrowserVersion(gpmVersionField.getText().trim());
        config.getGpm().setStartupUrl(gpmStartupUrlField.getText().trim());

        List<String> keys = Arrays.stream(proxyKeysArea.getText().split("\\R"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
        config.getTmproxy().setApiKeys(keys);
        return config;
    }

    private void addField(JPanel panel, GridBagConstraints gbc, String label, JComponent field) {
        gbc.gridx = 0;
        gbc.weightx = 0;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(field, gbc);
        gbc.gridy++;
    }

    private void addAreaField(JPanel panel, GridBagConstraints gbc, String label, JTextArea area) {
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        addField(panel, gbc, label, new JScrollPane(area));
    }

    private record RowUpdate(int rowIndex, String code, String username, String status, String detail) {
    }
}
