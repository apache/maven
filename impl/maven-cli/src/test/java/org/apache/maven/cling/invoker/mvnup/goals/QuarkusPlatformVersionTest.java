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
import org.apache.maven.cling.invoker.mvnup.UpgradeContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for Quarkus platform version detection and the Quarkus 2.x upgrade skip behavior
 * in {@link PluginUpgradeStrategy}.
 */
@DisplayName("Quarkus Platform Version Detection and Skip")
class QuarkusPlatformVersionTest {

    private PluginUpgradeStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new PluginUpgradeStrategy();
    }

    private UpgradeContext createMockContext() {
        return TestUtils.createMockContext();
    }

    @Nested
    @DisplayName("Platform Version Detection")
    class PlatformVersionDetectionTests {

        @Test
        @DisplayName("should detect version from quarkus-bom with io.quarkus.platform groupId")
        void shouldDetectFromQuarkusPlatformBom() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>io.quarkus.platform</groupId>
                                <artifactId>quarkus-bom</artifactId>
                                <version>2.16.7.Final</version>
                                <type>pom</type>
                                <scope>import</scope>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                </project>
                """;

            Document document = Document.of(pomXml);
            assertEquals("2.16.7.Final", strategy.detectQuarkusPlatformVersion(document));
        }

        @Test
        @DisplayName("should detect version from quarkus-bom with io.quarkus groupId")
        void shouldDetectFromQuarkusBom() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>io.quarkus</groupId>
                                <artifactId>quarkus-bom</artifactId>
                                <version>3.15.0</version>
                                <type>pom</type>
                                <scope>import</scope>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                </project>
                """;

            Document document = Document.of(pomXml);
            assertEquals("3.15.0", strategy.detectQuarkusPlatformVersion(document));
        }

        @Test
        @DisplayName("should resolve property reference in BOM version")
        void shouldResolvePropertyInBomVersion() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <properties>
                        <quarkus.platform.version>2.16.7.Final</quarkus.platform.version>
                    </properties>
                    <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>io.quarkus.platform</groupId>
                                <artifactId>quarkus-bom</artifactId>
                                <version>${quarkus.platform.version}</version>
                                <type>pom</type>
                                <scope>import</scope>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                </project>
                """;

            Document document = Document.of(pomXml);
            assertEquals("2.16.7.Final", strategy.detectQuarkusPlatformVersion(document));
        }

        @Test
        @DisplayName("should fall back to quarkus.platform.version property when no BOM present")
        void shouldFallbackToQuarkusPlatformVersionProperty() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <properties>
                        <quarkus.platform.version>2.16.7.Final</quarkus.platform.version>
                    </properties>
                </project>
                """;

            Document document = Document.of(pomXml);
            assertEquals("2.16.7.Final", strategy.detectQuarkusPlatformVersion(document));
        }

        @Test
        @DisplayName("should fall back to quarkus.version property")
        void shouldFallbackToQuarkusVersionProperty() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <properties>
                        <quarkus.version>2.13.5.Final</quarkus.version>
                    </properties>
                </project>
                """;

            Document document = Document.of(pomXml);
            assertEquals("2.13.5.Final", strategy.detectQuarkusPlatformVersion(document));
        }

        @Test
        @DisplayName("should return null when no Quarkus version can be detected")
        void shouldReturnNullWhenNoQuarkusVersion() {
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
            assertNull(strategy.detectQuarkusPlatformVersion(document));
        }
    }

    @Nested
    @DisplayName("Quarkus 2.x Upgrade Skip")
    class Quarkus2xUpgradeSkipTests {

        @Test
        @DisplayName("should skip quarkus-maven-plugin upgrade when project uses Quarkus 2.x BOM")
        void shouldSkipQuarkusUpgradeWhenQuarkus2xBom() throws Exception {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>io.quarkus.platform</groupId>
                                <artifactId>quarkus-bom</artifactId>
                                <version>2.16.7.Final</version>
                                <type>pom</type>
                                <scope>import</scope>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                    <build>
                        <plugins>
                            <plugin>
                                <groupId>io.quarkus</groupId>
                                <artifactId>quarkus-maven-plugin</artifactId>
                                <version>2.16.7.Final</version>
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
                    xml.contains("<version>2.16.7.Final</version>"),
                    "quarkus-maven-plugin should NOT be upgraded when project uses Quarkus 2.x");
            assertFalse(xml.contains("<version>3.26.0</version>"), "quarkus-maven-plugin should NOT be set to 3.26.0");
        }

        @Test
        @DisplayName("should skip quarkus-maven-plugin upgrade when Quarkus 2.x version comes from property")
        void shouldSkipQuarkusUpgradeWhenQuarkus2xProperty() throws Exception {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <properties>
                        <quarkus.platform.version>2.16.7.Final</quarkus.platform.version>
                    </properties>
                    <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>io.quarkus.platform</groupId>
                                <artifactId>quarkus-bom</artifactId>
                                <version>${quarkus.platform.version}</version>
                                <type>pom</type>
                                <scope>import</scope>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                    <build>
                        <plugins>
                            <plugin>
                                <groupId>io.quarkus</groupId>
                                <artifactId>quarkus-maven-plugin</artifactId>
                                <version>2.16.7.Final</version>
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
                    xml.contains("<version>2.16.7.Final</version>"),
                    "quarkus-maven-plugin should NOT be upgraded when project uses Quarkus 2.x (via property)");
        }

        @Test
        @DisplayName("should still upgrade quarkus-maven-plugin when project uses Quarkus 3.x")
        void shouldUpgradeQuarkusPluginWhenQuarkus3x() throws Exception {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>io.quarkus.platform</groupId>
                                <artifactId>quarkus-bom</artifactId>
                                <version>3.15.0</version>
                                <type>pom</type>
                                <scope>import</scope>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                    <build>
                        <plugins>
                            <plugin>
                                <groupId>io.quarkus</groupId>
                                <artifactId>quarkus-maven-plugin</artifactId>
                                <version>3.15.0</version>
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
                    xml.contains("<version>3.26.0</version>"),
                    "quarkus-maven-plugin should be upgraded when project uses Quarkus 3.x");
        }

        @Test
        @DisplayName("should skip quarkus-maven-plugin upgrade when quarkus.version property is 2.x")
        void shouldSkipQuarkusUpgradeFromQuarkusVersionProperty() throws Exception {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <properties>
                        <quarkus.version>2.13.5.Final</quarkus.version>
                    </properties>
                    <build>
                        <plugins>
                            <plugin>
                                <groupId>io.quarkus</groupId>
                                <artifactId>quarkus-maven-plugin</artifactId>
                                <version>2.13.5.Final</version>
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
                    xml.contains("<version>2.13.5.Final</version>"),
                    "quarkus-maven-plugin should NOT be upgraded when quarkus.version property is 2.x");
        }

        @Test
        @DisplayName("should skip upgrade for io.quarkus.platform groupId with Quarkus 2.x")
        void shouldSkipQuarkusPlatformGroupIdUpgradeWithQuarkus2x() throws Exception {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>io.quarkus</groupId>
                                <artifactId>quarkus-bom</artifactId>
                                <version>2.16.7.Final</version>
                                <type>pom</type>
                                <scope>import</scope>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                    <build>
                        <plugins>
                            <plugin>
                                <groupId>io.quarkus.platform</groupId>
                                <artifactId>quarkus-maven-plugin</artifactId>
                                <version>2.16.7.Final</version>
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
                    xml.contains("<version>2.16.7.Final</version>"),
                    "quarkus-maven-plugin should NOT be upgraded for io.quarkus.platform with Quarkus 2.x");
        }
    }
}
