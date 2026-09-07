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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the {@link NashornCompatibilityStrategy} class.
 * Tests detection of antrun JavaScript usage and injection of standalone Nashorn dependency.
 *
 * @see <a href="https://github.com/apache/maven/issues/12988">#12988</a>
 */
@DisplayName("NashornCompatibilityStrategy")
class NashornCompatibilityStrategyTest {

    private NashornCompatibilityStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new NashornCompatibilityStrategy();
    }

    private UpgradeContext createMockContext() {
        return TestUtils.createMockContext();
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

            UpgradeContext context = TestUtils.createMockContext(options);

            assertTrue(strategy.isApplicable(context), "Strategy should be applicable when --model is true");
        }

        @Test
        @DisplayName("should be applicable when --all option is specified")
        void shouldBeApplicableWhenAllOptionSpecified() {
            UpgradeOptions options = mock(UpgradeOptions.class);
            when(options.all()).thenReturn(Optional.of(true));
            when(options.model()).thenReturn(Optional.empty());

            UpgradeContext context = TestUtils.createMockContext(options);

            assertTrue(strategy.isApplicable(context), "Strategy should be applicable when --all is specified");
        }

        @Test
        @DisplayName("should be applicable by default when no specific options provided")
        void shouldBeApplicableByDefaultWhenNoSpecificOptions() {
            UpgradeOptions options = TestUtils.createDefaultOptions();

            UpgradeContext context = TestUtils.createMockContext(options);

            assertTrue(strategy.isApplicable(context), "Strategy should be applicable by default");
        }

        @Test
        @DisplayName("should not be applicable when --model option is false")
        void shouldNotBeApplicableWhenModelOptionFalse() {
            UpgradeOptions options = mock(UpgradeOptions.class);
            when(options.model()).thenReturn(Optional.of(false));
            when(options.all()).thenReturn(Optional.empty());

            UpgradeContext context = TestUtils.createMockContext(options);

            assertFalse(strategy.isApplicable(context), "Strategy should not be applicable when --model is false");
        }
    }

    @Nested
    @DisplayName("Antrun JavaScript Detection and Nashorn Injection")
    class AntrunJavaScriptTests {

        @Test
        @DisplayName("should inject nashorn-core when antrun plugin uses JavaScript script")
        void shouldInjectNashornWhenAntrunUsesJavaScript() throws Exception {
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
                                <artifactId>maven-antrun-plugin</artifactId>
                                <executions>
                                    <execution>
                                        <id>set-bree</id>
                                        <phase>initialize</phase>
                                        <configuration>
                                            <target>
                                                <script language="javascript"><![CDATA[
                                                    var System = java.lang.System;
                                                    var bree = "J2SE-1.5";
                                                    project.setProperty("sling.bree", bree);
                                                ]]></script>
                                            </target>
                                        </configuration>
                                        <goals>
                                            <goal>run</goal>
                                        </goals>
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

            assertTrue(result.success(), "Strategy should succeed");
            assertTrue(result.modifiedCount() > 0, "Should have modified the POM");

            String xml = DomUtils.toXml(document);
            assertTrue(xml.contains("org.openjdk.nashorn"), "Should contain nashorn groupId");
            assertTrue(xml.contains("nashorn-core"), "Should contain nashorn artifactId");
            assertTrue(xml.contains("15.7"), "Should contain nashorn version");
        }

        @Test
        @DisplayName("should inject nashorn-core when antrun plugin has no explicit groupId")
        void shouldInjectNashornWhenAntrunHasNoGroupId() throws Exception {
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
                                <artifactId>maven-antrun-plugin</artifactId>
                                <executions>
                                    <execution>
                                        <configuration>
                                            <target>
                                                <script language="javascript"><![CDATA[
                                                    project.setProperty("foo", "bar");
                                                ]]></script>
                                            </target>
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

            assertTrue(result.success(), "Strategy should succeed");
            assertTrue(result.modifiedCount() > 0, "Should have modified the POM");

            String xml = DomUtils.toXml(document);
            assertTrue(xml.contains("nashorn-core"), "Should contain nashorn dependency");
        }

        @Test
        @DisplayName("should inject nashorn-core in pluginManagement")
        void shouldInjectNashornInPluginManagement() throws Exception {
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
                                    <artifactId>maven-antrun-plugin</artifactId>
                                    <configuration>
                                        <target>
                                            <script language="javascript"><![CDATA[
                                                project.setProperty("foo", "bar");
                                            ]]></script>
                                        </target>
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
            assertTrue(result.modifiedCount() > 0, "Should have modified the POM");

            String xml = DomUtils.toXml(document);
            assertTrue(xml.contains("nashorn-core"), "Should contain nashorn dependency");
        }

        @Test
        @DisplayName("should inject nashorn-core in profile build")
        void shouldInjectNashornInProfile() throws Exception {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <profiles>
                        <profile>
                            <id>legacy</id>
                            <build>
                                <plugins>
                                    <plugin>
                                        <groupId>org.apache.maven.plugins</groupId>
                                        <artifactId>maven-antrun-plugin</artifactId>
                                        <executions>
                                            <execution>
                                                <configuration>
                                                    <target>
                                                        <script language="javascript"><![CDATA[
                                                            var x = 1;
                                                        ]]></script>
                                                    </target>
                                                </configuration>
                                            </execution>
                                        </executions>
                                    </plugin>
                                </plugins>
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
            assertTrue(result.modifiedCount() > 0, "Should have modified the POM");

            String xml = DomUtils.toXml(document);
            assertTrue(xml.contains("nashorn-core"), "Should contain nashorn dependency in profile");
        }

        @Test
        @DisplayName("should handle case-insensitive JavaScript language attribute")
        void shouldHandleCaseInsensitiveLanguage() throws Exception {
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
                                <artifactId>maven-antrun-plugin</artifactId>
                                <configuration>
                                    <target>
                                        <script language="JavaScript"><![CDATA[
                                            project.setProperty("foo", "bar");
                                        ]]></script>
                                    </target>
                                </configuration>
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
            assertTrue(result.modifiedCount() > 0, "Should detect JavaScript regardless of case");

            String xml = DomUtils.toXml(document);
            assertTrue(xml.contains("nashorn-core"), "Should contain nashorn dependency");
        }
    }

    @Nested
    @DisplayName("No-op Scenarios")
    class NoOpTests {

        @Test
        @DisplayName("should not modify POM without antrun plugin")
        void shouldNotModifyPomWithoutAntrun() throws Exception {
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
                                <version>3.11.0</version>
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
            assertEquals(0, result.modifiedCount(), "Should not have modified the POM");
        }

        @Test
        @DisplayName("should not modify POM when antrun plugin has no JavaScript scripts")
        void shouldNotModifyWhenNoJavaScript() throws Exception {
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
                                <artifactId>maven-antrun-plugin</artifactId>
                                <executions>
                                    <execution>
                                        <configuration>
                                            <target>
                                                <echo message="Hello"/>
                                            </target>
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

            assertTrue(result.success(), "Strategy should succeed");
            assertEquals(0, result.modifiedCount(), "Should not have modified the POM");

            String xml = DomUtils.toXml(document);
            assertFalse(xml.contains("nashorn"), "Should not contain nashorn dependency");
        }

        @Test
        @DisplayName("should not modify POM when antrun uses non-JavaScript script language")
        void shouldNotModifyWhenNonJavaScriptLanguage() throws Exception {
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
                                <artifactId>maven-antrun-plugin</artifactId>
                                <configuration>
                                    <target>
                                        <script language="groovy"><![CDATA[
                                            println "Hello from Groovy"
                                        ]]></script>
                                    </target>
                                </configuration>
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
            assertEquals(0, result.modifiedCount(), "Should not have modified the POM for Groovy");

            String xml = DomUtils.toXml(document);
            assertFalse(xml.contains("nashorn"), "Should not contain nashorn dependency");
        }

        @Test
        @DisplayName("should not modify POM when nashorn dependency already exists")
        void shouldNotModifyWhenNashornAlreadyPresent() throws Exception {
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
                                <artifactId>maven-antrun-plugin</artifactId>
                                <dependencies>
                                    <dependency>
                                        <groupId>org.openjdk.nashorn</groupId>
                                        <artifactId>nashorn-core</artifactId>
                                        <version>15.7</version>
                                    </dependency>
                                </dependencies>
                                <configuration>
                                    <target>
                                        <script language="javascript"><![CDATA[
                                            project.setProperty("foo", "bar");
                                        ]]></script>
                                    </target>
                                </configuration>
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
            assertEquals(0, result.modifiedCount(), "Should not have modified the POM");
        }

        @Test
        @DisplayName("should not modify POM without build section")
        void shouldNotModifyPomWithoutBuild() throws Exception {
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
            assertEquals(0, result.modifiedCount(), "Should not have modified the POM");
        }

        @Test
        @DisplayName("should not inject for non-maven-plugins groupId antrun")
        void shouldNotInjectForNonMavenPluginsGroupId() throws Exception {
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
                                <groupId>com.example</groupId>
                                <artifactId>maven-antrun-plugin</artifactId>
                                <configuration>
                                    <target>
                                        <script language="javascript"><![CDATA[
                                            var x = 1;
                                        ]]></script>
                                    </target>
                                </configuration>
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
            assertEquals(0, result.modifiedCount(), "Should not inject for non-standard groupId");
        }
    }

    @Nested
    @DisplayName("Sling Parent POM Pattern")
    class SlingPatternTests {

        @Test
        @DisplayName("should handle the real Sling parent POM pattern with BREE script")
        void shouldHandleSlingBreePattern() throws Exception {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.apache.sling</groupId>
                    <artifactId>sling</artifactId>
                    <version>22</version>
                    <packaging>pom</packaging>
                    <build>
                        <plugins>
                            <plugin>
                                <groupId>org.apache.maven.plugins</groupId>
                                <artifactId>maven-antrun-plugin</artifactId>
                                <executions>
                                    <execution>
                                        <id>set-bundle-required-execution-environment</id>
                                        <phase>initialize</phase>
                                        <goals>
                                            <goal>run</goal>
                                        </goals>
                                        <configuration>
                                            <exportAntProperties>true</exportAntProperties>
                                            <target>
                                                <script language="javascript"><![CDATA[
                                                    var System = java.lang.System;
                                                    var bree = "J2SE-1.5";
                                                    var slingJavaVersion = System.getProperty("sling.java.version");
                                                    if (slingJavaVersion == "6" || slingJavaVersion == "1.6") {
                                                        bree = "JavaSE-1.6";
                                                    } else if (slingJavaVersion == "7" || slingJavaVersion == "1.7") {
                                                        bree = "JavaSE-1.7";
                                                    } else if (slingJavaVersion == "8" || slingJavaVersion == "1.8") {
                                                        bree = "JavaSE-1.8";
                                                    }
                                                    project.setProperty("sling.bree", bree);
                                                ]]></script>
                                            </target>
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

            assertTrue(result.success(), "Strategy should succeed");
            assertTrue(result.modifiedCount() > 0, "Should have modified the Sling POM");

            String xml = DomUtils.toXml(document);
            assertTrue(xml.contains("org.openjdk.nashorn"), "Should contain nashorn groupId");
            assertTrue(xml.contains("nashorn-core"), "Should contain nashorn artifactId");
            assertTrue(xml.contains("15.7"), "Should contain nashorn version");
        }

        @Test
        @DisplayName("should handle antrun with existing dependencies and add nashorn")
        void shouldAddNashornToExistingDependencies() throws Exception {
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
                                <artifactId>maven-antrun-plugin</artifactId>
                                <dependencies>
                                    <dependency>
                                        <groupId>some.other</groupId>
                                        <artifactId>lib</artifactId>
                                        <version>1.0</version>
                                    </dependency>
                                </dependencies>
                                <configuration>
                                    <target>
                                        <script language="javascript"><![CDATA[
                                            project.setProperty("foo", "bar");
                                        ]]></script>
                                    </target>
                                </configuration>
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
            assertTrue(result.modifiedCount() > 0, "Should have modified the POM");

            String xml = DomUtils.toXml(document);
            assertTrue(xml.contains("nashorn-core"), "Should contain nashorn dependency");
            assertTrue(xml.contains("some.other"), "Should preserve existing dependency");
        }
    }

    @Nested
    @DisplayName("Strategy Description")
    class StrategyDescriptionTests {

        @Test
        @DisplayName("should provide meaningful description")
        void shouldProvideMeaningfulDescription() {
            String description = strategy.getDescription();

            assertNotNull(description, "Description should not be null");
            assertFalse(description.trim().isEmpty(), "Description should not be empty");
            assertTrue(
                    description.toLowerCase().contains("nashorn")
                            || description.toLowerCase().contains("antrun")
                            || description.toLowerCase().contains("javascript"),
                    "Description should mention nashorn, antrun, or javascript");
        }
    }
}
