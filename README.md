# AuditPad - Professional Burp Suite Extension

A comprehensive Burp Suite extension for managing security testing notes with professional features, structured documentation, multi-format export capabilities, and optimized dark/light theme support.

**Created by:** @ronydasx

## Features

- **Custom Main Tab**: Dedicated "Notes Binder" tab in Burp Suite for centralized note management
- **Context Menu Integration**: Right-click "Bind Note" option in Proxy History and Repeater tabs
- **Structured Note-Taking**: Organized fields for Title, Severity, Description, and Proof of Concept
- **Automatic PoC Population**: HTTP requests and responses are automatically captured
- **Note Management**: View, organize, and manage all notes in a user-friendly interface
- **Multi-Format Export**: Generate professional reports in Markdown, JSON, and CSV formats
- **Dark/Light Theme Support**: Optimized UI with proper text visibility in both Burp Suite themes
- **CRUD Operations**: Complete Create, Read, Update, Delete functionality for notes
- **Import/Export**: Save and restore note collections for persistence

## Quick Start

### Prerequisites
- Burp Suite Professional or Community Edition
- Java Development Kit (JDK) 11 or higher
- Montoya API JAR file (place in project root as `montoya-api-2025.8.jar`)

### Windows Users (Recommended)

**Automated Build with PowerShell:**

1. **Clone/Download the project**
2. **Place Montoya API JAR** in the project root directory
3. **Run the PowerShell build script**:
   ```powershell
   .\build.ps1
   ```

**PowerShell Build Script Features:**
- Automatically detects and validates Java installation
- Provides helpful error messages if Java/JDK is not properly installed
- Handles Java tools not in PATH (uses JAVA_HOME)
- UTF-8 encoding support for Unicode characters
- Clean build options

**PowerShell Build Options:**
```powershell
.\build.ps1              # Standard build
.\build.ps1 -Clean       # Clean build (removes old class files)
.\build.ps1 -Verbose     # Verbose output for debugging
.\build.ps1 -NoJar       # Compile only, don't create JAR
.\build.ps1 -Help        # Show all available options
```

### Linux/macOS Users

**Automated Build with Bash:**

1. **Clone/Download the project**
2. **Place Montoya API JAR** in the project root directory
3. **Run the build script**:
   ```bash
   ./build.sh
   ```

**Bash Build Options:**
```bash
./build.sh              # Standard build
./build.sh --clean      # Clean build (removes old class files)
./build.sh --help       # Show help and options
./build.sh --no-jar     # Compile only, don't create JAR
```

### Manual Build (Alternative)

If you prefer manual compilation:

**Windows:**
```cmd
mkdir build dist
javac -cp montoya-api-2025.8.jar -d build -encoding UTF-8 src/AuditPad.java
cd build && jar cf ../dist/AuditPad.jar *.class && cd ..
```

**Linux/macOS:**
```bash
mkdir -p build dist
javac -cp montoya-api-2025.8.jar -d build -encoding UTF-8 src/AuditPad.java
cd build && jar cf ../dist/AuditPad.jar *.class && cd ..
```

### Load in Burp Suite

1. Open Burp Suite
2. Go to **Extensions → Installed → Add**
3. Select **"Java"** as extension type
4. Choose **`dist/AuditPad.jar`**
5. Click **"Next"** to load

## Usage Guide

### Creating Notes

1. **Navigate to Proxy History or Repeater**
2. **Right-click on any HTTP request**
3. **Select "Bind Note" from the context menu**
4. **Fill in the note details**:
   - **Title**: Short, descriptive name for the finding
   - **Severity**: Select from Critical, High, Medium, Low, or Informational
   - **Description**: Detailed explanation of the vulnerability or finding
   - **Proof of Concept**: Automatically populated with the selected request/response
5. **Click "Save"** to store the note

### Managing Notes

1. **Switch to the "Notes Binder" tab**
2. **View all notes** in the table on the left
3. **Click on any note** to view full details on the right
4. **Edit notes** by double-clicking or using the context menu
5. **Delete notes** using the context menu
6. **Notes are sorted** by timestamp for easy organization

### Exporting Reports

The extension supports multiple export formats:

**Markdown Export:**
1. Go to the "Notes Binder" tab
2. Click "Export Notes (Markdown)"
3. Choose a location to save your report
4. Generated file includes professional formatting with timestamps and findings count

**JSON Export:**
- Click "Export as JSON" for structured data export
- Perfect for integration with other tools or custom processing

**CSV Export:**
- Click "Export as CSV" for spreadsheet-compatible format
- Easy import into Excel, Google Sheets, or other analysis tools

### Import/Export for Persistence

**Export Notes Collection:**
- Use "Export as JSON" to save all notes for later use
- Maintains all note data including timestamps and metadata

**Import Notes Collection:**
- Use "Import Notes" to restore previously saved note collections
- Merges with existing notes without duplicates

## Extension Architecture

### Core Components

- **AuditPad**: Main extension class implementing BurpExtension
- **Note**: Data class for storing note information with full CRUD support
- **NotesContextMenuProvider**: Handles context menu integration
- **ThemeColors**: Advanced theme detection and color management for dark/light modes
- **Export Listeners**: Multiple format export handlers (Markdown, JSON, CSV)

### Key Features Implementation

- **Memory-based Storage**: Notes persist during the Burp session
- **Advanced Theme Support**: Automatic detection and optimization for Burp Suite's dark/light themes
- **Swing UI Components**: Professional interface using Java Swing with theme-aware styling
- **Montoya API Integration**: Full compatibility with modern Burp Suite versions
- **Thread-safe Operations**: Proper handling of UI updates and data management
- **UTF-8 Support**: Full Unicode character support for international content

### Theme Support Details

The extension automatically detects Burp Suite's theme and adjusts:
- **Text Colors**: White text for dark theme, black text for light theme
- **Background Colors**: Theme-appropriate backgrounds for optimal readability
- **Border Colors**: Subtle borders that work in both themes
- **Severity Colors**: Color-coded severity levels optimized for each theme

## Project Structure

```
PentestNoteBinder/
├── src/
│   └── AuditPad.java                # Main extension source code (1577+ lines)
├── build/                           # Compiled class files (auto-generated)
│   ├── AuditPad.class
│   ├── AuditPad$Note.class
│   ├── AuditPad$ThemeColors.class
│   └── ...                          # Other inner class files
├── dist/
│   └── AuditPad.jar                 # Final JAR file (auto-generated)
├── build.sh                         # Linux/macOS build script
├── build.ps1                        # Windows PowerShell build script
├── montoya-api-2025.8.jar          # Montoya API (place here)
├── LICENSE                          # License file
└── README.md                        # This documentation
```

## Troubleshooting

### Environment Setup Issues

**Java Not Found:**
- **Windows**: The PowerShell script will guide you to download JDK from Oracle or Adoptium
- **All Platforms**: Ensure JDK 11+ is installed and JAVA_HOME is set correctly
- Verify with: `java -version` and `javac -version`

**javac/jar Not Found:**
- **Windows**: PowerShell script automatically finds tools in JAVA_HOME
- **All Platforms**: Ensure JDK (not just JRE) is installed
- Add `$JAVA_HOME/bin` to your PATH

### Extension Loading Issues

1. **Extension fails to load**:
   - Verify Java version compatibility (JDK 11+)
   - Check that Montoya API was in classpath during compilation
   - Ensure Burp Suite version supports Montoya API
   - Try rebuilding with `-Clean` option

2. **Compilation errors with Unicode characters**:
   - Use the provided build scripts (they include `-encoding UTF-8`)
   - For manual builds, always include `-encoding UTF-8` parameter

### UI and Theme Issues

3. **Text not visible in dark/light theme**:
   - Current version automatically handles theme detection
   - Rebuild the extension if you experience issues
   - Report theme-specific issues with your Burp Suite version

4. **Context menu not appearing**:
   - Verify you're right-clicking in Proxy History or Repeater tabs
   - Check that the extension loaded successfully in Extensions tab
   - Look for error messages in Burp Suite's output

### Export/Import Issues

5. **Export functionality not working**:
   - Ensure you have write permissions to the selected directory
   - Check that there are notes to export
   - Try different export formats (Markdown, JSON, CSV)

6. **Import not working**:
   - Ensure JSON file is properly formatted
   - Check file permissions
   - Verify JSON structure matches exported format

### Debug Information

The extension logs important events to Burp Suite's output:
- Extension loading/unloading messages
- Theme detection information
- Error information for troubleshooting
- Export/import operation status

## Technical Specifications

- **API**: Burp Suite Montoya API
- **Language**: Java 11+
- **UI Framework**: Java Swing with custom theme support
- **Storage**: In-memory (session-based) with export/import for persistence
- **Export Formats**: Markdown, JSON, CSV
- **Supported Tools**: Proxy History, Repeater
- **Theme Support**: Automatic dark/light theme detection and optimization
- **Character Encoding**: UTF-8 with full Unicode support
- **Build Systems**: PowerShell (Windows), Bash (Linux/macOS), Manual

## Version History

- **v2.0**: Major update with enhanced features
  - Multi-format export (Markdown, JSON, CSV)
  - Import/export functionality for persistence
  - Advanced dark/light theme support with automatic detection
  - CRUD operations for notes (Create, Read, Update, Delete)
  - Windows PowerShell build script with Java validation
  - UTF-8 encoding support for Unicode characters
  - Improved UI with theme-aware styling
  - Enhanced error handling and user feedback

- **v1.0**: Initial release with core functionality
  - Custom main tab
  - Context menu integration
  - Note-taking dialog
  - Basic Markdown export
  - Complete Montoya API implementation

## System Requirements

### Minimum Requirements
- Java Development Kit (JDK) 11 or higher
- Burp Suite Community or Professional Edition
- Windows 10+ (for PowerShell script) or Linux/macOS (for Bash script)

### Recommended Requirements
- Java Development Kit (JDK) 17 or higher
- Burp Suite Professional Edition (latest version)
- 4GB+ RAM for handling large note collections

## License

This extension is provided as-is for educational and professional penetration testing purposes. See LICENSE file for details.

## Support and Contributing

For issues, questions, or contributions:
1. Check the troubleshooting section above
2. Review the source code comments for implementation details
3. Refer to Burp Suite extension development documentation
4. Test with the latest Burp Suite version

## Acknowledgments

- Built using the Burp Suite Montoya API
- Designed for professional penetration testing workflows
- Optimized for both individual testers and team environments