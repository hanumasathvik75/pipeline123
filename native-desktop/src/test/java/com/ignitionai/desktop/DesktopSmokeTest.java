package com.ignitionai.desktop;

import javax.swing.*;
import java.awt.*;
import java.nio.file.*;

/** Runs actual Swing controls; captures only this application's window. */
public final class DesktopSmokeTest {
    public static void main(String[] args) throws Exception {
        final IgnitionDesktop[] frame = new IgnitionDesktop[1];
        SwingUtilities.invokeAndWait(() -> { frame[0] = new IgnitionDesktop(); frame[0].setVisible(true); });
        try {
            SwingUtilities.invokeAndWait(() -> {
                JTabbedPane tabs = find(frame[0], JTabbedPane.class, null); tabs.setSelectedIndex(1);
                JButton generate = find(frame[0], JButton.class, "Generate and assess"); generate.doClick();
            });
            long deadline = System.nanoTime() + 20_000_000_000L;
            final boolean[] ready = {false};
            do {
                Thread.sleep(100);
                SwingUtilities.invokeAndWait(() -> ready[0] = find(frame[0], JButton.class, "Generate and assess").isEnabled());
            } while (!ready[0] && System.nanoTime() < deadline);
            if (!ready[0]) throw new AssertionError("Simulation did not finish");
            SwingUtilities.invokeAndWait(() -> {
                JTable table = find(frame[0], JTable.class, null);
                if (table.getRowCount() != 5) throw new AssertionError("Assessment did not reach native table");
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
            System.out.println("PASS DesktopSmokeTest: real Swing simulator action -> backend -> native assessment table");
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
