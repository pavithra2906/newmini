import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

/**
 * Swing GUI application for file compression
 * Note: This is a desktop application version
 */
public class CompressionApp extends JFrame {
    private JTextField sourceField;
    private JTextField destField;
    private JComboBox<String> formatCombo;
    private JButton compressButton;
    private JTextArea logArea;
    
    public CompressionApp() {
        setTitle("File Compression Utility");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 400);
        setLocationRelativeTo(null);
        
        // Create UI components
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Source file selection
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Source File:"), gbc);
        
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        sourceField = new JTextField(20);
        panel.add(sourceField, gbc);
        
        gbc.gridx = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        JButton browseSource = new JButton("Browse...");
        browseSource.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                sourceField.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });
        panel.add(browseSource, gbc);
        
        // Destination file
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("Destination:"), gbc);
        
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        destField = new JTextField(20);
        panel.add(destField, gbc);
        
        // Format selection
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("Format:"), gbc);
        
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        formatCombo = new JComboBox<>(new String[]{"GZIP", "ZIP"});
        panel.add(formatCombo, gbc);
        
        // Compress button
        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.fill = GridBagConstraints.NONE;
        compressButton = new JButton("Compress");
        compressButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                compressFile();
            }
        });
        panel.add(compressButton, gbc);
        
        // Log area
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        logArea = new JTextArea(10, 30);
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);
        panel.add(scrollPane, gbc);
        
        add(panel);
    }
    
    private void compressFile() {
        String source = sourceField.getText();
        String dest = destField.getText();
        String format = (String) formatCombo.getSelectedItem();
        
        if (source.isEmpty() || dest.isEmpty()) {
            logArea.append("Error: Please select source and destination files\n");
            return;
        }
        
        logArea.append("Compressing: " + source + "\n");
        logArea.append("Format: " + format + "\n");
        
        CompressionResult result;
        if ("GZIP".equals(format)) {
            result = FileCompressor.compressFile(source, dest);
        } else {
            result = FileCompressor.compressToZip(source, dest);
        }
        
        if (result.isSuccess()) {
            logArea.append("✓ Compression successful!\n");
            logArea.append("Original: " + result.formatFileSize(result.getOriginalSize()) + "\n");
            logArea.append("Compressed: " + result.formatFileSize(result.getCompressedSize()) + "\n");
            logArea.append("Ratio: " + String.format("%.2f", result.getCompressionRatio()) + "%\n");
        } else {
            logArea.append("✗ Compression failed: " + result.getMessage() + "\n");
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new CompressionApp().setVisible(true);
        });
    }
}

