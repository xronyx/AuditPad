# AuditPad - Professional Burp Suite Extension

A comprehensive Burp Suite extension for managing security testing notes with professional features, structured documentation, and multi-format export capabilities.

**Created by:** @ronydasx

## Features

- **Custom Main Tab**: Dedicated "Notes Binder" tab in Burp Suite for centralized note management
- **Context Menu Integration**: Right-click "Bind Note" option in Proxy History and Repeater tabs
- **Structured Note-Taking**: Organized fields for Title, Severity, Description, and Proof of Concept
- **Automatic PoC Population**: HTTP requests and responses are automatically captured
- **Note Management**: View, organize, and manage all notes in a user-friendly interface
- **Markdown Export**: Generate professional penetration testing reports in Markdown format

## Quick Start

### Prerequisites
- Burp Suite Professional or Community Edition
- Java Development Kit (JDK) 11 or higher
- Montoya API JAR file (place in project root as `montoya-api-2025.8.jar`)

### Automated Build (Recommended)

1. **Clone/Download the project**
2. **Place Montoya API JAR** in the project root directory
3. **Run the build script**:
   ```bash
   ./build.sh
   ```

That's it! The script will automatically:
- Create the proper directory structure
- Compile the Java source
- Generate the JAR file in `dist/AuditPad.jar`

### Build Script Options

```bash
./build.sh              # Standard build
./build.sh --clean      # Clean build (removes old class files)
./build.sh --help       # Show help and options
./build.sh --no-jar     # Compile only, don't create JAR
```

### Manual Build (Alternative)

If you prefer manual compilation:

```bash
# Create directories
mkdir -p src build dist

# Compile
javac -cp montoya-api-2025.8.jar -d build src/AuditPad.java

# Create JAR
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
4. **Notes are sorted** by timestamp for easy organization

### Exporting Reports

1. **Go to the "Notes Binder" tab**
2. **Click "Export Notes (Markdown)"**
3. **Choose a location** to save your report
4. **The generated Markdown file** includes:
   - Report header with timestamp
   - Total findings count
   - Each note with full details
   - Properly formatted HTTP requests/responses in code blocks

## Extension Architecture

### Core Components

- **PentesterNotesBinder**: Main extension class implementing BurpExtension
- **Note**: Data class for storing note information
- **NotesContextMenuProvider**: Handles context menu integration
- **ExportActionListener**: Manages Markdown export functionality

### Key Features Implementation

- **Memory-based Storage**: Notes persist during the Burp session
- **Swing UI Components**: Professional interface using Java Swing
- **Montoya API Integration**: Full compatibility with modern Burp Suite versions
- **Thread-safe Operations**: Proper handling of UI updates and data management

## Project Structure

```
AuditPad/
├── src/
│   └── AuditPad.java                # Main extension source code
├── build/                           # Compiled class files (auto-generated)
│   ├── AuditPad.class
│   ├── AuditPad$Note.class
│   └── ...                          # Other inner class files
├── dist/
│   └── AuditPad.jar                 # Final JAR file (auto-generated)
├── build.sh                         # Automated build script
├── montoya-api-2025.8.jar          # Montoya API (place here)
└── README.md                        # This documentation
```

## Troubleshooting

### Common Issues

1. **Extension fails to load with "Extension class is not a recognized type"**:
   - Recompile the extension without package declaration (current version has this fixed)
   - Make sure to load the `.class` file directly, not a JAR with incorrect structure
   - Try loading `PentesterNotesBinder.class` directly instead of a JAR file

2. **Extension fails to load**:
   - Verify Java version compatibility (JDK 11+)
   - Check that Montoya API is in classpath during compilation
   - Ensure Burp Suite version supports Montoya API

3. **Context menu not appearing**:
   - Verify you're right-clicking in Proxy History or Repeater tabs
   - Check that the extension loaded successfully in Extensions tab

4. **Export functionality not working**:
   - Ensure you have write permissions to the selected directory
   - Check that there are notes to export

### Debug Information

The extension logs important events to Burp Suite's output:
- Extension loading/unloading messages
- Error information for troubleshooting

## Technical Specifications

- **API**: Burp Suite Montoya API
- **Language**: Java
- **UI Framework**: Java Swing
- **Storage**: In-memory (session-based)
- **Export Format**: Markdown
- **Supported Tools**: Proxy History, Repeater

## Version History

- **v1.0**: Initial release with core functionality
  - Custom main tab
  - Context menu integration
  - Note-taking dialog
  - Markdown export
  - Complete Montoya API implementation

## License

This extension is provided as-is for educational and professional penetration testing purposes.

## Support

For issues, questions, or contributions, please refer to the source code comments and Burp Suite extension development documentation.
