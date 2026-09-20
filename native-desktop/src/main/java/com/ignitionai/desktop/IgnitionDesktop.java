package com.ignitionai.desktop;

import com.ignitionai.application.*;
import com.ignitionai.obdinput.schema.ObdMessage;
import com.ignitionai.phase6.*;
import com.ignitionai.phase7.latex.LatexGenerator;
import com.ignitionai.phase7.validation.*;
import com.ignitionai.virtualsensors.index.DiagnosticIndexNarrator;
import com.ignitionai.virtualsensors.index.DiagnosticIndexResult;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Native Windows/Swing vehicle assessment application. This app only ingests telemetry (a real
 * capture or a log produced by the standalone {@link IgnitionSimulator} tool via "Open OBD
 * session") -- it does not generate any itself, so it cannot show a favorable result on data it
 * fabricated. See IgnitionSimulator for the separate log-generation tool. */
public final class IgnitionDesktop extends JFrame {
    // Same palette as the certificate's iaiNavy/iaiGood/iaiWarn/iaiBad/iaiSlate so the app chrome
    // and the PDF read as one product instead of a debug console next to a client-facing report.
    private static final Color BRAND_NAVY = Color.decode("#1F2937");
    private static final Color BRAND_SLATE = Color.decode("#475569");
    private static final Color BRAND_GOOD = Color.decode("#15803D");
    private static final Color BRAND_WARN = Color.decode("#B45309");
    private static final Color BRAND_BAD = Color.decode("#B91C1C");
    private static final Color BRAND_PANEL = Color.decode("#F3F4F6");

    private final JTextArea summary = new JTextArea("Open an OBD session file to begin (use the standalone Simulator tool to generate one).");
    private final JTextArea evidence = new JTextArea();
    private final JTextArea dtcArea = new JTextArea();
    private final DefaultTableModel scores = new DefaultTableModel(new String[]{"Indicator", "Health", "Severity", "Confidence", "Data status"}, 0) {
        public boolean isCellEditable(int row, int col) { return false; }
    };
    private final JLabel status = new JLabel("Ready • Stored on this computer");
    private final JComboBox<String> chartSignal = new JComboBox<>();
    private final TracePanel trace = new TracePanel();
    private final DefaultListModel<Path> history = new DefaultListModel<>();
    private final Path storage = Path.of(System.getProperty("user.home"), "IgnitionAI", "sessions");
    private final JPanel diagnosticIndexList = new JPanel();
    private SwingWorker<InspectionResult, Void> worker;
    private InspectionResult result;
    private final List<AbstractButton> taskButtons = new ArrayList<>();

    public IgnitionDesktop() {
        super("IgnitionAI • Vehicle Assessment");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(900, 650)); setSize(1160, 800); setLocationRelativeTo(null);
        JPanel root = new JPanel(new BorderLayout(12, 12)); root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16)); setContentPane(root);
        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        ImageIcon logo = logoIcon(30);
        if (logo != null) brand.add(new JLabel(logo));
        else { JLabel wordmark = new JLabel("ignitionAI"); wordmark.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 23)); wordmark.setForeground(BRAND_NAVY); brand.add(wordmark); }
        JLabel heading = new JLabel("Vehicle assessment"); heading.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18)); heading.setForeground(BRAND_SLATE);
        brand.add(heading);
        JPanel top = new JPanel(new BorderLayout()); top.add(brand, BorderLayout.NORTH);
        JToolBar actions = new JToolBar(); actions.setFloatable(false);
        addAction(actions, "Open OBD session", this::openSession);
        addAction(actions, "Capture live ELM327", this::captureLiveElm327);
        addAction(actions, "Save session", this::saveSession);
        addAction(actions, "Export certificate", this::exportCertificate);
        addAction(actions, "Export repair plan", this::exportRepairPlan);
        JButton cancel = new JButton("Cancel"); cancel.addActionListener(e -> { if (worker != null) worker.cancel(true); }); actions.add(cancel);
        top.add(actions, BorderLayout.SOUTH); root.add(top, BorderLayout.NORTH);
        JTabbedPane tabs = new JTabbedPane(); root.add(tabs, BorderLayout.CENTER);
        summary.setEditable(false); summary.setLineWrap(true); summary.setWrapStyleWord(true); summary.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
        JPanel assessment = new JPanel(new BorderLayout(8, 8)); assessment.add(new JScrollPane(summary), BorderLayout.NORTH);
        summary.setRows(8); JTable table = new JTable(scores); table.setRowHeight(30);
        JPanel middle = new JPanel(new BorderLayout(8, 8)); middle.add(new JScrollPane(table), BorderLayout.CENTER);
        dtcArea.setEditable(false); dtcArea.setFont(new Font(Font.MONOSPACED, Font.BOLD, 14)); dtcArea.setForeground(BRAND_BAD); dtcArea.setRows(2);
        JPanel dtcPanel = new JPanel(new BorderLayout(4, 2));
        JLabel dtcLabel = new JLabel("Diagnostic Trouble Codes"); dtcLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13)); dtcLabel.setForeground(BRAND_NAVY);
        dtcPanel.add(dtcLabel, BorderLayout.NORTH); dtcPanel.add(new JScrollPane(dtcArea), BorderLayout.CENTER);
        middle.add(dtcPanel, BorderLayout.SOUTH);
        assessment.add(middle, BorderLayout.CENTER);
        tabs.addTab("Assessment", assessment);
        diagnosticIndexList.setLayout(new BoxLayout(diagnosticIndexList, BoxLayout.Y_AXIS));
        diagnosticIndexList.setBackground(Color.WHITE);
        JScrollPane diagnosticIndexScroll = new JScrollPane(diagnosticIndexList);
        diagnosticIndexScroll.getVerticalScrollBar().setUnitIncrement(16);
        tabs.addTab("Diagnostic Index", diagnosticIndexScroll);
        JPanel charts = new JPanel(new BorderLayout(8, 8)); charts.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JLabel chartHeading = new JLabel("Signal trace"); chartHeading.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13)); chartHeading.setForeground(BRAND_NAVY);
        JPanel chartHeader = new JPanel(new BorderLayout(10, 0));
        chartHeader.add(chartHeading, BorderLayout.WEST); chartHeader.add(chartSignal, BorderLayout.EAST);
        chartSignal.setPreferredSize(new Dimension(260, chartSignal.getPreferredSize().height));
        charts.add(chartHeader, BorderLayout.NORTH); charts.add(trace, BorderLayout.CENTER);
        chartSignal.addActionListener(e -> { trace.signal = (String)chartSignal.getSelectedItem(); trace.repaint(); }); tabs.addTab("Telemetry", charts);
        evidence.setEditable(false); evidence.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12)); tabs.addTab("Evidence and model status", new JScrollPane(evidence));
        JList<Path> sessions = new JList<>(history); sessions.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        sessions.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13)); sessions.setFixedCellHeight(26);
        sessions.setCellRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean hasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, hasFocus);
                if (value instanceof Path path) setText(path.getFileName().toString());
                setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
                return c;
            }
        });
        JPanel historyPanel = new JPanel(new BorderLayout(8, 8)); historyPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JLabel historyHeading = new JLabel("Saved sessions"); historyHeading.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13)); historyHeading.setForeground(BRAND_NAVY);
        historyPanel.add(historyHeading, BorderLayout.NORTH);
        historyPanel.add(new JScrollPane(sessions), BorderLayout.CENTER);
        JPanel historyActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        addAction(historyActions, "Open selected", () -> { if (sessions.getSelectedValue() != null) load(sessions.getSelectedValue()); });
        addAction(historyActions, "Compare with current", () -> compare(sessions.getSelectedValue()));
        addAction(historyActions, "Delete selected", () -> {
            Path selected = sessions.getSelectedValue();
            if (selected != null && JOptionPane.showConfirmDialog(this, "Delete " + selected.getFileName() + "?", "Delete saved session", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
                attempt(() -> { Files.delete(selected); refreshHistory(); });
        });
        historyPanel.add(historyActions, BorderLayout.SOUTH); tabs.addTab("History", historyPanel);
        root.add(status, BorderLayout.SOUTH); refreshHistory();
        addWindowListener(new java.awt.event.WindowAdapter() { public void windowClosed(java.awt.event.WindowEvent e) { if (worker != null) worker.cancel(true); } });
    }
    private java.io.File sensorDirectory() {
        String override = System.getProperty("ignitionai.sensors");
        if (override != null) return new java.io.File(override);
        try {
            java.io.File location = new java.io.File(IgnitionDesktop.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            java.io.File packaged = new java.io.File(location.getParentFile(), "sensors");
            if (packaged.isDirectory()) return packaged;
        } catch (Exception ignored) { }
        return new java.io.File("virtual-sensors/src/main/resources/sensors");
    }
    private java.io.File logoFile() {
        String override = System.getProperty("ignitionai.logo");
        if (override != null) return new java.io.File(override);
        try {
            java.io.File location = new java.io.File(IgnitionDesktop.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            java.io.File packaged = new java.io.File(location.getParentFile(), "branding/ignitionai_logo.png");
            if (packaged.isFile()) return packaged;
        } catch (Exception ignored) { }
        return new java.io.File("latex-certificate-generator/src/main/resources/branding/ignitionai_logo.png");
    }
    /** Scaled app-chrome logo, or null if the branding asset isn't present (falls back to a text
     * wordmark), mirroring the same \IfFileExists graceful-degradation the certificate itself uses. */
    private ImageIcon logoIcon(int height) {
        try {
            java.io.File file = logoFile();
            if (!file.isFile()) return null;
            BufferedImage img = ImageIO.read(file);
            if (img == null) return null;
            int width = Math.max(1, Math.round(height * img.getWidth() / (float) img.getHeight()));
            return new ImageIcon(img.getScaledInstance(width, height, Image.SCALE_SMOOTH));
        } catch (Exception ignored) { return null; }
    }
    private Color stateColor(DiagnosticIndexResult.State state) {
        return switch (state) {
            case NORMAL -> BRAND_GOOD;
            case FLAGGED -> BRAND_BAD;
            case INSUFFICIENT_DATA -> BRAND_WARN;
            case UNSUPPORTED -> BRAND_SLATE;
        };
    }
    /** One card per Diagnostic Index result: a color-coded status chip, health/confidence, and the
     * same plain-English finding the certificate shows -- so the live app and the PDF never tell a
     * technician two different stories about the same result. */
    private JPanel buildIndexCard(DiagnosticIndexResult r) {
        JPanel card = new JPanel(new BorderLayout(10, 4));
        card.setBackground(BRAND_PANEL);
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0), BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 96));

        JLabel chip = new JLabel(" " + r.getState().name().replace('_', ' ') + " ");
        chip.setOpaque(true); chip.setBackground(stateColor(r.getState())); chip.setForeground(Color.WHITE);
        chip.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));

        JLabel title = new JLabel(r.getDisplayName()
            + (r.getHealthPct() != null ? String.format(Locale.ROOT, "  —  %.0f%% health", r.getHealthPct()) : "")
            + (r.getConfidencePct() != null ? String.format(Locale.ROOT, "  ·  %.0f%% confidence", r.getConfidencePct()) : ""));
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14)); title.setForeground(BRAND_NAVY);

        JPanel head = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); head.setOpaque(false);
        head.add(chip); head.add(title);

        JTextArea finding = new JTextArea(DiagnosticIndexNarrator.plainEnglishFinding(r));
        finding.setEditable(false); finding.setLineWrap(true); finding.setWrapStyleWord(true); finding.setOpaque(false);
        finding.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13)); finding.setForeground(BRAND_SLATE);

        card.add(head, BorderLayout.NORTH); card.add(finding, BorderLayout.CENTER);
        return card;
    }
    private void addAction(Container panel, String name, Runnable action) {
        JButton b = new JButton(name); b.addActionListener(e -> action.run()); panel.add(b); taskButtons.add(b);
    }
    private interface Task { InspectionResult run() throws Exception; }
    private interface Action { void run() throws Exception; }
    private void attempt(Action action) { try { action.run(); } catch (Exception e) { error(e); } }
    private void error(Exception e) { status.setText("Action failed"); JOptionPane.showMessageDialog(this, e.getMessage(), "IgnitionAI", JOptionPane.ERROR_MESSAGE); }
    private void runTask(Task task) {
        taskButtons.forEach(b -> b.setEnabled(false)); status.setText("Processing…");
        worker = new SwingWorker<>() {
            protected InspectionResult doInBackground() throws Exception { return task.run(); }
            protected void done() {
                taskButtons.forEach(b -> b.setEnabled(true));
                try { if (isCancelled()) { status.setText("Cancelled"); return; } result = get(); display(); status.setText("Assessment complete • Preliminary model"); }
                catch (Exception e) { error(new Exception(e.getCause() != null ? e.getCause().getMessage() : e.getMessage(), e)); }
            }
        }; worker.execute();
    }
    private void display() {
        VehicleHealthAssessment a = result.certificate.getHealthAssessment();
        summary.setText(diagnosticIndexHeadline(result.certificate.getDiagnosticIndex())
            + "\nVehicle: " + a.getVehicleId() + "\nAssessed: " + a.getAssessmentTimestamp()
            + "\nLegacy VHI (provisional, superseded above): " + value(a.getVehicleHealthIndex()) + "   |   " + a.getHealthBand() + "   |   Data: " + a.getDataQualityStatus()
            + "\nCoverage: " + value(a.getSubsystemCoverage()) + "   |   Confidence: " + value(a.getOverallConfidence())
            + "\nEpisodes: " + result.episodes.size() + "   |   Records: " + result.messages.size()
            + "\n" + String.join("\n", result.notes));
        summary.setCaretPosition(0);
        scores.setRowCount(0);
        for (SubsystemHealthAssessment s : a.getSubsystemAssessments()) scores.addRow(new Object[]{s.getSubsystemName(), value(s.getScore()), s.getSeverity(), value(s.getConfidence()), String.join(", ", s.getDegradedDataFlags())});
        List<String> dtcs = result.certificate.getDtcCodes();
        dtcArea.setText(dtcs.isEmpty() ? "No active DTCs in the latest complete scan." : String.join(", ", dtcs));
        diagnosticIndexList.removeAll();
        for (DiagnosticIndexResult r : result.certificate.getDiagnosticIndex()) {
            diagnosticIndexList.add(buildIndexCard(r));
            diagnosticIndexList.add(Box.createRigidArea(new Dimension(0, 6)));
        }
        diagnosticIndexList.revalidate(); diagnosticIndexList.repaint();
        StringBuilder out = new StringBuilder();
        result.observations.forEach(o -> out.append(o.getObservationId()).append(" = ").append(o.getValue()).append(" ").append(o.getUnit()).append(" [").append(o.getQualityState()).append("]\n"));
        result.episodes.forEach(ep -> out.append("\nEpisode ").append(ep.getEpisodeId()).append("\n").append(String.join("\n", ep.getEvidenceReferences())).append("\n"));
        result.degradation.forEach(d -> out.append("\n").append(d.getSubsystemId()).append(": ").append(d.getDegradationState()).append("; risk ").append(d.getRiskStatus()).append("\n"));
        evidence.setText(out.toString()); evidence.setCaretPosition(0);
        trace.messages = result.messages; chartSignal.removeAllItems();
        TreeSet<String> ids = new TreeSet<>(); result.messages.forEach(m -> m.getSensorReadings().forEach(r -> ids.add(r.getSignalId())));
        ids.forEach(chartSignal::addItem); trace.repaint();
    }
    private static String value(Double n) { return n == null ? "Unavailable" : String.format(Locale.ROOT, "%.2f", n); }
    /** Same overall_condition-preferring headline as the certificate's at-a-glance page
     * (LatexGenerator.atAGlanceSection) -- kept in sync deliberately so the on-screen summary and
     * the exported PDF never tell a technician two different top-line numbers for the same session. */
    private static String diagnosticIndexHeadline(List<DiagnosticIndexResult> idx) {
        DiagnosticIndexResult overall = idx.stream().filter(r -> "overall_condition".equals(r.getIndexId())).findFirst().orElse(null);
        long supported, flagged;
        String health;
        if (overall != null && overall.getHealthPct() != null) {
            supported = parseRatioPart(overall.getEvidence().get("components_supported"), 1);
            flagged = Long.parseLong(overall.getEvidence().getOrDefault("components_flagged", "0"));
            health = String.format(Locale.ROOT, "%.0f", overall.getHealthPct());
        } else {
            supported = idx.stream().filter(r -> r.getHealthPct() != null && !"overall_condition".equals(r.getIndexId())).count();
            flagged = idx.stream().filter(r -> r.getState() == DiagnosticIndexResult.State.FLAGGED && !"overall_condition".equals(r.getIndexId())).count();
            double healthSum = 0; int healthCount = 0;
            for (DiagnosticIndexResult r : idx) if (r.getHealthPct() != null && !"overall_condition".equals(r.getIndexId())) { healthSum += r.getHealthPct(); healthCount++; }
            health = healthCount > 0 ? String.format(Locale.ROOT, "%.0f", healthSum / healthCount) : "UNAVAILABLE";
        }
        return "IgnitionAI Diagnostic Index: " + health + "/100  (" + flagged + " of " + supported + " assessed systems flagged -- see Diagnostic Index tab)";
    }
    private static long parseRatioPart(String ratio, int part) {
        if (ratio == null) return 0;
        String[] parts = ratio.split("/");
        try { return Long.parseLong(parts[part].trim()); } catch (Exception e) { return 0; }
    }
    private Path choose(boolean save, String suggested) {
        JFileChooser chooser = new JFileChooser(); if (suggested != null) chooser.setSelectedFile(new java.io.File(suggested));
        int answer = save ? chooser.showSaveDialog(this) : chooser.showOpenDialog(this);
        if (answer != JFileChooser.APPROVE_OPTION) return null;
        Path path = chooser.getSelectedFile().toPath();
        if (save && Files.exists(path) && JOptionPane.showConfirmDialog(this, "Overwrite existing file?", "Save", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return null;
        return path;
    }
    private void openSession() { Path p = choose(false, null); if (p != null) load(p); }
    // Package-visible (not private) so DesktopSmokeTest, in the same package, can drive a real
    // session through the app without automating the OS file-picker dialog `openSession` uses.
    void load(Path p) { runTask(() -> new AssessmentHistory(sensorDirectory(), storage).assess(SessionFiles.read(p))); }
    /** Connects to a genuinely independent third-party ELM327 protocol emulator (see
     * {@link tools.pipeline_runner.Elm327LiveCaptureRunner}) over TCP, captures real telemetry
     * frames, and feeds the capture through the same unmodified pipeline as any other session --
     * the honest answer to "you could be gaming your own system with your own simulator." Requires
     * the emulator already running separately (pip install elm327-emulator; python -m elm -n <port> -s car). */
    private void captureLiveElm327() {
        JTextField hostField = new JTextField("127.0.0.1");
        JTextField portField = new JTextField("35000");
        JTextField durationField = new JTextField("15000");
        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        form.add(new JLabel("Emulator host:")); form.add(hostField);
        form.add(new JLabel("Emulator port:")); form.add(portField);
        form.add(new JLabel("Capture duration (ms):")); form.add(durationField);
        if (JOptionPane.showConfirmDialog(this, form, "Capture live ELM327 session", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        String host = hostField.getText().trim();
        int port; long durationMs;
        try { port = Integer.parseInt(portField.getText().trim()); durationMs = Long.parseLong(durationField.getText().trim()); }
        catch (NumberFormatException e) { status.setText("Port and duration must be numbers"); return; }
        runTask(() -> {
            Path sessionFile = tools.pipeline_runner.Elm327LiveCaptureRunner.captureToFile(host, port, durationMs);
            return new AssessmentHistory(sensorDirectory(), storage).assess(SessionFiles.read(sessionFile));
        });
    }
    private void saveSession() {
        if (result == null) { status.setText("Generate or open a session first"); return; }
        attempt(() -> { Files.createDirectories(storage); Path p = storage.resolve(result.certificate.getCertificateId() + ".jsonl"); SessionFiles.write(p, result.messages); refreshHistory(); status.setText("Saved " + p); });
        Path export = choose(true, result.certificate.getCertificateId() + ".jsonl");
        if (export != null) attempt(() -> SessionFiles.write(export, result.messages));
    }
    private void refreshHistory() {
        history.clear(); if (!Files.isDirectory(storage)) return;
        try (java.util.stream.Stream<Path> paths = Files.list(storage)) { paths.filter(p -> p.toString().endsWith(".jsonl")).sorted().forEach(history::addElement); }
        catch (Exception e) { status.setText("History unavailable: " + e.getMessage()); }
    }
    private void compare(Path p) {
        if (p == null || result == null) { status.setText("Select a saved session and open a current assessment first"); return; }
        InspectionResult current = result;
        runTask(() -> {
            InspectionResult previous = new AssessmentHistory(sensorDirectory(), storage).assess(SessionFiles.read(p));
            VehicleHealthAssessment a = previous.certificate.getHealthAssessment(), b = current.certificate.getHealthAssessment();
            if (!a.getVehicleId().equals(b.getVehicleId())) throw new IllegalArgumentException("Compare assessments from the same vehicle");
            List<String> notes = new ArrayList<>(current.notes);
            notes.add("Comparison with " + a.getAssessmentTimestamp() + ": VHI " + value(a.getVehicleHealthIndex()) + " -> " + value(b.getVehicleHealthIndex()));
            return new InspectionResult(current.messages, current.episodes, current.degradation, current.observations, notes, current.certificate);
        });
    }
    /** Compiles into a throwaway temp directory and copies out only the finished PDF to where the
     * user asked to save -- the .tex source and pdflatex's .aux/.log/.out byproducts all stay in
     * the temp directory and never clutter the save location. On failure the temp directory is
     * kept (and named in the error) instead of being deleted, so the log is still inspectable. */
    private void exportCertificate() {
        if (result == null) { status.setText("Run an assessment first"); return; }
        Path pdfTarget = choose(true, result.certificate.getCertificateId() + ".pdf"); if (pdfTarget == null) return;
        InspectionResult selected = result;
        runTask(() -> {
            ValidationResult validation = new CertificateValidator().validateSnapshot(selected.certificate);
            if (!validation.isValid()) throw new IllegalArgumentException(String.join("\n", validation.getErrors()));
            compilePdf(new LatexGenerator().generateCertificate(selected.certificate), selected.certificate.getCertificateId(), pdfTarget);
            return selected;
        });
    }
    /** Root Cause Analysis & Repair Plan is deliberately a separate document from the certificate
     * (technician-facing action list vs. buyer/dealer-facing evidence), rendered from the same
     * CertificateSnapshot so the two can never disagree about what was found. */
    private void exportRepairPlan() {
        if (result == null) { status.setText("Run an assessment first"); return; }
        Path pdfTarget = choose(true, result.certificate.getCertificateId() + "-repair-plan.pdf"); if (pdfTarget == null) return;
        InspectionResult selected = result;
        runTask(() -> {
            ValidationResult validation = new CertificateValidator().validateSnapshot(selected.certificate);
            if (!validation.isValid()) throw new IllegalArgumentException(String.join("\n", validation.getErrors()));
            compilePdf(new com.ignitionai.phase7.latex.RepairPlanGenerator().generateReport(selected.certificate),
                selected.certificate.getCertificateId() + "-repair-plan", pdfTarget);
            return selected;
        });
    }
    /** Compiles into a throwaway temp directory and copies out only the finished PDF to
     * pdfTarget -- the .tex source and pdflatex's .aux/.log/.out byproducts never reach the save
     * location. On failure the temp directory is kept (and named in the error) for debugging. */
    private void compilePdf(String texSource, String baseName, Path pdfTarget) throws Exception {
        Path buildDir = Files.createTempDirectory("ignitionai-cert-");
        Path texFile = buildDir.resolve(baseName + ".tex");
        Files.writeString(texFile, texSource);
        java.io.File logo = logoFile();
        if (logo.isFile()) Files.copy(logo.toPath(), buildDir.resolve("ignitionai_logo.png"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        Process process;
        try { process = new ProcessBuilder("pdflatex", "-interaction=nonstopmode", "-halt-on-error", "-no-shell-escape", "-output-directory=" + buildDir, texFile.toString()).redirectErrorStream(true).redirectOutput(buildDir.resolve("latex-build.log").toFile()).start(); }
        catch (java.io.IOException e) { throw new java.io.IOException("PDF compilation requires pdflatex installed on this PC.", e); }
        try {
            if (!process.waitFor(45, TimeUnit.SECONDS)) throw new java.io.IOException("PDF compilation timed out; see " + buildDir.resolve("latex-build.log"));
            if (process.exitValue() != 0) throw new java.io.IOException("PDF compilation failed; see " + buildDir.resolve("latex-build.log"));
        } finally { if (process.isAlive()) process.destroyForcibly(); }
        Path pdfSource = buildDir.resolve(baseName + ".pdf");
        Files.copy(pdfSource, pdfTarget, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        try (var files = Files.walk(buildDir)) { files.sorted(Comparator.reverseOrder()).forEach(p -> { try { Files.delete(p); } catch (java.io.IOException ignored) { } }); }
    }
    static final class TracePanel extends JPanel {
        List<ObdMessage> messages = List.of(); String signal;
        TracePanel() { setBackground(Color.WHITE); }
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics); Graphics2D g = (Graphics2D)graphics.create();
            try {
                List<double[]> points = new ArrayList<>();
                for (ObdMessage m : messages) for (var r : m.getSensorReadings()) if (r.getSignalId().equals(signal) && Double.isFinite(r.getValue())) points.add(new double[]{r.getMonotonicMs()/1000.0, r.getValue()});
                if (points.isEmpty()) { g.drawString("No telemetry loaded", 35, 40); return; }
                double maxT = Math.max(1, points.get(points.size()-1)[0]), lo = points.stream().mapToDouble(p -> p[1]).min().orElse(0), hi = points.stream().mapToDouble(p -> p[1]).max().orElse(1);
                if (hi == lo) hi = lo + 1;
                int w = Math.max(1, getWidth()-100), h = Math.max(1, getHeight()-90);
                g.setColor(Color.GRAY); g.drawRect(65, 25, w, h); g.drawString(String.format(Locale.ROOT,"%.1f",hi), 5, 30); g.drawString(String.format(Locale.ROOT,"%.1f",lo), 5, h+25); g.drawString("Elapsed seconds: 0 to " + maxT,65,h+55);
                g.setColor(new Color(20,110,155)); g.setStroke(new BasicStroke(1.6f)); int lastX=-1,lastY=-1;
                for (double[] p : points) { int x=65+(int)(w*p[0]/maxT), y=25+h-(int)(h*(p[1]-lo)/(hi-lo)); if(lastX>=0)g.drawLine(lastX,lastY,x,y); lastX=x;lastY=y; }
            } finally { g.dispose(); }
        }
    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) { }
            new IgnitionDesktop().setVisible(true);
        });
    }
}
