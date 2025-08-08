#!/bin/bash

# Script to create test dependency JARs for Maven 4 new scopes integration test

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$SCRIPT_DIR/repo"

# Create temporary directory for building JARs
TEMP_DIR=$(mktemp -d)
trap "rm -rf $TEMP_DIR" EXIT

echo "Creating test dependency JARs..."

# Function to create a simple JAR with a single class
create_jar() {
    local artifact_id="$1"
    local class_name="$2"
    local message="$3"
    
    echo "Creating $artifact_id..."
    
    # Create directory structure
    local work_dir="$TEMP_DIR/$artifact_id"
    local src_dir="$work_dir/src/main/java/org/apache/maven/its/mng8750/deps"
    mkdir -p "$src_dir"
    
    # Create Java source file
    cat > "$src_dir/$class_name.java" << EOF
package org.apache.maven.its.mng8750.deps;

public class $class_name {
    public String getMessage() {
        return "$message";
    }
}
EOF
    
    # Compile the Java file
    local classes_dir="$work_dir/classes"
    mkdir -p "$classes_dir"
    javac -d "$classes_dir" "$src_dir/$class_name.java"
    
    # Create JAR
    local jar_dir="$REPO_DIR/org/apache/maven/its/mng8750/$artifact_id/1.0"
    mkdir -p "$jar_dir"
    (cd "$classes_dir" && jar cf "$jar_dir/$artifact_id-1.0.jar" .)
    
    # Create POM
    cat > "$jar_dir/$artifact_id-1.0.pom" << EOF
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <groupId>org.apache.maven.its.mng8750</groupId>
  <artifactId>$artifact_id</artifactId>
  <version>1.0</version>
  <packaging>jar</packaging>

  <name>$class_name</name>
  <description>Test dependency for Maven 4 new scopes: $message</description>
</project>
EOF
    
    echo "Created $artifact_id JAR and POM"
}

# Create all test dependencies
create_jar "compile-only-dep" "CompileOnlyDep" "Compile-only dependency"
create_jar "test-only-dep" "TestOnlyDep" "Test-only dependency"
create_jar "test-runtime-dep" "TestRuntimeDep" "Test runtime dependency"
create_jar "compile-dep" "CompileDep" "Regular compile dependency"
create_jar "test-dep" "TestDep" "Regular test dependency"

echo "All test dependency JARs created successfully!"
echo "Repository location: $REPO_DIR"
