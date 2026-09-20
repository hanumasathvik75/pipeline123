package com.ignitionai.desktop;

import com.ignitionai.application.SessionFiles;
import com.ignitionai.obdgenerator.config.ScenarioFile;
import com.ignitionai.obdgenerator.generator.Elm327FrameEncoder;
import com.ignitionai.obdgenerator.generator.ObdGenerator;
import com.ignitionai.obdinput.schema.DtcObservation;
import com.ignitionai.obdinput.schema.ObdMessage;
import com.ignitionai.obdinput.schema.SensorReading;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * Standalone OBD-II telemetry log generator -- deliberately a separate application from
 * {@link IgnitionDesktop}, not a tab inside it. This mirrors what a real capture session would be:
 * a device/tool that produces a log, handed to the assessment app as a file. IgnitionDesktop's own
 * "Open OBD session" is the only way it ingests telemetry; it does not generate any itself.
 *
 * Two outputs from one generated scenario:
 *  1. A live console rendering the exact byte-level AT-command/hex-response traffic a real
 *     ELM327-class dongle would put on its serial line for the same PIDs (Elm327FrameEncoder).
 *  2. The structured obd-input.v1 session file (.jsonl) IgnitionDesktop's "Open OBD session"
 *     already reads -- the same file format used by "Save session" there, so a log produced here
 *     drops straight into the real assessment pipeline unchanged.
 */
public final class IgnitionSimulator extends JFrame {
    private static final Color BRAND_NAVY = Color.decode("#1F2937");
    private static final Color BRAND_SLATE = Color.decode("#475569");

    private final JTextArea scenario = new JTextArea(ScenarioFile.example());
    private final JTextArea console = new JTextArea();
    private final JLabel status = new JLabel("Ready");
    private List<ObdMessage> lastGenerated;

    public IgnitionSimulator() {
        super("IgnitionAI Simulator — OBD-II Log Generator (standalone, not the assessment app)");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(900, 650)); setSize(1100, 750); setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        setContentPane(root);

        JLabel heading = new JLabel("ignitionAI · OBD-II Log Generator");
        heading.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20)); heading.setForeground(BRAND_NAVY);
        JLabel sub = new JLabel("Standalone tool. Produces a session log; open it from the main app's \"Open OBD session\".");
        sub.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13)); sub.setForeground(BRAND_SLATE);
        JPanel top = new JPanel(new GridLayout(2, 1)); top.add(heading); top.add(sub);
        root.add(top, BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setResizeWeight(0.42);

        JPanel left = new JPanel(new BorderLayout(6, 6));
        left.add(new JLabel("Scenario configuration (trajectories, rates, faults, seed):"), BorderLayout.NORTH);
        scenario.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        left.add(new JScrollPane(scenario), BorderLayout.CENTER);
        JPanel leftActions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        button(leftActions, "Load scenario", () -> chooseAndLoad());
        button(leftActions, "Save scenario", () -> chooseAndSave());
        button(leftActions, "Generate log", this::generate);
        left.add(leftActions, BorderLayout.SOUTH);
        split.setLeftComponent(left);

        JPanel right = new JPanel(new BorderLayout(6, 6));
        right.add(new JLabel("Raw adapter traffic — exactly what a USB/Bluetooth ELM327-class OBD-II dongle emits:"), BorderLayout.NORTH);
        console.setEditable(false); console.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        console.setBackground(Color.BLACK); console.setForeground(new Color(80, 220, 120));
        right.add(new JScrollPane(console), BorderLayout.CENTER);
        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        button(rightActions, "Save session for assessment app…", this::saveSession);
        button(rightActions, "Save raw log as text…", this::saveRawLog);
        right.add(rightActions, BorderLayout.SOUTH);
        split.setRightComponent(right);

        root.add(split, BorderLayout.CENTER);
        root.add(status, BorderLayout.SOUTH);
    }

    private void button(Container panel, String name, Runnable action) {
        JButton b = new JButton(name); b.addActionListener(e -> action.run()); panel.add(b);
    }

    private void generate() {
        try {
            var config = ScenarioFile.parse(scenario.getText());
            List<ObdMessage> messages = new ObdGenerator(config).generate().getPublicStream();
            lastGenerated = messages;
            renderConsole(messages);
            status.setText("Generated " + messages.size() + " messages for " + config.getVehicleId() + ". Not yet saved.");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Generation failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void renderConsole(List<ObdMessage> messages) {
        StringBuilder log = new StringBuilder();
        log.append(">ATZ\nELM327 v1.5\n>ATE0\nOK\n>ATL0\nOK\n>ATSP0\nOK\n");
        boolean completeDtcSeen = false;
        for (ObdMessage m : messages) {
            for (SensorReading r : m.getSensorReadings()) {
                Integer pid = Elm327FrameEncoder.pidFor(r.getSignalId());
                if (pid == null) continue;
                String response = Elm327FrameEncoder.responseLine(r.getSignalId(), r.getValue());
                if (response == null) continue;
                log.append(Elm327FrameEncoder.requestLine(pid)).append('\n').append(response).append('\n');
            }
            if (!m.getDtcObservations().isEmpty()) {
                completeDtcSeen = true;
                java.util.List<String> codes = new java.util.ArrayList<>();
                for (DtcObservation dtc : m.getDtcObservations()) codes.add(dtc.getCode());
                log.append(">03\n").append(Elm327FrameEncoder.dtcResponseLine(codes)).append('\n');
            }
        }
        if (!completeDtcSeen) log.append(">03\n43 00\n");
        console.setText(log.toString());
        console.setCaretPosition(0);
    }

    private void saveSession() {
        if (lastGenerated == null) { status.setText("Generate a log first"); return; }
        Path p = choose(true, "session.jsonl");
        if (p == null) return;
        try { SessionFiles.write(p, lastGenerated); status.setText("Saved session for the assessment app: " + p.toAbsolutePath()); }
        catch (Exception e) { JOptionPane.showMessageDialog(this, e.getMessage(), "Save failed", JOptionPane.ERROR_MESSAGE); }
    }

    private void saveRawLog() {
        if (console.getText().isBlank()) { status.setText("Generate a log first"); return; }
        Path p = choose(true, "obd-raw-log.txt");
        if (p == null) return;
        try { Files.writeString(p, console.getText()); status.setText("Saved raw adapter log: " + p.toAbsolutePath()); }
        catch (Exception e) { JOptionPane.showMessageDialog(this, e.getMessage(), "Save failed", JOptionPane.ERROR_MESSAGE); }
    }

    private void chooseAndLoad() {
        Path p = choose(false, null); if (p == null) return;
        try { scenario.setText(Files.readString(p)); } catch (Exception e) { JOptionPane.showMessageDialog(this, e.getMessage(), "Load failed", JOptionPane.ERROR_MESSAGE); }
    }

    private void chooseAndSave() {
        Path p = choose(true, "scenario.properties"); if (p == null) return;
        try { ScenarioFile.parse(scenario.getText()); Files.writeString(p, scenario.getText()); }
        catch (Exception e) { JOptionPane.showMessageDialog(this, e.getMessage(), "Save failed", JOptionPane.ERROR_MESSAGE); }
    }

    private Path choose(boolean save, String suggested) {
        JFileChooser chooser = new JFileChooser();
        if (suggested != null) chooser.setSelectedFile(new java.io.File(suggested));
        int answer = save ? chooser.showSaveDialog(this) : chooser.showOpenDialog(this);
        if (answer != JFileChooser.APPROVE_OPTION) return null;
        return chooser.getSelectedFile().toPath();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) { }
            new IgnitionSimulator().setVisible(true);
        });
    }
}
