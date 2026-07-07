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

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Tests for Maven 4 compatibility warnings emitted by {@link CompatibilityFixStrategy}.
 * These cover warning-only checks that detect patterns known to fail with Maven 4
 * but which cannot be auto-fixed.
 */
@DisplayName("CompatibilityFixStrategy Warnings")
class CompatibilityWarningTest {

    private CompatibilityFixStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new CompatibilityFixStrategy();
    }

    private UpgradeContext createMockContext() {
        return TestUtils.createMockContext();
    }

    @Nested
    @DisplayName("Incompatible Plugin Warnings (#12432)")
    class IncompatiblePluginWarningTests {

        @Test
        @DisplayName("should warn about gmavenplus-plugin in build/plugins")
        void shouldWarnAboutGmavenplusPlugin() throws Exception {
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
                                <groupId>org.codehaus.gmavenplus</groupId>
                                <artifactId>gmavenplus-plugin</artifactId>
                                <version>4.1.1</version>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            strategy.doApply(context, pomMap);

            verify(context.logger, atLeastOnce())
                    .warn(argThat(
                            msg -> msg.contains("gmavenplus-plugin") && msg.contains("UnsupportedOperationException")));
        }

        @Test
        @DisplayName("should warn about gmavenplus-plugin in pluginManagement")
        void shouldWarnAboutGmavenplusPluginInPluginManagement() throws Exception {
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
                                    <groupId>org.codehaus.gmavenplus</groupId>
                                    <artifactId>gmavenplus-plugin</artifactId>
                                    <version>4.1.1</version>
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

            verify(context.logger, atLeastOnce()).warn(argThat(msg -> msg.contains("gmavenplus-plugin")));
        }

        @Test
        @DisplayName("should not warn about compatible plugins")
        void shouldNotWarnAboutCompatiblePlugins() throws Exception {
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
                                <version>3.13.0</version>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            strategy.doApply(context, pomMap);

            verify(context.logger, never()).warn(argThat(msg -> msg.contains("Known Maven 4 incompatibility")));
        }
    }

    @Nested
    @DisplayName("Third-Party Repository Prefix Filtering Warnings (#12433)")
    class ThirdPartyRepositoryWarningTests {

        @Test
        @DisplayName("should warn about third-party repositories")
        void shouldWarnAboutThirdPartyRepositories() throws Exception {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <repositories>
                        <repository>
                            <id>jenkins-ci</id>
                            <url>https://repo.jenkins-ci.org/public</url>
                        </repository>
                    </repositories>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            strategy.doApply(context, pomMap);

            verify(context.logger, atLeastOnce())
                    .warn(argThat(msg -> msg.contains("jenkins-ci") && msg.contains("prefix")));
        }

        @Test
        @DisplayName("should not warn about Maven Central")
        void shouldNotWarnAboutMavenCentral() throws Exception {
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
            strategy.doApply(context, pomMap);

            verify(context.logger, never()).warn(argThat(msg -> msg.contains("prefix")));
        }

        @Test
        @DisplayName("should skip repositories with property expressions in URL")
        void shouldSkipRepositoriesWithPropertyExpressionsInUrl() throws Exception {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <repositories>
                        <repository>
                            <id>internal</id>
                            <url>${repo.url}/releases</url>
                        </repository>
                    </repositories>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            strategy.doApply(context, pomMap);

            verify(context.logger, never()).warn(argThat(msg -> msg.contains("internal") && msg.contains("prefix")));
        }
    }
}
