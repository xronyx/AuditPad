#!/bin/bash

# AuditPad Build Script
# Author: @ronydasx
# Description: Automated build script for AuditPad Burp Suite Extension

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Project configuration
PROJECT_NAME="AuditPad"
MAIN_CLASS="AuditPad"
MONTOYA_API="montoya-api-2025.8.jar"
SRC_DIR="src"
BUILD_DIR="build"
DIST_DIR="dist"

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check if file exists
check_file() {
    if [ ! -f "$1" ]; then
        print_error "File not found: $1"
        exit 1
    fi
}

# Function to create directories if they don't exist
create_dirs() {
    print_status "Creating directory structure..."
    mkdir -p "$SRC_DIR" "$BUILD_DIR" "$DIST_DIR"
    print_success "Directory structure created"
}

# Function to clean build directory
clean_build() {
    print_status "Cleaning build directory..."
    rm -rf "$BUILD_DIR"/*
    print_success "Build directory cleaned"
}

# Function to compile Java source
compile_java() {
    print_status "Compiling Java source files..."
    
    # Check if Montoya API exists
    check_file "$MONTOYA_API"
    
    # Check if source file exists
    check_file "$SRC_DIR/${MAIN_CLASS}.java"
    
    # Compile with Montoya API in classpath
    javac -cp "$MONTOYA_API" -d "$BUILD_DIR" "$SRC_DIR/${MAIN_CLASS}.java"
    
    if [ $? -eq 0 ]; then
        print_success "Java compilation completed successfully"
    else
        print_error "Java compilation failed"
        exit 1
    fi
}

# Function to create JAR file
create_jar() {
    print_status "Creating JAR file..."
    
    # Change to build directory to avoid including directory structure in JAR
    cd "$BUILD_DIR"
    
    # Create JAR file
    jar cf "../$DIST_DIR/${PROJECT_NAME}.jar" *.class
    
    if [ $? -eq 0 ]; then
        cd ..
        print_success "JAR file created: $DIST_DIR/${PROJECT_NAME}.jar"
    else
        cd ..
        print_error "JAR creation failed"
        exit 1
    fi
}

# Function to show build summary
show_summary() {
    echo
    echo "=========================================="
    echo -e "${BLUE}  AuditPad Build Summary${NC}"
    echo "=========================================="
    echo -e "Project Name: ${GREEN}$PROJECT_NAME${NC}"
    echo -e "Source File:  ${GREEN}$SRC_DIR/${MAIN_CLASS}.java${NC}"
    echo -e "Build Dir:    ${GREEN}$BUILD_DIR${NC}"
    echo -e "Output JAR:   ${GREEN}$DIST_DIR/${PROJECT_NAME}.jar${NC}"
    echo
    
    # Show file sizes
    if [ -f "$DIST_DIR/${PROJECT_NAME}.jar" ]; then
        JAR_SIZE=$(ls -lh "$DIST_DIR/${PROJECT_NAME}.jar" | awk '{print $5}')
        echo -e "JAR Size:     ${GREEN}$JAR_SIZE${NC}"
    fi
    
    # Count class files
    CLASS_COUNT=$(find "$BUILD_DIR" -name "*.class" | wc -l)
    echo -e "Class Files:  ${GREEN}$CLASS_COUNT${NC}"
    echo
    
    # Show next steps
    echo -e "${YELLOW}Next Steps:${NC}"
    echo "1. Load $DIST_DIR/${PROJECT_NAME}.jar in Burp Suite"
    echo "2. Go to Extensions → Installed → Add"
    echo "3. Select the JAR file and click Next"
    echo
}

# Function to show help
show_help() {
    echo "AuditPad Build Script"
    echo "Usage: $0 [OPTIONS]"
    echo
    echo "Options:"
    echo "  -h, --help     Show this help message"
    echo "  -c, --clean    Clean build directory before building"
    echo "  -v, --verbose  Enable verbose output"
    echo "  --no-jar       Compile only, don't create JAR"
    echo
    echo "Examples:"
    echo "  $0              # Standard build"
    echo "  $0 --clean     # Clean build"
    echo "  $0 --no-jar    # Compile only"
    echo
}

# Main build function
main() {
    local clean_first=false
    local create_jar_file=true
    local verbose=false
    
    # Parse command line arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            -h|--help)
                show_help
                exit 0
                ;;
            -c|--clean)
                clean_first=true
                shift
                ;;
            -v|--verbose)
                verbose=true
                shift
                ;;
            --no-jar)
                create_jar_file=false
                shift
                ;;
            *)
                print_error "Unknown option: $1"
                show_help
                exit 1
                ;;
        esac
    done
    
    # Start build process
    echo "=========================================="
    echo -e "${BLUE}  AuditPad Build Script${NC}"
    echo -e "${BLUE}  Created by @ronyda${NC}"
    echo "=========================================="
    echo
    
    # Create directory structure
    create_dirs
    
    # Clean if requested
    if [ "$clean_first" = true ]; then
        clean_build
    fi
    
    # Compile Java source
    compile_java
    
    # Create JAR if requested
    if [ "$create_jar_file" = true ]; then
        create_jar
    fi
    
    # Show build summary
    show_summary
    
    print_success "Build completed successfully!"
}

# Run main function with all arguments
main "$@"
