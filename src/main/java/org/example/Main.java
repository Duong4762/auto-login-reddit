package org.example;

import org.example.ui.LoginBotFrame;

import javax.swing.*;
import java.util.TimeZone;

public class Main {
    public static void main(String[] args) {
        // PostgreSQL may reject legacy timezone id "Asia/Saigon".
        // Force a modern valid timezone before any DB connection is created.
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        System.setProperty("user.timezone", "Asia/Ho_Chi_Minh");

        SwingUtilities.invokeLater(() -> {
            LoginBotFrame frame = new LoginBotFrame();
            frame.setVisible(true);
        });
    }
}