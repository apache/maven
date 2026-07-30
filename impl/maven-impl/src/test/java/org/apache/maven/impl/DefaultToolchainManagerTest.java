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
package org.apache.maven.impl;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.maven.api.JavaToolchain;
import org.apache.maven.api.Project;
import org.apache.maven.api.Session;
import org.apache.maven.api.SessionData;
import org.apache.maven.api.Toolchain;
import org.apache.maven.api.Version;
import org.apache.maven.api.model.Build;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.Source;
import org.apache.maven.api.services.Lookup;
import org.apache.maven.api.services.ToolchainFactory;
import org.apache.maven.api.toolchain.ToolchainModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultToolchainManagerTest {

    @Mock
    private Session session;

    @Mock
    private Lookup lookup;

    @Mock
    private Project project;

    @Mock
    private ToolchainFactory jdkFactory;

    @Mock
    private Toolchain mockToolchain;

    private ToolchainModel toolchainModel;

    private DefaultToolchainManager manager;

    @BeforeEach
    void setUp() {
        manager = new DefaultToolchainManager(Map.of("jdk", jdkFactory));
    }

    @Test
    void getToolchainsWithValidTypeAndRequirements() {
        toolchainModel = ToolchainModel.newBuilder().type("jdk").build();
        when(session.getToolchains()).thenReturn(List.of(toolchainModel));
        when(jdkFactory.createToolchain(toolchainModel)).thenReturn(mockToolchain);
        when(jdkFactory.createDefaultToolchain()).thenReturn(Optional.empty());
        when(mockToolchain.matchesRequirements(any())).thenReturn(true);

        List<Toolchain> result = manager.getToolchains(session, "jdk", Map.of("version", "11"));

        assertEquals(1, result.size());
        assertEquals(mockToolchain, result.get(0));
    }

    @Test
    void getToolchainsWithInvalidType() {
        List<Toolchain> result = manager.getToolchains(session, "invalid", null);
        assertTrue(result.isEmpty());
    }

    @Test
    void storeAndRetrieveToolchainFromBuildContext() {
        Map<String, Object> context = new ConcurrentHashMap<>();
        SessionData data = mock(SessionData.class);
        toolchainModel = ToolchainModel.newBuilder().type("jdk").build();
        when(session.getService(Lookup.class)).thenReturn(lookup);
        when(lookup.lookupOptional(Project.class)).thenReturn(Optional.of(project));
        when(session.getData()).thenReturn(data);
        when(data.computeIfAbsent(any(), any())).thenReturn(context);
        when(mockToolchain.getType()).thenReturn("jdk");
        when(mockToolchain.getModel()).thenReturn(toolchainModel);
        when(jdkFactory.createToolchain(any(ToolchainModel.class))).thenReturn(mockToolchain);

        manager.storeToolchainToBuildContext(session, mockToolchain);
        Optional<Toolchain> result = manager.getToolchainFromBuildContext(session, "jdk");

        assertTrue(result.isPresent());
        assertEquals(mockToolchain, result.get());
    }

    @Test
    void retrieveContextWithoutProject() {
        when(session.getService(Lookup.class)).thenReturn(lookup);
        when(lookup.lookupOptional(Project.class)).thenReturn(Optional.empty());

        assertTrue(manager.retrieveContext(session).isEmpty());
    }

    @Test
    void getToolchainsWithNullType() {
        assertThrows(NullPointerException.class, () -> manager.getToolchains(session, null, null));
    }

    // --- Auto-selection tests ---

    @Test
    void autoSelectJdkToolchainWhenNoTargetVersion() {
        // Project has no targetVersion configured — should not auto-select
        when(session.getService(Lookup.class)).thenReturn(lookup);
        when(lookup.lookupOptional(Project.class)).thenReturn(Optional.of(project));
        Model model = Model.newBuilder().build(Build.newBuilder().build()).build();
        when(project.getModel()).thenReturn(model);

        DefaultToolchainManager testManager = new DefaultToolchainManager(Map.of("jdk", jdkFactory)) {
            @Override
            int getRunningJdkMajor() {
                return 17;
            }
        };

        Optional<Toolchain> result = testManager.autoSelectJdkToolchain(session);
        assertTrue(result.isEmpty());
    }

    @Test
    void autoSelectJdkToolchainWhenRunningJdkSupportsLevel() {
        // Project targets source 11, running JDK 17 supports it — no auto-select
        when(session.getService(Lookup.class)).thenReturn(lookup);
        when(lookup.lookupOptional(Project.class)).thenReturn(Optional.of(project));
        Model model = Model.newBuilder()
                .build(Build.newBuilder()
                        .sources(List.of(Source.newBuilder().targetVersion("11").build()))
                        .build())
                .build();
        when(project.getModel()).thenReturn(model);

        DefaultToolchainManager testManager = new DefaultToolchainManager(Map.of("jdk", jdkFactory)) {
            @Override
            int getRunningJdkMajor() {
                return 17;
            }
        };

        Optional<Toolchain> result = testManager.autoSelectJdkToolchain(session);
        assertTrue(result.isEmpty());
    }

    @Test
    void autoSelectJdkToolchainWhenRunningJdkDoesNotSupportLevel() {
        // Project targets source 6, running JDK 17 doesn't support it
        // JDK 11 toolchain available and supports source 6
        Logger testLogger = mock(Logger.class);
        DefaultToolchainManager testManager = new DefaultToolchainManager(Map.of("jdk", jdkFactory), testLogger) {
            @Override
            int getRunningJdkMajor() {
                return 17;
            }
        };

        // Set up project with targetVersion 6
        when(session.getService(Lookup.class)).thenReturn(lookup);
        when(lookup.lookupOptional(Project.class)).thenReturn(Optional.of(project));
        Model model = Model.newBuilder()
                .build(Build.newBuilder()
                        .sources(List.of(Source.newBuilder().targetVersion("6").build()))
                        .build())
                .build();
        when(project.getModel()).thenReturn(model);

        // Set up available JDK 11 toolchain
        JavaToolchain jdk11Toolchain = mock(JavaToolchain.class);
        Version jdk11Version = mock(Version.class);
        when(jdk11Version.toString()).thenReturn("11");
        when(jdk11Toolchain.getJavaVersion()).thenReturn(jdk11Version);
        when(jdk11Toolchain.getJavaHome()).thenReturn("/usr/lib/jvm/java-11");

        ToolchainModel jdk11Model = ToolchainModel.newBuilder().type("jdk").build();
        when(session.getToolchains()).thenReturn(List.of(jdk11Model));
        when(jdkFactory.createToolchain(jdk11Model)).thenReturn(jdk11Toolchain);
        when(jdkFactory.createDefaultToolchain()).thenReturn(Optional.empty());

        Optional<Toolchain> result = testManager.autoSelectJdkToolchain(session);

        assertTrue(result.isPresent());
        assertEquals(jdk11Toolchain, result.get());
        verify(testLogger).warn("Project requires --source {} which is not supported by JDK {}.", 6, 17);
        verify(testLogger)
                .warn(
                        "Automatically selected JDK {} (discovered at {}) for compilation.",
                        jdk11Version,
                        "/usr/lib/jvm/java-11");
    }

    @Test
    void autoSelectJdkToolchainPrefersNewestCompatible() {
        // Project targets source 6, running JDK 17
        // JDK 8 and JDK 11 both support source 6; should select JDK 11 (newest)
        Logger testLogger = mock(Logger.class);
        DefaultToolchainManager testManager = new DefaultToolchainManager(Map.of("jdk", jdkFactory), testLogger) {
            @Override
            int getRunningJdkMajor() {
                return 17;
            }
        };

        when(session.getService(Lookup.class)).thenReturn(lookup);
        when(lookup.lookupOptional(Project.class)).thenReturn(Optional.of(project));
        Model model = Model.newBuilder()
                .build(Build.newBuilder()
                        .sources(List.of(Source.newBuilder().targetVersion("6").build()))
                        .build())
                .build();
        when(project.getModel()).thenReturn(model);

        // JDK 8 toolchain
        JavaToolchain jdk8Toolchain = mock(JavaToolchain.class);
        Version jdk8Version = mock(Version.class);
        when(jdk8Version.toString()).thenReturn("8");
        when(jdk8Toolchain.getJavaVersion()).thenReturn(jdk8Version);

        // JDK 11 toolchain
        JavaToolchain jdk11Toolchain = mock(JavaToolchain.class);
        Version jdk11Version = mock(Version.class);
        when(jdk11Version.toString()).thenReturn("11");
        when(jdk11Toolchain.getJavaVersion()).thenReturn(jdk11Version);
        when(jdk11Toolchain.getJavaHome()).thenReturn("/usr/lib/jvm/java-11");

        // Use distinct provides so ToolchainModel.equals() distinguishes them
        ToolchainModel jdk8Model = ToolchainModel.newBuilder()
                .type("jdk")
                .provides(Map.of("version", "8"))
                .build();
        ToolchainModel jdk11Model = ToolchainModel.newBuilder()
                .type("jdk")
                .provides(Map.of("version", "11"))
                .build();
        when(session.getToolchains()).thenReturn(List.of(jdk8Model, jdk11Model));
        when(jdkFactory.createToolchain(jdk8Model)).thenReturn(jdk8Toolchain);
        when(jdkFactory.createToolchain(jdk11Model)).thenReturn(jdk11Toolchain);
        when(jdkFactory.createDefaultToolchain()).thenReturn(Optional.empty());

        Optional<Toolchain> result = testManager.autoSelectJdkToolchain(session);

        assertTrue(result.isPresent());
        assertEquals(jdk11Toolchain, result.get());
    }

    @Test
    void autoSelectJdkToolchainNoCompatibleToolchainAvailable() {
        // Project targets source 5, running JDK 17
        // Only JDK 11 toolchain available (min source 6, doesn't support 5)
        DefaultToolchainManager testManager = new DefaultToolchainManager(Map.of("jdk", jdkFactory)) {
            @Override
            int getRunningJdkMajor() {
                return 17;
            }
        };

        when(session.getService(Lookup.class)).thenReturn(lookup);
        when(lookup.lookupOptional(Project.class)).thenReturn(Optional.of(project));
        Model model = Model.newBuilder()
                .build(Build.newBuilder()
                        .sources(List.of(Source.newBuilder().targetVersion("5").build()))
                        .build())
                .build();
        when(project.getModel()).thenReturn(model);

        // JDK 11 doesn't support source 5
        JavaToolchain jdk11Toolchain = mock(JavaToolchain.class);
        Version jdk11Version = mock(Version.class);
        when(jdk11Version.toString()).thenReturn("11");
        when(jdk11Toolchain.getJavaVersion()).thenReturn(jdk11Version);

        ToolchainModel jdk11Model = ToolchainModel.newBuilder().type("jdk").build();
        when(session.getToolchains()).thenReturn(List.of(jdk11Model));
        when(jdkFactory.createToolchain(jdk11Model)).thenReturn(jdk11Toolchain);
        when(jdkFactory.createDefaultToolchain()).thenReturn(Optional.empty());

        Optional<Toolchain> result = testManager.autoSelectJdkToolchain(session);
        assertTrue(result.isEmpty());
    }

    @Test
    void autoSelectJdkToolchainFromLegacyProperties() {
        // Project uses maven.compiler.release=6 (legacy property), running JDK 17
        Logger testLogger = mock(Logger.class);
        DefaultToolchainManager testManager = new DefaultToolchainManager(Map.of("jdk", jdkFactory), testLogger) {
            @Override
            int getRunningJdkMajor() {
                return 17;
            }
        };

        when(session.getService(Lookup.class)).thenReturn(lookup);
        when(lookup.lookupOptional(Project.class)).thenReturn(Optional.of(project));
        Model model = Model.newBuilder()
                .properties(Map.of("maven.compiler.release", "6"))
                .build();
        when(project.getModel()).thenReturn(model);

        JavaToolchain jdk11Toolchain = mock(JavaToolchain.class);
        Version jdk11Version = mock(Version.class);
        when(jdk11Version.toString()).thenReturn("11");
        when(jdk11Toolchain.getJavaVersion()).thenReturn(jdk11Version);
        when(jdk11Toolchain.getJavaHome()).thenReturn("/usr/lib/jvm/java-11");

        ToolchainModel jdk11Model = ToolchainModel.newBuilder().type("jdk").build();
        when(session.getToolchains()).thenReturn(List.of(jdk11Model));
        when(jdkFactory.createToolchain(jdk11Model)).thenReturn(jdk11Toolchain);
        when(jdkFactory.createDefaultToolchain()).thenReturn(Optional.empty());

        Optional<Toolchain> result = testManager.autoSelectJdkToolchain(session);

        assertTrue(result.isPresent());
        assertEquals(jdk11Toolchain, result.get());
    }

    @Test
    void autoSelectJdkToolchainFromLegacySourceProperty() {
        // Project uses maven.compiler.source=1.6 (legacy property), running JDK 17
        Logger testLogger = mock(Logger.class);
        DefaultToolchainManager testManager = new DefaultToolchainManager(Map.of("jdk", jdkFactory), testLogger) {
            @Override
            int getRunningJdkMajor() {
                return 17;
            }
        };

        when(session.getService(Lookup.class)).thenReturn(lookup);
        when(lookup.lookupOptional(Project.class)).thenReturn(Optional.of(project));
        Model model = Model.newBuilder()
                .properties(Map.of("maven.compiler.source", "1.6"))
                .build();
        when(project.getModel()).thenReturn(model);

        JavaToolchain jdk11Toolchain = mock(JavaToolchain.class);
        Version jdk11Version = mock(Version.class);
        when(jdk11Version.toString()).thenReturn("11");
        when(jdk11Toolchain.getJavaVersion()).thenReturn(jdk11Version);
        when(jdk11Toolchain.getJavaHome()).thenReturn("/usr/lib/jvm/java-11");

        ToolchainModel jdk11Model = ToolchainModel.newBuilder().type("jdk").build();
        when(session.getToolchains()).thenReturn(List.of(jdk11Model));
        when(jdkFactory.createToolchain(jdk11Model)).thenReturn(jdk11Toolchain);
        when(jdkFactory.createDefaultToolchain()).thenReturn(Optional.empty());

        Optional<Toolchain> result = testManager.autoSelectJdkToolchain(session);

        assertTrue(result.isPresent());
        assertEquals(jdk11Toolchain, result.get());
    }

    @Test
    void getToolchainFromBuildContextAutoSelectsFallback() {
        // Verify getToolchainFromBuildContext calls auto-selection when no explicit toolchain
        Logger testLogger = mock(Logger.class);
        Map<String, Object> context = new ConcurrentHashMap<>();
        SessionData data = mock(SessionData.class);

        DefaultToolchainManager testManager = new DefaultToolchainManager(Map.of("jdk", jdkFactory), testLogger) {
            @Override
            int getRunningJdkMajor() {
                return 17;
            }
        };

        when(session.getService(Lookup.class)).thenReturn(lookup);
        when(lookup.lookupOptional(Project.class)).thenReturn(Optional.of(project));
        when(session.getData()).thenReturn(data);
        when(data.computeIfAbsent(any(), any())).thenReturn(context);

        Model model = Model.newBuilder()
                .build(Build.newBuilder()
                        .sources(List.of(Source.newBuilder().targetVersion("6").build()))
                        .build())
                .build();
        when(project.getModel()).thenReturn(model);

        JavaToolchain jdk11Toolchain = mock(JavaToolchain.class);
        Version jdk11Version = mock(Version.class);
        when(jdk11Version.toString()).thenReturn("11");
        when(jdk11Toolchain.getJavaVersion()).thenReturn(jdk11Version);
        when(jdk11Toolchain.getJavaHome()).thenReturn("/usr/lib/jvm/java-11");

        ToolchainModel jdk11Model = ToolchainModel.newBuilder().type("jdk").build();
        when(jdk11Toolchain.getModel()).thenReturn(jdk11Model);
        when(session.getToolchains()).thenReturn(List.of(jdk11Model));
        when(jdkFactory.createToolchain(jdk11Model)).thenReturn(jdk11Toolchain);
        when(jdkFactory.createDefaultToolchain()).thenReturn(Optional.empty());

        Optional<Toolchain> result = testManager.getToolchainFromBuildContext(session, "jdk");

        assertTrue(result.isPresent());
        assertEquals(jdk11Toolchain, result.get());
    }

    @Test
    void getToolchainFromBuildContextReturnsExplicitOverAutoSelect() {
        // When an explicit toolchain is stored via storeToolchainToBuildContext,
        // it takes precedence over auto-selection
        Map<String, Object> context = new ConcurrentHashMap<>();
        SessionData data = mock(SessionData.class);
        toolchainModel = ToolchainModel.newBuilder().type("jdk").build();

        when(session.getService(Lookup.class)).thenReturn(lookup);
        when(lookup.lookupOptional(Project.class)).thenReturn(Optional.of(project));
        when(session.getData()).thenReturn(data);
        when(data.computeIfAbsent(any(), any())).thenReturn(context);
        when(mockToolchain.getType()).thenReturn("jdk");
        when(mockToolchain.getModel()).thenReturn(toolchainModel);
        when(jdkFactory.createToolchain(any(ToolchainModel.class))).thenReturn(mockToolchain);

        // Store explicit toolchain using the proper API
        manager.storeToolchainToBuildContext(session, mockToolchain);

        // Now retrieve — should get the explicit one, not auto-select
        Optional<Toolchain> result = manager.getToolchainFromBuildContext(session, "jdk");

        assertTrue(result.isPresent());
        assertEquals(mockToolchain, result.get());
    }

    @Test
    void getToolchainFromBuildContextNonJdkTypeDoesNotAutoSelect() {
        // Auto-selection should only apply to "jdk" type
        Map<String, Object> context = new ConcurrentHashMap<>();
        SessionData data = mock(SessionData.class);

        when(session.getService(Lookup.class)).thenReturn(lookup);
        when(lookup.lookupOptional(Project.class)).thenReturn(Optional.of(project));
        when(session.getData()).thenReturn(data);
        when(data.computeIfAbsent(any(), any())).thenReturn(context);

        // No "otherType" factory registered; getToolchainFromBuildContext should return empty
        // without attempting auto-selection
        Optional<Toolchain> result = manager.getToolchainFromBuildContext(session, "otherType");
        assertTrue(result.isEmpty());
    }

    @Test
    void getProjectRequiredSourceLevelTargetVersionTakesPrecedence() {
        // targetVersion in sources should take precedence over properties
        DefaultToolchainManager testManager = new DefaultToolchainManager(Map.of("jdk", jdkFactory));

        when(session.getService(Lookup.class)).thenReturn(lookup);
        when(lookup.lookupOptional(Project.class)).thenReturn(Optional.of(project));
        Model model = Model.newBuilder()
                .properties(Map.of("maven.compiler.release", "11"))
                .build(Build.newBuilder()
                        .sources(List.of(Source.newBuilder().targetVersion("8").build()))
                        .build())
                .build();
        when(project.getModel()).thenReturn(model);

        assertEquals(8, testManager.getProjectRequiredSourceLevel(session));
    }

    @Test
    void getProjectRequiredSourceLevelNoProject() {
        when(session.getService(Lookup.class)).thenReturn(lookup);
        when(lookup.lookupOptional(Project.class)).thenReturn(Optional.empty());

        assertEquals(-1, manager.getProjectRequiredSourceLevel(session));
    }
}
