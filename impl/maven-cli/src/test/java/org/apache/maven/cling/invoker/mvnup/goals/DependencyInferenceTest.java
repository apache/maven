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

import java.io.StringReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.api.cli.InvokerRequest;
import org.apache.maven.cling.invoker.mvnup.UpgradeContext;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for dependency inference functionality in Maven upgrade tools.
 * Tests the logic that determines when dependency information can be removed because
 * it can be inferred from project artifacts in the same reactor.
 */
class DependencyInferenceTest {

    private BaseUpgradeGoal upgrade;
    private SAXBuilder saxBuilder;

    @BeforeEach
    void setUp() {
        upgrade = new Apply();
        saxBuilder = new SAXBuilder();
    }

    private UpgradeContext createMockContext() {
        InvokerRequest request = Mockito.mock(InvokerRequest.class);

        // Mock all required properties for LookupContext constructor
        Mockito.when(request.cwd()).thenReturn(Paths.get("/project"));
        Mockito.when(request.installationDirectory()).thenReturn(Paths.get("/maven"));
        Mockito.when(request.userHomeDirectory()).thenReturn(Paths.get("/home/user"));
        Mockito.when(request.topDirectory()).thenReturn(Paths.get("/project"));
        Mockito.when(request.rootDirectory()).thenReturn(java.util.Optional.empty());
        Mockito.when(request.userProperties()).thenReturn(java.util.Map.of());
        Mockito.when(request.systemProperties()).thenReturn(java.util.Map.of());

        // Mock parserRequest and logger
        org.apache.maven.api.cli.ParserRequest parserRequest =
                Mockito.mock(org.apache.maven.api.cli.ParserRequest.class);
        org.apache.maven.api.cli.Logger logger = Mockito.mock(org.apache.maven.api.cli.Logger.class);
        Mockito.when(request.parserRequest()).thenReturn(parserRequest);
        Mockito.when(parserRequest.logger()).thenReturn(logger);

        UpgradeContext context = new UpgradeContext(request);
        return context;
    }

    @Test
    void testRemoveDependencyVersionForProjectArtifact() throws Exception {
        // Test that dependency version is removed when it points to a project artifact
        String parentPomXml =
                """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.1.0">
                <groupId>com.example</groupId>
                <artifactId>parent-project</artifactId>
                <version>1.0.0</version>
                <packaging>pom</packaging>
            </project>
            """;

        String moduleAPomXml =
                """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.1.0">
                <parent>
                    <groupId>com.example</groupId>
                    <artifactId>parent-project</artifactId>
                    <version>1.0.0</version>
                </parent>
                <artifactId>module-a</artifactId>
            </project>
            """;

        String moduleBPomXml =
                """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.1.0">
                <parent>
                    <groupId>com.example</groupId>
                    <artifactId>parent-project</artifactId>
                    <version>1.0.0</version>
                </parent>
                <artifactId>module-b</artifactId>
                <dependencies>
                    <dependency>
                        <groupId>com.example</groupId>
                        <artifactId>module-a</artifactId>
                        <version>1.0.0</version>
                    </dependency>
                </dependencies>
            </project>
            """;

        Document parentDoc = saxBuilder.build(new StringReader(parentPomXml));
        Document moduleADoc = saxBuilder.build(new StringReader(moduleAPomXml));
        Document moduleBDoc = saxBuilder.build(new StringReader(moduleBPomXml));

        Map<Path, Document> pomMap = new HashMap<>();
        Path parentPath = Paths.get("/project/pom.xml");
        Path moduleAPath = Paths.get("/project/module-a/pom.xml");
        Path moduleBPath = Paths.get("/project/module-b/pom.xml");
        pomMap.put(parentPath, parentDoc);
        pomMap.put(moduleAPath, moduleADoc);
        pomMap.put(moduleBPath, moduleBDoc);

        Element moduleBRoot = moduleBDoc.getRootElement();
        Element dependencies = moduleBRoot.getChild("dependencies", moduleBRoot.getNamespace());
        Element dependency = dependencies.getChild("dependency", moduleBRoot.getNamespace());

        // Verify dependency elements exist before inference
        assertNotNull(dependency.getChild("groupId", moduleBRoot.getNamespace()));
        assertNotNull(dependency.getChild("artifactId", moduleBRoot.getNamespace()));
        assertNotNull(dependency.getChild("version", moduleBRoot.getNamespace()));

        // Apply dependency inference
        UpgradeContext context = createMockContext();
        upgrade.removeDependencyInferenceRedundancy(context, moduleBDoc, moduleBPath, pomMap);

        // Verify version was removed (can be inferred from project)
        assertNull(dependency.getChild("version", moduleBRoot.getNamespace()));
        // groupId should also be removed (can be inferred from project)
        assertNull(dependency.getChild("groupId", moduleBRoot.getNamespace()));
        // artifactId should remain (always required)
        assertNotNull(dependency.getChild("artifactId", moduleBRoot.getNamespace()));
    }

    @Test
    void testKeepDependencyVersionForExternalArtifact() throws Exception {
        // Test that dependency version is kept when it points to an external artifact
        String modulePomXml =
                """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.1.0">
                <groupId>com.example</groupId>
                <artifactId>my-module</artifactId>
                <version>1.0.0</version>
                <dependencies>
                    <dependency>
                        <groupId>org.apache.commons</groupId>
                        <artifactId>commons-lang3</artifactId>
                        <version>3.12.0</version>
                    </dependency>
                </dependencies>
            </project>
            """;

        Document moduleDoc = saxBuilder.build(new StringReader(modulePomXml));

        Map<Path, Document> pomMap = new HashMap<>();
        Path modulePath = Paths.get("/project/pom.xml");
        pomMap.put(modulePath, moduleDoc);

        Element moduleRoot = moduleDoc.getRootElement();
        Element dependencies = moduleRoot.getChild("dependencies", moduleRoot.getNamespace());
        Element dependency = dependencies.getChild("dependency", moduleRoot.getNamespace());

        // Apply dependency inference
        UpgradeContext context = createMockContext();
        upgrade.removeDependencyInferenceRedundancy(context, moduleDoc, modulePath, pomMap);

        // Verify all dependency elements remain (external dependency)
        assertNotNull(dependency.getChild("groupId", moduleRoot.getNamespace()));
        assertNotNull(dependency.getChild("artifactId", moduleRoot.getNamespace()));
        assertNotNull(dependency.getChild("version", moduleRoot.getNamespace()));
    }

    @Test
    void testKeepDependencyVersionWhenVersionMismatch() throws Exception {
        // Test that dependency version is kept when it doesn't match the project version
        String moduleAPomXml =
                """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.1.0">
                <groupId>com.example</groupId>
                <artifactId>module-a</artifactId>
                <version>1.0.0</version>
            </project>
            """;

        String moduleBPomXml =
                """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.1.0">
                <groupId>com.example</groupId>
                <artifactId>module-b</artifactId>
                <version>1.0.0</version>
                <dependencies>
                    <dependency>
                        <groupId>com.example</groupId>
                        <artifactId>module-a</artifactId>
                        <version>0.9.0</version>
                    </dependency>
                </dependencies>
            </project>
            """;

        Document moduleADoc = saxBuilder.build(new StringReader(moduleAPomXml));
        Document moduleBDoc = saxBuilder.build(new StringReader(moduleBPomXml));

        Map<Path, Document> pomMap = new HashMap<>();
        Path moduleAPath = Paths.get("/project/module-a/pom.xml");
        Path moduleBPath = Paths.get("/project/module-b/pom.xml");
        pomMap.put(moduleAPath, moduleADoc);
        pomMap.put(moduleBPath, moduleBDoc);

        Element moduleBRoot = moduleBDoc.getRootElement();
        Element dependencies = moduleBRoot.getChild("dependencies", moduleBRoot.getNamespace());
        Element dependency = dependencies.getChild("dependency", moduleBRoot.getNamespace());

        // Apply dependency inference
        UpgradeContext context = createMockContext();
        upgrade.removeDependencyInferenceRedundancy(context, moduleBDoc, moduleBPath, pomMap);

        // Verify correct behavior when version doesn't match:
        // - groupId should be removed (can be inferred from project regardless of version)
        // - version should remain (doesn't match project version, so can't be inferred)
        // - artifactId should remain (always required)
        assertNull(dependency.getChild("groupId", moduleBRoot.getNamespace()));
        assertNotNull(dependency.getChild("artifactId", moduleBRoot.getNamespace()));
        assertNotNull(dependency.getChild("version", moduleBRoot.getNamespace()));
    }

    @Test
    void testRemoveDependencyFromPluginDependencies() throws Exception {
        // Test that dependency inference works for plugin dependencies
        String moduleAPomXml =
                """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.1.0">
                <groupId>com.example</groupId>
                <artifactId>module-a</artifactId>
                <version>1.0.0</version>
            </project>
            """;

        String moduleBPomXml =
                """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.1.0">
                <groupId>com.example</groupId>
                <artifactId>module-b</artifactId>
                <version>1.0.0</version>
                <build>
                    <plugins>
                        <plugin>
                            <groupId>org.apache.maven.plugins</groupId>
                            <artifactId>maven-compiler-plugin</artifactId>
                            <dependencies>
                                <dependency>
                                    <groupId>com.example</groupId>
                                    <artifactId>module-a</artifactId>
                                    <version>1.0.0</version>
                                </dependency>
                            </dependencies>
                        </plugin>
                    </plugins>
                </build>
            </project>
            """;

        Document moduleADoc = saxBuilder.build(new StringReader(moduleAPomXml));
        Document moduleBDoc = saxBuilder.build(new StringReader(moduleBPomXml));

        Map<Path, Document> pomMap = new HashMap<>();
        Path moduleAPath = Paths.get("/project/module-a/pom.xml");
        Path moduleBPath = Paths.get("/project/module-b/pom.xml");
        pomMap.put(moduleAPath, moduleADoc);
        pomMap.put(moduleBPath, moduleBDoc);

        Element moduleBRoot = moduleBDoc.getRootElement();
        Element build = moduleBRoot.getChild("build", moduleBRoot.getNamespace());
        Element plugins = build.getChild("plugins", moduleBRoot.getNamespace());
        Element plugin = plugins.getChild("plugin", moduleBRoot.getNamespace());
        Element dependencies = plugin.getChild("dependencies", moduleBRoot.getNamespace());
        Element dependency = dependencies.getChild("dependency", moduleBRoot.getNamespace());

        // Apply dependency inference
        UpgradeContext context = createMockContext();
        upgrade.removeDependencyInferenceRedundancy(context, moduleBDoc, moduleBPath, pomMap);

        // Verify version and groupId were removed from plugin dependency
        assertNull(dependency.getChild("version", moduleBRoot.getNamespace()));
        assertNull(dependency.getChild("groupId", moduleBRoot.getNamespace()));
        assertNotNull(dependency.getChild("artifactId", moduleBRoot.getNamespace()));
    }

    @Test
    void testFindDependencyPomInMap() throws Exception {
        // Test the helper method that finds dependency POMs in the map
        String moduleAPomXml =
                """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.1.0">
                <groupId>com.example</groupId>
                <artifactId>module-a</artifactId>
                <version>1.0.0</version>
            </project>
            """;

        Document moduleADoc = saxBuilder.build(new StringReader(moduleAPomXml));

        Map<Path, Document> pomMap = new HashMap<>();
        Path moduleAPath = Paths.get("/project/module-a/pom.xml");
        pomMap.put(moduleAPath, moduleADoc);

        // Test finding by groupId and artifactId
        Document found = upgrade.findDependencyPomInMap("com.example", "module-a", pomMap);
        assertNotNull(found);

        // Test finding by artifactId only (groupId null)
        Document foundByArtifactId = upgrade.findDependencyPomInMap(null, "module-a", pomMap);
        assertNotNull(foundByArtifactId);

        // Test not finding non-existent artifact
        Document notFound = upgrade.findDependencyPomInMap("com.example", "non-existent", pomMap);
        assertNull(notFound);
    }

    @Test
    void testCanInferDependencyVersion() throws Exception {
        // Test the logic for determining if dependency version can be inferred
        String modulePomXml =
                """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.1.0">
                <groupId>com.example</groupId>
                <artifactId>module-a</artifactId>
                <version>1.0.0</version>
            </project>
            """;

        Document moduleDoc = saxBuilder.build(new StringReader(modulePomXml));

        // Test matching version
        assertTrue(upgrade.canInferDependencyVersion(moduleDoc, "1.0.0"));

        // Test non-matching version
        assertFalse(upgrade.canInferDependencyVersion(moduleDoc, "2.0.0"));
    }
}
