#!/bin/bash

# Exit on error
set -e

# Maven version
MAVEN_VERSION="4.0.0-beta-6-SNAPSHOT"

echo "Creating new directory structure..."
mkdir -p impl
mkdir -p compat

# Create impl/pom.xml first
echo "Creating impl/pom.xml..."
cat > impl/pom.xml << EOF
<?xml version="1.0" encoding="UTF-8"?>
<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <parent>
    <groupId>org.apache.maven</groupId>
    <artifactId>maven</artifactId>
    <version>${MAVEN_VERSION}</version>
  </parent>

  <artifactId>maven-impl-modules</artifactId>
  <packaging>pom</packaging>

  <name>Maven Implementation Modules</name>

  <modules>
    <module>maven-impl</module>
    <module>maven-di</module>
    <module>maven-xml</module>
    <module>maven-jline</module>
    <module>maven-logging</module>
    <module>maven-core</module>
    <module>maven-cli</module>
  </modules>
</project>
EOF

# Create compat/pom.xml
echo "Creating compat/pom.xml..."
cat > compat/pom.xml << EOF
<?xml version="1.0" encoding="UTF-8"?>
<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <parent>
    <groupId>org.apache.maven</groupId>
    <artifactId>maven</artifactId>
    <version>${MAVEN_VERSION}</version>
  </parent>

  <artifactId>maven-compat-modules</artifactId>
  <packaging>pom</packaging>

  <name>Maven Compatibility Modules</name>

  <modules>
    <module>maven-plugin-api</module>
    <module>maven-builder-support</module>
    <module>maven-model</module>
    <module>maven-model-builder</module>
    <module>maven-settings</module>
    <module>maven-settings-builder</module>
    <module>maven-artifact</module>
    <module>maven-resolver-provider</module>
    <module>maven-repository-metadata</module>
    <module>maven-embedder</module>
    <module>maven-compat</module>
    <module>maven-toolchain-model</module>
    <module>maven-toolchain-builder</module>
  </modules>
</project>
EOF

# Add the new poms to git
git add impl/pom.xml compat/pom.xml

# Function to update relative paths in pom file
update_pom_paths() {
    local pom_file="$1"
    local is_moved_down="$2"  # true if module is moved down one level

    if [ ! -f "$pom_file" ]; then
        echo "POM file $pom_file does not exist!"
        return 1
    fi

    echo "Updating paths in $pom_file..."

    if [ "$is_moved_down" = true ]; then
        # Add relativePath to parent if it doesn't exist
        perl -i -pe 'BEGIN{undef $/;} s|(<parent>.*?)</parent>|$1\n    <relativePath>../\.\.\/</relativePath>\n  </parent>|s unless /relativePath/' "$pom_file"

        # Update ${basedir}/../ references to ${project.basedir}/../../
        perl -i -pe 's|\${basedir}/\.\./|\${project.basedir}/\.\./\.\./|g' "$pom_file"
        perl -i -pe 's|\${project.basedir\}/\.\./|\${project.basedir\}/\.\./\.\./|g' "$pom_file"
        perl -i -pe 's|<model>\.\./api/|<model>\.\./\.\./api/|g' "$pom_file"
    fi
}

# Function to update artifactId in pom files
update_artifact_id() {
    local old_id="$1"
    local new_id="$2"

    echo "Updating artifactId from $old_id to $new_id in all pom.xml files..."

    # Update in dependencies and dependencyManagement sections
    find . -name "pom.xml" -exec perl -i -pe \
        "s|<artifactId>$old_id</artifactId>|<artifactId>$new_id</artifactId>|g" \
        {} +
}

# Function to move and optionally rename a module
move_module() {
    local src="$1"
    local dest="$2"
    local is_renamed="$3"  # true if module is being renamed
    local new_artifact_id="$4"  # optional new artifactId

    if [ ! -d "$src" ]; then
        echo "Source directory $src does not exist!"
        return 1
    fi

    echo "Moving $src to $dest"
    if [ "$is_renamed" = true ]; then
        # For renamed modules, do a direct move
        rm -rf "$dest"  # Remove destination if it exists
        mkdir -p "$(dirname "$dest")"  # Create parent directory
        git mv "$src" "$dest"

        # Update artifactId if provided
        if [ ! -z "$new_artifact_id" ]; then
            local old_artifact_id=$(basename "$src")
            update_artifact_id "$old_artifact_id" "$new_artifact_id"
        fi

        # Update paths in POM
        update_pom_paths "$dest/pom.xml" true
    else
        # For non-renamed modules, we need to ensure clean destination
        rm -rf "$dest"  # Remove destination if it exists
        mkdir -p "$dest"
        # Move all files and directories, including hidden ones
        git ls-files --cached --others --exclude-standard "$src" | while read -r file; do
            if [[ "$file" == "$src"/* ]]; then
                target="${file/$src/$dest}"
                mkdir -p "$(dirname "$target")"
                git mv "$file" "$target"
            fi
        done

        # Update paths in POM
        update_pom_paths "$dest/pom.xml" true

        # Remove empty source directory
        rm -rf "$src"
    fi
}

echo "Moving implementation modules..."
# Move modules to impl directory (specifying which ones are renamed)
move_module "maven-api-impl" "impl/maven-impl" true "maven-impl"
move_module "maven-di" "impl/maven-di" false
move_module "maven-xml-impl" "impl/maven-xml" true "maven-xml"
move_module "maven-jline" "impl/maven-jline" false
move_module "maven-logging" "impl/maven-logging" false
move_module "maven-core" "impl/maven-core" false
move_module "maven-cli" "impl/maven-cli" false

echo "Moving compatibility modules..."
# Move modules to compat directory (none are renamed)
move_module "maven-plugin-api" "compat/maven-plugin-api" false
move_module "maven-builder-support" "compat/maven-builder-support" false
move_module "maven-model" "compat/maven-model" false
move_module "maven-model-builder" "compat/maven-model-builder" false
move_module "maven-settings" "compat/maven-settings" false
move_module "maven-settings-builder" "compat/maven-settings-builder" false
move_module "maven-artifact" "compat/maven-artifact" false
move_module "maven-resolver-provider" "compat/maven-resolver-provider" false
move_module "maven-repository-metadata" "compat/maven-repository-metadata" false
move_module "maven-embedder" "compat/maven-embedder" false
move_module "maven-compat" "compat/maven-compat" false
move_module "maven-toolchain-model" "compat/maven-toolchain-model" false
move_module "maven-toolchain-builder" "compat/maven-toolchain-builder" false

# Update root pom.xml - using perl instead of sed for macOS compatibility
echo "Updating root pom.xml..."
perl -i -pe 'BEGIN{undef $/;} s|<modules>.*?</modules>|<modules>\n    <module>api</module>\n    <module>impl</module>\n    <module>compat</module>\n    <module>maven-docgen</module>\n    <module>apache-maven</module>\n  </modules>|s' pom.xml

# Commit changes
echo "Committing changes..."
git add -A
git commit -m "Refactor: Reorganize Maven modules into impl and compat directories"

echo "Repository refactoring complete!"