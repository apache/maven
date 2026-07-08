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
        @DisplayName("should not warn about gmavenplus-plugin (now handled by PluginUpgradeStrategy)")
        void shouldNotWarnAboutGmavenplusPlugin() throws Exception {
            // gmavenplus-plugin was fixed in 4.2.0 (groovy/GMavenPlus#328) and is now
            // handled as a plugin upgrade rather than a permanent incompatibility warning
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

            verify(context.logger, never()).warn(argThat(msg -> msg.contains("Known Maven 4 incompatibility")));
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
    @DisplayName("Property-Interpolated Module Path Warnings (#12434)")
    class PropertyInterpolatedModulePathTests {

        @Test
        @DisplayName("should warn about property expressions in module paths")
        void shouldWarnAboutPropertyExpressionsInModulePaths() throws Exception {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <packaging>pom</packaging>
                    <modules>
                        <module>core</module>
                        <module>spark-${spark.compat.version}</module>
                        <module>v${spark.major.version}/mixed-spark</module>
                    </modules>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            strategy.doApply(context, pomMap);

            verify(context.logger, atLeastOnce())
                    .warn(argThat(msg ->
                            msg.contains("spark-${spark.compat.version}") && msg.contains("property expression")));
        }

        @Test
        @DisplayName("should not warn about static module paths")
        void shouldNotWarnAboutStaticModulePaths() throws Exception {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <packaging>pom</packaging>
                    <modules>
                        <module>core</module>
                        <module>web</module>
                    </modules>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            strategy.doApply(context, pomMap);

            verify(context.logger, never()).warn(argThat(msg -> msg.contains("property expression")));
        }

        @Test
        @DisplayName("should warn about property expressions in profile module paths")
        void shouldWarnAboutPropertyExpressionsInProfileModulePaths() throws Exception {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <packaging>pom</packaging>
                    <profiles>
                        <profile>
                            <id>spark-3.5</id>
                            <modules>
                                <module>spark-${spark.version}</module>
                            </modules>
                        </profile>
                    </profiles>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            strategy.doApply(context, pomMap);

            verify(context.logger, atLeastOnce())
                    .warn(argThat(msg -> msg.contains("spark-${spark.version}") && msg.contains("profile")));
        }
    }

    @Nested
    @DisplayName("CI-Friendly Missing Dependency Version Warnings (#12435)")
    class CiFriendlyDependencyVersionTests {

        @Test
        @DisplayName("should warn about versionless dependencies in CI-friendly project")
        void shouldWarnAboutVersionlessDependenciesInCiFriendlyProject() throws Exception {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <parent>
                        <groupId>org.example</groupId>
                        <artifactId>parent</artifactId>
                        <version>${revision}</version>
                    </parent>
                    <artifactId>child</artifactId>
                    <dependencies>
                        <dependency>
                            <groupId>org.springframework</groupId>
                            <artifactId>spring-context</artifactId>
                        </dependency>
                        <dependency>
                            <groupId>io.protostuff</groupId>
                            <artifactId>protostuff-core</artifactId>
                        </dependency>
                    </dependencies>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            strategy.doApply(context, pomMap);

            verify(context.logger, atLeastOnce())
                    .warn(argThat(msg -> msg.contains("CI-friendly")
                            && msg.contains("${revision}")
                            && msg.contains("2 dependencies")));
        }

        @Test
        @DisplayName("should not warn when parent version is not CI-friendly")
        void shouldNotWarnWhenParentVersionNotCiFriendly() throws Exception {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <parent>
                        <groupId>org.example</groupId>
                        <artifactId>parent</artifactId>
                        <version>1.0.0</version>
                    </parent>
                    <artifactId>child</artifactId>
                    <dependencies>
                        <dependency>
                            <groupId>org.springframework</groupId>
                            <artifactId>spring-context</artifactId>
                        </dependency>
                    </dependencies>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            strategy.doApply(context, pomMap);

            verify(context.logger, never()).warn(argThat(msg -> msg.contains("CI-friendly")));
        }

        @Test
        @DisplayName("should not warn when dependencies have explicit versions")
        void shouldNotWarnWhenDependenciesHaveVersions() throws Exception {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <parent>
                        <groupId>org.example</groupId>
                        <artifactId>parent</artifactId>
                        <version>${revision}</version>
                    </parent>
                    <artifactId>child</artifactId>
                    <dependencies>
                        <dependency>
                            <groupId>org.springframework</groupId>
                            <artifactId>spring-context</artifactId>
                            <version>6.0.0</version>
                        </dependency>
                    </dependencies>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            strategy.doApply(context, pomMap);

            verify(context.logger, never()).warn(argThat(msg -> msg.contains("CI-friendly")));
        }

        @Test
        @DisplayName("should handle ${changelist} as CI-friendly expression")
        void shouldHandleChangelistAsCiFriendly() throws Exception {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <parent>
                        <groupId>org.example</groupId>
                        <artifactId>parent</artifactId>
                        <version>1.0.0${changelist}</version>
                    </parent>
                    <artifactId>child</artifactId>
                    <dependencies>
                        <dependency>
                            <groupId>commons-io</groupId>
                            <artifactId>commons-io</artifactId>
                        </dependency>
                    </dependencies>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            strategy.doApply(context, pomMap);

            verify(context.logger, atLeastOnce()).warn(argThat(msg -> msg.contains("CI-friendly")));
        }

        @Test
        @DisplayName("should not warn when no parent element exists")
        void shouldNotWarnWhenNoParentExists() throws Exception {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>org.springframework</groupId>
                            <artifactId>spring-context</artifactId>
                        </dependency>
                    </dependencies>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            strategy.doApply(context, pomMap);

            verify(context.logger, never()).warn(argThat(msg -> msg.contains("CI-friendly")));
        }
    }
}
