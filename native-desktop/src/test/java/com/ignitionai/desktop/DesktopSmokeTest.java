package com.ignitionai.desktop;

import com.ignitionai.application.SessionFiles;
import com.ignitionai.obdgenerator.config.ScenarioFile;
import com.ignitionai.obdgenerator.generator.ObdGenerator;
import com.ignitionai.obdinput.schema.ObdMessage;

import javax.swing.*;
import java.awt.*;
import java.nio.file.*;
import java.util.List;

/** Runs actual Swing controls; captures only this application's window. Drives a real session
 * through {@link IgnitionDesktop#load} directly (package-visible for this reason) rather than
 * automating the OS file-picker dialog behind "Open OBD session" -- the app itself no longer
 * generates test data since the Simulator was split into its own standalone tool. */
public final class DesktopSmokeTest {
    public static void main(String[] args) throws Exception {
        Path sessionFile = Files.createTempDirectory("desktop-smoke-test").resolve("session.jsonl");
        String scenarioText = Files.readString(Path.of("fixtures/test-scenario-healthy.properties"));
        var config = ScenarioFile.parse(scenarioText);
        List<ObdMessage> stream = new ObdGenerator(config).generate().getPublicStream();
        SessionFiles.write(sessionFile, stream);

        final IgnitionDesktop[] frame = new IgnitionDesktop[1];
        SwingUtilities.invokeAndWait(() -> { frame[0] = new IgnitionDesktop(); frame[0].setVisible(true); });
        try {
            SwingUtilities.invokeAndWait(() -> frame[0].load(sessionFile));

            long deadline = System.nanoTime() + 20_000_000_000L;
            final boolean[] ready = {false};
            do {
                Thread.sleep(100);
                SwingUtilities.invokeAndWait(() -> {
                    JTable table = find(frame[0], JTable.class, null);
                    ready[0] = table != null && table.getRowCount() > 0;
                });
            } while (!ready[0] && System.nanoTime() < deadline);
            if (!ready[0]) throw new AssertionError("Assessment did not finish within 20s");

            SwingUtilities.invokeAndWait(() -> {
                JTable table = find(frame[0], JTable.class, null);
                if (table.getRowCount() != 5) throw new AssertionError("Expected 5 subsystem rows in the native table, got " + table.getRowCount());
                JTabbedPane tabs = find(frame[0], JTabbedPane.class, null); tabs.setSelectedIndex(0);
            });
            Files.createDirectories(Path.of("out/ui-verification"));
            SwingUtilities.invokeAndWait(() -> {
                try {
                    var image = new java.awt.image.BufferedImage(frame[0].getWidth(), frame[0].getHeight(), java.awt.image.BufferedImage.TYPE_INT_RGB);
                    Graphics2D g=image.createGraphics(); frame[0].paint(g); g.dispose();
                    javax.imageio.ImageIO.write(image,"png", Path.of("out/ui-verification/desktop-assessment.png").toFile());
                } catch (Exception e) { throw new RuntimeException(e); }
            });
            System.out.println("PASS DesktopSmokeTest: real session file -> IgnitionDesktop.load -> backend -> native assessment table");
        } finally { SwingUtilities.invokeAndWait(() -> frame[0].dispose()); }
    }
    private static <T> T find(Container parent, Class<T> type, String text) {
        for (Component c : parent.getComponents()) {
            if (type.isInstance(c) && (text == null || c instanceof AbstractButton && text.equals(((AbstractButton)c).getText()))) return type.cast(c);
            if (c instanceof Container) { T result = find((Container)c, type, text); if(result != null)return result; }
        }
        return null;
    }
}
