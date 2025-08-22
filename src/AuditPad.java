import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ToolType;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.ui.UserInterface;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;
import burp.api.montoya.ui.contextmenu.MessageEditorHttpRequestResponse;
import burp.api.montoya.ui.editor.HttpRequestEditor;
import burp.api.montoya.ui.editor.HttpResponseEditor;
import burp.api.montoya.ui.editor.EditorOptions;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import java.io.*;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Base64;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * AuditPad - A Professional Burp Suite Extension for Security Testing Notes
 * 
 * This extension provides:
 * - A custom main tab for managing security findings
 * - Context menu integration for binding notes to HTTP requests
 * - Structured note-taking with severity levels and professional table layout
 * - Automatic PoC population with labeled request/response viewers
 * - Complete CRUD operations (Create, Read, Update, Delete)
 * - Multi-format export: Markdown, JSON, and CSV/Excel
 * - Import functionality for note persistence
 * - Professional UI with color-coded severity levels
 * 
 * @author @ronydasx
 * @version 2.0
 * @since 2025
 */
public class AuditPad implements BurpExtension {
    
    private MontoyaApi api;
    private List<Note> notes;
    private JTable notesTable;
    private DefaultTableModel tableModel;
    private JTable detailsTable;
    private DefaultTableModel detailsTableModel;
    private JPanel mainPanel;
    private HttpRequestEditor requestEditor;
    private HttpResponseEditor responseEditor;
    private JSplitPane requestResponseSplitPane;
    
    /**
     * Data class to represent a penetration testing note
     */
    private static class Note {
        private String title;
        private String severity;
        private String description;
        private String proofOfConcept;
        private HttpRequestResponse requestResponse;
        private LocalDateTime timestamp;
        
        public Note(String title, String severity, String description, String proofOfConcept, HttpRequestResponse requestResponse) {
            this.title = title;
            this.severity = severity;
            this.description = description;
            this.proofOfConcept = proofOfConcept;
            this.requestResponse = requestResponse;
            this.timestamp = LocalDateTime.now();
        }
        
        // Getters
        public String getTitle() { return title; }
        public String getSeverity() { return severity; }
        public String getDescription() { return description; }
        public String getProofOfConcept() { return proofOfConcept; }
        public HttpRequestResponse getRequestResponse() { return requestResponse; }
        public LocalDateTime getTimestamp() { return timestamp; }
        
        // Get color for severity
        public Color getSeverityColor() {
            return ThemeColors.getSeverityColor(severity);
        }
        
        // Convert to JSON-like string for export
        public String toJsonString() {
            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"title\": \"").append(escapeJson(title)).append("\",\n");
            json.append("  \"severity\": \"").append(escapeJson(severity)).append("\",\n");
            json.append("  \"description\": \"").append(escapeJson(description)).append("\",\n");
            json.append("  \"timestamp\": \"").append(timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\",\n");
            json.append("  \"proofOfConcept\": \"").append(escapeJson(proofOfConcept)).append("\",\n");
            
            // Encode request/response as base64 for safe storage
            if (requestResponse != null) {
                String requestBase64 = Base64.getEncoder().encodeToString(requestResponse.request().toByteArray().getBytes());
                json.append("  \"request\": \"").append(requestBase64).append("\",\n");
                
                if (requestResponse.response() != null) {
                    String responseBase64 = Base64.getEncoder().encodeToString(requestResponse.response().toByteArray().getBytes());
                    json.append("  \"response\": \"").append(responseBase64).append("\"\n");
                } else {
                    json.append("  \"response\": null\n");
                }
            } else {
                json.append("  \"request\": null,\n");
                json.append("  \"response\": null\n");
            }
            
            json.append("}");
            return json.toString();
        }
        
        // Helper method to escape JSON strings
        private String escapeJson(String str) {
            if (str == null) return "";
            return str.replace("\\", "\\\\")
                     .replace("\"", "\\\"")
                     .replace("\n", "\\n")
                     .replace("\r", "\\r")
                     .replace("\t", "\\t");
        }
        
        // Extract URL from request
        public String getUrl() {
            if (requestResponse == null || requestResponse.request() == null) {
                return "N/A";
            }
            
            try {
                String requestStr = requestResponse.request().toString();
                String[] lines = requestStr.split("\n");
                if (lines.length > 0) {
                    String firstLine = lines[0];
                    // Extract URL from first line (e.g., "GET /path HTTP/1.1")
                    String[] parts = firstLine.split(" ");
                    if (parts.length >= 2) {
                        String path = parts[1];
                        
                        // Find Host header
                        String host = "unknown-host";
                        for (String line : lines) {
                            if (line.toLowerCase().startsWith("host:")) {
                                host = line.substring(5).trim();
                                break;
                            }
                        }
                        
                        // Construct full URL
                        String protocol = "https"; // Default to HTTPS
                        if (host.contains(":80") || host.contains(":8080")) {
                            protocol = "http";
                        }
                        
                        return protocol + "://" + host + path;
                    }
                }
            } catch (Exception e) {
                // If extraction fails, return N/A
            }
            
            return "N/A";
        }
    }
    
    // Theme-aware color helper methods
    private static class ThemeColors {
        // Get theme-aware colors that work in both light and dark modes
        public static Color getTextColor() {
            // Try multiple UIManager keys for text color
            Color textColor = null;
            String[] textColorKeys = {
                "Label.foreground",
                "TextField.foreground", 
                "TextArea.foreground",
                "Table.foreground",
                "Tree.textForeground",
                "List.foreground"
            };
            
            for (String key : textColorKeys) {
                textColor = UIManager.getColor(key);
                if (textColor != null) {
                    break;
                }
            }
            
            // Determine if we're in dark theme
            boolean isDark = isDarkTheme();
            
            // If we couldn't get any text color, use fallback
            if (textColor == null) {
                return isDark ? new Color(240, 240, 240) : new Color(40, 40, 40);
            }
            
            // Check text brightness
            int textBrightness = textColor.getRed() + textColor.getGreen() + textColor.getBlue();
            
            // If it's dark theme but text is too dark (< 300), force light text
            if (isDark && textBrightness < 300) {
                return new Color(240, 240, 240); // Light gray for dark theme
            }
            
            // If it's light theme but text is too light (> 500), force dark text  
            if (!isDark && textBrightness > 500) {
                return new Color(40, 40, 40); // Dark gray for light theme
            }
            
            return textColor;
        }
        
        public static Color getBackgroundColor() {
            // Try multiple UIManager keys for background color
            Color bgColor = null;
            String[] bgColorKeys = {
                "Panel.background",
                "TextField.background",
                "Table.background",
                "List.background",
                "control"
            };
            
            for (String key : bgColorKeys) {
                bgColor = UIManager.getColor(key);
                if (bgColor != null) {
                    break;
                }
            }
            
            if (bgColor == null) {
                // Fallback based on theme detection
                return isDarkTheme() ? new Color(60, 63, 65) : Color.WHITE;
            }
            return bgColor;
        }
        
        public static Color getSecondaryTextColor() {
            Color text = getTextColor();
            Color bg = getBackgroundColor();
            // Create a muted text color by blending with background (no alpha transparency)
            return new Color(
                (text.getRed() + bg.getRed()) / 2,
                (text.getGreen() + bg.getGreen()) / 2,
                (text.getBlue() + bg.getBlue()) / 2
            );
        }
        
        public static Color getBorderColor() {
            Color border = UIManager.getColor("Component.borderColor");
            if (border != null) {
                return border;
            }
            // Fallback: create a border color from text and background
            Color text = getTextColor();
            Color bg = getBackgroundColor();
            return new Color(
                (text.getRed() + bg.getRed() * 3) / 4,
                (text.getGreen() + bg.getGreen() * 3) / 4,
                (text.getBlue() + bg.getBlue() * 3) / 4
            );
        }
        
        public static Color getSelectionBackgroundColor() {
            return UIManager.getColor("List.selectionBackground");
        }
        
        public static Color getTableHeaderBackgroundColor() {
            return UIManager.getColor("TableHeader.background");
        }
        
        public static Color getTitledBorderTextColor() {
            // Simple and clear: WHITE text for dark theme, BLACK text for light theme
            boolean isDark = isDarkTheme();
            
            if (isDark) {
                return Color.WHITE; // Pure white text for dark theme
            } else {
                return Color.BLACK; // Pure black text for light theme
            }
        }
        
        public static Color getTableGridColor() {
            Color bg = getBackgroundColor();
            Color text = getTextColor();
            // Create a subtle grid color by blending background and text
            return new Color(
                (bg.getRed() + text.getRed()) / 2,
                (bg.getGreen() + text.getGreen()) / 2,
                (bg.getBlue() + text.getBlue()) / 2,
                64 // Light opacity
            );
        }
        
        public static Color getFieldBackgroundColor() {
            Color bg = getBackgroundColor();
            // Slightly lighter/darker than main background
            boolean isDark = bg.getRed() + bg.getGreen() + bg.getBlue() < 384; // < 128*3
            if (isDark) {
                return new Color(
                    Math.min(255, bg.getRed() + 15),
                    Math.min(255, bg.getGreen() + 15),
                    Math.min(255, bg.getBlue() + 15)
                );
            } else {
                return new Color(
                    Math.max(0, bg.getRed() - 15),
                    Math.max(0, bg.getGreen() - 15),
                    Math.max(0, bg.getBlue() - 15)
                );
            }
        }
        
        // Severity colors that work in both themes
        public static Color getSeverityColor(String severity) {
            boolean isDarkTheme = isDarkTheme();
            switch (severity.toLowerCase()) {
                case "critical": 
                    return isDarkTheme ? new Color(255, 99, 99) : new Color(139, 0, 0); // Light red in dark, dark red in light
                case "high": 
                    return isDarkTheme ? new Color(255, 159, 64) : new Color(255, 69, 0); // Light orange in dark, orange red in light
                case "medium": 
                    return isDarkTheme ? new Color(255, 205, 86) : new Color(255, 140, 0); // Light yellow in dark, dark orange in light
                case "low": 
                    return isDarkTheme ? new Color(255, 235, 59) : new Color(218, 165, 32); // Bright yellow in dark, goldenrod in light
                case "informational": 
                    return isDarkTheme ? new Color(100, 181, 246) : new Color(70, 130, 180); // Light blue in dark, steel blue in light
                default: 
                    return getTextColor();
            }
        }
        
        private static boolean isDarkTheme() {
            // Try multiple approaches to detect dark theme
            
            // Method 1: Check background color from UIManager directly
            Color bg = UIManager.getColor("Panel.background");
            if (bg != null) {
                int brightness = bg.getRed() + bg.getGreen() + bg.getBlue();
                if (brightness < 384) { // 128*3
                    return true;
                }
            }
            
            // Method 2: Check other UI elements that might be more reliable
            Color textColor = UIManager.getColor("Label.foreground");
            if (textColor != null) {
                int textBrightness = textColor.getRed() + textColor.getGreen() + textColor.getBlue();
                // If text is very bright, it's likely a dark theme
                if (textBrightness > 600) { // 200*3
                    return true;
                }
            }
            
            // Method 3: Check control colors
            Color controlBg = UIManager.getColor("control");
            if (controlBg != null) {
                int controlBrightness = controlBg.getRed() + controlBg.getGreen() + controlBg.getBlue();
                if (controlBrightness < 384) {
                    return true;
                }
            }
            
            // Method 4: Check window background (often used by Burp Suite)
            Color windowBg = UIManager.getColor("window");
            if (windowBg != null) {
                int windowBrightness = windowBg.getRed() + windowBg.getGreen() + windowBg.getBlue();
                if (windowBrightness < 384) {
                    return true;
                }
            }
            
            // Method 5: Check table background (Burp uses many tables)
            Color tableBg = UIManager.getColor("Table.background");
            if (tableBg != null) {
                int tableBrightness = tableBg.getRed() + tableBg.getGreen() + tableBg.getBlue();
                if (tableBrightness < 384) {
                    return true;
                }
            }
            
            // Method 6: Assume dark theme if we can't get proper colors (conservative approach for visibility)
            if (bg == null && textColor == null && controlBg == null) {
                // If we can't determine colors, err on the side of dark theme for better visibility
                return true;
            }
            
            // Default to light theme if we can't determine
            return false;
        }
    }
    
    /**
     * Initialize the extension
     */
    @Override
    public void initialize(MontoyaApi api) {
        this.api = api;
        this.notes = new ArrayList<>();
        
        // Set extension name
        api.extension().setName("AuditPad");
        
        // Create the main UI
        createMainUI();
        
        // Register the main tab
        api.userInterface().registerSuiteTab("AuditPad", mainPanel);
        
        // Register context menu provider
        api.userInterface().registerContextMenuItemsProvider(new NotesContextMenuProvider());
        
        // Log successful initialization
        api.logging().logToOutput("AuditPad extension loaded successfully - Created by @ronydasx");
    }
    
    /**
     * Create the main UI for the Notes Binder tab
     */
    private void createMainUI() {
        mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // Create header panel
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel titleLabel = new JLabel("AuditPad");
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        headerPanel.add(titleLabel);
        
        // Add author credits
        JLabel creditsLabel = new JLabel("by @ronydasx");
        creditsLabel.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 12));
        creditsLabel.setForeground(ThemeColors.getSecondaryTextColor());
        headerPanel.add(Box.createHorizontalStrut(10));
        headerPanel.add(creditsLabel);
        
        // Create unified export menu
        JButton exportButton = new JButton("📤 Export");
        JPopupMenu exportMenu = new JPopupMenu();
        
        JMenuItem exportMarkdownItem = new JMenuItem("📄 Export as Markdown");
        exportMarkdownItem.addActionListener(new ExportMarkdownActionListener());
        exportMenu.add(exportMarkdownItem);
        
        JMenuItem exportJsonItem = new JMenuItem("💾 Export as JSON");
        exportJsonItem.addActionListener(new ExportJsonActionListener());
        exportMenu.add(exportJsonItem);
        
        JMenuItem exportXlsxItem = new JMenuItem("📊 Export as XLSX");
        exportXlsxItem.addActionListener(new ExportXlsxActionListener());
        exportMenu.add(exportXlsxItem);
        
        exportButton.addActionListener(e -> exportMenu.show(exportButton, 0, exportButton.getHeight()));
        
        JButton importButton = new JButton("📥 Import Notes");
        importButton.addActionListener(new ImportJsonActionListener());
        
        headerPanel.add(Box.createHorizontalStrut(20));
        headerPanel.add(exportButton);
        headerPanel.add(Box.createHorizontalStrut(10));
        headerPanel.add(importButton);
        
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        
        // Create split pane for notes list and details
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(400);
        
        // Create notes table
        String[] columnNames = {"Title", "Severity", "Timestamp"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table read-only
            }
        };
        
        notesTable = new JTable(tableModel);
        notesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        notesTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                displaySelectedNote();
            }
        });
        
        // Add custom cell renderer for severity column with color coding
        notesTable.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, 
                    boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                if (!isSelected && row < notes.size()) {
                    Note note = notes.get(row);
                    c.setForeground(note.getSeverityColor());
                    setFont(getFont().deriveFont(Font.BOLD));
                } else if (!isSelected) {
                    c.setForeground(ThemeColors.getTextColor());
                    setFont(getFont().deriveFont(Font.PLAIN));
                }
                
                return c;
            }
        });
        
        // Add context menu for edit/delete functionality
        JPopupMenu tableContextMenu = new JPopupMenu();
        JMenuItem editItem = new JMenuItem("✏️ Edit Note");
        JMenuItem deleteItem = new JMenuItem("🗑️ Delete Note");
        
        editItem.addActionListener(e -> editSelectedNote());
        deleteItem.addActionListener(e -> deleteSelectedNote());
        
        tableContextMenu.add(editItem);
        tableContextMenu.add(deleteItem);
        
        notesTable.setComponentPopupMenu(tableContextMenu);
        
        JScrollPane tableScrollPane = new JScrollPane(notesTable);
        tableScrollPane.setPreferredSize(new Dimension(400, 300));
        splitPane.setLeftComponent(tableScrollPane);
        
        // Create details panel with tabbed pane
        JTabbedPane detailsTabbedPane = new JTabbedPane();
        
        // Note details tab with professional table layout
        JPanel noteDetailsPanel = new JPanel(new BorderLayout());
        noteDetailsPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // Create details table model with two columns: Field and Value
        String[] detailsColumns = {"Field", "Value"};
        detailsTableModel = new DefaultTableModel(detailsColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table read-only
            }
        };
        
        // Create the details table
        detailsTable = new JTable(detailsTableModel);
        detailsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        detailsTable.setRowHeight(25);
        detailsTable.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        detailsTable.setGridColor(ThemeColors.getTableGridColor());
        detailsTable.setShowGrid(true);
        detailsTable.setIntercellSpacing(new Dimension(1, 1));
        
        // Style the table header
        detailsTable.getTableHeader().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        detailsTable.getTableHeader().setBackground(ThemeColors.getTableHeaderBackgroundColor());
        detailsTable.getTableHeader().setForeground(ThemeColors.getTextColor());
        detailsTable.getTableHeader().setBorder(BorderFactory.createRaisedBevelBorder());
        
        // Set column widths
        detailsTable.getColumnModel().getColumn(0).setPreferredWidth(120);
        detailsTable.getColumnModel().getColumn(0).setMaxWidth(150);
        detailsTable.getColumnModel().getColumn(1).setPreferredWidth(400);
        
        // Add custom cell renderer for better styling
        detailsTable.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, 
                    boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                // Style the field names (first column)
                setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
                setBackground(ThemeColors.getFieldBackgroundColor());
                setForeground(ThemeColors.getTextColor());
                setHorizontalAlignment(SwingConstants.RIGHT);
                setBorder(new EmptyBorder(5, 10, 5, 10));
                
                if (isSelected) {
                    setBackground(ThemeColors.getSelectionBackgroundColor());
                }
                
                return c;
            }
        });
        
        detailsTable.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, 
                    boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                // Style the values (second column)
                setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
                setBackground(ThemeColors.getBackgroundColor());
                setForeground(ThemeColors.getTextColor());
                setHorizontalAlignment(SwingConstants.LEFT);
                setBorder(new EmptyBorder(5, 10, 5, 10));
                
                // Special styling for severity
                if (row < detailsTableModel.getRowCount() && 
                    detailsTableModel.getValueAt(row, 0).toString().equals("Severity")) {
                    setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
                    String severity = value.toString();
                    setForeground(ThemeColors.getSeverityColor(severity));
                }
                
                if (isSelected) {
                    setBackground(ThemeColors.getSelectionBackgroundColor());
                }
                
                return c;
            }
        });
        
        JScrollPane detailsScrollPane = new JScrollPane(detailsTable);
        detailsScrollPane.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(ThemeColors.getBorderColor(), 1),
                "Note Information",
                0, 0,
                new Font(Font.SANS_SERIF, Font.BOLD, 12),
                ThemeColors.getTitledBorderTextColor()
            ),
            new EmptyBorder(5, 5, 5, 5)
        ));
        
        noteDetailsPanel.add(detailsScrollPane, BorderLayout.CENTER);
        detailsTabbedPane.addTab("📝 Note Details", noteDetailsPanel);
        
        // Request/Response tab
        JPanel requestResponsePanel = new JPanel(new BorderLayout());
        
        // Create request and response editors
        requestEditor = api.userInterface().createHttpRequestEditor(EditorOptions.READ_ONLY);
        responseEditor = api.userInterface().createHttpResponseEditor(EditorOptions.READ_ONLY);
        
        // Create labeled panels for request and response
        JPanel requestPanel = new JPanel(new BorderLayout());
        requestPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(ThemeColors.getBorderColor(), 1),
            "📤 HTTP Request",
            0, 0,
            new Font(Font.SANS_SERIF, Font.BOLD, 12),
            ThemeColors.getTitledBorderTextColor()
        ));
        requestPanel.add(requestEditor.uiComponent(), BorderLayout.CENTER);
        
        JPanel responsePanel = new JPanel(new BorderLayout());
        responsePanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(ThemeColors.getBorderColor(), 1),
            "📥 HTTP Response",
            0, 0,
            new Font(Font.SANS_SERIF, Font.BOLD, 12),
            ThemeColors.getTitledBorderTextColor()
        ));
        responsePanel.add(responseEditor.uiComponent(), BorderLayout.CENTER);
        
        // Create split pane for request/response with labeled panels
        requestResponseSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        requestResponseSplitPane.setLeftComponent(requestPanel);
        requestResponseSplitPane.setRightComponent(responsePanel);
        requestResponseSplitPane.setResizeWeight(0.5);
        requestResponseSplitPane.setOneTouchExpandable(true);
        
        // Add component listener to center the divider when component is resized
        requestResponseSplitPane.addComponentListener(new ComponentAdapter() {
            private boolean initialized = false;
            
            @Override
            public void componentResized(ComponentEvent e) {
                if (!initialized && requestResponseSplitPane.getWidth() > 0) {
                    SwingUtilities.invokeLater(() -> {
                        requestResponseSplitPane.setDividerLocation(0.5);
                        initialized = true;
                    });
                }
            }
        });
        
        // Also try to set it immediately after adding to parent
        SwingUtilities.invokeLater(() -> {
            SwingUtilities.invokeLater(() -> {
                if (requestResponseSplitPane.getWidth() > 0) {
                    requestResponseSplitPane.setDividerLocation(0.5);
                }
            });
        });
        
        requestResponsePanel.add(requestResponseSplitPane, BorderLayout.CENTER);
        detailsTabbedPane.addTab("🌐 Request/Response", requestResponsePanel);
        
        splitPane.setRightComponent(detailsTabbedPane);
        mainPanel.add(splitPane, BorderLayout.CENTER);
    }
    
    /**
     * Display the selected note in the details panel
     */
    private void displaySelectedNote() {
        int selectedRow = notesTable.getSelectedRow();
        if (selectedRow >= 0 && selectedRow < notes.size()) {
            Note note = notes.get(selectedRow);
            
            // Clear existing table data
            detailsTableModel.setRowCount(0);
            
            // Populate the details table with note information
            detailsTableModel.addRow(new Object[]{"📋 Title", note.getTitle()});
            detailsTableModel.addRow(new Object[]{"🚨 Severity", note.getSeverity()});
            detailsTableModel.addRow(new Object[]{"🔗 URL", note.getUrl()});
            detailsTableModel.addRow(new Object[]{"🕒 Timestamp", note.getTimestamp().format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy 'at' HH:mm:ss"))});
            
            // Add description with proper handling of long text
            String description = note.getDescription().trim();
            if (description.isEmpty()) {
                detailsTableModel.addRow(new Object[]{"📝 Description", "No description provided"});
            } else {
                // Split long descriptions into multiple lines for better readability
                if (description.length() > 100) {
                    String[] lines = description.split("\\n");
                    for (int i = 0; i < lines.length; i++) {
                        String fieldName = (i == 0) ? "📝 Description" : "";
                        detailsTableModel.addRow(new Object[]{fieldName, lines[i]});
                    }
                } else {
                    detailsTableModel.addRow(new Object[]{"📝 Description", description});
                }
            }
            
            // Add HTTP method and status code if available
            if (note.getRequestResponse() != null) {
                try {
                    String requestStr = note.getRequestResponse().request().toString();
                    String[] requestLines = requestStr.split("\n");
                    if (requestLines.length > 0) {
                        String[] parts = requestLines[0].split(" ");
                        if (parts.length > 0) {
                            detailsTableModel.addRow(new Object[]{"🌐 HTTP Method", parts[0]});
                        }
                    }
                    
                    if (note.getRequestResponse().response() != null) {
                        String responseStr = note.getRequestResponse().response().toString();
                        String[] responseLines = responseStr.split("\n");
                        if (responseLines.length > 0) {
                            String[] parts = responseLines[0].split(" ");
                            if (parts.length > 1) {
                                detailsTableModel.addRow(new Object[]{"📊 Status Code", parts[1]});
                            }
                        }
                    }
                } catch (Exception e) {
                    // If parsing fails, just skip HTTP details
                }
            }
            
            // Auto-resize rows for better text display
            for (int row = 0; row < detailsTable.getRowCount(); row++) {
                String value = detailsTableModel.getValueAt(row, 1).toString();
                int lines = value.split("\n").length;
                int height = Math.max(25, lines * 20);
                detailsTable.setRowHeight(row, height);
            }
            
            // Update request/response editors
            if (note.getRequestResponse() != null) {
                requestEditor.setRequest(note.getRequestResponse().request());
                if (note.getRequestResponse().response() != null) {
                    responseEditor.setResponse(note.getRequestResponse().response());
                } else {
                    responseEditor.setResponse(null);
                }
            } else {
                requestEditor.setRequest(null);
                responseEditor.setResponse(null);
            }
        } else {
            // Clear table when no note is selected
            detailsTableModel.setRowCount(0);
            requestEditor.setRequest(null);
            responseEditor.setResponse(null);
        }
    }
    
    /**
     * Add a new note to the collection
     */
    private void addNote(Note note) {
        notes.add(note);
        Object[] rowData = {
            note.getTitle(),
            note.getSeverity(),
            note.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        };
        tableModel.addRow(rowData);
        
        // Refresh the table to apply color coding
        notesTable.repaint();
    }
    
    /**
     * Edit the selected note
     */
    private void editSelectedNote() {
        int selectedRow = notesTable.getSelectedRow();
        if (selectedRow < 0 || selectedRow >= notes.size()) {
            JOptionPane.showMessageDialog(mainPanel, "Please select a note to edit", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        Note noteToEdit = notes.get(selectedRow);
        showEditNoteDialog(noteToEdit, selectedRow);
    }
    
    /**
     * Delete the selected note
     */
    private void deleteSelectedNote() {
        int selectedRow = notesTable.getSelectedRow();
        if (selectedRow < 0 || selectedRow >= notes.size()) {
            JOptionPane.showMessageDialog(mainPanel, "Please select a note to delete", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        Note noteToDelete = notes.get(selectedRow);
        int result = JOptionPane.showConfirmDialog(
            mainPanel,
            "Are you sure you want to delete the note:\n\"" + noteToDelete.getTitle() + "\"?",
            "Confirm Deletion",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        
        if (result == JOptionPane.YES_OPTION) {
            notes.remove(selectedRow);
            tableModel.removeRow(selectedRow);
            
            // Clear details if this was the selected note
            if (notesTable.getSelectedRow() == -1) {
                detailsTableModel.setRowCount(0);
                requestEditor.setRequest(null);
                responseEditor.setResponse(null);
            }
            
            JOptionPane.showMessageDialog(mainPanel, "Note deleted successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    /**
     * Context menu provider for adding "Bind Note" option
     */
    private class NotesContextMenuProvider implements ContextMenuItemsProvider {
        @Override
        public List<Component> provideMenuItems(ContextMenuEvent event) {
            List<Component> menuItems = new ArrayList<>();
            
            // Only show menu item for Proxy History and Repeater
            if (event.isFromTool(ToolType.PROXY) || event.isFromTool(ToolType.REPEATER)) {
                JMenuItem bindNoteItem = new JMenuItem("Bind Note");
                bindNoteItem.addActionListener(e -> {
                    if (event.messageEditorRequestResponse().isPresent()) {
                        MessageEditorHttpRequestResponse editor = event.messageEditorRequestResponse().get();
                        showNoteDialog(editor.requestResponse());
                    } else {
                        showNoteDialog(null);
                    }
                });
                menuItems.add(bindNoteItem);
            }
            
            return menuItems;
        }
    }
    
    /**
     * Show the note-taking dialog
     */
    private void showNoteDialog(HttpRequestResponse requestResponse) {
        if (requestResponse == null) {
            JOptionPane.showMessageDialog(mainPanel, "No request/response selected", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Create dialog
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(mainPanel), "Bind Note", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(900, 700);
        dialog.setLocationRelativeTo(mainPanel);
        
        // Create form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        
        // Title field
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST;
        formPanel.add(new JLabel("Title:"), gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        JTextField titleField = new JTextField(30);
        formPanel.add(titleField, gbc);
        
        // Severity dropdown
        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        formPanel.add(new JLabel("Severity:"), gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        String[] severityOptions = {"Critical", "High", "Medium", "Low", "Informational"};
        JComboBox<String> severityCombo = new JComboBox<>(severityOptions);
        formPanel.add(severityCombo, gbc);
        
        // Description area
        gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        formPanel.add(new JLabel("Description:"), gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.BOTH; gbc.weightx = 1.0; gbc.weighty = 0.3;
        JTextArea descriptionArea = new JTextArea(5, 30);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        JScrollPane descScrollPane = new JScrollPane(descriptionArea);
        formPanel.add(descScrollPane, gbc);
        
        // Proof of Concept area with split pane for request/response
        gbc.gridx = 0; gbc.gridy = 3; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0; gbc.weighty = 0;
        formPanel.add(new JLabel("Proof of Concept:"), gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.BOTH; gbc.weightx = 1.0; gbc.weighty = 0.7;
        
        // Create request and response editors for the dialog
        HttpRequestEditor dialogRequestEditor = api.userInterface().createHttpRequestEditor(EditorOptions.READ_ONLY);
        HttpResponseEditor dialogResponseEditor = api.userInterface().createHttpResponseEditor(EditorOptions.READ_ONLY);
        
        // Set the request and response
        dialogRequestEditor.setRequest(requestResponse.request());
        if (requestResponse.response() != null) {
            dialogResponseEditor.setResponse(requestResponse.response());
        }
        
        // Create labeled panels for request and response in dialog
        JPanel dialogRequestPanel = new JPanel(new BorderLayout());
        dialogRequestPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(ThemeColors.getBorderColor(), 1),
            "📤 HTTP Request",
            0, 0,
            new Font(Font.SANS_SERIF, Font.BOLD, 11),
            ThemeColors.getTitledBorderTextColor()
        ));
        dialogRequestPanel.add(dialogRequestEditor.uiComponent(), BorderLayout.CENTER);
        
        JPanel dialogResponsePanel = new JPanel(new BorderLayout());
        dialogResponsePanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(ThemeColors.getBorderColor(), 1),
            "📥 HTTP Response",
            0, 0,
            new Font(Font.SANS_SERIF, Font.BOLD, 11),
            ThemeColors.getTitledBorderTextColor()
        ));
        dialogResponsePanel.add(dialogResponseEditor.uiComponent(), BorderLayout.CENTER);
        
        // Create split pane for request/response in dialog with labeled panels
        JSplitPane dialogSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        dialogSplitPane.setLeftComponent(dialogRequestPanel);
        dialogSplitPane.setRightComponent(dialogResponsePanel);
        dialogSplitPane.setResizeWeight(0.5);
        dialogSplitPane.setOneTouchExpandable(true);
        dialogSplitPane.setPreferredSize(new Dimension(800, 300));
        
        // Set divider location after component is properly sized
        SwingUtilities.invokeLater(() -> {
            dialogSplitPane.setDividerLocation(400); // Half of 800px width
        });
        
        formPanel.add(dialogSplitPane, gbc);
        
        dialog.add(formPanel, BorderLayout.CENTER);
        
        // Create button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(e -> {
            String title = titleField.getText().trim();
            if (title.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please enter a title", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            String severity = (String) severityCombo.getSelectedItem();
            String description = descriptionArea.getText().trim();
            
            // Generate PoC text for export purposes
            StringBuilder poc = new StringBuilder();
            poc.append("=== HTTP REQUEST ===\n");
            poc.append(requestResponse.request().toString());
            poc.append("\n\n=== HTTP RESPONSE ===\n");
            if (requestResponse.response() != null) {
                poc.append(requestResponse.response().toString());
            } else {
                poc.append("No response available");
            }
            
            Note note = new Note(title, severity, description, poc.toString(), requestResponse);
            addNote(note);
            
            dialog.dispose();
            JOptionPane.showMessageDialog(mainPanel, "Note saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
        });
        
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.setVisible(true);
    }
    
    /**
     * Show the edit note dialog
     */
    private void showEditNoteDialog(Note noteToEdit, int noteIndex) {
        // Create dialog
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(mainPanel), "Edit Note", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(900, 700);
        dialog.setLocationRelativeTo(mainPanel);
        
        // Create form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        
        // Title field
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST;
        formPanel.add(new JLabel("Title:"), gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        JTextField titleField = new JTextField(30);
        titleField.setText(noteToEdit.getTitle());
        formPanel.add(titleField, gbc);
        
        // Severity dropdown
        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        formPanel.add(new JLabel("Severity:"), gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        String[] severityOptions = {"Critical", "High", "Medium", "Low", "Informational"};
        JComboBox<String> severityCombo = new JComboBox<>(severityOptions);
        severityCombo.setSelectedItem(noteToEdit.getSeverity());
        formPanel.add(severityCombo, gbc);
        
        // Description area
        gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        formPanel.add(new JLabel("Description:"), gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.BOTH; gbc.weightx = 1.0; gbc.weighty = 0.3;
        JTextArea descriptionArea = new JTextArea(5, 30);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setText(noteToEdit.getDescription());
        JScrollPane descScrollPane = new JScrollPane(descriptionArea);
        formPanel.add(descScrollPane, gbc);
        
        // Proof of Concept area (read-only, showing original data)
        gbc.gridx = 0; gbc.gridy = 3; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0; gbc.weighty = 0;
        formPanel.add(new JLabel("Proof of Concept (Read-only):"), gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.BOTH; gbc.weightx = 1.0; gbc.weighty = 0.7;
        
        if (noteToEdit.getRequestResponse() != null) {
            // Create request and response editors for the dialog
            HttpRequestEditor dialogRequestEditor = api.userInterface().createHttpRequestEditor(EditorOptions.READ_ONLY);
            HttpResponseEditor dialogResponseEditor = api.userInterface().createHttpResponseEditor(EditorOptions.READ_ONLY);
            
            // Set the request and response
            dialogRequestEditor.setRequest(noteToEdit.getRequestResponse().request());
            if (noteToEdit.getRequestResponse().response() != null) {
                dialogResponseEditor.setResponse(noteToEdit.getRequestResponse().response());
            }
            
            // Create labeled panels for request and response in edit dialog
            JPanel editRequestPanel = new JPanel(new BorderLayout());
            editRequestPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(ThemeColors.getBorderColor(), 1),
                "📤 HTTP Request",
                0, 0,
                new Font(Font.SANS_SERIF, Font.BOLD, 11),
                ThemeColors.getTitledBorderTextColor()
            ));
            editRequestPanel.add(dialogRequestEditor.uiComponent(), BorderLayout.CENTER);
            
            JPanel editResponsePanel = new JPanel(new BorderLayout());
            editResponsePanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(ThemeColors.getBorderColor(), 1),
                "📥 HTTP Response",
                0, 0,
                new Font(Font.SANS_SERIF, Font.BOLD, 11),
                ThemeColors.getTitledBorderTextColor()
            ));
            editResponsePanel.add(dialogResponseEditor.uiComponent(), BorderLayout.CENTER);
            
            // Create split pane for request/response in edit dialog with labeled panels
            JSplitPane dialogSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
            dialogSplitPane.setLeftComponent(editRequestPanel);
            dialogSplitPane.setRightComponent(editResponsePanel);
            dialogSplitPane.setResizeWeight(0.5);
            dialogSplitPane.setOneTouchExpandable(true);
            dialogSplitPane.setPreferredSize(new Dimension(800, 300));
            
            // Set divider location after component is properly sized
            SwingUtilities.invokeLater(() -> {
                dialogSplitPane.setDividerLocation(400); // Half of 800px width
            });
            
            formPanel.add(dialogSplitPane, gbc);
        } else {
            // Show PoC as text if no HttpRequestResponse available
            JTextArea pocArea = new JTextArea(10, 30);
            pocArea.setEditable(false);
            pocArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
            pocArea.setBackground(ThemeColors.getFieldBackgroundColor());
            pocArea.setText(noteToEdit.getProofOfConcept());
            
            JScrollPane pocScrollPane = new JScrollPane(pocArea);
            formPanel.add(pocScrollPane, gbc);
        }
        
        dialog.add(formPanel, BorderLayout.CENTER);
        
        // Create button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        JButton saveButton = new JButton("💾 Save Changes");
        saveButton.addActionListener(e -> {
            String title = titleField.getText().trim();
            if (title.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please enter a title", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            String severity = (String) severityCombo.getSelectedItem();
            String description = descriptionArea.getText().trim();
            
            // Update the note
            try {
                java.lang.reflect.Field titleField_ref = Note.class.getDeclaredField("title");
                java.lang.reflect.Field severityField = Note.class.getDeclaredField("severity");
                java.lang.reflect.Field descriptionField = Note.class.getDeclaredField("description");
                
                titleField_ref.setAccessible(true);
                severityField.setAccessible(true);
                descriptionField.setAccessible(true);
                
                titleField_ref.set(noteToEdit, title);
                severityField.set(noteToEdit, severity);
                descriptionField.set(noteToEdit, description);
                
                // Update the table row
                tableModel.setValueAt(title, noteIndex, 0);
                tableModel.setValueAt(severity, noteIndex, 1);
                
                // Refresh the display
                notesTable.repaint();
                displaySelectedNote();
                
                dialog.dispose();
                JOptionPane.showMessageDialog(mainPanel, "Note updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Error updating note: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        JButton cancelButton = new JButton("❌ Cancel");
        cancelButton.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.setVisible(true);
    }
    
    /**
     * Action listener for the markdown export button
     */
    private class ExportMarkdownActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (notes.isEmpty()) {
                JOptionPane.showMessageDialog(mainPanel, "No notes to export", "Export Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Save Markdown Report");
            fileChooser.setSelectedFile(new File("auditpad_report_" + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".md"));
            
            int userSelection = fileChooser.showSaveDialog(mainPanel);
            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File fileToSave = fileChooser.getSelectedFile();
                try {
                    exportToMarkdown(fileToSave);
                    JOptionPane.showMessageDialog(mainPanel, 
                        "Notes exported successfully to: " + fileToSave.getAbsolutePath(), 
                        "Export Success", JOptionPane.INFORMATION_MESSAGE);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(mainPanel, 
                        "Error exporting notes: " + ex.getMessage(), 
                        "Export Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }
    
    /**
     * Action listener for the JSON export button
     */
    private class ExportJsonActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (notes.isEmpty()) {
                JOptionPane.showMessageDialog(mainPanel, "No notes to export", "Export Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Export Notes to JSON");
            fileChooser.setSelectedFile(new File("auditpad_notes_" + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".json"));
            
            int userSelection = fileChooser.showSaveDialog(mainPanel);
            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File fileToSave = fileChooser.getSelectedFile();
                try {
                    exportToJson(fileToSave);
                    JOptionPane.showMessageDialog(mainPanel, 
                        "Notes exported successfully to: " + fileToSave.getAbsolutePath(), 
                        "Export Success", JOptionPane.INFORMATION_MESSAGE);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(mainPanel, 
                        "Error exporting notes: " + ex.getMessage(), 
                        "Export Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }
    
    /**
     * Action listener for the JSON import button
     */
    private class ImportJsonActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Import Notes from JSON");
            
            int userSelection = fileChooser.showOpenDialog(mainPanel);
            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File fileToImport = fileChooser.getSelectedFile();
                try {
                    int importedCount = importFromJson(fileToImport);
                    JOptionPane.showMessageDialog(mainPanel, 
                        "Successfully imported " + importedCount + " notes from: " + fileToImport.getAbsolutePath(), 
                        "Import Success", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(mainPanel, 
                        "Error importing notes: " + ex.getMessage(), 
                        "Import Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }
    
    /**
     * Action listener for the XLSX export button
     */
    private class ExportXlsxActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (notes.isEmpty()) {
                JOptionPane.showMessageDialog(mainPanel, "No notes to export", "Export Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Export Notes to CSV (Excel Compatible)");
            fileChooser.setSelectedFile(new File("auditpad_report_" + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv"));
            
            int userSelection = fileChooser.showSaveDialog(mainPanel);
            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File fileToSave = fileChooser.getSelectedFile();
                try {
                    exportToCsv(fileToSave);
                    JOptionPane.showMessageDialog(mainPanel, 
                        "Notes exported successfully to: " + fileToSave.getAbsolutePath() + 
                        "\n\nThis CSV file can be opened in Excel for further analysis.", 
                        "Export Success", JOptionPane.INFORMATION_MESSAGE);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(mainPanel, 
                        "Error exporting notes: " + ex.getMessage(), 
                        "Export Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }
    
    /**
     * Export notes to a Markdown file
     */
    private void exportToMarkdown(File file) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("# AuditPad Security Assessment Report\n\n");
            writer.write("**Generated by:** AuditPad (Created by @ronydasx)\n\n");
            writer.write("**Generated on:** " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\n\n");
            writer.write("**Total findings:** " + notes.size() + "\n\n");
            writer.write("---\n\n");
            
            for (int i = 0; i < notes.size(); i++) {
                Note note = notes.get(i);
                writer.write("## " + (i + 1) + ". " + note.getTitle() + "\n\n");
                writer.write("**Severity:** " + note.getSeverity() + "\n\n");
                writer.write("**Timestamp:** " + note.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\n\n");
                writer.write("**Description:**\n\n");
                writer.write(note.getDescription() + "\n\n");
                writer.write("**Proof of Concept:**\n\n");
                writer.write("```\n");
                writer.write(note.getProofOfConcept());
                writer.write("\n```\n\n");
                writer.write("---\n\n");
            }
        }
    }
    
    /**
     * Export notes to a JSON file
     */
    private void exportToJson(File file) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("{\n");
            writer.write("  \"exportInfo\": {\n");
            writer.write("    \"version\": \"1.0\",\n");
            writer.write("    \"exportDate\": \"" + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "\",\n");
            writer.write("    \"totalNotes\": " + notes.size() + "\n");
            writer.write("  },\n");
            writer.write("  \"notes\": [\n");
            
            for (int i = 0; i < notes.size(); i++) {
                Note note = notes.get(i);
                writer.write("    " + note.toJsonString());
                if (i < notes.size() - 1) {
                    writer.write(",");
                }
                writer.write("\n");
            }
            
            writer.write("  ]\n");
            writer.write("}\n");
        }
    }
    
    /**
     * Export notes to a CSV file (Excel compatible)
     */
    private void exportToCsv(File file) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            // Write CSV header
            writer.write("\"Title\",\"Severity\",\"URL\",\"Timestamp\",\"Description\",\"HTTP Method\",\"Status Code\",\"Request Headers\",\"Response Headers\",\"Request Body\",\"Response Body\"\n");
            
            for (Note note : notes) {
                // Basic note information
                writer.write("\"" + escapeCsv(note.getTitle()) + "\",");
                writer.write("\"" + escapeCsv(note.getSeverity()) + "\",");
                writer.write("\"" + escapeCsv(note.getUrl()) + "\",");
                writer.write("\"" + note.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\",");
                writer.write("\"" + escapeCsv(note.getDescription()) + "\",");
                
                // Extract HTTP details if available
                if (note.getRequestResponse() != null) {
                    try {
                        String requestStr = note.getRequestResponse().request().toString();
                        String[] requestLines = requestStr.split("\n");
                        
                        // Extract HTTP method
                        String method = "N/A";
                        if (requestLines.length > 0) {
                            String[] parts = requestLines[0].split(" ");
                            if (parts.length > 0) {
                                method = parts[0];
                            }
                        }
                        writer.write("\"" + method + "\",");
                        
                        // Extract status code from response
                        String statusCode = "N/A";
                        if (note.getRequestResponse().response() != null) {
                            String responseStr = note.getRequestResponse().response().toString();
                            String[] responseLines = responseStr.split("\n");
                            if (responseLines.length > 0) {
                                String[] parts = responseLines[0].split(" ");
                                if (parts.length > 1) {
                                    statusCode = parts[1];
                                }
                            }
                        }
                        writer.write("\"" + statusCode + "\",");
                        
                        // Extract headers and body
                        String[] requestParts = requestStr.split("\n\n", 2);
                        String requestHeaders = requestParts.length > 0 ? requestParts[0] : "";
                        String requestBody = requestParts.length > 1 ? requestParts[1] : "";
                        
                        writer.write("\"" + escapeCsv(requestHeaders) + "\",");
                        
                        if (note.getRequestResponse().response() != null) {
                            String responseStr = note.getRequestResponse().response().toString();
                            String[] responseParts = responseStr.split("\n\n", 2);
                            String responseHeaders = responseParts.length > 0 ? responseParts[0] : "";
                            String responseBody = responseParts.length > 1 ? responseParts[1] : "";
                            
                            writer.write("\"" + escapeCsv(responseHeaders) + "\",");
                            writer.write("\"" + escapeCsv(requestBody) + "\",");
                            writer.write("\"" + escapeCsv(responseBody) + "\"");
                        } else {
                            writer.write("\"N/A\",");
                            writer.write("\"" + escapeCsv(requestBody) + "\",");
                            writer.write("\"N/A\"");
                        }
                    } catch (Exception e) {
                        // If parsing fails, fill with N/A
                        writer.write("\"N/A\",\"N/A\",\"N/A\",\"N/A\",\"N/A\",\"N/A\"");
                    }
                } else {
                    // No request/response data available
                    writer.write("\"N/A\",\"N/A\",\"N/A\",\"N/A\",\"N/A\",\"N/A\"");
                }
                
                writer.write("\n");
            }
        }
    }
    
    /**
     * Escape CSV values
     */
    private String escapeCsv(String value) {
        if (value == null) return "";
        // Replace quotes with double quotes and limit length for Excel compatibility
        String escaped = value.replace("\"", "\"\"");
        // Limit length to prevent Excel issues
        if (escaped.length() > 32000) {
            escaped = escaped.substring(0, 32000) + "... [truncated]";
        }
        return escaped;
    }
    
    /**
     * Import notes from a JSON file
     */
    private int importFromJson(File file) throws Exception {
        String content = new String(Files.readAllBytes(file.toPath()));
        int importedCount = 0;
        
        // Simple JSON parsing (basic implementation)
        String[] noteBlocks = content.split("\\{\\s*\"title\":");
        
        for (int i = 1; i < noteBlocks.length; i++) {
            try {
                String noteBlock = "{\"title\":" + noteBlocks[i];
                // Find the end of this note block
                int braceCount = 0;
                int endIndex = 0;
                for (int j = 0; j < noteBlock.length(); j++) {
                    if (noteBlock.charAt(j) == '{') braceCount++;
                    if (noteBlock.charAt(j) == '}') braceCount--;
                    if (braceCount == 0) {
                        endIndex = j + 1;
                        break;
                    }
                }
                noteBlock = noteBlock.substring(0, endIndex);
                
                // Parse the note
                Note importedNote = parseNoteFromJson(noteBlock);
                if (importedNote != null) {
                    addNote(importedNote);
                    importedCount++;
                }
            } catch (Exception e) {
                api.logging().logToError("Error parsing note: " + e.getMessage());
            }
        }
        
        return importedCount;
    }
    
    /**
     * Parse a single note from JSON string
     */
    private Note parseNoteFromJson(String jsonString) {
        try {
            String title = extractJsonValue(jsonString, "title");
            String severity = extractJsonValue(jsonString, "severity");
            String description = extractJsonValue(jsonString, "description");
            String timestampStr = extractJsonValue(jsonString, "timestamp");
            String proofOfConcept = extractJsonValue(jsonString, "proofOfConcept");
            String requestBase64 = extractJsonValue(jsonString, "request");
            String responseBase64 = extractJsonValue(jsonString, "response");
            
            // Parse timestamp
            LocalDateTime timestamp = LocalDateTime.parse(timestampStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            
            // For imported notes, we'll create them without HttpRequestResponse for simplicity
            // The PoC text contains the original request/response data
            Note note = new Note(title, severity, description, proofOfConcept, null);
            
            // Set the original timestamp using reflection
            try {
                java.lang.reflect.Field timestampField = Note.class.getDeclaredField("timestamp");
                timestampField.setAccessible(true);
                timestampField.set(note, timestamp);
            } catch (Exception e) {
                // If reflection fails, just use current timestamp
                api.logging().logToError("Could not set original timestamp: " + e.getMessage());
            }
            
            return note;
        } catch (Exception e) {
            api.logging().logToError("Error parsing note from JSON: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Extract a value from JSON string (simple implementation)
     */
    private String extractJsonValue(String json, String key) {
        String pattern = "\"" + key + "\":\\s*\"";
        int startIndex = json.indexOf(pattern);
        if (startIndex == -1) return null;
        
        startIndex += pattern.length();
        int endIndex = startIndex;
        
        // Handle escaped quotes
        while (endIndex < json.length()) {
            if (json.charAt(endIndex) == '"' && (endIndex == 0 || json.charAt(endIndex - 1) != '\\')) {
                break;
            }
            endIndex++;
        }
        
        if (endIndex >= json.length()) return null;
        
        String value = json.substring(startIndex, endIndex);
        // Unescape JSON strings
        return value.replace("\\\"", "\"")
                   .replace("\\n", "\n")
                   .replace("\\r", "\r")
                   .replace("\\t", "\t")
                   .replace("\\\\", "\\");
    }
    
}
