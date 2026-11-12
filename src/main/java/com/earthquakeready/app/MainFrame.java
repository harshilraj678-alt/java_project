package com.earthquakeready.app;

import com.earthquakeready.db.*;
import com.earthquakeready.domain.*;
import com.earthquakeready.engine.*;
import com.earthquakeready.io.*;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;

public final class MainFrame extends JFrame {
    private static final Color BG = AppTheme.BG;
    private static final Color PANEL = AppTheme.PANEL;
    private static final Color PANEL_ALT = AppTheme.PANEL_ALT;
    private static final Color BLUE = AppTheme.BLUE;
    private static final Color MUTED = AppTheme.MUTED;
    private static final Color TEXT = AppTheme.TEXT;
    private static final String[] NAV = {"Dashboard", "Scenario Setup", "City Map", "Preparedness",
            "Simulation Control", "Response Center", "Analytics", "History & Reports", "Engineering Inspector", "Settings"};

    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final JLabel scenarioLabel = new JLabel();
    private final JLabel stateLabel = new JLabel();
    private final JLabel databaseLabel = new JLabel();
    private final JLabel clockLabel = new JLabel();
    private final JLabel statusLabel = new JLabel("Ready for an educational simulation");
    private final AppPaths paths;
    private final SessionManager sessions;
    private final DatabaseService database;
    private final ArchiveService archive;
    private final LogService logService;
    private EarthquakeEngine engine;
    private SimulationSnapshot latest;
    private String selectedZone = "RES";
    private String selectedPage = "Dashboard";
    private final JPanel dashboard = new JPanel(new BorderLayout(16, 16));
    private final JPanel metricCards = new JPanel(new GridLayout(1, 5, 12, 0));
    private final JTextArea timeline = textArea();
    private final JComboBox<String> timelineFilter = new JComboBox<>(new String[]{"All events", "Earthquakes", "Aftershocks",
            "Infrastructure", "Recovery", "Preparedness", "Missions"});
    private final JLabel mapDetails = new JLabel();
    private final List<RiskMap> maps = new ArrayList<>();
    private final RiskMap map = new RiskMap();
    private final JTextArea inspectorText = textArea();
    private final JTextArea historyText = textArea();
    private final JTextArea missionText = textArea();
    private final JLabel analyticsSummary = new JLabel();
    private final Set<String> persistedEventIds = ConcurrentHashMap.newKeySet();
    private final Map<String, String> persistedMissionsState = new ConcurrentHashMap<>();
    private final JProgressBar readinessBar = new JProgressBar(0, 100);
    private final JComboBox<String> prepZone = new JComboBox<>();
    private final JComboBox<String> responseZone = new JComboBox<>();
    private final JLabel responseResources = new JLabel();
    private final JSlider intensity = new JSlider(10, 90, 52);
    private final JSlider duration = new JSlider(5, 60, 30);
    private final JSlider resources = new JSlider(5, 50, 24);
    private final JSlider preparedness = new JSlider(10, 90, 55);
    private final JComboBox<String> preset = new JComboBox<>(new String[]{"Mild shaking exercise", "Moderate shaking exercise", "Severe shaking exercise", "Custom scenario"});
    private final JTextField seedField = new JTextField("20260415", 14);
    private final JLabel configSummary = new JLabel();
    private String customScenarioName;
    private final JSlider affectedZones = new JSlider(1, 6, 6);
    private final JSlider aftershocks = new JSlider(0, 20, 3);
    private final Map<String, JCheckBox> preparednessChecks = new LinkedHashMap<>();
    private final Map<String, JButton> navigationButtons = new LinkedHashMap<>();
    private final JLabel controlStateLabel = new JLabel("READY");
    private final JLabel controlDetailsLabel = new JLabel();
    private final JProgressBar simulationProgress = new JProgressBar(0, 1);
    private JButton startButton;
    private JButton pauseButton;
    private JButton stopButton;
    private JButton stepButton;
    private boolean timelineCleared;
    private long timelineEventCount;
    private volatile String savedSessionsSummary = "Session save details not loaded";

    public MainFrame() {
        super("EarthquakeReady — Disaster Preparedness Simulator");
        AppPaths initializedPaths;
        try { initializedPaths = new AppPaths(); }
        catch (IOException ex) { throw new IllegalStateException("Cannot create application data folders", ex); }
        paths = initializedPaths;
        sessions = new SessionManager(paths.saves());
        database = new DatabaseService(new DatabaseManager(paths.database()));
        archive = new ArchiveService(paths.archive());
        logService = new LogService(paths.logs().resolve("operations.log"));
        engine = new EarthquakeEngine(ScenarioConfig.moderate());
        preset.setSelectedIndex(1);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(1120, 720));
        setSize(1420, 900);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout());
        add(buildHeader(), BorderLayout.NORTH);
        add(buildSidebar(), BorderLayout.WEST);
        content.setBackground(BG);
        buildPages();
        add(content, BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
        bindEngine();
        setPage("Dashboard");
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent event) { shutdown(); }
        });
        new javax.swing.Timer(1000, event -> updateHeader()).start();
    }

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout(18, 0));
        header.setBackground(new Color(16, 28, 46));
        header.setBorder(new EmptyBorder(15, 22, 15, 22));
        JLabel brand = new JLabel("◆  EARTHQUAKEREADY");
        brand.setForeground(TEXT);
        brand.setFont(new Font("Segoe UI", Font.BOLD, 18));
        header.add(brand, BorderLayout.WEST);
        JPanel context = new JPanel(new FlowLayout(FlowLayout.RIGHT, 22, 0));
        context.setOpaque(false);
        styleLabel(scenarioLabel, 13, true);
        styleLabel(stateLabel, 13, true);
        styleLabel(clockLabel, 13, false);
        styleLabel(databaseLabel, 12, false);
        context.add(scenarioLabel);
        context.add(stateLabel);
        context.add(clockLabel);
        context.add(databaseLabel);
        header.add(context, BorderLayout.EAST);
        return header;
    }

    private JComponent buildSidebar() {
        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBackground(new Color(15, 26, 43));
        side.setBorder(new EmptyBorder(18, 12, 14, 12));
        side.setPreferredSize(new Dimension(220, 0));
        JLabel title = new JLabel("OPERATIONS");
        title.setForeground(MUTED);
        title.setFont(new Font("Segoe UI", Font.BOLD, 10));
        title.setBorder(new EmptyBorder(4, 10, 14, 0));
        side.add(title);
        for (String page : NAV) {
            JButton button = new JButton(page);
            button.setHorizontalAlignment(SwingConstants.LEFT);
            button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
            button.setForeground(TEXT);
            button.setBackground(side.getBackground());
            button.setOpaque(true);
            button.setBorderPainted(true);
            button.setBorder(new EmptyBorder(0, 12, 0, 8));
            button.setFocusPainted(false);
            button.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            button.addActionListener(event -> setPage(page));
            navigationButtons.put(page, button);
            side.add(button);
            side.add(Box.createVerticalStrut(3));
        }
        side.add(Box.createVerticalGlue());
        JLabel disclaimer = new JLabel("<html><div style='width:170px'>SIMULATED DATA ONLY<br><br>Not a prediction, engineering assessment, emergency alert, or safe-route service.</div></html>");
        disclaimer.setForeground(new Color(246, 190, 86));
        disclaimer.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        disclaimer.setBorder(new EmptyBorder(14, 9, 8, 8));
        side.add(disclaimer);
        return side;
    }

    private JComponent buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(new Color(16, 28, 46));
        footer.setBorder(new EmptyBorder(8, 14, 8, 14));
        statusLabel.setForeground(MUTED);
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        footer.add(statusLabel, BorderLayout.WEST);
        JLabel note = new JLabel("Fictional educational exercise · Follow local authorities for real emergencies");
        note.setForeground(MUTED);
        note.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        footer.add(note, BorderLayout.EAST);
        return footer;
    }

    private void buildPages() {
        addPage("Dashboard", createDashboard());
        addPage("Scenario Setup", createScenarioPage());
        addPage("City Map", createMapPage());
        addPage("Preparedness", createPreparednessPage());
        addPage("Simulation Control", createControlPage());
        addPage("Response Center", createResponsePage());
        addPage("Analytics", createAnalyticsPage());
        addPage("History & Reports", createHistoryPage());
        addPage("Engineering Inspector", createInspectorPage());
        addPage("Settings", createSettingsPage());
    }
    private void addPage(String name, JComponent page) {
        page.setBackground(BG);
        content.add(page, name);
    }

    private JComponent createDashboard() {
        dashboard.setBackground(BG);
        dashboard.setBorder(new EmptyBorder(22, 22, 18, 22));
        JPanel title = titleBlock("Operations dashboard", "Live view of the current fictional earthquake exercise.");
        dashboard.add(title, BorderLayout.NORTH);
        JPanel center = new JPanel(new BorderLayout(15, 15));
        center.setOpaque(false);
        metricCards.setOpaque(false);
        center.add(metricCards, BorderLayout.NORTH);
        JPanel lower = new JPanel(new GridLayout(1, 2, 15, 0));
        lower.setOpaque(false);
        JPanel mapCard = panelCard();
        mapCard.setLayout(new BorderLayout(10, 10));
        mapCard.add(sectionLabel("CITY RISK OVERVIEW"), BorderLayout.NORTH);
        RiskMap dashMap = new RiskMap();
        dashMap.setPreferredSize(new Dimension(420, 270));
        mapCard.add(dashMap, BorderLayout.CENTER);
        lower.add(mapCard);
        JPanel activityCard = panelCard();
        activityCard.setLayout(new BorderLayout(10, 10));
        JPanel activityHeader = new JPanel(new BorderLayout(8, 0));
        activityHeader.setOpaque(false);
        activityHeader.add(sectionLabel("RECENT ACTIVITY"), BorderLayout.WEST);
        JPanel activityTools = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        activityTools.setOpaque(false);
        timelineFilter.setBackground(PANEL_ALT);
        timelineFilter.setForeground(TEXT);
        styleComboBox(timelineFilter);
        timelineFilter.addActionListener(event -> refreshTimeline(latest));
        JButton clearTimeline = secondaryButton("Clear view");
        clearTimeline.addActionListener(event -> {
            timelineCleared = true;
            timeline.setText("");
            statusLabel.setText("Timeline view cleared; saved history and archives are retained.");
        });
        activityTools.add(timelineFilter); activityTools.add(clearTimeline);
        activityHeader.add(activityTools, BorderLayout.EAST);
        activityCard.add(activityHeader, BorderLayout.NORTH);
        activityCard.add(new JScrollPane(timeline), BorderLayout.CENTER);
        lower.add(activityCard);
        center.add(lower, BorderLayout.CENTER);
        dashboard.add(center, BorderLayout.CENTER);
        return dashboard;
    }

    private JComponent createScenarioPage() {
        JPanel page = pagePanel();
        page.add(titleBlock("Scenario configuration", "Adjust transparent illustrative parameters before starting a run."), BorderLayout.NORTH);
        JPanel form = panelCard();
        form.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(10, 10, 10, 10);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        int row = 0;
        styleComboBox(preset);
        styleTextField(seedField);
        addFormRow(form, c, row++, "Preset", preset);
        addSliderRow(form, c, row++, "Illustrative shaking intensity", intensity);
        addSliderRow(form, c, row++, "Simulation duration (ticks)", duration);
        addSliderRow(form, c, row++, "Affected zones (1–6)", affectedZones);
        addSliderRow(form, c, row++, "Simulated aftershocks (0–20)", aftershocks);
        addSliderRow(form, c, row++, "Initial resource units", resources);
        addSliderRow(form, c, row++, "Initial preparedness", preparedness);
        addFormRow(form, c, row++, "Random seed (reproducible)", seedField);
        JButton apply = primaryButton("Apply scenario settings");
        apply.addActionListener(event -> applyScenario());
        c.gridx = 1; c.gridy = row; c.weightx = 1;
        form.add(apply, c);
        JPanel fileActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        fileActions.setOpaque(false);
        JButton importConfig = secondaryButton("Import .properties");
        JButton exportConfig = secondaryButton("Export .properties");
        importConfig.addActionListener(event -> importScenarioConfig());
        exportConfig.addActionListener(event -> exportScenarioConfig());
        fileActions.add(importConfig); fileActions.add(exportConfig);
        configSummary.setForeground(MUTED);
        c.gridx = 0; c.gridy = ++row; c.gridwidth = 2;
        form.add(fileActions, c);
        c.gridy = ++row;
        form.add(configSummary, c);
        preset.addActionListener(event -> setPresetValues());
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(form, BorderLayout.NORTH);
        page.add(wrap, BorderLayout.CENTER);
        return page;
    }

    private JComponent createMapPage() {
        JPanel page = pagePanel();
        page.add(titleBlock("Interactive city map", "Schematic fictional map. Risk values are simulated—not geographic or structural assessments."), BorderLayout.NORTH);
        JPanel body = new JPanel(new BorderLayout(16, 0));
        body.setOpaque(false);
        JPanel mapCard = panelCard();
        mapCard.setLayout(new BorderLayout(8, 8));
        mapCard.add(map, BorderLayout.CENTER);
        map.setPreferredSize(new Dimension(680, 490));
        JLabel legend = new JLabel("  ● Low     ● Moderate     ● High     ● Critical       Click a zone for details");
        legend.setForeground(MUTED);
        mapCard.add(legend, BorderLayout.SOUTH);
        body.add(mapCard, BorderLayout.CENTER);
        JPanel detail = panelCard();
        detail.setPreferredSize(new Dimension(280, 0));
        detail.setLayout(new BorderLayout());
        detail.add(sectionLabel("SELECTED ZONE"), BorderLayout.NORTH);
        mapDetails.setForeground(TEXT);
        mapDetails.setVerticalAlignment(SwingConstants.TOP);
        mapDetails.setBorder(new EmptyBorder(20, 4, 4, 4));
        detail.add(mapDetails, BorderLayout.CENTER);
        body.add(detail, BorderLayout.EAST);
        page.add(body, BorderLayout.CENTER);
        return page;
    }

    private JComponent createPreparednessPage() {
        JPanel page = pagePanel();
        page.add(titleBlock("Preparedness management", "Complete activities to improve the selected zone's educational readiness score."), BorderLayout.NORTH);
        JPanel body = new JPanel(new GridLayout(1, 2, 16, 0));
        body.setOpaque(false);
        JPanel list = panelCard();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.add(sectionLabel("COMMUNITY READINESS CHECKLIST"));
        String[] tasks = {"Emergency kit readiness", "Household communication plan", "Emergency contact plan",
                "Meeting-point planning", "Earthquake safety awareness", "Building preparedness review",
                "Community awareness activity", "Shelter readiness"};
        int i = 0;
        for (String task : tasks) {
            JCheckBox box = new JCheckBox(task);
            preparednessChecks.put(task, box);
            box.setForeground(TEXT); box.setOpaque(false);
            box.addActionListener(event -> {
                if (box.isSelected()) {
                    String zoneId = selectedId(prepZone);
                    if (engine.improvePreparedness(zoneId, task, 5)) {
                        database.saveTask(task, preparednessCategory(task).name(), true).whenComplete((ignored, error) -> {
                            if (error != null) SwingUtilities.invokeLater(() ->
                                    statusLabel.setText("Preparedness saved locally; database error: " + rootMessage(error)));
                        });
                        statusLabel.setText("Completed: " + task + " (+5 readiness points)");
                    } else statusLabel.setText("This preparedness activity has already been completed.");
                }
            });
            list.add(box); list.add(Box.createVerticalStrut(9)); i++;
        }
        body.add(list);
        JPanel inventory = panelCard();
        inventory.setLayout(new BoxLayout(inventory, BoxLayout.Y_AXIS));
        inventory.add(sectionLabel("ZONE READINESS"));
        addZoneChoices(prepZone);
        styleComboBox(prepZone);
        prepZone.addActionListener(event -> updateSnapshot(latest));
        inventory.add(labeled("Target zone", prepZone));
        readinessBar.setStringPainted(true);
        readinessBar.setForeground(new Color(62, 188, 143));
        inventory.add(Box.createVerticalStrut(16));
        inventory.add(readinessBar);
        inventory.add(Box.createVerticalStrut(16));
        inventory.add(new JLabel("<html><div style='width:260px;color:#9caec5'>Checklist actions are simple illustrative scoring rules (+5 points, capped at 100). They are not a substitute for official preparedness guidance.</div></html>"));
        body.add(inventory);
        page.add(body, BorderLayout.CENTER);
        return page;
    }

    private JComponent createControlPage() {
        JPanel page = pagePanel();
        page.add(titleBlock("Simulation control", "Start a live exercise or advance it one deterministic tick at a time."), BorderLayout.NORTH);
        JPanel box = panelCard();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.add(sectionLabel("RUN CONTROLS"));

        JPanel statusCard = new JPanel(new BorderLayout(16, 10));
        statusCard.setBackground(PANEL_ALT);
        statusCard.setBorder(new CompoundBorder(new LineBorder(new Color(56, 76, 100), 1, true),
                new EmptyBorder(16, 18, 16, 18)));
        JPanel statusText = new JPanel();
        statusText.setOpaque(false);
        statusText.setLayout(new BoxLayout(statusText, BoxLayout.Y_AXIS));
        JLabel statusCaption = new JLabel("EXERCISE STATUS");
        statusCaption.setForeground(MUTED);
        statusCaption.setFont(new Font("Segoe UI", Font.BOLD, 10));
        controlStateLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        controlDetailsLabel.setText("Ready — start the feed or advance one tick.");
        controlDetailsLabel.setForeground(MUTED);
        controlDetailsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusText.add(statusCaption);
        statusText.add(Box.createVerticalStrut(5));
        statusText.add(controlStateLabel);
        statusText.add(Box.createVerticalStrut(4));
        statusText.add(controlDetailsLabel);
        statusCard.add(statusText, BorderLayout.CENTER);

        simulationProgress.setStringPainted(true);
        simulationProgress.setString("0 / " + engine.config().durationTicks() + " ticks");
        simulationProgress.setBorderPainted(false);
        simulationProgress.setPreferredSize(new Dimension(220, 22));
        statusCard.add(simulationProgress, BorderLayout.SOUTH);
        limitHeight(statusCard);
        box.add(statusCard);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 15));
        buttons.setOpaque(false);
        startButton = primaryButton("Start simulation");
        pauseButton = secondaryButton("Pause simulation");
        stopButton = secondaryButton("Stop simulation");
        stepButton = secondaryButton("Advance one step");
        startButton.addActionListener(event -> startSimulation());
        pauseButton.addActionListener(event -> {
            if (engine.state() == SimulationState.PAUSED) engine.resume();
            else engine.pause();
        });
        stopButton.addActionListener(event -> new Thread(() -> {
            engine.stop();
            SwingUtilities.invokeLater(() -> statusLabel.setText("Simulation stopped"));
        }, "earthquakeready-shutdown").start());
        stepButton.addActionListener(event -> new Thread(() -> {
            try { engine.advanceOneStep(); }
            catch (RuntimeException ex) { SwingUtilities.invokeLater(() -> showError("Could not advance simulation", ex)); }
        }, "earthquakeready-single-step").start());
        buttons.add(startButton); buttons.add(pauseButton); buttons.add(stopButton); buttons.add(stepButton);
        limitHeight(buttons);
        box.add(buttons);
        JSlider speed = new JSlider(1, 16, 4);
        speed.setMajorTickSpacing(3); speed.setPaintTicks(true); speed.setOpaque(false);
        speed.addChangeListener(event -> engine.setSpeed(speed.getValue() / 4.0));
        JPanel speedRow = labeled("Feed speed (0.25x–4x)", speed);
        limitHeight(speedRow);
        box.add(speedRow);
        JButton priority = secondaryButton("Apply low feed priority (demo)");
        priority.addActionListener(event -> {
            engine.runPriorityExperiment(Thread.MIN_PRIORITY);
            JOptionPane.showMessageDialog(this, "Thread priority is only a scheduling hint; it cannot guarantee ordering.");
        });
        limitHeight(priority);
        box.add(priority);
        box.add(Box.createVerticalStrut(14));
        JLabel note = new JLabel("<html><div style='width:760px;color:#9caec5'>The fixed seed makes event attributes reproducible. Feed pacing is illustrative; accepted events drain on shutdown. No real sensors or emergency services are contacted.</div></html>");
        limitHeight(note);
        box.add(note);
        box.add(Box.createVerticalStrut(15));
        page.add(box, BorderLayout.NORTH);
        return page;
    }

    private JComponent createResponsePage() {
        JPanel page = pagePanel();
        page.add(titleBlock("Response center", "Reserve limited fictional resources atomically and track concurrent mission progress."), BorderLayout.NORTH);
        JPanel body = new JPanel(new GridLayout(1, 2, 16, 0));
        body.setOpaque(false);
        JPanel dispatch = panelCard();
        dispatch.setLayout(new BoxLayout(dispatch, BoxLayout.Y_AXIS));
        dispatch.add(sectionLabel("CREATE RESPONSE MISSION"));
        JComboBox<String> missionType = new JComboBox<>(new String[]{"Prepare an emergency shelter",
                "Distribute emergency supplies", "Assess a simulated affected zone",
                "Coordinate utility restoration", "Community communication support"});
        styleComboBox(missionType);
        addZoneChoices(responseZone);
        styleComboBox(responseZone);
        dispatch.add(labeled("Mission", missionType));
        dispatch.add(labeled("Target zone", responseZone));
        responseResources.setForeground(MUTED);
        dispatch.add(responseResources);
        JButton launch = primaryButton("Allocate resources and launch");
        launch.addActionListener(event -> {
            String type = (String) missionType.getSelectedItem();
            String zone = selectedId(responseZone);
            Map<ResourceType, Integer> request = allocationFor(type);
            EarthquakeEngine activeEngine = engine;
            statusLabel.setText("Allocating mission resources…");
            new SwingWorker<Boolean, Void>() {
                @Override protected Boolean doInBackground() { return activeEngine.launchMission(type, zone, request); }
                @Override protected void done() {
                    try {
                        if (get()) statusLabel.setText("Mission launched; reserved resources are reflected below.");
                        else JOptionPane.showMessageDialog(MainFrame.this, "Not enough available resources for this mission.",
                                "Resource limit", JOptionPane.WARNING_MESSAGE);
                    } catch (Exception ex) { showError("Could not launch mission", ex); }
                }
            }.execute();
        });
        dispatch.add(Box.createVerticalStrut(16)); dispatch.add(launch);
        body.add(dispatch);
        JPanel tracking = panelCard();
        tracking.setLayout(new BorderLayout());
        tracking.add(sectionLabel("ACTIVE AND COMPLETED MISSIONS"), BorderLayout.NORTH);
        tracking.add(new JScrollPane(missionText), BorderLayout.CENTER);
        body.add(tracking);
        responseZone.addActionListener(event -> updateSnapshot(latest));
        page.add(body, BorderLayout.CENTER);
        return page;
    }

    private JComponent createAnalyticsPage() {
        JPanel page = pagePanel();
        page.add(titleBlock("Analytics & evaluation", "Zone-wise readiness, simulated risk distribution, mission and resource status."), BorderLayout.NORTH);
        JPanel body = new JPanel(new GridLayout(1, 2, 16, 0));
        body.setOpaque(false);
        JPanel chartCard = panelCard();
        chartCard.setLayout(new BorderLayout(8, 8));
        chartCard.add(sectionLabel("SIMULATED ZONE HAZARD SCORES"), BorderLayout.NORTH);
        chartCard.add(new RiskChart(), BorderLayout.CENTER);
        body.add(chartCard);
        JPanel metrics = panelCard();
        metrics.setLayout(new BoxLayout(metrics, BoxLayout.Y_AXIS));
        metrics.add(sectionLabel("RESPONSE & RECOVERY"));
        analyticsSummary.setForeground(TEXT);
        metrics.add(analyticsSummary);
        metrics.add(Box.createVerticalStrut(15));
        metrics.add(new JLabel("<html><div style='width:300px;color:#9caec5'>Evaluation summarizes only the fictional run. Hazard scoring is a transparent teaching heuristic, not a forecast or engineering calculation.</div></html>"));
        body.add(metrics);
        page.add(body, BorderLayout.CENTER);
        return page;
    }

    private JComponent createHistoryPage() {
        JPanel page = pagePanel();
        page.add(titleBlock("History & reports", "Local session saves, event database status, and exportable simulation summaries."), BorderLayout.NORTH);
        JPanel body = new JPanel(new BorderLayout(12, 12));
        body.setOpaque(false);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        JButton save = secondaryButton("Save session");
        JButton load = secondaryButton("Load session");
        JButton txt = secondaryButton("Export TXT");
        JButton csv = secondaryButton("Export CSV");
        save.addActionListener(event -> saveSession());
        load.addActionListener(event -> loadSession());
        txt.addActionListener(event -> exportReport(false));
        csv.addActionListener(event -> exportReport(true));
        actions.add(save); actions.add(load); actions.add(txt); actions.add(csv);
        body.add(actions, BorderLayout.NORTH);
        historyText.setText("Database and session activity will appear here.");
        body.add(new JScrollPane(historyText), BorderLayout.CENTER);
        page.add(body, BorderLayout.CENTER);
        return page;
    }

    private JComponent createInspectorPage() {
        JPanel page = pagePanel();
        page.add(titleBlock("Engineering Inspector", "Live runtime status and explanations of the architecture used in this application."), BorderLayout.NORTH);
        JPanel tabs = new JPanel(new BorderLayout());
        tabs.setOpaque(false);
        JTabbedPane tabbed = new JTabbedPane();
        inspectorText.setText("System status updates while the application is open.");
        tabbed.addTab("Live systems", new JScrollPane(inspectorText));
        JTextArea concepts = textArea();
        concepts.setText("""
                ENCAPSULATION — Definition: keep state private and validate controlled changes.
                Why: invalid scenario inputs would make a run misleading. Class: ScenarioConfig.
                Example: new ScenarioConfig(name, intensity, duration, zones, aftershocks, resources, readiness, seed)
                Without it: invalid ranges and inconsistent scores could enter the model.

                POLYMORPHISM — Definition: process specialized objects through a shared contract.
                Why: each observation applies its own domain effect. Classes: SimulationEvent and event subclasses.
                Example: event.applyTo(zone, randomFactor)
                Without it: every worker would need type-specific GUI or conditional logic.

                PRODUCER–CONSUMER — Definition: producers enqueue work and consumers wait for it.
                Why: feed and state processing must run independently. Classes: ScenarioEventFeed, EventIntake, EventBuffer.
                Example: buffer.put(event); event = buffer.take();
                Without it: busy polling wastes CPU or shared UI processing stalls.

                SYNCHRONIZATION — Definition: protect a shared invariant as one atomic operation.
                Why: limited supplies cannot be over-allocated. Class: ResourceAllocator.
                Example: reserve(validatedAllocation)
                Without it: concurrent missions could each see the same last unit.

                TRANSACTION — Definition: commit related database changes together or roll them back.
                Why: inventory decrement and mission resource rows must agree. Class: DatabaseManager.
                Example: setAutoCommit(false) → updates/inserts → commit(); on error rollback().
                Without it: partial mission records could permanently lose resources.

                IMMUTABLE SNAPSHOT — Definition: publish a value that workers no longer mutate.
                Why: the EDT must render a consistent view. Class: SimulationSnapshot.
                Example: SwingUtilities.invokeLater(() -> updateSnapshot(snapshot))
                Without it: Swing races could show mixed or unsafe component state.

                SERIALIZATION FILTER — Definition: constrain classes and object graph size on restore.
                Why: local session loading must reject unsupported object graphs. Class: SessionManager.
                Example: input.setObjectInputFilter(info -> ALLOWED_OR_REJECTED)
                Without it: corrupt or unexpected data could crash or abuse restoration.

                THREAD PRIORITY — Definition: a scheduler hint, not a synchronization mechanism.
                Why: the inspector demonstrates this limitation. Class: ScenarioEventFeed.
                Example: feed.setPriority(Thread.MIN_PRIORITY)
                Without queue ordering, priorities never guarantee correct event order.

                DEADLOCK PREVENTION
                Work queue monitor ──(release)──► allocator monitor ──(release)──► database executor
                No nested lock is held across I/O, waits, sleeps, or Swing callbacks; no deadlock is intentionally created.

                SAFETY: scores, maps and missions are fictional educational data; not predictions, engineering assessments, routes or dispatch.
                """);
        tabbed.addTab("Java concept explorer", new JScrollPane(concepts));
        JTextArea architecture = textArea();
        architecture.setText("FEED THREAD\n    │ piped characters\n    ▼\nINTAKE RUNNABLE\n    │ put()/wait()\n    ▼\nBOUNDED EVENT BUFFER\n    │ take()/notifyAll()\n    ▼\nSIMULATION WORKER ──► CITY MODEL\n\nRESPONSE WORKER ───► RESOURCE ALLOCATOR\n\nWorker callbacks publish immutable snapshots; SwingUtilities.invokeLater performs all UI updates on the EDT.\n\nDatabase path: UI action → controlled executor → JDBC repository → embedded SQLite.\n\nLock policy: one allocator monitor; never nest locks or hold a lock across I/O, sleeps or Swing updates.");
        tabbed.addTab("Architecture diagrams", new JScrollPane(architecture));
        tabs.add(tabbed, BorderLayout.CENTER);
        page.add(tabs, BorderLayout.CENTER);
        return page;
    }

    private JComponent createSettingsPage() {
        JPanel page = pagePanel();
        page.add(titleBlock("Settings & local data", "Offline application data locations and simulation limitations."), BorderLayout.NORTH);
        JPanel details = panelCard();
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
        details.add(sectionLabel("APPLICATION DATA"));
        details.add(new JLabel("Root: " + paths.root()));
        details.add(new JLabel("Sessions: " + paths.saves()));
        details.add(new JLabel("Reports: " + paths.reports()));
        details.add(new JLabel("SQLite: " + paths.database()));
        details.add(Box.createVerticalStrut(18));
        details.add(new JLabel("<html><div style='width:700px;color:#9caec5'>Educational limitations: the schematic map has no geographic coordinates; scoring inputs are invented; mission results and preparedness point changes are illustrative. In an actual emergency, follow local emergency-management authority guidance.</div></html>"));
        page.add(details, BorderLayout.NORTH);
        return page;
    }

    private void bindEngine() {
        EarthquakeEngine boundEngine = engine;
        database.syncInventory(engine.resources().totalInventory(), engine.resources().inventory());
        database.saveScenario(engine.config().name(), "EDUCATIONAL_EXERCISE", engine.config().intensity(), engine.config().seed());
        engine.addListener(snapshot -> SwingUtilities.invokeLater(() -> {
            if (engine != boundEngine) return;
            for (var event : boundEngine.city().events()) {
                if (persistedEventIds.add(event.id())) {
                    database.recordEvent(event.id(), event.type(), event.zoneId(), event.severity(),
                            event.tick(), event.description(), event.timestamp().toString());
                    database.saveOperationalLog(event.type(), event.description(), event.timestamp().toString());
                    archive.append(event).whenComplete((ignored, error) -> {
                        if (error != null) SwingUtilities.invokeLater(() -> statusLabel.setText("Archive write failed: " + rootMessage(error)));
                    });
                    logService.log(event.type() + " tick=" + event.tick() + " zone=" + event.zoneId());
                }
            }
            for (var mission : snapshot.missions()) {
                String signature = mission.status() + "|" + mission.progress();
                String prior = persistedMissionsState.put(mission.id(), signature);
                if (prior == null) database.createMission(mission.id(), mission.type(), mission.zone(), mission.allocation());
                else if (!prior.equals(signature)) database.updateMission(mission.id(), mission.status().name(),
                        mission.progress(), mission.allocation());
            }
            for (var zone : snapshot.zones()) database.saveZoneHistory(zone.id(), zone.hazard(),
                    zone.preparedness(), zone.infrastructure(), snapshot.tick());
            updateSnapshot(snapshot);
        }));
        updateSnapshot(engine.snapshot());
    }

    private void updateSnapshot(SimulationSnapshot snapshot) {
        if (snapshot == null) return;
        if (snapshot.processedEvents() > timelineEventCount) timelineCleared = false;
        timelineEventCount = Math.max(timelineEventCount, snapshot.processedEvents());
        latest = snapshot;
        maps.forEach(riskMap -> { riskMap.snapshot = snapshot; riskMap.repaint(); });
        scenarioLabel.setText(engine.config().name());
        stateLabel.setText("● " + snapshot.state());
        stateLabel.setForeground(switch (snapshot.state()) {
            case RUNNING -> new Color(78, 208, 154);
            case PAUSED -> new Color(255, 195, 91);
            case STOPPED -> new Color(241, 113, 113);
            default -> MUTED;
        });
        clockLabel.setText("SIM T+" + snapshot.tick());
        databaseLabel.setText(database.available() ? "DB ● online" : "DB ○ offline");
        databaseLabel.setForeground(database.available() ? new Color(78, 208, 154) : new Color(255, 195, 91));
        metricCards.removeAll();
        metricCards.add(metric("Preparedness score", snapshot.preparedness() + "%", "Community readiness index"));
        metricCards.add(metric("Affected zones", snapshot.zones().stream().filter(z -> z.hazard() > 0).count() + " / 6", "Simulated zone indicators"));
        metricCards.add(metric("Active incidents", snapshot.zones().stream()
                .filter(z -> !z.infrastructure().equals("Operational")).count() + "", "Zones with simulated disruption"));
        metricCards.add(metric("Active missions", snapshot.missions().stream()
                .filter(m -> m.status() == MissionStatus.ACTIVE).count() + "", "Concurrent response work"));
        metricCards.add(metric("Available resources", snapshot.resources().values().stream().mapToInt(Integer::intValue).sum() + "", "Across all resource types"));
        metricCards.revalidate(); metricCards.repaint();
        refreshTimeline(snapshot);
        readinessBar.setValue(snapshot.zones().stream().filter(z -> z.id().equals(selectedId(prepZone))).findFirst().map(SimulationSnapshot.ZoneView::preparedness).orElse(snapshot.preparedness()));
        for (var entry : preparednessChecks.entrySet()) {
            boolean complete = engine.city().completedTasks().contains(entry.getKey());
            entry.getValue().setSelected(complete);
            entry.getValue().setEnabled(!complete);
        }
        updateControlStatus(snapshot);
        var selected = snapshot.zones().stream().filter(z -> z.id().equals(selectedZone)).findFirst().orElse(snapshot.zones().get(0));
        mapDetails.setText("<html><b>" + selected.name() + "</b><br><br>Simulated risk: " + selected.hazard()
                + " (" + SeverityLevel.fromScore(selected.hazard()) + ")<br>Readiness: " + selected.preparedness()
                + "%<br>Fictional vulnerability input: " + selected.vulnerability()
                + "<br>Population exposure index: " + selected.exposure()
                + "<br>Infrastructure: " + selected.infrastructure()
                + "<br><br>Available support:<br>" + snapshot.resources().entrySet().stream()
                .map(e -> e.getKey().label() + ": " + e.getValue())
                .collect(java.util.stream.Collectors.joining("<br>"))
                + "<br><br><font color='#9caec5'>Illustrative values only; not a structural assessment.</font></html>");
        responseResources.setText("Available units: " + snapshot.resources());
        missionText.setText(snapshot.missions().isEmpty() ? "No missions dispatched yet." :
                String.join("\n\n", snapshot.missions().stream().map(m -> m.type() + "\n" + m.zone()
                        + " · " + m.status() + " · " + m.progress() + "%").toList()));
        long completed = snapshot.missions().stream().filter(m -> m.status() == MissionStatus.COMPLETED).count();
        int resourceTotal = engine.resources().totalInventory().values().stream().mapToInt(Integer::intValue).sum();
        int resourceAvailable = snapshot.resources().values().stream().mapToInt(Integer::intValue).sum();
        String riskCounts = java.util.Arrays.stream(SeverityLevel.values()).map(level -> level + ": "
                + snapshot.zones().stream().filter(zone -> SeverityLevel.fromScore(zone.hazard()) == level).count())
                .collect(java.util.stream.Collectors.joining(" · "));
        String readinessByZone = snapshot.zones().stream().map(zone -> zone.id() + " " + zone.preparedness() + "%")
                .collect(java.util.stream.Collectors.joining(" · "));
        analyticsSummary.setText("<html>Processed events: " + snapshot.processedEvents()
                + "<br>Readiness before / after: " + engine.config().preparedness() + "% / " + snapshot.preparedness() + "%"
                + "<br>Risk distribution: " + riskCounts
                + "<br>Zone readiness: " + readinessByZone
                + "<br>Missions completed: " + completed + " / " + snapshot.missions().size()
                + "<br>Mission recovery progress: " + (snapshot.missions().isEmpty() ? 0
                : snapshot.missions().stream().mapToInt(SimulationSnapshot.MissionView::progress).sum() / snapshot.missions().size())
                + "%<br>Resource utilization: " + (resourceTotal - resourceAvailable) + " / " + resourceTotal
                + " units<br><br>Available resource inventory:<br>" + snapshot.resources().entrySet().stream()
                .map(e -> e.getKey().label() + ": " + e.getValue()).collect(java.util.stream.Collectors.joining("<br>"))
                + "</html>");
        inspectorText.setText("Simulation state: " + snapshot.state() + "\nTick: " + snapshot.tick()
                + "\nEvent buffer: " + snapshot.queueSize() + " / 12\nProcessed events: " + snapshot.processedEvents()
                + "\nDatabase online: " + database.available() + "\nDatabase operations: " + database.operations()
                + "\nDatabase worker: " + database.workerStatus()
                + "\nLast database error: " + (database.lastError().isBlank() ? "none" : database.lastError())
                + "\nArchive file: " + paths.archive() + "\nArchive bytes: " + archiveSize()
                + "\nSaved sessions: " + savedSessionsSummary
                + "\n\nWorker states:\n" + String.join("\n", engine.workerStates()));
        repaint();
    }

    private void updateHeader() {
        if (latest != null) {
            clockLabel.setText("SIM T+" + latest.tick());
            databaseLabel.setText(database.available() ? "DB ● online" : "DB ○ offline");
            if (selectedPage.equals("Analytics")) updateAnalytics();
        }
    }

    private void updateAnalytics() {
        content.repaint();
        // The labels and chart read from the immutable latest snapshot when painted.
    }

    private void refreshTimeline(SimulationSnapshot snapshot) {
        if (timelineCleared || snapshot == null) return;
        String filter = Objects.toString(timelineFilter.getSelectedItem(), "All events");
        List<String> visible = snapshot.recentEvents().stream().filter(entry -> switch (filter) {
            case "Earthquakes" -> entry.startsWith("EARTHQUAKE");
            case "Aftershocks" -> entry.startsWith("AFTERSHOCK");
            case "Infrastructure" -> entry.startsWith("INFRASTRUCTURE") || entry.startsWith("POWER")
                    || entry.startsWith("WATER") || entry.startsWith("ROAD");
            case "Recovery" -> entry.startsWith("RECOVERY");
            case "Preparedness" -> entry.startsWith("OPERATOR_ACTION");
            case "Missions" -> entry.startsWith("Mission");
            default -> true;
        }).toList();
        timeline.setText(visible.isEmpty()
                ? (snapshot.processedEvents() == 0 ? "No simulation events yet. Start an exercise to see live activity here."
                : "No events match this filter.")
                : String.join("\n\n", visible));
        timeline.setCaretPosition(timeline.getDocument().getLength());
    }

    private void updateControlStatus(SimulationSnapshot snapshot) {
        if (startButton == null) return;
        int durationTicks = engine.config().durationTicks();
        int tick = (int) Math.min(snapshot.tick(), durationTicks);
        simulationProgress.setMaximum(Math.max(1, durationTicks));
        simulationProgress.setValue(tick);
        simulationProgress.setString(tick + " / " + durationTicks + " ticks");
        controlStateLabel.setText(snapshot.state().toString());
        controlStateLabel.setForeground(switch (snapshot.state()) {
            case RUNNING -> new Color(78, 208, 154);
            case PAUSED -> new Color(255, 195, 91);
            case STOPPED -> new Color(241, 113, 113);
            default -> TEXT;
        });
        controlDetailsLabel.setText(switch (snapshot.state()) {
            case READY -> "Ready — start the feed or advance one tick.";
            case RUNNING -> "Live feed active · " + snapshot.processedEvents() + " events processed.";
            case PAUSED -> "Feed paused at tick " + snapshot.tick() + " · resume when ready.";
            case STOPPED -> snapshot.tick() >= durationTicks
                    ? "Exercise complete · " + snapshot.processedEvents() + " events processed."
                    : "Exercise stopped · apply a new scenario to run again.";
        });
        startButton.setText(snapshot.state() == SimulationState.PAUSED ? "Resume simulation" : "Start simulation");
        startButton.setEnabled(snapshot.state() == SimulationState.READY || snapshot.state() == SimulationState.PAUSED);
        pauseButton.setText(snapshot.state() == SimulationState.PAUSED ? "Resume feed" : "Pause feed");
        pauseButton.setEnabled(snapshot.state() == SimulationState.RUNNING || snapshot.state() == SimulationState.PAUSED);
        stopButton.setEnabled(snapshot.state() == SimulationState.RUNNING || snapshot.state() == SimulationState.PAUSED);
        stepButton.setEnabled(snapshot.state() == SimulationState.READY && snapshot.tick() < durationTicks);
    }

    private void refreshSavedSessions() {
        new SwingWorker<List<SessionManager.SaveInfo>, Void>() {
            @Override protected List<SessionManager.SaveInfo> doInBackground() throws Exception { return sessions.list(); }
            @Override protected void done() {
                try {
                    List<SessionManager.SaveInfo> saved = get();
                    savedSessionsSummary = saved.isEmpty() ? "none" : saved.size() + " · latest " + saved.get(0).modified();
                    if (latest != null) updateSnapshot(latest);
                } catch (Exception ex) { showError("Could not inspect saved sessions", ex); }
            }
        }.execute();
    }
    private void updateHistory() {
        database.recentEvents(200).whenComplete((rows, error) -> SwingUtilities.invokeLater(() -> {
            String db = error == null ? "SQLite recent event history (up to 200 records):\n"
                    + (rows.isEmpty() ? "No database events recorded yet." : String.join("\n", rows))
                    : "SQLite unavailable: " + rootMessage(error);
            historyText.setText(db + "\n\nSession saves are stored locally under:\n" + paths.saves()
                    + "\n\nCurrent session activity:\n" + (latest == null ? "" : String.join("\n", latest.recentEvents())));
        }));
    }

    private void startSimulation() {
        try {
            if (engine.state() == SimulationState.READY) engine.start();
            else if (engine.state() == SimulationState.PAUSED) engine.resume();
            statusLabel.setText("Simulation feed started.");
        }
        catch (IOException | RuntimeException ex) { showError("Could not start simulation", ex); }
    }

    private void setPresetValues() {
        int index = preset.getSelectedIndex();
        if (index != 3) customScenarioName = null;
        if (index == 0) { intensity.setValue(28); preparedness.setValue(65); aftershocks.setValue(1); }
        else if (index == 1) { intensity.setValue(52); preparedness.setValue(55); aftershocks.setValue(3); }
        else if (index == 2) { intensity.setValue(82); preparedness.setValue(42); aftershocks.setValue(5); }
        configSummary.setText("Preset sliders are illustrative scenario parameters; this app does not predict earthquake intensity.");
    }

    private void applyScenario() {
        try {
            ScenarioConfig config = configurationFromControls();
            long seed = config.seed();
            EarthquakeEngine prior = engine;
            if (prior.state() == SimulationState.RUNNING || prior.state() == SimulationState.PAUSED)
                new Thread(prior::stop, "earthquakeready-old-engine-shutdown").start();
            engine = new EarthquakeEngine(config);
            bindEngine();
            configSummary.setText("Applied. Reproducible seed " + seed + " · " + config.aftershocks() + " simulated aftershocks.");
            statusLabel.setText("Scenario configured; start it from Simulation Control.");
        } catch (IllegalArgumentException ex) {
            showError("Invalid scenario configuration", ex);
        }
    }

    private ScenarioConfig configurationFromControls() {
        String name = preset.getSelectedIndex() == 3 && customScenarioName != null
                ? customScenarioName : Objects.toString(preset.getSelectedItem());
        return new ScenarioConfig(name, intensity.getValue(),
                duration.getValue(), affectedZones.getValue(), aftershocks.getValue(),
                resources.getValue(), preparedness.getValue(), Long.parseLong(seedField.getText().trim()));
    }

    private void importScenarioConfig() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path selected = chooser.getSelectedFile().toPath();
        new SwingWorker<ScenarioConfig, Void>() {
            @Override protected ScenarioConfig doInBackground() throws Exception {
                return new ScenarioConfigLoader().read(selected);
            }
            @Override protected void done() {
                try {
                    ScenarioConfig config = get();
                    customScenarioName = config.name();
                    preset.setSelectedIndex(3);
                    intensity.setValue(config.intensity());
                    duration.setValue(config.durationTicks());
                    affectedZones.setValue(config.affectedZones());
                    aftershocks.setValue(config.aftershocks());
                    resources.setValue(config.initialResources());
                    preparedness.setValue(config.preparedness());
                    seedField.setText(Long.toString(config.seed()));
                    configSummary.setText("Loaded " + config.name() + " from " + selected.getFileName() + "; apply to activate.");
                } catch (Exception ex) { showError("Could not import scenario configuration", ex); }
            }
        }.execute();
    }

    private void exportScenarioConfig() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new java.io.File("earthquakeready-scenario.properties"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path selected = chooser.getSelectedFile().toPath();
        final ScenarioConfig config;
        try { config = configurationFromControls(); }
        catch (IllegalArgumentException ex) { showError("Invalid scenario configuration", ex); return; }
        new SwingWorker<Path, Void>() {
            @Override protected Path doInBackground() throws Exception {
                new ScenarioConfigLoader().write(selected, config);
                return selected;
            }
            @Override protected void done() {
                try { statusLabel.setText("Scenario configuration exported: " + get()); }
                catch (Exception ex) { showError("Could not export scenario configuration", ex); }
            }
        }.execute();
    }

    private void saveSession() {
        String name = JOptionPane.showInputDialog(this, "Session name", "EarthquakeReady session");
        if (name == null) return;
        EarthquakeEngine activeEngine = engine;
        new SwingWorker<Path, Void>() {
            @Override protected Path doInBackground() throws Exception { return sessions.save(name, activeEngine); }
            @Override protected void done() {
                try { statusLabel.setText("Session saved: " + get().getFileName()); }
                catch (Exception ex) { showError("Could not save session", ex); }
            }
        }.execute();
    }

    private void loadSession() {
        new SwingWorker<List<SessionManager.SaveInfo>, Void>() {
            @Override protected List<SessionManager.SaveInfo> doInBackground() throws Exception { return sessions.list(); }
            @Override protected void done() {
                try {
                    List<SessionManager.SaveInfo> saves = get();
                    if (saves.isEmpty()) { JOptionPane.showMessageDialog(MainFrame.this, "No saved sessions found."); return; }
                    List<String> labels = saves.stream().map(info -> info.path().getFileName() + " · " + info.size() + " bytes · "
                            + DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(info.modified().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime())).toList();
                    Object choice = JOptionPane.showInputDialog(MainFrame.this, "Choose a saved session", "Load session",
                            JOptionPane.PLAIN_MESSAGE, null, labels.toArray(), null);
                    if (choice == null) return;
                    int index = labels.indexOf(choice);
                    if (index < 0) return;
                    Path selected = saves.get(index).path();
                    EarthquakeEngine previous = engine;
                    new SwingWorker<SessionManager.Restored, Void>() {
                        @Override protected SessionManager.Restored doInBackground() throws Exception {
                            SessionManager.Restored restored = sessions.load(selected);
                            previous.stop();
                            return restored;
                        }
                        @Override protected void done() {
                            try {
                                SessionManager.Restored restored = get();
                                engine = restored.createEngine();
                                bindEngine();
                                setPage("Dashboard");
                                statusLabel.setText("Session restored in a safe ready state (previous state: " + restored.previousState() + ").");
                            } catch (Exception ex) { showError("Could not load session", ex); }
                        }
                    }.execute();
                } catch (Exception ex) { showError("Could not load session", ex); }
            }
        }.execute();
    }

    private void exportReport(boolean csv) {
        EarthquakeEngine activeEngine = engine;
        new SwingWorker<Path, Void>() {
            @Override protected Path doInBackground() throws Exception {
                return csv ? ReportExporter.exportCsv(paths.reports(), activeEngine) : ReportExporter.exportTxt(paths.reports(), activeEngine);
            }
            @Override protected void done() {
                try { statusLabel.setText("Report exported: " + get()); }
                catch (Exception ex) { showError("Could not export report", ex); }
            }
        }.execute();
    }

    private void setPage(String page) {
        selectedPage = page;
        cards.show(content, page);
        navigationButtons.forEach((name, button) -> {
            boolean selected = name.equals(page);
            button.setBackground(selected ? PANEL_ALT : new Color(15, 26, 43));
            button.setForeground(selected ? TEXT : MUTED);
            button.setFont(new Font("Segoe UI", selected ? Font.BOLD : Font.PLAIN, 13));
            button.setBorder(selected
                    ? new CompoundBorder(new MatteBorder(0, 3, 0, 0, BLUE), new EmptyBorder(0, 9, 0, 8))
                    : new EmptyBorder(0, 12, 0, 8));
        });
        if (page.equals("History & Reports")) updateHistory();
        if (page.equals("Analytics")) updateAnalytics();
        if (page.equals("Engineering Inspector")) refreshSavedSessions();
        statusLabel.setText(page);
    }

    private void addZoneChoices(JComboBox<String> combo) {
        if (combo.getItemCount() > 0) return;
        for (var zone : engine.snapshot().zones()) combo.addItem(zone.id() + " — " + zone.name());
        styleComboBox(combo);
    }
    private String selectedId(JComboBox<String> combo) {
        Object value = combo.getSelectedItem();
        if (value == null) return "RES";
        String text = value.toString();
        return text.substring(0, Math.min(3, text.length()));
    }

    private static Map<ResourceType, Integer> allocationFor(String type) {
        if (type.startsWith("Prepare")) return Map.of(ResourceType.SHELTER_BEDS, 8, ResourceType.SUPPLY_KITS, 1);
        if (type.startsWith("Distribute")) return Map.of(ResourceType.SUPPLY_KITS, 3, ResourceType.COMMUNITY_TEAMS, 1);
        if (type.startsWith("Assess")) return Map.of(ResourceType.INSPECTION_TEAMS, 1);
        if (type.startsWith("Coordinate")) return Map.of(ResourceType.UTILITY_TEAMS, 1);
        return Map.of(ResourceType.FIRST_AID, 1, ResourceType.COMMUNITY_TEAMS, 1);
    }

    private static PreparednessCategory preparednessCategory(String task) {
        if (task.startsWith("Emergency kit")) return PreparednessCategory.HOUSEHOLD;
        if (task.startsWith("Household communication")) return PreparednessCategory.COMMUNICATION;
        if (task.startsWith("Emergency contact")) return PreparednessCategory.CONTACTS;
        if (task.startsWith("Meeting-point")) return PreparednessCategory.MEETING_POINT;
        if (task.startsWith("Earthquake safety")) return PreparednessCategory.SAFETY_AWARENESS;
        if (task.startsWith("Building")) return PreparednessCategory.BUILDING;
        if (task.startsWith("Community")) return PreparednessCategory.COMMUNITY;
        if (task.startsWith("Shelter")) return PreparednessCategory.SHELTER;
        throw new IllegalArgumentException("Uncategorized preparedness activity: " + task);
    }

    private void shutdown() {
        statusLabel.setText("Closing services…");
        new Thread(() -> {
            engine.close();
            try { SwingUtilities.invokeAndWait(() -> {}); }
            catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
            catch (java.lang.reflect.InvocationTargetException ex) {
                System.err.println("Could not flush final UI updates: " + ex.getMessage());
            }
            database.close();
            archive.close();
            logService.close();
            SwingUtilities.invokeLater(() -> dispose());
        }, "earthquakeready-app-shutdown").start();
    }
    private long archiveSize() {
        try { return java.nio.file.Files.exists(paths.archive()) ? java.nio.file.Files.size(paths.archive()) : 0L; }
        catch (IOException ex) { return -1L; }
    }
    private void showError(String title, Throwable error) {
        JOptionPane.showMessageDialog(this, rootMessage(error), title, JOptionPane.ERROR_MESSAGE);
    }
    private static String rootMessage(Throwable error) {
        Throwable current = error;
        while ((current.getCause() != null) && (current instanceof CompletionException || current instanceof java.util.concurrent.ExecutionException))
            current = current.getCause();
        return current.getMessage() == null ? current.toString() : current.getMessage();
    }

    private static JPanel pagePanel() {
        JPanel panel = new JPanel(new BorderLayout(16, 18));
        panel.setBackground(BG);
        panel.setBorder(new EmptyBorder(24, 24, 20, 24));
        return panel;
    }
    private static JPanel titleBlock(String title, String subtitle) {
        JPanel block = new JPanel();
        block.setOpaque(false);
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
        JLabel heading = new JLabel(title);
        heading.setForeground(TEXT); heading.setFont(new Font("Segoe UI", Font.BOLD, 24));
        JLabel sub = new JLabel(subtitle);
        sub.setForeground(MUTED); sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        block.add(heading); block.add(Box.createVerticalStrut(5)); block.add(sub);
        return block;
    }
    private static JPanel panelCard() {
        JPanel panel = new JPanel();
        panel.setBackground(PANEL);
        panel.setBorder(new CompoundBorder(new LineBorder(new Color(42, 59, 81), 1, true), new EmptyBorder(17, 18, 17, 18)));
        return panel;
    }
    private static JLabel sectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(new Color(131, 160, 191));
        label.setFont(new Font("Segoe UI", Font.BOLD, 11));
        label.setBorder(new EmptyBorder(0, 0, 10, 0));
        return label;
    }
    private static JPanel metric(String title, String value, String caption) {
        JPanel card = panelCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        JLabel top = new JLabel(title.toUpperCase(Locale.ROOT));
        top.setForeground(MUTED); top.setFont(new Font("Segoe UI", Font.BOLD, 10));
        JLabel number = new JLabel(value);
        number.setForeground(TEXT); number.setFont(new Font("Segoe UI", Font.BOLD, 26));
        JLabel note = new JLabel(caption);
        note.setForeground(MUTED); note.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        card.add(top); card.add(Box.createVerticalStrut(8)); card.add(number); card.add(Box.createVerticalStrut(3)); card.add(note);
        return card;
    }
    private static JTextArea textArea() {
        JTextArea area = new JTextArea();
        area.setEditable(false); area.setLineWrap(true); area.setWrapStyleWord(true);
        area.setBackground(PANEL_ALT); area.setForeground(TEXT);
        area.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        area.setBorder(new EmptyBorder(12, 12, 12, 12));
        return area;
    }
    private static JButton primaryButton(String text) {
        JButton button = new JButton(text);
        button.setForeground(Color.WHITE); button.setBackground(BLUE);
        button.setBorder(new EmptyBorder(10, 16, 10, 16));
        button.setOpaque(true);
        button.setBorderPainted(true);
        button.setFocusPainted(false); button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }
    private static JButton secondaryButton(String text) {
        JButton button = new JButton(text);
        button.setForeground(TEXT); button.setBackground(PANEL_ALT);
        button.setBorder(new CompoundBorder(new LineBorder(new Color(56, 76, 100), 1, true), new EmptyBorder(8, 12, 8, 12)));
        button.setOpaque(true);
        button.setBorderPainted(true);
        button.setFocusPainted(false); button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }
    private static void styleLabel(JLabel label, int size, boolean bold) {
        label.setForeground(TEXT); label.setFont(new Font("Segoe UI", bold ? Font.BOLD : Font.PLAIN, size));
    }
    private static JPanel labeled(String label, JComponent component) {
        JPanel row = new JPanel(new BorderLayout(10, 6));
        row.setOpaque(false);
        JLabel name = new JLabel(label);
        name.setForeground(MUTED);
        row.add(name, BorderLayout.NORTH); row.add(component, BorderLayout.CENTER);
        row.setBorder(new EmptyBorder(8, 0, 8, 0));
        limitHeight(row);
        return row;
    }
    private static void limitHeight(JComponent component) {
        Dimension preferred = component.getPreferredSize();
        component.setMaximumSize(new Dimension(Integer.MAX_VALUE, preferred.height));
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
    }
    private static void styleTextField(JTextField field) {
        field.setBackground(PANEL_ALT);
        field.setForeground(TEXT);
        field.setCaretColor(TEXT);
        field.setSelectionColor(BLUE);
        field.setSelectedTextColor(TEXT);
        field.setBorder(new CompoundBorder(new LineBorder(new Color(56, 76, 100), 1, true),
                new EmptyBorder(6, 9, 6, 9)));
    }
    private static void styleComboBox(JComboBox<?> combo) {
        combo.setBackground(PANEL_ALT);
        combo.setForeground(TEXT);
        combo.setOpaque(true);
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                                     boolean selected, boolean focus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focus);
                label.setBackground(selected ? BLUE : PANEL_ALT);
                label.setForeground(TEXT);
                label.setBorder(new EmptyBorder(5, 8, 5, 8));
                return label;
            }
        });
    }
    private static void addFormRow(JPanel panel, GridBagConstraints c, int row, String label, JComponent component) {
        c.gridy = row; c.gridx = 0; c.weightx = 0.3;
        JLabel title = new JLabel(label); title.setForeground(MUTED); panel.add(title, c);
        c.gridx = 1; c.weightx = 0.7; panel.add(component, c);
    }
    private static void addSliderRow(JPanel panel, GridBagConstraints c, int row, String label, JSlider slider) {
        slider.setOpaque(false); slider.setForeground(TEXT);
        slider.setMajorTickSpacing(Math.max(10, slider.getMaximum() / 4)); slider.setPaintTicks(true);
        JPanel control = new JPanel(new BorderLayout(8, 0));
        control.setOpaque(false);
        JLabel value = new JLabel(Integer.toString(slider.getValue()));
        value.setForeground(TEXT);
        slider.addChangeListener(event -> value.setText(Integer.toString(slider.getValue())));
        control.add(slider, BorderLayout.CENTER);
        control.add(value, BorderLayout.EAST);
        addFormRow(panel, c, row, label, control);
    }

    private final class RiskMap extends JPanel {
        private SimulationSnapshot snapshot;
        RiskMap() {
            maps.add(this);
            setOpaque(true); setBackground(new Color(17, 31, 49));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent event) {
                    int col = event.getX() * 3 / Math.max(1, getWidth());
                    int row = event.getY() * 2 / Math.max(1, getHeight());
                    int index = Math.min(5, row * 3 + col);
                    if (snapshot != null) { selectedZone = snapshot.zones().get(index).id(); updateSnapshot(snapshot); }
                }
            });
        }
        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (snapshot == null) { g.dispose(); return; }
            int gap = 12, cellW = (getWidth() - gap * 4) / 3, cellH = (getHeight() - gap * 3) / 2;
            for (int i = 0; i < snapshot.zones().size(); i++) {
                var zone = snapshot.zones().get(i);
                int x = gap + (i % 3) * (cellW + gap), y = gap + (i / 3) * (cellH + gap);
                Color fill = riskColor(zone.hazard());
                g.setColor(fill); g.fillRoundRect(x, y, cellW, cellH, 17, 17);
                g.setColor(zone.id().equals(selectedZone) ? Color.WHITE : new Color(255, 255, 255, 70));
                g.setStroke(new BasicStroke(zone.id().equals(selectedZone) ? 2.4f : 1f));
                g.drawRoundRect(x, y, cellW, cellH, 17, 17);
                g.setColor(Color.WHITE);
                g.setFont(new Font("Segoe UI", Font.BOLD, 13));
                g.drawString(zone.id() + "  " + zone.hazard() + " risk", x + 13, y + 25);
                g.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                g.drawString(zone.name(), x + 13, y + 46);
                g.drawString("Readiness " + zone.preparedness() + "%", x + 13, y + 65);
            }
            g.dispose();
        }
    }

    private final class RiskChart extends JPanel {
        RiskChart() { setOpaque(false); }
        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (latest == null) return;
            Graphics2D g = (Graphics2D) graphics.create();
            int baseY = getHeight() - 34, chartH = getHeight() - 70, barW = Math.max(24, (getWidth() - 60) / 12);
            g.setColor(new Color(70, 91, 115)); g.drawLine(35, baseY, getWidth() - 12, baseY);
            int x = 45;
            for (var zone : latest.zones()) {
                int h = Math.max(3, chartH * zone.hazard() / 100);
                g.setColor(riskColor(zone.hazard())); g.fillRoundRect(x, baseY - h, barW, h, 8, 8);
                g.setColor(TEXT); g.setFont(new Font("Segoe UI", Font.BOLD, 11)); g.drawString(Integer.toString(zone.hazard()), x, baseY - h - 6);
                g.setFont(new Font("Segoe UI", Font.PLAIN, 10)); g.setColor(MUTED); g.drawString(zone.id(), x + 3, baseY + 16);
                x += barW + 12;
            }
            g.dispose();
        }
    }

    private static Color riskColor(int score) {
        if (score < 25) return new Color(39, 139, 103);
        if (score < 50) return new Color(185, 148, 48);
        if (score < 75) return new Color(201, 111, 42);
        return new Color(177, 64, 69);
    }
}
