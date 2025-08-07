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
# Maven Mixins

Maven Mixins provide a powerful mechanism for sharing common POM configuration across multiple projects without the limitations of traditional inheritance. Mixins allow you to compose your project configuration from multiple sources, promoting better code reuse and maintainability.

## Overview

Maven Mixins address several limitations of traditional POM inheritance:

- **Single inheritance limitation**: Maven POMs can only inherit from one parent, limiting reusability
- **Deep inheritance hierarchies**: Complex inheritance chains can be difficult to understand and maintain
- **Configuration duplication**: Common configurations often need to be duplicated across projects
- **Tight coupling**: Changes to parent POMs can have unintended effects on child projects

Mixins solve these problems by allowing you to include configuration from multiple sources in a composable way.

## Basic Usage

### Declaring Mixins

Mixins are declared in the `<mixins>` section of your POM:

```xml
<project>
    <modelVersion>4.2.0</modelVersion>
    
    <groupId>com.example</groupId>
    <artifactId>my-project</artifactId>
    <version>1.0.0</version>
    
    <mixins>
        <mixin>
            <groupId>com.example.mixins</groupId>
            <artifactId>java-mixin</artifactId>
            <version>1.0.0</version>
        </mixin>
        <mixin>
            <groupId>com.example.mixins</groupId>
            <artifactId>testing-mixin</artifactId>
            <version>1.0.0</version>
        </mixin>
    </mixins>
    
    <!-- Your project-specific configuration -->
</project>
```

### Model Version Requirement

Mixins require **model version 4.2.0** or higher. This new model version was introduced specifically to support the mixins feature while maintaining backward compatibility.

## Creating Mixin POMs

A mixin is simply a regular Maven POM that contains reusable configuration. Here's an example of a Java compilation mixin:

```xml
<project>
    <modelVersion>4.2.0</modelVersion>
    
    <groupId>com.example.mixins</groupId>
    <artifactId>java-mixin</artifactId>
    <version>1.0.0</version>
    <packaging>pom</packaging>
    
    <name>Java Compilation Mixin</name>
    <description>Common Java compilation settings</description>
    
    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
                <configuration>
                    <release>17</release>
                    <compilerArgs>
                        <arg>-Xlint:all</arg>
                        <arg>-Werror</arg>
                    </compilerArgs>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

## Advanced Features

### Mixin Composition Order

Mixins are applied in the order they are declared. Later mixins can override configuration from earlier mixins:

```xml
<mixins>
    <mixin>
        <groupId>com.example.mixins</groupId>
        <artifactId>base-mixin</artifactId>
        <version>1.0.0</version>
    </mixin>
    <mixin>
        <groupId>com.example.mixins</groupId>
        <artifactId>override-mixin</artifactId>
        <version>1.0.0</version>
    </mixin>
</mixins>
```

### Combining with Inheritance

Mixins work alongside traditional POM inheritance. The resolution order is:

1. Parent POM configuration
2. Mixin configurations (in declaration order)
3. Current POM configuration

```xml
<project>
    <modelVersion>4.2.0</modelVersion>
    
    <parent>
        <groupId>com.example</groupId>
        <artifactId>parent-pom</artifactId>
        <version>1.0.0</version>
    </parent>
    
    <mixins>
        <mixin>
            <groupId>com.example.mixins</groupId>
            <artifactId>java-mixin</artifactId>
            <version>1.0.0</version>
        </mixin>
    </mixins>
    
    <artifactId>my-project</artifactId>
    <!-- Project-specific overrides -->
</project>
```

### Version Management

Like dependencies, mixin versions can be managed centrally:

```xml
<project>
    <modelVersion>4.2.0</modelVersion>
    
    <mixinManagement>
        <mixins>
            <mixin>
                <groupId>com.example.mixins</groupId>
                <artifactId>java-mixin</artifactId>
                <version>1.0.0</version>
            </mixin>
            <mixin>
                <groupId>com.example.mixins</groupId>
                <artifactId>testing-mixin</artifactId>
                <version>2.0.0</version>
            </mixin>
        </mixins>
    </mixinManagement>
    
    <mixins>
        <mixin>
            <groupId>com.example.mixins</groupId>
            <artifactId>java-mixin</artifactId>
            <!-- Version inherited from mixinManagement -->
        </mixin>
    </mixins>
</project>
```

## Common Use Cases

### 1. Technology Stack Mixins

Create mixins for specific technology stacks:

```xml
<!-- spring-boot-mixin -->
<project>
    <modelVersion>4.2.0</modelVersion>
    <groupId>com.example.mixins</groupId>
    <artifactId>spring-boot-mixin</artifactId>
    <version>1.0.0</version>
    <packaging>pom</packaging>
    
    <properties>
        <spring-boot.version>3.1.0</spring-boot.version>
    </properties>
    
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <version>${spring-boot.version}</version>
            </plugin>
        </plugins>
    </build>
</project>
```

### 2. Quality Assurance Mixins

Standardize code quality tools across projects:

```xml
<!-- quality-mixin -->
<project>
    <modelVersion>4.2.0</modelVersion>
    <groupId>com.example.mixins</groupId>
    <artifactId>quality-mixin</artifactId>
    <version>1.0.0</version>
    <packaging>pom</packaging>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.sonarsource.scanner.maven</groupId>
                <artifactId>sonar-maven-plugin</artifactId>
                <version>3.9.1.2184</version>
            </plugin>
            <plugin>
                <groupId>com.github.spotbugs</groupId>
                <artifactId>spotbugs-maven-plugin</artifactId>
                <version>4.7.3.0</version>
            </plugin>
            <plugin>
                <groupId>org.jacoco</groupId>
                <artifactId>jacoco-maven-plugin</artifactId>
                <version>0.8.8</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>prepare-agent</goal>
                        </goals>
                    </execution>
                    <execution>
                        <id>report</id>
                        <phase>test</phase>
                        <goals>
                            <goal>report</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

### 3. Testing Mixins

Standardize testing configurations:

```xml
<!-- testing-mixin -->
<project>
    <modelVersion>4.2.0</modelVersion>
    <groupId>com.example.mixins</groupId>
    <artifactId>testing-mixin</artifactId>
    <version>1.0.0</version>
    <packaging>pom</packaging>
    
    <properties>
        <junit.version>5.9.2</junit.version>
        <mockito.version>5.1.1</mockito.version>
    </properties>
    
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.junit.jupiter</groupId>
                <artifactId>junit-jupiter</artifactId>
                <version>${junit.version}</version>
                <scope>test</scope>
            </dependency>
            <dependency>
                <groupId>org.mockito</groupId>
                <artifactId>mockito-core</artifactId>
                <version>${mockito.version}</version>
                <scope>test</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.0.0-M9</version>
                <configuration>
                    <includes>
                        <include>**/*Test.java</include>
                        <include>**/*Tests.java</include>
                    </includes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

## Best Practices

### 1. Keep Mixins Focused

Each mixin should have a single, well-defined purpose:

- ✅ `java-17-mixin` - Java 17 compilation settings
- ✅ `spring-boot-mixin` - Spring Boot configuration
- ✅ `testing-mixin` - Testing framework setup
- ❌ `everything-mixin` - Kitchen sink approach

### 2. Use Semantic Versioning

Version your mixins using semantic versioning to communicate compatibility:

- `1.0.0` - Initial release
- `1.1.0` - New features, backward compatible
- `2.0.0` - Breaking changes

### 3. Document Your Mixins

Include clear documentation in your mixin POMs:

```xml
<project>
    <modelVersion>4.2.0</modelVersion>
    <groupId>com.example.mixins</groupId>
    <artifactId>java-mixin</artifactId>
    <version>1.0.0</version>
    <packaging>pom</packaging>

    <name>Java Compilation Mixin</name>
    <description>
        Provides standardized Java compilation settings including:
        - Java 17 source/target compatibility
        - UTF-8 encoding
        - Compiler warnings and error handling
        - Code quality checks
    </description>

    <url>https://github.com/example/maven-mixins</url>

    <!-- Configuration follows -->
</project>
```

### 4. Test Your Mixins

Create test projects that use your mixins to ensure they work correctly:

```
maven-mixins/
├── java-mixin/
│   └── pom.xml
├── testing-mixin/
│   └── pom.xml
└── test-projects/
    ├── simple-java-project/
    │   └── pom.xml (uses java-mixin)
    └── spring-boot-project/
        └── pom.xml (uses java-mixin + spring-boot-mixin)
```

### 5. Organize Mixins in a Dedicated Repository

Consider organizing your mixins in a dedicated repository or module:

```
company-maven-mixins/
├── pom.xml (parent for all mixins)
├── java-mixin/
├── spring-boot-mixin/
├── testing-mixin/
├── quality-mixin/
└── README.md
```

## Migration from Parent POMs

### Before (Traditional Inheritance)

```xml
<!-- parent-pom -->
<project>
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>parent-pom</artifactId>
    <version>1.0.0</version>
    <packaging>pom</packaging>

    <!-- Everything mixed together -->
    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <spring-boot.version>3.1.0</spring-boot.version>
        <junit.version>5.9.2</junit.version>
    </properties>

    <!-- All dependencies and plugins -->
</project>

<!-- child-project -->
<project>
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.example</groupId>
        <artifactId>parent-pom</artifactId>
        <version>1.0.0</version>
    </parent>

    <artifactId>my-project</artifactId>
    <!-- Inherits everything, wanted or not -->
</project>
```

### After (With Mixins)

```xml
<!-- my-project -->
<project>
    <modelVersion>4.2.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>my-project</artifactId>
    <version>1.0.0</version>

    <mixins>
        <mixin>
            <groupId>com.example.mixins</groupId>
            <artifactId>java-mixin</artifactId>
            <version>1.0.0</version>
        </mixin>
        <mixin>
            <groupId>com.example.mixins</groupId>
            <artifactId>spring-boot-mixin</artifactId>
            <version>1.0.0</version>
        </mixin>
        <mixin>
            <groupId>com.example.mixins</groupId>
            <artifactId>testing-mixin</artifactId>
            <version>1.0.0</version>
        </mixin>
    </mixins>

    <!-- Only project-specific configuration -->
</project>
```

## Troubleshooting

### Common Issues

1. **Model Version Error**
   ```
   Error: mixins require model version 4.2.0 or higher
   ```
   Solution: Update your `<modelVersion>` to `4.2.0`

2. **Mixin Not Found**
   ```
   Error: Could not resolve mixin com.example:my-mixin:1.0.0
   ```
   Solution: Ensure the mixin is deployed to your repository

3. **Circular Dependencies**
   ```
   Error: Circular mixin dependency detected
   ```
   Solution: Review your mixin dependencies and remove cycles

### Debugging Mixin Resolution

Use the Maven help plugin to see the effective POM after mixin resolution:

```bash
mvn help:effective-pom
```

## Conclusion

Maven Mixins provide a powerful and flexible way to share configuration across projects while avoiding the limitations of traditional inheritance. By composing your project configuration from focused, reusable mixins, you can:

- Reduce configuration duplication
- Improve maintainability
- Enable better separation of concerns
- Facilitate standardization across teams and organizations

Start small by creating mixins for your most common configuration patterns, and gradually build a library of reusable components that can accelerate your development process.
