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

import eu.maveniverse.domtrip.Document;
import eu.maveniverse.domtrip.Editor;
import eu.maveniverse.domtrip.Element;
import org.apache.maven.cling.invoker.mvnup.UpgradeContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for JDK compatibility checking in {@link PluginUpgradeStrategy}.
 * Verifies that plugin upgrades requiring a higher JDK than the project targets
 * are skipped to avoid {@code UnsupportedClassVersionError}.
 *
 * @see <a href="https://github.com/apache/maven/issues/12989">apache/maven#12989</a>
 */
@DisplayName("PluginUpgradeStrategy — JDK Compatibility")
class PluginUpgradeJdkCompatibilityTest {

    private PluginUpgradeStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new PluginUpgradeStrategy();
    }

    private UpgradeContext createMockContext() {
        return TestUtils.createMockContext();
    }

    @Nested
    @DisplayName("Project JDK Detection")
    class ProjectJdkDetectionTests {

        @Test
        @DisplayName("should detect JDK version from maven.compiler.release property")
        void shouldDetectFromCompilerRelease() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <properties>
                        <maven.compiler.release>17</maven.compiler.release>
                    </properties>
                </project>
                """;

            Document document = Document.of(pomXml);
            assertEquals(17, strategy.detectProjectJdkVersion(document));
        }

        @Test
        @DisplayName("should detect JDK version from maven.compiler.source property")
        void shouldDetectFromCompilerSource() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <properties>
                        <maven.compiler.source>11</maven.compiler.source>
                    </properties>
                </project>
                """;

            Document document = Document.of(pomXml);
            assertEquals(11, strategy.detectProjectJdkVersion(document));
        }

        @Test
        @DisplayName("should detect JDK version from maven.compiler.target property")
        void shouldDetectFromCompilerTarget() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <properties>
                        <maven.compiler.target>17</maven.compiler.target>
                    </properties>
                </project>
                """;

            Document document = Document.of(pomXml);
            assertEquals(17, strategy.detectProjectJdkVersion(document));
        }

        @Test
        @DisplayName("should prefer maven.compiler.release over source/target")
        void shouldPreferReleaseOverSource() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <properties>
                        <maven.compiler.release>21</maven.compiler.release>
                        <maven.compiler.source>17</maven.compiler.source>
                        <maven.compiler.target>17</maven.compiler.target>
                    </properties>
                </project>
                """;

            Document document = Document.of(pomXml);
            assertEquals(21, strategy.detectProjectJdkVersion(document));
        }

        @Test
        @DisplayName("should normalize old-style version like 1.8 to 8")
        void shouldNormalizeOldStyleVersion() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <properties>
                        <maven.compiler.source>1.8</maven.compiler.source>
                    </properties>
                </project>
                """;

            Document document = Document.of(pomXml);
            assertEquals(8, strategy.detectProjectJdkVersion(document));
        }

        @Test
        @DisplayName("should detect JDK version from compiler plugin configuration")
        void shouldDetectFromCompilerPluginConfig() {
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
                                <artifactId>maven-compiler-plugin</artifactId>
                                <configuration>
                                    <release>17</release>
                                </configuration>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """;

            Document document = Document.of(pomXml);
            assertEquals(17, strategy.detectProjectJdkVersion(document));
        }

        @Test
        @DisplayName("should return -1 when no JDK version is configured")
        void shouldReturnNegativeOneWhenNoJdkConfigured() {
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
            assertEquals(-1, strategy.detectProjectJdkVersion(document));
        }

        @Test
        @DisplayName("should skip property references (${...}) and return -1")
        void shouldSkipPropertyReferences() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <properties>
                        <maven.compiler.release>${java.version}</maven.compiler.release>
                    </properties>
                </project>
                """;

            Document document = Document.of(pomXml);
            assertEquals(-1, strategy.detectProjectJdkVersion(document));
        }
    }

    @Nested
    @DisplayName("JDK Compatibility Skip")
    class JdkCompatibilitySkipTests {

        @Test
        @DisplayName("should skip checkstyle-plugin upgrade when project targets JDK 17")
        void shouldSkipCheckstyleUpgradeForJdk17() throws Exception {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <properties>
                        <maven.compiler.release>17</maven.compiler.release>
                    </properties>
                    <build>
                        <plugins>
                            <plugin>
                                <groupId>org.apache.maven.plugins</groupId>
                                <artifactId>maven-checkstyle-plugin</artifactId>
                                <version>3.4.0</version>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            UpgradeResult result = strategy.doApply(context, pomMap);

            assertTrue(result.success(), "Strategy should succeed");

            String xml = DomUtils.toXml(document);
            assertTrue(
                    xml.contains("<version>3.4.0</version>"),
                    "checkstyle-plugin should NOT be upgraded when project targets JDK 17");
            assertFalse(xml.contains("<version>3.6.0</version>"), "checkstyle-plugin should NOT be set to 3.6.0");
        }

        @Test
        @DisplayName("should skip checkstyle-plugin upgrade when project targets JDK 11")
        void shouldSkipCheckstyleUpgradeForJdk11() throws Exception {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <properties>
                        <maven.compiler.source>11</maven.compiler.source>
                        <maven.compiler.target>11</maven.compiler.target>
                    </properties>
                    <build>
                        <plugins>
                            <plugin>
                                <groupId>org.apache.maven.plugins</groupId>
                                <artifactId>maven-checkstyle-plugin</artifactId>
                                <version>3.3.0</version>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            UpgradeResult result = strategy.doApply(context, pomMap);

            assertTrue(result.success(), "Strategy should succeed");

            String xml = DomUtils.toXml(document);
            assertTrue(
                    xml.contains("<version>3.3.0</version>"),
                    "checkstyle-plugin should NOT be upgraded when project targets JDK 11");
        }

        @Test
        @DisplayName("should upgrade checkstyle-plugin when project targets JDK 21")
        void shouldUpgradeCheckstyleForJdk21() throws Exception {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <properties>
                        <maven.compiler.release>21</maven.compiler.release>
                    </properties>
                    <build>
                        <plugins>
                            <plugin>
                                <groupId>org.apache.maven.plugins</groupId>
                                <artifactId>maven-checkstyle-plugin</artifactId>
                                <version>3.4.0</version>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            UpgradeResult result = strategy.doApply(context, pomMap);

            assertTrue(result.success(), "Strategy should succeed");

            Editor editor = new Editor(document);
            String version = editor.root()
                    .path("build", "plugins", "plugin", "version")
                    .map(Element::textContentTrimmed)
                    .orElse(null);
            assertEquals("3.6.0", version, "checkstyle-plugin should be upgraded to 3.6.0 for JDK 21");
        }

        @Test
        @DisplayName("should upgrade checkstyle-plugin when project targets JDK 23")
        void shouldUpgradeCheckstyleForJdk23() throws Exception {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <properties>
                        <maven.compiler.release>23</maven.compiler.release>
                    </properties>
                    <build>
                        <plugins>
                            <plugin>
                                <groupId>org.apache.maven.plugins</groupId>
                                <artifactId>maven-checkstyle-plugin</artifactId>
                                <version>3.4.0</version>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            UpgradeResult result = strategy.doApply(context, pomMap);

            assertTrue(result.success(), "Strategy should succeed");

            Editor editor = new Editor(document);
            String version = editor.root()
                    .path("build", "plugins", "plugin", "version")
                    .map(Element::textContentTrimmed)
                    .orElse(null);
            assertEquals("3.6.0", version, "checkstyle-plugin should be upgraded to 3.6.0 for JDK 23");
        }

        @Test
        @DisplayName("should upgrade checkstyle-plugin when no JDK version is configured")
        void shouldUpgradeCheckstyleWhenNoJdkConfigured() throws Exception {
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
                                <artifactId>maven-checkstyle-plugin</artifactId>
                                <version>3.4.0</version>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            UpgradeResult result = strategy.doApply(context, pomMap);

            assertTrue(result.success(), "Strategy should succeed");

            Editor editor = new Editor(document);
            String version = editor.root()
                    .path("build", "plugins", "plugin", "version")
                    .map(Element::textContentTrimmed)
                    .orElse(null);
            assertEquals("3.6.0", version, "checkstyle-plugin should be upgraded when no JDK version is configured");
        }

        @Test
        @DisplayName("should skip checkstyle-plugin upgrade via property version for JDK 17")
        void shouldSkipCheckstylePropertyUpgradeForJdk17() throws Exception {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <properties>
                        <maven.compiler.release>17</maven.compiler.release>
                        <checkstyle-plugin.version>3.4.0</checkstyle-plugin.version>
                    </properties>
                    <build>
                        <plugins>
                            <plugin>
                                <groupId>org.apache.maven.plugins</groupId>
                                <artifactId>maven-checkstyle-plugin</artifactId>
                                <version>${checkstyle-plugin.version}</version>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            UpgradeResult result = strategy.doApply(context, pomMap);

            assertTrue(result.success(), "Strategy should succeed");

            String xml = DomUtils.toXml(document);
            assertTrue(
                    xml.contains("<checkstyle-plugin.version>3.4.0</checkstyle-plugin.version>"),
                    "checkstyle-plugin property should NOT be upgraded for JDK 17");
        }

        @Test
        @DisplayName("should not skip plugins without minJdk requirement")
        void shouldNotSkipPluginsWithoutMinJdk() throws Exception {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <properties>
                        <maven.compiler.release>11</maven.compiler.release>
                    </properties>
                    <build>
                        <plugins>
                            <plugin>
                                <groupId>org.apache.maven.plugins</groupId>
                                <artifactId>maven-surefire-plugin</artifactId>
                                <version>3.0.0</version>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            UpgradeResult result = strategy.doApply(context, pomMap);

            assertTrue(result.success(), "Strategy should succeed");

            Editor editor = new Editor(document);
            String version = editor.root()
                    .path("build", "plugins", "plugin", "version")
                    .map(Element::textContentTrimmed)
                    .orElse(null);
            assertEquals("3.5.2", version, "surefire-plugin should still be upgraded (no minJdk requirement)");
        }

        @Test
        @DisplayName("should skip checkstyle-plugin in pluginManagement for JDK 17")
        void shouldSkipCheckstyleInPluginManagementForJdk17() throws Exception {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <properties>
                        <maven.compiler.release>17</maven.compiler.release>
                    </properties>
                    <build>
                        <pluginManagement>
                            <plugins>
                                <plugin>
                                    <groupId>org.apache.maven.plugins</groupId>
                                    <artifactId>maven-checkstyle-plugin</artifactId>
                                    <version>3.4.0</version>
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

            String xml = DomUtils.toXml(document);
            assertTrue(
                    xml.contains("<version>3.4.0</version>"),
                    "checkstyle-plugin in pluginManagement should NOT be upgraded for JDK 17");
        }
    }

    @Nested
    @DisplayName("PluginUpgrade Record")
    class PluginUpgradeRecordTests {

        @Test
        @DisplayName("should have minJdk=0 for convenience constructor without JDK")
        void shouldHaveDefaultMinJdk() {
            PluginUpgrade upgrade = new PluginUpgrade("g", "a", "1.0", "reason");
            assertEquals(0, upgrade.minJdk(), "Default minJdk should be 0");
        }

        @Test
        @DisplayName("should have minJdk=0 for five-arg convenience constructor")
        void shouldHaveDefaultMinJdkForFiveArgConstructor() {
            PluginUpgrade upgrade = new PluginUpgrade("g", "a", "1.0", "2.0-beta-1", "reason");
            assertEquals(0, upgrade.minJdk(), "Default minJdk should be 0 for five-arg constructor");
        }

        @Test
        @DisplayName("should preserve minJdk value in full constructor")
        void shouldPreserveMinJdk() {
            PluginUpgrade upgrade = new PluginUpgrade("g", "a", "1.0", null, "reason", 21);
            assertEquals(21, upgrade.minJdk(), "minJdk should be preserved");
        }

        @Test
        @DisplayName("checkstyle plugin upgrade should have minJdk=21")
        void checkstylePluginShouldHaveMinJdk21() {
            PluginUpgrade checkstyleUpgrade = PluginUpgradeStrategy.getPluginUpgrades().stream()
                    .filter(u -> "maven-checkstyle-plugin".equals(u.artifactId()))
                    .findFirst()
                    .orElse(null);

            assertTrue(checkstyleUpgrade != null, "checkstyle-plugin should be in PLUGIN_UPGRADES");
            assertEquals(21, checkstyleUpgrade.minJdk(), "checkstyle-plugin should require JDK 21");
            assertEquals("3.6.0", checkstyleUpgrade.minVersion(), "checkstyle-plugin minVersion should be 3.6.0");
        }
    }
}
