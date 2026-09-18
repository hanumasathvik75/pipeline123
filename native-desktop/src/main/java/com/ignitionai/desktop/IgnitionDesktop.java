package com.ignitionai.desktop;

import com.ignitionai.application.*;
import com.ignitionai.obdgenerator.config.ScenarioFile;
import com.ignitionai.obdgenerator.generator.ObdGenerator;
import com.ignitionai.obdinput.schema.ObdMessage;
import com.ignitionai.phase6.*;
import com.ignitionai.phase7.latex.LatexGenerator;
import com.ignitionai.phase7.validation.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Native Windows/Swing assessment and configurable simulator application. */
public final class IgnitionDesktop extends JFrame {
    private final JTextArea scenario = new JTextArea(ScenarioFile.example());
    private final JTextArea summary = new JTextArea("Generate a simulation or open a recorded OBD session to begin.");
    private final JTextArea evidence = new JTextArea();
    private final DefaultTableModel scores = new DefaultTableModel(new String[]{"Indicator", "Health", "Severity", "Confidence", "Data status"}, 0) {
        public boolean isCellEditable(int row, int col) { return false; }
    };
    private final JLabel status = new JLabel("Ready • Stored on this computer");
    private final JComboBox<String> chartSignal = new JComboBox<>();
    private final TracePanel trace = new TracePanel();
    private final DefaultListModel<Path> history = new DefaultListModel<>();
    private final Path storage = Path.of(System.getProperty("user.home"), "IgnitionAI", "sessions");
    private SwingWorker<InspectionResult, Void> worker;
    private InspectionResult result;
    private final List<AbstractButton> taskButtons = new ArrayList<>();

    public IgnitionDesktop() {
        super("IgnitionAI • Vehicle Assessment & Simulator");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(900, 650)); setSize(1160, 800); setLocationRelativeTo(null);
        JPanel root = new JPanel(new BorderLayout(12, 12)); root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16)); setContentPane(root);
        JLabel heading = new JLabel("IgnitionAI   /   Vehicle assessment"); heading.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 23));
        JPanel top = new JPanel(new BorderLayout()); top.add(heading, BorderLayout.NORTH);
        JToolBar actions = new JToolBar(); actions.setFloatable(false);
        addAction(actions, "Open OBD session", this::openSession);
        addAction(actions, "Save session", this::saveSession);
        addAction(actions, "Export certificate", this::exportCertificate);
        JButton cancel = new JButton("Cancel"); cancel.addActionListener(e -> { if (worker != null) worker.cancel(true); }); actions.add(cancel);
        top.add(actions, BorderLayout.SOUTH); root.add(top, BorderLayout.NORTH);
        JTabbedPane tabs = new JTabbedPane(); root.add(tabs, BorderLayout.CENTER);
        summary.setEditable(false); summary.setLineWrap(true); summary.setWrapStyleWord(true); summary.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
        JPanel assessment = new JPanel(new BorderLayout(8, 8)); assessment.add(new JScrollPane(summary), BorderLayout.NORTH);
        summary.setRows(8); JTable table = new JTable(scores); table.setRowHeight(30); assessment.add(new JScrollPane(table));
        tabs.addTab("Assessment", assessment);
        JPanel sim = new JPanel(new BorderLayout(8, 8));
        sim.add(new JLabel("Edit trajectories, rates and fault windows. This produces labelled simulation data, not a real inspection."), BorderLayout.NORTH);
        scenario.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13)); sim.add(new JScrollPane(scenario));
        JPanel simActions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        addAction(simActions, "Generate and assess", () -> { String input = scenario.getText(); runTask(() -> new AssessmentHistory(sensorDirectory(), storage).assess(new ObdGenerator(ScenarioFile.parse(input)).generate().getPublicStream())); });
        addAction(simActions, "Load scenario", () -> scenarioFile(false)); addAction(simActions, "Save scenario", () -> scenarioFile(true));
        sim.add(simActions, BorderLayout.SOUTH); tabs.addTab("Simulator", sim);
        JPanel charts = new JPanel(new BorderLayout()); charts.add(chartSignal, BorderLayout.NORTH); charts.add(trace);
        chartSignal.addActionListener(e -> { trace.signal = (String)chartSignal.getSelectedItem(); trace.repaint(); }); tabs.addTab("Telemetry", charts);
        evidence.setEditable(false); evidence.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12)); tabs.addTab("Evidence and model status", new JScrollPane(evidence));
        JList<Path> sessions = new JList<>(history); sessions.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JPanel historyPanel = new JPanel(new BorderLayout()); historyPanel.add(new JScrollPane(sessions));
        JPanel historyActions = new JPanel();
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
        summary.setText("Vehicle: " + a.getVehicleId() + "\nAssessed: " + a.getAssessmentTimestamp()
            + "\nVHI: " + value(a.getVehicleHealthIndex()) + "   |   " + a.getHealthBand() + "   |   Data: " + a.getDataQualityStatus()
            + "\nCoverage: " + value(a.getSubsystemCoverage()) + "   |   Confidence: " + value(a.getOverallConfidence())
            + "\nEpisodes: " + result.episodes.size() + "   |   Records: " + result.messages.size()
            + "\n" + String.join("\n", result.notes));
        summary.setCaretPosition(0);
        scores.setRowCount(0);
        for (SubsystemHealthAssessment s : a.getSubsystemAssessments()) scores.addRow(new Object[]{s.getSubsystemName(), value(s.getScore()), s.getSeverity(), value(s.getConfidence()), String.join(", ", s.getDegradedDataFlags())});
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
    private Path choose(boolean save, String suggested) {
        JFileChooser chooser = new JFileChooser(); if (suggested != null) chooser.setSelectedFile(new java.io.File(suggested));
        int answer = save ? chooser.showSaveDialog(this) : chooser.showOpenDialog(this);
        if (answer != JFileChooser.APPROVE_OPTION) return null;
        Path path = chooser.getSelectedFile().toPath();
        if (save && Files.exists(path) && JOptionPane.showConfirmDialog(this, "Overwrite existing file?", "Save", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return null;
        return path;
    }
    private void openSession() { Path p = choose(false, null); if (p != null) load(p); }
    private void load(Path p) { runTask(() -> new AssessmentHistory(sensorDirectory(), storage).assess(SessionFiles.read(p))); }
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
    private void scenarioFile(boolean save) {
        Path p = choose(save, save ? "scenario.properties" : null); if (p == null) return;
        attempt(() -> { if (save) { ScenarioFile.parse(scenario.getText()); Files.writeString(p, scenario.getText()); } else scenario.setText(Files.readString(p)); });
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
    private void exportCertificate() {
        if (result == null) { status.setText("Run an assessment first"); return; }
        Path file = choose(true, result.certificate.getCertificateId() + ".tex"); if (file == null) return;
        InspectionResult selected = result;
        runTask(() -> {
            ValidationResult validation = new CertificateValidator().validateSnapshot(selected.certificate);
            if (!validation.isValid()) throw new IllegalArgumentException(String.join("\n", validation.getErrors()));
            Files.writeString(file, new LatexGenerator().generateCertificate(selected.certificate));
            Path absolute = file.toAbsolutePath();
            Process process;
            try { process = new ProcessBuilder("pdflatex", "-interaction=nonstopmode", "-halt-on-error", "-no-shell-escape", "-output-directory=" + absolute.getParent(), absolute.toString()).redirectErrorStream(true).redirectOutput(absolute.resolveSibling("latex-build.log").toFile()).start(); }
            catch (java.io.IOException e) { throw new java.io.IOException("LaTeX saved. PDF compilation requires pdflatex installed on this PC.", e); }
            try {
                if (!process.waitFor(45, TimeUnit.SECONDS)) throw new java.io.IOException("PDF compilation timed out; see latex-build.log");
                if (process.exitValue() != 0) throw new java.io.IOException("PDF compilation failed; see latex-build.log");
            } finally { if (process.isAlive()) process.destroyForcibly(); }
            return selected;
        });
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
