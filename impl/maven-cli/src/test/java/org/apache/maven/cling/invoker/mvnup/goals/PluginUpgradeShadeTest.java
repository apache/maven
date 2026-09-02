/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.cling.invoker.mvnup.goals;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import eu.maveniverse.domtrip.Document;
import eu.maveniverse.domtrip.Editor;
import eu.maveniverse.domtrip.Element;
import org.apache.maven.cling.invoker.mvnup.UpgradeContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

/**
 * Tests for shade-plugin custom ResourceTransformer detection in {@link PluginUpgradeStrategy}.
 * Verifies that mvnup skips shade-plugin upgrades when custom transformers are present
 * (see <a href="https://github.com/apache/maven/issues/12991">MAVEN-12991</a>).
 */
@DisplayName("PluginUpgradeStrategy — Shade Plugin Custom Transformers")
class PluginUpgradeShadeTest {

    private PluginUpgradeStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new PluginUpgradeStrategy();
    }

    private UpgradeContext createMockContext() {
        return TestUtils.createMockContext();
    }

    @Test
    @DisplayName("should skip shade-plugin upgrade when custom ResourceTransformer is present")
    void shouldSkipUpgradeWithCustomTransformer() throws Exception {
        String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <build>
                        <plugins>
                            <plugin>
                                <groupId>org.apache.maven.plugins</groupId>
                                <artifactId>maven-shade-plugin</artifactId>
                                <version>1.3.3</version>
                                <executions>
                                    <execution>
                                        <goals>
                                            <goal>shade</goal>
                                        </goals>
                                        <configuration>
                                            <transformers>
                                                <transformer implementation="org.apache.myfaces.extensions.cdi.maven.BeansXmlTransformer"/>
                                            </transformers>
                                        </configuration>
                                    </execution>
                                </executions>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """;

        Document document = Document.of(pomXml);
        Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

        UpgradeContext context = createMockContext();
        UpgradeResult result = strategy.doApply(context, pomMap);

        assertTrue(result.success(), "Strategy should succeed without errors");

        // Verify the version was NOT upgraded
        Editor editor = new Editor(document);
        String version = editor.root()
                .path("build", "plugins", "plugin", "version")
                .map(Element::textContentTrimmed)
                .orElse(null);
        assertEquals("1.3.3", version, "Shade plugin version should remain unchanged");

        // Verify a warning was logged
        verify(context.logger, atLeastOnce())
                .warn(argThat(msg -> msg.contains("Skipping maven-shade-plugin upgrade")
                        && msg.contains("custom ResourceTransformer")
                        && msg.contains("BeansXmlTransformer")));
    }

    @Test
    @DisplayName("should skip shade-plugin upgrade when custom transformer is in top-level configuration")
    void shouldSkipUpgradeWithCustomTransformerInTopLevelConfig() throws Exception {
        String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <build>
                        <plugins>
                            <plugin>
                                <groupId>org.apache.maven.plugins</groupId>
                                <artifactId>maven-shade-plugin</artifactId>
                                <version>1.3.3</version>
                                <configuration>
                                    <transformers>
                                        <transformer implementation="com.example.CustomTransformer"/>
                                    </transformers>
                                </configuration>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """;

        Document document = Document.of(pomXml);
        Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

        UpgradeContext context = createMockContext();
        strategy.doApply(context, pomMap);

        // Verify the version was NOT upgraded
        Editor editor = new Editor(document);
        String version = editor.root()
                .path("build", "plugins", "plugin", "version")
                .map(Element::textContentTrimmed)
                .orElse(null);
        assertEquals("1.3.3", version, "Shade plugin version should remain unchanged");
    }

    @Test
    @DisplayName("should not detect custom transformers when only standard transformers are present")
    void shouldUpgradeWithStandardTransformers() throws Exception {
        String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <build>
                        <plugins>
                            <plugin>
                                <groupId>org.apache.maven.plugins</groupId>
                                <artifactId>maven-shade-plugin</artifactId>
                                <version>3.0.0</version>
                                <configuration>
                                    <transformers>
                                        <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                                            <mainClass>com.example.Main</mainClass>
                                        </transformer>
                                        <transformer implementation="org.apache.maven.plugins.shade.resource.ServicesResourceTransformer"/>
                                    </transformers>
                                </configuration>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """;

        // Verify no custom transformers are detected — standard transformers should not
        // block the shade-plugin upgrade
        Document document = Document.of(pomXml);
        Element pluginElement = document.root()
                .childElement("build")
                .flatMap(b -> b.childElement("plugins"))
                .flatMap(p -> p.childElement("plugin"))
                .orElseThrow();
        List<String> customTransformers = strategy.findCustomTransformerClasses(pluginElement);
        assertTrue(customTransformers.isEmpty(), "Standard transformers should not be detected as custom");
    }

    @Test
    @DisplayName("should not detect custom transformers when no transformers are configured")
    void shouldUpgradeWithNoTransformers() throws Exception {
        String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <build>
                        <plugins>
                            <plugin>
                                <groupId>org.apache.maven.plugins</groupId>
                                <artifactId>maven-shade-plugin</artifactId>
                                <version>3.0.0</version>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """;

        // Verify no custom transformers are detected when no transformers are configured
        Document document = Document.of(pomXml);
        Element pluginElement = document.root()
                .childElement("build")
                .flatMap(b -> b.childElement("plugins"))
                .flatMap(p -> p.childElement("plugin"))
                .orElseThrow();
        List<String> customTransformers = strategy.findCustomTransformerClasses(pluginElement);
        assertTrue(customTransformers.isEmpty(), "No transformers configured should return empty list");
    }

    @Test
    @DisplayName("should skip shade-plugin upgrade with mixed standard and custom transformers")
    void shouldSkipUpgradeWithMixedTransformers() throws Exception {
        String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <build>
                        <plugins>
                            <plugin>
                                <groupId>org.apache.maven.plugins</groupId>
                                <artifactId>maven-shade-plugin</artifactId>
                                <version>1.3.3</version>
                                <executions>
                                    <execution>
                                        <configuration>
                                            <transformers>
                                                <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer"/>
                                                <transformer implementation="org.apache.myfaces.extensions.cdi.maven.BeansXmlTransformer"/>
                                            </transformers>
                                        </configuration>
                                    </execution>
                                </executions>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """;

        Document document = Document.of(pomXml);
        Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

        UpgradeContext context = createMockContext();
        strategy.doApply(context, pomMap);

        // Verify the version was NOT upgraded
        Editor editor = new Editor(document);
        String version = editor.root()
                .path("build", "plugins", "plugin", "version")
                .map(Element::textContentTrimmed)
                .orElse(null);
        assertEquals("1.3.3", version, "Shade plugin version should remain unchanged");
    }

    @Test
    @DisplayName("should skip shade-plugin upgrade with property version and custom transformers")
    void shouldSkipUpgradeWithPropertyVersionAndCustomTransformers() throws Exception {
        String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <properties>
                        <shade.plugin.version>1.3.3</shade.plugin.version>
                    </properties>
                    <build>
                        <plugins>
                            <plugin>
                                <groupId>org.apache.maven.plugins</groupId>
                                <artifactId>maven-shade-plugin</artifactId>
                                <version>${shade.plugin.version}</version>
                                <configuration>
                                    <transformers>
                                        <transformer implementation="org.apache.myfaces.extensions.cdi.maven.BeansXmlTransformer"/>
                                    </transformers>
                                </configuration>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """;

        Document document = Document.of(pomXml);
        Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

        UpgradeContext context = createMockContext();
        strategy.doApply(context, pomMap);

        // Verify the property was NOT upgraded
        Editor editor = new Editor(document);
        String version = editor.root()
                .path("properties", "shade.plugin.version")
                .map(Element::textContentTrimmed)
                .orElse(null);
        assertEquals("1.3.3", version, "Shade plugin property should remain unchanged");
    }

    @Test
    @DisplayName("should skip shade-plugin upgrade without explicit groupId when custom transformers present")
    void shouldSkipUpgradeWithoutGroupIdAndCustomTransformers() throws Exception {
        String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <build>
                        <plugins>
                            <plugin>
                                <artifactId>maven-shade-plugin</artifactId>
                                <version>1.3.3</version>
                                <configuration>
                                    <transformers>
                                        <transformer implementation="com.example.CustomTransformer"/>
                                    </transformers>
                                </configuration>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """;

        Document document = Document.of(pomXml);
        Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

        UpgradeContext context = createMockContext();
        strategy.doApply(context, pomMap);

        // Verify the version was NOT upgraded
        Editor editor = new Editor(document);
        String version = editor.root()
                .path("build", "plugins", "plugin", "version")
                .map(Element::textContentTrimmed)
                .orElse(null);
        assertEquals("1.3.3", version, "Shade plugin version should remain unchanged");
    }

    @Test
    @DisplayName("should detect custom transformers in pluginManagement section")
    void shouldSkipUpgradeWithCustomTransformerInPluginManagement() throws Exception {
        String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <build>
                        <pluginManagement>
                            <plugins>
                                <plugin>
                                    <groupId>org.apache.maven.plugins</groupId>
                                    <artifactId>maven-shade-plugin</artifactId>
                                    <version>1.3.3</version>
                                    <configuration>
                                        <transformers>
                                            <transformer implementation="com.example.CustomTransformer"/>
                                        </transformers>
                                    </configuration>
                                </plugin>
                            </plugins>
                        </pluginManagement>
                    </build>
                </project>
                """;

        Document document = Document.of(pomXml);
        Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

        UpgradeContext context = createMockContext();
        strategy.doApply(context, pomMap);

        // Verify the version was NOT upgraded
        Editor editor = new Editor(document);
        String version = editor.root()
                .path("build", "pluginManagement", "plugins", "plugin", "version")
                .map(Element::textContentTrimmed)
                .orElse(null);
        assertEquals("1.3.3", version, "Shade plugin version in pluginManagement should remain unchanged");
    }

    @Test
    @DisplayName("findCustomTransformerClasses should return custom class names")
    void findCustomTransformerClassesShouldReturnCustomClassNames() throws Exception {
        String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <build>
                        <plugins>
                            <plugin>
                                <groupId>org.apache.maven.plugins</groupId>
                                <artifactId>maven-shade-plugin</artifactId>
                                <version>1.3.3</version>
                                <executions>
                                    <execution>
                                        <configuration>
                                            <transformers>
                                                <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer"/>
                                                <transformer implementation="org.apache.myfaces.extensions.cdi.maven.BeansXmlTransformer"/>
                                                <transformer implementation="com.example.AnotherTransformer"/>
                                            </transformers>
                                        </configuration>
                                    </execution>
                                </executions>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """;

        Document document = Document.of(pomXml);
        Element pluginElement =
                document.root().path("build", "plugins", "plugin").orElseThrow();

        List<String> customTransformers = strategy.findCustomTransformerClasses(pluginElement);

        assertEquals(2, customTransformers.size(), "Should find exactly 2 custom transformers");
        assertTrue(
                customTransformers.contains("org.apache.myfaces.extensions.cdi.maven.BeansXmlTransformer"),
                "Should contain BeansXmlTransformer");
        assertTrue(customTransformers.contains("com.example.AnotherTransformer"), "Should contain AnotherTransformer");
    }

    @Test
    @DisplayName("findCustomTransformerClasses should return empty for standard transformers only")
    void findCustomTransformerClassesShouldReturnEmptyForStandardTransformers() throws Exception {
        String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <build>
                        <plugins>
                            <plugin>
                                <groupId>org.apache.maven.plugins</groupId>
                                <artifactId>maven-shade-plugin</artifactId>
                                <version>3.0.0</version>
                                <configuration>
                                    <transformers>
                                        <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer"/>
                                        <transformer implementation="org.apache.maven.plugins.shade.resource.ServicesResourceTransformer"/>
                                        <transformer implementation="org.apache.maven.plugins.shade.resource.ApacheLicenseResourceTransformer"/>
                                    </transformers>
                                </configuration>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """;

        Document document = Document.of(pomXml);
        Element pluginElement =
                document.root().path("build", "plugins", "plugin").orElseThrow();

        List<String> customTransformers = strategy.findCustomTransformerClasses(pluginElement);

        assertTrue(customTransformers.isEmpty(), "Should not find any custom transformers");
    }

    @Test
    @DisplayName("findCustomTransformerClasses should return empty when no transformers configured")
    void findCustomTransformerClassesShouldReturnEmptyWhenNoTransformers() throws Exception {
        String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <build>
                        <plugins>
                            <plugin>
                                <groupId>org.apache.maven.plugins</groupId>
                                <artifactId>maven-shade-plugin</artifactId>
                                <version>3.0.0</version>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """;

        Document document = Document.of(pomXml);
        Element pluginElement =
                document.root().path("build", "plugins", "plugin").orElseThrow();

        List<String> customTransformers = strategy.findCustomTransformerClasses(pluginElement);

        assertTrue(customTransformers.isEmpty(), "Should not find any custom transformers");
    }
}
