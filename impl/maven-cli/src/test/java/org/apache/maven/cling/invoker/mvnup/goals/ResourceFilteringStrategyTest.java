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
import java.util.Map;
import java.util.Set;

import eu.maveniverse.domtrip.Document;
import eu.maveniverse.domtrip.Element;
import org.apache.maven.cling.invoker.mvnup.UpgradeContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link ResourceFilteringStrategy}.
 */
@DisplayName("ResourceFilteringStrategy")
class ResourceFilteringStrategyTest {

    private ResourceFilteringStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new ResourceFilteringStrategy();
    }

    private UpgradeContext createMockContext() {
        return TestUtils.createMockContext();
    }

    @Nested
    @DisplayName("Filtering Detection")
    class FilteringDetectionTests {

        @Test
        @DisplayName("should detect filtering enabled in resources")
        void shouldDetectFilteringInResources() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <build>
                        <resources>
                            <resource>
                                <directory>src/main/resources</directory>
                                <filtering>true</filtering>
                            </resource>
                        </resources>
                    </build>
                </project>
                """;

            Document document = Document.of(pomXml);
            Element build = document.root().childElement("build").orElse(null);
            assertNotNull(build);
            assertTrue(strategy.hasFilteringEnabled(build, "resources", "resource"));
        }

        @Test
        @DisplayName("should detect filtering enabled in testResources")
        void shouldDetectFilteringInTestResources() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <build>
                        <testResources>
                            <testResource>
                                <directory>src/test/resources</directory>
                                <filtering>true</filtering>
                            </testResource>
                        </testResources>
                    </build>
                </project>
                """;

            Document document = Document.of(pomXml);
            Element build = document.root().childElement("build").orElse(null);
            assertNotNull(build);
            assertTrue(strategy.hasFilteringEnabled(build, "testResources", "testResource"));
        }

        @Test
        @DisplayName("should not detect filtering when set to false")
        void shouldNotDetectFilteringWhenFalse() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <build>
                        <resources>
                            <resource>
                                <directory>src/main/resources</directory>
                                <filtering>false</filtering>
                            </resource>
                        </resources>
                    </build>
                </project>
                """;

            Document document = Document.of(pomXml);
            Element build = document.root().childElement("build").orElse(null);
            assertNotNull(build);
            assertFalse(strategy.hasFilteringEnabled(build, "resources", "resource"));
        }

        @Test
        @DisplayName("should not detect filtering when no filtering element present")
        void shouldNotDetectFilteringWhenAbsent() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <build>
                        <resources>
                            <resource>
                                <directory>src/main/resources</directory>
                            </resource>
                        </resources>
                    </build>
                </project>
                """;

            Document document = Document.of(pomXml);
            Element build = document.root().childElement("build").orElse(null);
            assertNotNull(build);
            assertFalse(strategy.hasFilteringEnabled(build, "resources", "resource"));
        }
    }

    @Nested
    @DisplayName("Existing Extensions Detection")
    class ExistingExtensionsTests {

        @Test
        @DisplayName("should find existing nonFilteredFileExtensions in pluginManagement")
        void shouldFindExistingExtensionsInPluginManagement() {
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
                                    <artifactId>maven-resources-plugin</artifactId>
                                    <configuration>
                                        <nonFilteredFileExtensions>
                                            <nonFilteredFileExtension>pdf</nonFilteredFileExtension>
                                            <nonFilteredFileExtension>zip</nonFilteredFileExtension>
                                        </nonFilteredFileExtensions>
                                    </configuration>
                                </plugin>
                            </plugins>
                        </pluginManagement>
                    </build>
                </project>
                """;

            Document document = Document.of(pomXml);
            Set<String> extensions = strategy.findExistingNonFilteredExtensions(document.root());
            assertNotNull(extensions);
            assertEquals(2, extensions.size());
            assertTrue(extensions.contains("pdf"));
            assertTrue(extensions.contains("zip"));
        }

        @Test
        @DisplayName("should return null when no maven-resources-plugin configured")
        void shouldReturnNullWhenNoResourcesPlugin() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <build>
                        <resources>
                            <resource>
                                <directory>src/main/resources</directory>
                                <filtering>true</filtering>
                            </resource>
                        </resources>
                    </build>
                </project>
                """;

            Document document = Document.of(pomXml);
            assertNull(strategy.findExistingNonFilteredExtensions(document.root()));
        }
    }

    @Nested
    @DisplayName("Full Strategy Application")
    class StrategyApplicationTests {

        @Test
        @DisplayName("should add nonFilteredFileExtensions when filtering is enabled and no config exists")
        void shouldAddExtensionsWhenFilteringEnabled() throws Exception {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <build>
                        <resources>
                            <resource>
                                <directory>src/main/resources</directory>
                                <filtering>true</filtering>
                            </resource>
                        </resources>
                    </build>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            UpgradeResult result = strategy.doApply(context, pomMap);

            assertTrue(result.success(), "Strategy should succeed");
            assertEquals(1, result.modifiedCount(), "Should have modified 1 POM");

            String xml = DomUtils.toXml(document);
            assertTrue(xml.contains("<nonFilteredFileExtensions>"), "Should have added nonFilteredFileExtensions");
            assertTrue(xml.contains("<nonFilteredFileExtension>xlsx</nonFilteredFileExtension>"));
            assertTrue(xml.contains("<nonFilteredFileExtension>docx</nonFilteredFileExtension>"));
            assertTrue(xml.contains("<nonFilteredFileExtension>pdf</nonFilteredFileExtension>"));
            assertTrue(xml.contains("<nonFilteredFileExtension>jar</nonFilteredFileExtension>"));
            assertTrue(xml.contains("<nonFilteredFileExtension>keystore</nonFilteredFileExtension>"));
            assertTrue(xml.contains("<nonFilteredFileExtension>jks</nonFilteredFileExtension>"));
            // Should be in pluginManagement
            assertTrue(xml.contains("<pluginManagement>"), "Should have created pluginManagement");
            assertTrue(xml.contains("maven-resources-plugin"), "Should reference maven-resources-plugin");
        }

        @Test
        @DisplayName("should not modify POM when filtering is not enabled")
        void shouldNotModifyWhenNoFiltering() throws Exception {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <build>
                        <resources>
                            <resource>
                                <directory>src/main/resources</directory>
                            </resource>
                        </resources>
                    </build>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            UpgradeResult result = strategy.doApply(context, pomMap);

            assertTrue(result.success(), "Strategy should succeed");
            assertEquals(0, result.modifiedCount(), "Should not have modified any POM");
        }

        @Test
        @DisplayName("should not modify POM when nonFilteredFileExtensions already covers all binary types")
        void shouldNotModifyWhenExtensionsAlreadyComplete() throws Exception {
            StringBuilder extensionsXml = new StringBuilder();
            for (String ext : ResourceFilteringStrategy.BINARY_EXTENSIONS) {
                extensionsXml
                        .append("                                <nonFilteredFileExtension>")
                        .append(ext)
                        .append("</nonFilteredFileExtension>\n");
            }

            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <build>
                        <resources>
                            <resource>
                                <directory>src/main/resources</directory>
                                <filtering>true</filtering>
                            </resource>
                        </resources>
                        <pluginManagement>
                            <plugins>
                                <plugin>
                                    <artifactId>maven-resources-plugin</artifactId>
                                    <configuration>
                                        <nonFilteredFileExtensions>
                """ + extensionsXml + """
                                        </nonFilteredFileExtensions>
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
            UpgradeResult result = strategy.doApply(context, pomMap);

            assertTrue(result.success(), "Strategy should succeed");
            assertEquals(0, result.modifiedCount(), "Should not have modified any POM");
        }

        @Test
        @DisplayName("should merge missing extensions into existing configuration")
        void shouldMergeMissingExtensions() throws Exception {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <build>
                        <resources>
                            <resource>
                                <directory>src/main/resources</directory>
                                <filtering>true</filtering>
                            </resource>
                        </resources>
                        <pluginManagement>
                            <plugins>
                                <plugin>
                                    <artifactId>maven-resources-plugin</artifactId>
                                    <configuration>
                                        <nonFilteredFileExtensions>
                                            <nonFilteredFileExtension>pdf</nonFilteredFileExtension>
                                        </nonFilteredFileExtensions>
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
            UpgradeResult result = strategy.doApply(context, pomMap);

            assertTrue(result.success(), "Strategy should succeed");
            assertEquals(1, result.modifiedCount(), "Should have modified 1 POM");

            String xml = DomUtils.toXml(document);
            // Original extension still present
            assertTrue(xml.contains("<nonFilteredFileExtension>pdf</nonFilteredFileExtension>"));
            // New extensions added
            assertTrue(xml.contains("<nonFilteredFileExtension>xlsx</nonFilteredFileExtension>"));
            assertTrue(xml.contains("<nonFilteredFileExtension>docx</nonFilteredFileExtension>"));
            assertTrue(xml.contains("<nonFilteredFileExtension>jar</nonFilteredFileExtension>"));
        }

        @Test
        @DisplayName("should handle testResources with filtering enabled")
        void shouldHandleTestResourcesFiltering() throws Exception {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <build>
                        <testResources>
                            <testResource>
                                <directory>src/test/resources</directory>
                                <filtering>true</filtering>
                            </testResource>
                        </testResources>
                    </build>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            UpgradeResult result = strategy.doApply(context, pomMap);

            assertTrue(result.success(), "Strategy should succeed");
            assertEquals(1, result.modifiedCount(), "Should have modified 1 POM");

            String xml = DomUtils.toXml(document);
            assertTrue(xml.contains("<nonFilteredFileExtensions>"));
        }

        @Test
        @DisplayName("should handle filtering in profile resources")
        void shouldHandleProfileResourcesFiltering() throws Exception {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <profiles>
                        <profile>
                            <id>production</id>
                            <build>
                                <resources>
                                    <resource>
                                        <directory>src/main/resources</directory>
                                        <filtering>true</filtering>
                                    </resource>
                                </resources>
                            </build>
                        </profile>
                    </profiles>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            UpgradeResult result = strategy.doApply(context, pomMap);

            assertTrue(result.success(), "Strategy should succeed");
            assertEquals(1, result.modifiedCount(), "Should have modified 1 POM");

            String xml = DomUtils.toXml(document);
            assertTrue(xml.contains("<nonFilteredFileExtensions>"));
        }

        @Test
        @DisplayName("should add to existing pluginManagement without duplicating plugin entry")
        void shouldAddToExistingPluginManagement() throws Exception {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <build>
                        <resources>
                            <resource>
                                <directory>src/main/resources</directory>
                                <filtering>true</filtering>
                            </resource>
                        </resources>
                        <pluginManagement>
                            <plugins>
                                <plugin>
                                    <artifactId>maven-compiler-plugin</artifactId>
                                    <version>3.11.0</version>
                                </plugin>
                            </plugins>
                        </pluginManagement>
                    </build>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            UpgradeResult result = strategy.doApply(context, pomMap);

            assertTrue(result.success(), "Strategy should succeed");
            assertEquals(1, result.modifiedCount(), "Should have modified 1 POM");

            String xml = DomUtils.toXml(document);
            assertTrue(xml.contains("maven-resources-plugin"), "Should have added maven-resources-plugin");
            assertTrue(xml.contains("maven-compiler-plugin"), "Should still have maven-compiler-plugin");
            assertTrue(xml.contains("<nonFilteredFileExtensions>"));
        }

        @Test
        @DisplayName("should not modify POM with no build section")
        void shouldNotModifyWithNoBuildSection() throws Exception {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            UpgradeResult result = strategy.doApply(context, pomMap);

            assertTrue(result.success(), "Strategy should succeed");
            assertEquals(0, result.modifiedCount(), "Should not have modified any POM");
        }

        @Test
        @DisplayName("should add config to existing maven-resources-plugin in pluginManagement")
        void shouldAddConfigToExistingResourcesPlugin() throws Exception {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <build>
                        <resources>
                            <resource>
                                <directory>src/main/resources</directory>
                                <filtering>true</filtering>
                            </resource>
                        </resources>
                        <pluginManagement>
                            <plugins>
                                <plugin>
                                    <artifactId>maven-resources-plugin</artifactId>
                                    <version>3.3.1</version>
                                </plugin>
                            </plugins>
                        </pluginManagement>
                    </build>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            UpgradeResult result = strategy.doApply(context, pomMap);

            assertTrue(result.success(), "Strategy should succeed");
            assertEquals(1, result.modifiedCount(), "Should have modified 1 POM");

            String xml = DomUtils.toXml(document);
            assertTrue(xml.contains("<version>3.3.1</version>"), "Should preserve existing version");
            assertTrue(xml.contains("<nonFilteredFileExtensions>"));
            assertTrue(xml.contains("<nonFilteredFileExtension>xlsx</nonFilteredFileExtension>"));
        }

        @Test
        @DisplayName("should include all binary extensions")
        void shouldIncludeAllBinaryExtensions() throws Exception {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <build>
                        <resources>
                            <resource>
                                <directory>src/main/resources</directory>
                                <filtering>true</filtering>
                            </resource>
                        </resources>
                    </build>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            strategy.doApply(context, pomMap);

            String xml = DomUtils.toXml(document);
            for (String ext : ResourceFilteringStrategy.BINARY_EXTENSIONS) {
                assertTrue(
                        xml.contains("<nonFilteredFileExtension>" + ext + "</nonFilteredFileExtension>"),
                        "Should include extension: " + ext);
            }

            assertEquals(33, ResourceFilteringStrategy.BINARY_EXTENSIONS.size(), "Should have exactly 33 extensions");
        }
    }

    @Nested
    @DisplayName("Applicability")
    class ApplicabilityTests {

        @Test
        @DisplayName("should be applicable with default options")
        void shouldBeApplicableWithDefaults() {
            UpgradeContext context = createMockContext();
            assertTrue(strategy.isApplicable(context));
        }

        @Test
        @DisplayName("should be applicable when --all is set")
        void shouldBeApplicableWithAll() {
            UpgradeContext context = TestUtils.createMockContext(TestUtils.createOptionsWithAll(true));
            assertTrue(strategy.isApplicable(context));
        }

        @Test
        @DisplayName("should be applicable when --model is true")
        void shouldBeApplicableWithModelTrue() {
            UpgradeContext context = TestUtils.createMockContext(TestUtils.createOptionsWithFixModel(true));
            assertTrue(strategy.isApplicable(context));
        }

        @Test
        @DisplayName("should not be applicable when --model is false")
        void shouldNotBeApplicableWithModelFalse() {
            UpgradeContext context = TestUtils.createMockContext(TestUtils.createOptionsWithFixModel(false));
            assertFalse(strategy.isApplicable(context));
        }
    }
}
