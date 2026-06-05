package aidetector;

import aidetector.core.Analyzer;
import aidetector.core.FileReport;
import aidetector.core.SourceFile;
import aidetector.input.SourceLoader;
import aidetector.report.HtmlReport;
import aidetector.signals.DefaultSignals;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class DesktopApp {

    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final JFrame frame = new JFrame("AI Code Detector");
    private final JTextField targetField = new JTextField(46);
    private final JTextArea log = new JTextArea(10, 64);
    private final JButton analyzeButton = new JButton("Analyze");
    private final JButton openReportButton = new JButton("Open report");
    private final JButton openFolderButton = new JButton("Open folder");

    private Path lastReport;
    private Path lastBrowseDir = Path.of(System.getProperty("user.home"));

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DesktopApp().show(args));
    }

    private void show(String[] args) {
        configureLookAndFeel();
        if (args.length > 0) {
            targetField.setText(args[0]);
        }
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(content());
        frame.pack();
        frame.setMinimumSize(frame.getSize());
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private JPanel content() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("Analyze a file, a folder, or a GitHub/Git URL");
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        panel.add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.LINE_START;
        form.add(new JLabel("Target"), c);

        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        form.add(targetField, c);

        c.gridy = 1;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        JPanel browseRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton browseFile = new JButton("Browse file...");
        JButton browseFolder = new JButton("Browse folder...");
        browseFile.addActionListener(event -> chooseFile());
        browseFolder.addActionListener(event -> chooseFolder());
        browseRow.add(browseFile);
        browseRow.add(browseFolder);
        form.add(browseRow, c);

        c.gridy = 2;
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        analyzeButton.addActionListener(event -> analyze());
        openReportButton.addActionListener(event -> openReport());
        openFolderButton.addActionListener(event -> openFolder());
        openReportButton.setEnabled(false);
        openFolderButton.setEnabled(false);
        actions.add(analyzeButton);
        actions.add(openReportButton);
        actions.add(openFolderButton);
        form.add(actions, c);

        panel.add(form, BorderLayout.CENTER);

        log.setEditable(false);
        log.setLineWrap(true);
        log.setWrapStyleWord(true);
        log.setText("Ready. Paste a path or a URL, or use Browse.\n");
        JScrollPane scroll = new JScrollPane(log);
        scroll.setPreferredSize(new Dimension(640, 200));
        panel.add(scroll, BorderLayout.SOUTH);
        return panel;
    }

    private void chooseFile() {
        FileDialog dialog = new FileDialog(frame, "Choose a file", FileDialog.LOAD);
        if (lastBrowseDir != null) {
            dialog.setDirectory(lastBrowseDir.toString());
        }
        dialog.setVisible(true);
        String directory = dialog.getDirectory();
        String file = dialog.getFile();
        if (file != null && directory != null) {
            Path chosen = Path.of(directory, file);
            targetField.setText(chosen.toString());
            lastBrowseDir = chosen.getParent();
        }
    }

    private void chooseFolder() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Choose a project folder");
        chooser.setApproveButtonText("Select this folder");
        chooser.setApproveButtonToolTipText("Analyze the highlighted folder, or the folder you are currently in");
        if (lastBrowseDir != null && Files.isDirectory(lastBrowseDir)) {
            chooser.setCurrentDirectory(lastBrowseDir.toFile());
        }
        if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            File picked = chooser.getSelectedFile();
            if (picked == null) {
                picked = chooser.getCurrentDirectory();
            }
            Path chosen = picked.toPath();
            targetField.setText(chosen.toString());
            lastBrowseDir = chosen;
        }
    }

    private void analyze() {
        String target = targetField.getText().trim();
        if (target.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Enter a file path, a folder path, or a GitHub/Git URL.",
                    "Target required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        setBusy(true);
        append("Analyzing " + target);
        new SwingWorker<Path, String>() {
            @Override
            protected Path doInBackground() throws Exception {
                publish("Loading sources...");
                List<SourceFile> sources = new SourceLoader().load(target, null);

                publish("Scoring " + sources.size() + " file(s)...");
                Analyzer analyzer = new Analyzer(DefaultSignals.all(), 50, 25);
                List<FileReport> reports = new ArrayList<>(sources.size());
                for (SourceFile source : sources) {
                    reports.add(new FileReport(source, analyzer.analyze(source)));
                }

                Path report = reportPath();
                Files.createDirectories(report.getParent());
                String html = new HtmlReport(50, 25, 5).render(reports);
                Files.writeString(report, html, StandardCharsets.UTF_8);
                return report;
            }

            @Override
            protected void process(List<String> chunks) {
                for (String message : chunks) {
                    append(message);
                }
            }

            @Override
            protected void done() {
                try {
                    lastReport = get();
                    append("Report written to " + lastReport);
                    openReportButton.setEnabled(true);
                    openFolderButton.setEnabled(true);
                    openReport();
                } catch (Exception e) {
                    append("Error: " + rootMessage(e));
                    JOptionPane.showMessageDialog(frame, rootMessage(e), "Analysis failed", JOptionPane.ERROR_MESSAGE);
                } finally {
                    setBusy(false);
                }
            }
        }.execute();
    }

    private Path reportPath() {
        String stamp = LocalDateTime.now().format(FILE_TIME);
        return Path.of(System.getProperty("user.home"), "AI Code Detector", "reports",
                "ai-detector-report-" + stamp + ".html");
    }

    private void openReport() {
        browse(lastReport, "report");
    }

    private void openFolder() {
        browse(lastReport == null ? null : lastReport.getParent(), "report folder");
    }

    private void browse(Path path, String what) {
        if (path == null) {
            return;
        }
        if (!Desktop.isDesktopSupported()) {
            append("Cannot open " + what + " on this system. It is at " + path);
            return;
        }
        try {
            Desktop.getDesktop().browse(path.toUri());
        } catch (IOException e) {
            append("Could not open " + what + ": " + e.getMessage());
        }
    }

    private void setBusy(boolean busy) {
        analyzeButton.setEnabled(!busy);
        targetField.setEnabled(!busy);
        frame.setTitle(busy ? "AI Code Detector - analyzing" : "AI Code Detector");
    }

    private void append(String message) {
        log.append(message + "\n");
        log.setCaretPosition(log.getDocument().getLength());
    }

    private static void configureLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            UIManager.getDefaults();
        }
    }

    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null || message.isBlank() ? current.toString() : message;
    }
}
