package com.earthquakeready.app;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

final class AppTheme {
    static final Color BG = new Color(12, 20, 35);
    static final Color PANEL = new Color(21, 33, 52);
    static final Color PANEL_ALT = new Color(27, 43, 65);
    static final Color BLUE = new Color(47, 133, 216);
    static final Color MUTED = new Color(156, 174, 197);
    static final Color TEXT = new Color(236, 243, 250);
    static final Color BORDER = new Color(42, 59, 81);

    private AppTheme() {}

    static void install() {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                 | UnsupportedLookAndFeelException ex) {
            System.err.println("Using default Swing appearance: " + ex.getMessage());
        }

        UIManager.put("control", PANEL);
        UIManager.put("info", PANEL_ALT);
        UIManager.put("text", TEXT);
        UIManager.put("nimbusBase", PANEL_ALT);
        UIManager.put("nimbusLightBackground", PANEL_ALT);
        UIManager.put("nimbusSelectionBackground", BLUE);
        UIManager.put("nimbusSelectedText", TEXT);
        UIManager.put("Panel.background", PANEL);
        UIManager.put("Label.foreground", TEXT);
        UIManager.put("Button.background", PANEL_ALT);
        UIManager.put("Button.foreground", TEXT);
        UIManager.put("Button.select", BLUE);
        UIManager.put("Button.disabledText", MUTED);
        UIManager.put("TextField.background", PANEL_ALT);
        UIManager.put("TextField.foreground", TEXT);
        UIManager.put("TextField.caretForeground", TEXT);
        UIManager.put("TextField.selectionBackground", BLUE);
        UIManager.put("TextField.selectionForeground", TEXT);
        UIManager.put("ComboBox.background", PANEL_ALT);
        UIManager.put("ComboBox.foreground", TEXT);
        UIManager.put("ComboBox.selectionBackground", BLUE);
        UIManager.put("ComboBox.selectionForeground", TEXT);
        UIManager.put("ComboBox.disabledBackground", PANEL);
        UIManager.put("ComboBox.disabledForeground", MUTED);
        UIManager.put("List.background", PANEL_ALT);
        UIManager.put("List.foreground", TEXT);
        UIManager.put("List.selectionBackground", BLUE);
        UIManager.put("List.selectionForeground", TEXT);
        UIManager.put("CheckBox.background", PANEL);
        UIManager.put("CheckBox.foreground", TEXT);
        UIManager.put("ProgressBar.background", PANEL_ALT);
        UIManager.put("ProgressBar.foreground", new Color(62, 188, 143));
        UIManager.put("TabbedPane.background", PANEL);
        UIManager.put("TabbedPane.foreground", TEXT);
        UIManager.put("TabbedPane.selected", PANEL_ALT);
        UIManager.put("OptionPane.background", PANEL);
        UIManager.put("OptionPane.messageForeground", TEXT);
        UIManager.put("ScrollPane.background", PANEL);
        UIManager.put("Viewport.background", PANEL);
        UIManager.put("ToolTip.background", PANEL_ALT);
        UIManager.put("ToolTip.foreground", TEXT);
        UIManager.put("ToolTip.border", new LineBorder(BORDER));
        UIManager.put("TextField.border", BorderFactory.createCompoundBorder(
                new LineBorder(BORDER), new EmptyBorder(6, 9, 6, 9)));
    }
}
