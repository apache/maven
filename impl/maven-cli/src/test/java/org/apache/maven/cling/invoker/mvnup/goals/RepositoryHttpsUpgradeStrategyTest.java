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
import java.util.Optional;

import eu.maveniverse.domtrip.Document;
import org.apache.maven.api.cli.mvnup.UpgradeOptions;
import org.apache.maven.cling.invoker.mvnup.UpgradeContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the {@link RepositoryHttpsUpgradeStrategy} class.
 * Tests HTTP to HTTPS repository URL upgrades for all POM sections.
 */
@DisplayName("RepositoryHttpsUpgradeStrategy")
class RepositoryHttpsUpgradeStrategyTest {

    private RepositoryHttpsUpgradeStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new RepositoryHttpsUpgradeStrategy();
    }

    private UpgradeContext createMockContext() {
        return TestUtils.createMockContext();
    }

    private UpgradeContext createMockContext(UpgradeOptions options) {
        return TestUtils.createMockContext(options);
    }

    @Nested
    @DisplayName("Applicability")
    class ApplicabilityTests {

        @Test
        @DisplayName("should be applicable when --model option is true")
        void shouldBeApplicableWhenModelOptionTrue() {
            UpgradeOptions options = mock(UpgradeOptions.class);
            when(options.model()).thenReturn(Optional.of(true));
            when(options.all()).thenReturn(Optional.empty());
            when(options.infer()).thenReturn(Optional.empty());
            when(options.plugins()).thenReturn(Optional.empty());
            when(options.modelVersion()).thenReturn(Optional.empty());

            UpgradeContext context = createMockContext(options);

            assertTrue(strategy.isApplicable(context), "Strategy should be applicable when --model is true");
        }

        @Test
        @DisplayName("should be applicable when --all option is specified")
        void shouldBeApplicableWhenAllOptionSpecified() {
            UpgradeOptions options = mock(UpgradeOptions.class);
            when(options.all()).thenReturn(Optional.of(true));
            when(options.model()).thenReturn(Optional.empty());
            when(options.infer()).thenReturn(Optional.empty());
            when(options.plugins()).thenReturn(Optional.empty());
            when(options.modelVersion()).thenReturn(Optional.empty());

            UpgradeContext context = createMockContext(options);

            assertTrue(strategy.isApplicable(context), "Strategy should be applicable when --all is specified");
        }

        @Test
        @DisplayName("should be applicable by default when no specific options provided")
        void shouldBeApplicableByDefaultWhenNoSpecificOptions() {
            UpgradeOptions options = TestUtils.createDefaultOptions();

            UpgradeContext context = createMockContext(options);

            assertTrue(strategy.isApplicable(context), "Strategy should be applicable by default");
        }

        @Test
        @DisplayName("should not be applicable when --model option is false and other options set")
        void shouldNotBeApplicableWhenModelOptionFalseAndOtherOptionsSet() {
            UpgradeOptions options = mock(UpgradeOptions.class);
            when(options.model()).thenReturn(Optional.of(false));
            when(options.all()).thenReturn(Optional.empty());
            when(options.infer()).thenReturn(Optional.empty());
            when(options.plugins()).thenReturn(Optional.of(true));
            when(options.modelVersion()).thenReturn(Optional.empty());

            UpgradeContext context = createMockContext(options);

            assertFalse(strategy.isApplicable(context), "Strategy should not be applicable when --model is false");
        }
    }

    @Nested
    @DisplayName("Well-Known URL Mappings")
    class WellKnownUrlMappingTests {

        @Test
        @DisplayName("should map Apache snapshots repository URL")
        void shouldMapApacheSnapshotsUrl() {
            assertEquals(
                    "https://repository.apache.org/snapshots",
                    RepositoryHttpsUpgradeStrategy.mapWellKnownUrl("http://repository.apache.org/snapshots"));
        }

        @Test
        @DisplayName("should map Apache releases repository URL")
        void shouldMapApacheReleasesUrl() {
            assertEquals(
                    "https://repository.apache.org/releases",
                    RepositoryHttpsUpgradeStrategy.mapWellKnownUrl("http://repository.apache.org/releases"));
        }

        @Test
        @DisplayName("should map repo1.maven.org to canonical HTTPS URL")
        void shouldMapRepo1MavenOrg() {
            assertEquals(
                    "https://repo.maven.apache.org/maven2",
                    RepositoryHttpsUpgradeStrategy.mapWellKnownUrl("http://repo1.maven.org/maven2"));
        }

        @Test
        @DisplayName("should map repo.maven.apache.org to canonical HTTPS URL")
        void shouldMapRepoMavenApacheOrg() {
            assertEquals(
                    "https://repo.maven.apache.org/maven2",
                    RepositoryHttpsUpgradeStrategy.mapWellKnownUrl("http://repo.maven.apache.org/maven2"));
        }

        @Test
        @DisplayName("should map central.maven.org to canonical HTTPS URL")
        void shouldMapCentralMavenOrg() {
            assertEquals(
                    "https://repo.maven.apache.org/maven2",
                    RepositoryHttpsUpgradeStrategy.mapWellKnownUrl("http://central.maven.org/maven2"));
        }

        @Test
        @DisplayName("should preserve trailing slash for well-known URLs")
        void shouldPreserveTrailingSlash() {
            assertEquals(
                    "https://repo.maven.apache.org/maven2/",
                    RepositoryHttpsUpgradeStrategy.mapWellKnownUrl("http://repo1.maven.org/maven2/"));
        }

        @Test
        @DisplayName("should return null for unknown URLs")
        void shouldReturnNullForUnknownUrls() {
            assertNull(RepositoryHttpsUpgradeStrategy.mapWellKnownUrl("http://example.com/repo"));
        }
    }

    @Nested
    @DisplayName("Repository URL Upgrades")
    class RepositoryUrlUpgradeTests {

        @Test
        @DisplayName("should upgrade HTTP repository URL to HTTPS")
        void shouldUpgradeHttpRepositoryUrl() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <repositories>
                        <repository>
                            <id>my-repo</id>
                            <url>http://example.com/maven2</url>
                        </repository>
                    </repositories>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            UpgradeResult result = strategy.doApply(context, pomMap);

            assertTrue(result.success(), "Upgrade should succeed");
            assertTrue(result.modifiedCount() > 0, "Should have modified POMs");

            String xml = DomUtils.toXml(document);
            assertTrue(xml.contains("https://example.com/maven2"), "URL should be upgraded to HTTPS");
            assertFalse(
                    xml.contains("http://example.com/maven2"),
                    "Original HTTP URL should be replaced (excluding xmlns)");
        }

        @Test
        @DisplayName("should upgrade well-known Apache snapshots URL")
        void shouldUpgradeWellKnownApacheSnapshotsUrl() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <repositories>
                        <repository>
                            <id>apache-snapshots</id>
                            <url>http://repository.apache.org/snapshots</url>
                        </repository>
                    </repositories>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            UpgradeResult result = strategy.doApply(context, pomMap);

            assertTrue(result.success(), "Upgrade should succeed");
            assertTrue(result.modifiedCount() > 0, "Should have modified POMs");

            String xml = DomUtils.toXml(document);
            assertTrue(
                    xml.contains("https://repository.apache.org/snapshots"),
                    "URL should be upgraded to canonical HTTPS URL");
        }

        @Test
        @DisplayName("should normalize repo1.maven.org to canonical URL")
        void shouldNormalizeRepo1MavenOrg() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <repositories>
                        <repository>
                            <id>central</id>
                            <url>http://repo1.maven.org/maven2</url>
                        </repository>
                    </repositories>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            strategy.doApply(context, pomMap);

            String xml = DomUtils.toXml(document);
            assertTrue(
                    xml.contains("https://repo.maven.apache.org/maven2"),
                    "URL should be normalized to canonical Maven Central HTTPS URL");
        }

        @Test
        @DisplayName("should not modify URLs already using HTTPS")
        void shouldNotModifyHttpsUrls() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <repositories>
                        <repository>
                            <id>central</id>
                            <url>https://repo.maven.apache.org/maven2</url>
                        </repository>
                    </repositories>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            UpgradeResult result = strategy.doApply(context, pomMap);

            assertTrue(result.success(), "Upgrade should succeed");
            assertEquals(0, result.modifiedCount(), "No POMs should be modified");

            String xml = DomUtils.toXml(document);
            assertTrue(xml.contains("https://repo.maven.apache.org/maven2"), "HTTPS URL should remain unchanged");
        }

        @Test
        @DisplayName("should handle POM with no repositories")
        void shouldHandlePomWithNoRepositories() {
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

            assertTrue(result.success(), "Upgrade should succeed");
            assertEquals(0, result.modifiedCount(), "No POMs should be modified");
        }
    }

    @Nested
    @DisplayName("Plugin Repository URL Upgrades")
    class PluginRepositoryUrlUpgradeTests {

        @Test
        @DisplayName("should upgrade HTTP pluginRepository URL to HTTPS")
        void shouldUpgradeHttpPluginRepositoryUrl() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <pluginRepositories>
                        <pluginRepository>
                            <id>my-plugin-repo</id>
                            <url>http://plugins.example.com/maven2</url>
                        </pluginRepository>
                    </pluginRepositories>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            UpgradeResult result = strategy.doApply(context, pomMap);

            assertTrue(result.success(), "Upgrade should succeed");
            assertTrue(result.modifiedCount() > 0, "Should have modified POMs");

            String xml = DomUtils.toXml(document);
            assertTrue(xml.contains("https://plugins.example.com/maven2"), "Plugin repository URL should be upgraded");
        }
    }

    @Nested
    @DisplayName("Distribution Management URL Upgrades")
    class DistributionManagementUrlUpgradeTests {

        @Test
        @DisplayName("should upgrade distributionManagement repository URL")
        void shouldUpgradeDistributionManagementRepositoryUrl() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <distributionManagement>
                        <repository>
                            <id>releases</id>
                            <url>http://repository.apache.org/releases</url>
                        </repository>
                    </distributionManagement>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            UpgradeResult result = strategy.doApply(context, pomMap);

            assertTrue(result.success(), "Upgrade should succeed");
            assertTrue(result.modifiedCount() > 0, "Should have modified POMs");

            String xml = DomUtils.toXml(document);
            assertTrue(
                    xml.contains("https://repository.apache.org/releases"),
                    "Distribution management repository URL should be upgraded");
        }

        @Test
        @DisplayName("should upgrade distributionManagement snapshotRepository URL")
        void shouldUpgradeDistributionManagementSnapshotRepositoryUrl() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <distributionManagement>
                        <snapshotRepository>
                            <id>snapshots</id>
                            <url>http://repository.apache.org/snapshots</url>
                        </snapshotRepository>
                    </distributionManagement>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            UpgradeResult result = strategy.doApply(context, pomMap);

            assertTrue(result.success(), "Upgrade should succeed");
            assertTrue(result.modifiedCount() > 0, "Should have modified POMs");

            String xml = DomUtils.toXml(document);
            assertTrue(
                    xml.contains("https://repository.apache.org/snapshots"),
                    "Snapshot repository URL should be upgraded");
        }

        @Test
        @DisplayName("should upgrade both repository and snapshotRepository in distributionManagement")
        void shouldUpgradeBothRepositoryAndSnapshotRepository() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <distributionManagement>
                        <repository>
                            <id>releases</id>
                            <url>http://nexus.example.com/releases</url>
                        </repository>
                        <snapshotRepository>
                            <id>snapshots</id>
                            <url>http://nexus.example.com/snapshots</url>
                        </snapshotRepository>
                    </distributionManagement>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            UpgradeResult result = strategy.doApply(context, pomMap);

            assertTrue(result.success(), "Upgrade should succeed");
            assertTrue(result.modifiedCount() > 0, "Should have modified POMs");

            String xml = DomUtils.toXml(document);
            assertTrue(xml.contains("https://nexus.example.com/releases"), "Repository URL should be upgraded");
            assertTrue(
                    xml.contains("https://nexus.example.com/snapshots"), "Snapshot repository URL should be upgraded");
        }
    }

    @Nested
    @DisplayName("Profile-Scoped Repository URL Upgrades")
    class ProfileScopedRepositoryUrlUpgradeTests {

        @Test
        @DisplayName("should upgrade HTTP repository URLs in profiles")
        void shouldUpgradeHttpRepositoryUrlsInProfiles() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <profiles>
                        <profile>
                            <id>staging</id>
                            <repositories>
                                <repository>
                                    <id>staging-repo</id>
                                    <url>http://staging.example.com/maven2</url>
                                </repository>
                            </repositories>
                        </profile>
                    </profiles>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            UpgradeResult result = strategy.doApply(context, pomMap);

            assertTrue(result.success(), "Upgrade should succeed");
            assertTrue(result.modifiedCount() > 0, "Should have modified POMs");

            String xml = DomUtils.toXml(document);
            assertTrue(
                    xml.contains("https://staging.example.com/maven2"),
                    "Profile repository URL should be upgraded to HTTPS");
        }

        @Test
        @DisplayName("should upgrade HTTP pluginRepository URLs in profiles")
        void shouldUpgradeHttpPluginRepositoryUrlsInProfiles() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <profiles>
                        <profile>
                            <id>staging</id>
                            <pluginRepositories>
                                <pluginRepository>
                                    <id>staging-plugins</id>
                                    <url>http://staging.example.com/plugins</url>
                                </pluginRepository>
                            </pluginRepositories>
                        </profile>
                    </profiles>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            UpgradeResult result = strategy.doApply(context, pomMap);

            assertTrue(result.success(), "Upgrade should succeed");
            assertTrue(result.modifiedCount() > 0, "Should have modified POMs");

            String xml = DomUtils.toXml(document);
            assertTrue(
                    xml.contains("https://staging.example.com/plugins"),
                    "Profile plugin repository URL should be upgraded to HTTPS");
        }
    }

    @Nested
    @DisplayName("Multiple Repositories")
    class MultipleRepositoriesTests {

        @Test
        @DisplayName("should upgrade multiple HTTP repository URLs in a single POM")
        void shouldUpgradeMultipleHttpRepositoryUrls() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <repositories>
                        <repository>
                            <id>central</id>
                            <url>http://repo1.maven.org/maven2</url>
                        </repository>
                        <repository>
                            <id>custom</id>
                            <url>http://custom.example.com/repo</url>
                        </repository>
                        <repository>
                            <id>secure</id>
                            <url>https://secure.example.com/repo</url>
                        </repository>
                    </repositories>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            UpgradeResult result = strategy.doApply(context, pomMap);

            assertTrue(result.success(), "Upgrade should succeed");
            assertTrue(result.modifiedCount() > 0, "Should have modified POMs");

            String xml = DomUtils.toXml(document);
            // Well-known URL should be normalized
            assertTrue(
                    xml.contains("https://repo.maven.apache.org/maven2"),
                    "repo1.maven.org should be normalized to canonical URL");
            // Generic HTTP should be upgraded
            assertTrue(xml.contains("https://custom.example.com/repo"), "Custom HTTP URL should be upgraded to HTTPS");
            // HTTPS should remain unchanged
            assertTrue(xml.contains("https://secure.example.com/repo"), "Existing HTTPS URL should remain unchanged");
        }

        @Test
        @DisplayName("should upgrade repositories across all sections")
        void shouldUpgradeRepositoriesAcrossAllSections() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <repositories>
                        <repository>
                            <id>repo1</id>
                            <url>http://repo.example.com/releases</url>
                        </repository>
                    </repositories>
                    <pluginRepositories>
                        <pluginRepository>
                            <id>plugin-repo1</id>
                            <url>http://plugins.example.com/releases</url>
                        </pluginRepository>
                    </pluginRepositories>
                    <distributionManagement>
                        <repository>
                            <id>dist-repo</id>
                            <url>http://dist.example.com/releases</url>
                        </repository>
                    </distributionManagement>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            UpgradeResult result = strategy.doApply(context, pomMap);

            assertTrue(result.success(), "Upgrade should succeed");
            assertTrue(result.modifiedCount() > 0, "Should have modified POMs");

            String xml = DomUtils.toXml(document);
            assertTrue(xml.contains("https://repo.example.com/releases"), "Repository URL should be upgraded");
            assertTrue(
                    xml.contains("https://plugins.example.com/releases"), "Plugin repository URL should be upgraded");
            assertTrue(
                    xml.contains("https://dist.example.com/releases"),
                    "Distribution management repository URL should be upgraded");
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("should handle repository with no URL element")
        void shouldHandleRepositoryWithNoUrlElement() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <repositories>
                        <repository>
                            <id>no-url-repo</id>
                        </repository>
                    </repositories>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            UpgradeResult result = strategy.doApply(context, pomMap);

            assertTrue(result.success(), "Upgrade should succeed without errors");
            assertEquals(0, result.modifiedCount(), "No POMs should be modified");
        }

        @Test
        @DisplayName("should handle repository with no id element")
        void shouldHandleRepositoryWithNoIdElement() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <repositories>
                        <repository>
                            <url>http://example.com/maven2</url>
                        </repository>
                    </repositories>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            UpgradeResult result = strategy.doApply(context, pomMap);

            assertTrue(result.success(), "Upgrade should succeed");
            assertTrue(result.modifiedCount() > 0, "Should have modified POMs");

            String xml = DomUtils.toXml(document);
            assertTrue(xml.contains("https://example.com/maven2"), "URL should be upgraded even without id");
        }

        @Test
        @DisplayName("should normalize central.maven.org to canonical URL")
        void shouldNormalizeCentralMavenOrg() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <repositories>
                        <repository>
                            <id>central</id>
                            <url>http://central.maven.org/maven2</url>
                        </repository>
                    </repositories>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            strategy.doApply(context, pomMap);

            String xml = DomUtils.toXml(document);
            assertTrue(
                    xml.contains("https://repo.maven.apache.org/maven2"),
                    "central.maven.org should be normalized to canonical Maven Central URL");
        }
    }
}
