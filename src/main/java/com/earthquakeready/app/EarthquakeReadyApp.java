package com.earthquakeready.app;

import javax.swing.*;
import java.awt.*;

public final class EarthquakeReadyApp {
    private EarthquakeReadyApp() {}
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            AppTheme.install();
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
