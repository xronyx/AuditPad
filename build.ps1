# AuditPad Build Script for Windows PowerShell
# Author: @ronydasx
# Description: Automated build script for AuditPad Burp Suite Extension (Windows version)

param(
    [switch]$Help,
    [switch]$Clean,
    [switch]$Verbose,
    [switch]$NoJar
)

# Project configuration
$PROJECT_NAME = "AuditPad"
$MAIN_CLASS = "AuditPad"
$MONTOYA_API = "montoya-api-2025.8.jar"
$SRC_DIR = "src"
$BUILD_DIR = "build"
$DIST_DIR = "dist"

# Color functions for output
function Write-Status {
    param([string]$Message)
    Write-Host "[INFO] $Message" -ForegroundColor Blue
}

function Write-Success {
    param([string]$Message)
    Write-Host "[SUCCESS] $Message" -ForegroundColor Green
}

function Write-Warning {
    param([string]$Message)
    Write-Host "[WARNING] $Message" -ForegroundColor Yellow
}

function Write-Error {
    param([string]$Message)
    Write-Host "[ERROR] $Message" -ForegroundColor Red
}

# Global variables for Java tool paths
$script:JavaPath = ""
$script:JavacPath = ""
$script:JarPath = ""

# Function to check Java installation
function Test-JavaInstallation {
    Write-Status "Checking Java installation..."
    
    # Check for java runtime
    try {
        $javaCommand = Get-Command java -ErrorAction Stop
        $script:JavaPath = $javaCommand.Source
        $javaVersion = & $script:JavaPath -version 2>&1 | Select-String "version"
        if ($javaVersion) {
            Write-Success "Java Runtime found: $($javaVersion.Line.Trim())"
        } else {
            Write-Error "Java Runtime not found!"
            Write-Warning "Please install Java Development Kit (JDK) 11 or higher"
            Write-Warning "Download from: https://www.oracle.com/java/technologies/downloads/"
            Write-Warning "Or install OpenJDK from: https://adoptium.net/"
            exit 1
        }
    } catch {
        Write-Error "Java Runtime not found in PATH!"
        Write-Warning "Please install Java Development Kit (JDK) 11 or higher"
        Write-Warning "Download from: https://www.oracle.com/java/technologies/downloads/"
        Write-Warning "Or install OpenJDK from: https://adoptium.net/"
        Write-Warning "After installation, make sure java.exe is in your PATH"
        exit 1
    }
    
    # Check for javac compiler
    try {
        $javacCommand = Get-Command javac -ErrorAction Stop
        $script:JavacPath = $javacCommand.Source
        $javacVersion = & $script:JavacPath -version 2>&1
        if ($javacVersion) {
            Write-Success "Java Compiler found: $($javacVersion.Trim())"
        } else {
            Write-Error "Java Compiler (javac) not found!"
            Write-Warning "Please install Java Development Kit (JDK) - not just JRE"
            Write-Warning "Download JDK from: https://www.oracle.com/java/technologies/downloads/"
            Write-Warning "Or install OpenJDK from: https://adoptium.net/"
            exit 1
        }
    } catch {
        # Try to find javac in JAVA_HOME if not in PATH
        if ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\javac.exe")) {
            $script:JavacPath = "$env:JAVA_HOME\bin\javac.exe"
            $javacVersion = & $script:JavacPath -version 2>&1
            Write-Success "Java Compiler found in JAVA_HOME: $($javacVersion.Trim())"
        } else {
            Write-Error "Java Compiler (javac) not found in PATH or JAVA_HOME!"
            Write-Warning "Please install Java Development Kit (JDK) - not just JRE"
            Write-Warning "Download JDK from: https://www.oracle.com/java/technologies/downloads/"
            Write-Warning "Or install OpenJDK from: https://adoptium.net/"
            Write-Warning "Make sure JAVA_HOME is set or javac.exe is in your PATH"
            exit 1
        }
    }
    
    # Check for jar tool
    try {
        $jarCommand = Get-Command jar -ErrorAction Stop
        $script:JarPath = $jarCommand.Source
        $jarTest = & $script:JarPath 2>&1 | Select-String "Usage"
        if ($jarTest) {
            Write-Success "JAR tool found and working"
        } else {
            Write-Error "JAR tool not working properly!"
            exit 1
        }
    } catch {
        # Try to find jar in JAVA_HOME if not in PATH
        if ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\jar.exe")) {
            $script:JarPath = "$env:JAVA_HOME\bin\jar.exe"
            $jarTest = & $script:JarPath 2>&1 | Select-String "Usage"
            if ($jarTest) {
                Write-Success "JAR tool found in JAVA_HOME and working"
            } else {
                Write-Error "JAR tool found but not working properly!"
                exit 1
            }
        } else {
            Write-Error "JAR tool not found in PATH or JAVA_HOME!"
            Write-Warning "JAR tool should be included with JDK installation"
            Write-Warning "Make sure JAVA_HOME is set or jar.exe is in your PATH"
            exit 1
        }
    }
}

# Function to check if file exists
function Test-FileExists {
    param([string]$FilePath)
    if (!(Test-Path $FilePath)) {
        Write-Error "File not found: $FilePath"
        exit 1
    }
}

# Function to create directories if they don't exist
function New-DirectoryStructure {
    Write-Status "Creating directory structure..."
    
    if (!(Test-Path $SRC_DIR)) {
        New-Item -ItemType Directory -Path $SRC_DIR -Force | Out-Null
    }
    if (!(Test-Path $BUILD_DIR)) {
        New-Item -ItemType Directory -Path $BUILD_DIR -Force | Out-Null
    }
    if (!(Test-Path $DIST_DIR)) {
        New-Item -ItemType Directory -Path $DIST_DIR -Force | Out-Null
    }
    
    Write-Success "Directory structure created"
}

# Function to clean build directory
function Clear-BuildDirectory {
    Write-Status "Cleaning build directory..."
    
    if (Test-Path $BUILD_DIR) {
        Remove-Item "$BUILD_DIR\*" -Recurse -Force -ErrorAction SilentlyContinue
    }
    
    Write-Success "Build directory cleaned"
}

# Function to compile Java source
function Invoke-JavaCompilation {
    Write-Status "Compiling Java source files..."
    
    # Check if Montoya API exists
    Test-FileExists $MONTOYA_API
    
    # Check if source file exists
    $sourceFile = "$SRC_DIR\$MAIN_CLASS.java"
    Test-FileExists $sourceFile
    
    # Compile with Montoya API in classpath
    $compileArgs = @("-cp", $MONTOYA_API, "-d", $BUILD_DIR, "-encoding", "UTF-8", $sourceFile)
    
    if ($Verbose) {
        Write-Status "Executing: $script:JavacPath $($compileArgs -join ' ')"
    }
    
    $result = & $script:JavacPath $compileArgs 2>&1
    
    if ($LASTEXITCODE -eq 0) {
        Write-Success "Java compilation completed successfully"
    } else {
        Write-Error "Java compilation failed"
        if ($result) {
            Write-Host $result -ForegroundColor Red
        }
        exit 1
    }
}

# Function to create JAR file
function New-JarFile {
    Write-Status "Creating JAR file..."
    
    # Save current location
    $originalLocation = Get-Location
    
    try {
        # Change to build directory to avoid including directory structure in JAR
        Set-Location $BUILD_DIR
        
        # Create JAR file
        $jarArgs = @("cf", "..\$DIST_DIR\$PROJECT_NAME.jar")
        $jarArgs += Get-ChildItem "*.class" | ForEach-Object { $_.Name }
        
        if ($Verbose) {
            Write-Status "Executing: $script:JarPath $($jarArgs -join ' ')"
        }
        
        $result = & $script:JarPath $jarArgs 2>&1
        
        if ($LASTEXITCODE -eq 0) {
            Write-Success "JAR file created: $DIST_DIR\$PROJECT_NAME.jar"
        } else {
            Write-Error "JAR creation failed"
            if ($result) {
                Write-Host $result -ForegroundColor Red
            }
            exit 1
        }
    } finally {
        # Return to original location
        Set-Location $originalLocation
    }
}

# Function to show build summary
function Show-BuildSummary {
    Write-Host ""
    Write-Host "==========================================" -ForegroundColor Blue
    Write-Host "  AuditPad Build Summary" -ForegroundColor Blue
    Write-Host "==========================================" -ForegroundColor Blue
    Write-Host "Project Name: " -NoNewline
    Write-Host "$PROJECT_NAME" -ForegroundColor Green
    Write-Host "Source File:  " -NoNewline
    Write-Host "$SRC_DIR\$MAIN_CLASS.java" -ForegroundColor Green
    Write-Host "Build Dir:    " -NoNewline
    Write-Host "$BUILD_DIR" -ForegroundColor Green
    Write-Host "Output JAR:   " -NoNewline
    Write-Host "$DIST_DIR\$PROJECT_NAME.jar" -ForegroundColor Green
    Write-Host ""
    
    # Show file sizes
    $jarPath = "$DIST_DIR\$PROJECT_NAME.jar"
    if (Test-Path $jarPath) {
        $jarSize = (Get-Item $jarPath).Length
        $jarSizeKB = [math]::Round($jarSize / 1KB, 2)
        Write-Host "JAR Size:     " -NoNewline
        Write-Host "$jarSizeKB KB" -ForegroundColor Green
    }
    
    # Count class files
    $classFiles = Get-ChildItem "$BUILD_DIR\*.class" -ErrorAction SilentlyContinue
    $classCount = if ($classFiles) { $classFiles.Count } else { 0 }
    Write-Host "Class Files:  " -NoNewline
    Write-Host "$classCount" -ForegroundColor Green
    Write-Host ""
    
    # Show next steps
    Write-Host "Next Steps:" -ForegroundColor Yellow
    Write-Host "1. Load $DIST_DIR\$PROJECT_NAME.jar in Burp Suite"
    Write-Host "2. Go to Extensions → Installed → Add"
    Write-Host "3. Select the JAR file and click Next"
    Write-Host ""
}

# Function to show help
function Show-Help {
    Write-Host "AuditPad Build Script for Windows PowerShell"
    Write-Host "Usage: .\build.ps1 [OPTIONS]"
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  -Help          Show this help message"
    Write-Host "  -Clean         Clean build directory before building"
    Write-Host "  -Verbose       Enable verbose output"
    Write-Host "  -NoJar         Compile only, don't create JAR"
    Write-Host ""
    Write-Host "Examples:"
    Write-Host "  .\build.ps1              # Standard build"
    Write-Host "  .\build.ps1 -Clean       # Clean build"
    Write-Host "  .\build.ps1 -NoJar       # Compile only"
    Write-Host "  .\build.ps1 -Verbose     # Verbose output"
    Write-Host ""
}

# Main execution
function Main {
    # Show help if requested
    if ($Help) {
        Show-Help
        exit 0
    }
    
    # Start build process
    Write-Host "==========================================" -ForegroundColor Blue
    Write-Host "  AuditPad Build Script" -ForegroundColor Blue
    Write-Host "  Created by @ronydasx" -ForegroundColor Blue
    Write-Host "==========================================" -ForegroundColor Blue
    Write-Host ""
    
    # Check Java installation first
    Test-JavaInstallation
    
    # Create directory structure
    New-DirectoryStructure
    
    # Clean if requested
    if ($Clean) {
        Clear-BuildDirectory
    }
    
    # Compile Java source
    Invoke-JavaCompilation
    
    # Create JAR if requested
    if (!$NoJar) {
        New-JarFile
    }
    
    # Show build summary
    Show-BuildSummary
    
    Write-Success "Build completed successfully!"
}

# Run main function
Main
