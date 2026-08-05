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

import org.apache.maven.api.Project;
import org.apache.maven.api.Session;
import org.apache.maven.api.SessionData;
import org.apache.maven.api.Toolchain;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
        assertTrue(
                result.isEmpty(), "Expected collection to be empty but had " + result.size() + " elements: " + result);
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

        assertTrue(result.isPresent(), "Expected " + result + ".isPresent() to return true");
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

    // --- Source level compatibility check tests ---

    @Test
    void checkCompatibilityNoTargetVersion() {
        // Project has no targetVersion configured — no error
        Logger testLogger = mock(Logger.class);
        DefaultToolchainManager testManager = new DefaultToolchainManager(Map.of("jdk", jdkFactory), testLogger) {
            @Override
            int getRunningJdkMajor() {
                return 17;
            }
        };

        when(session.getService(Lookup.class)).thenReturn(lookup);
        when(lookup.lookupOptional(Project.class)).thenReturn(Optional.of(project));
        Model model = Model.newBuilder().build(Build.newBuilder().build()).build();
        when(project.getModel()).thenReturn(model);

        testManager.checkJdkSourceLevelCompatibility(session);

        verify(testLogger, never()).error(any(String.class), any(), any(), any());
    }

    @Test
    void checkCompatibilityRunningJdkSupportsLevel() {
        // Project targets source 11, running JDK 17 supports it — no error
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
                        .sources(List.of(Source.newBuilder().targetVersion("11").build()))
                        .build())
                .build();
        when(project.getModel()).thenReturn(model);

        testManager.checkJdkSourceLevelCompatibility(session);

        verify(testLogger, never()).error(any(String.class), any(), any(), any());
    }

    @Test
    void checkCompatibilityEmitsErrorWhenIncompatible() {
        // Project targets source 6, running JDK 17 doesn't support it — should emit error
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

        testManager.checkJdkSourceLevelCompatibility(session);

        verify(testLogger)
                .error(
                        "Project requires --source {} which needs JDK <= {}, but the running JDK {} no longer supports it.",
                        6,
                        11,
                        17);
    }

    @Test
    void checkCompatibilityEmitsErrorForSource5() {
        // Project targets source 5, running JDK 21 — max JDK is 8
        Logger testLogger = mock(Logger.class);
        DefaultToolchainManager testManager = new DefaultToolchainManager(Map.of("jdk", jdkFactory), testLogger) {
            @Override
            int getRunningJdkMajor() {
                return 21;
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

        testManager.checkJdkSourceLevelCompatibility(session);

        verify(testLogger)
                .error(
                        "Project requires --source {} which needs JDK <= {}, but the running JDK {} no longer supports it.",
                        5,
                        8,
                        21);
    }

    @Test
    void checkCompatibilityFromLegacyProperties() {
        // Project uses maven.compiler.release=6, running JDK 17
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

        testManager.checkJdkSourceLevelCompatibility(session);

        verify(testLogger)
                .error(
                        "Project requires --source {} which needs JDK <= {}, but the running JDK {} no longer supports it.",
                        6,
                        11,
                        17);
    }

    @Test
    void checkCompatibilityFromLegacySourceProperty() {
        // Project uses maven.compiler.source=1.6, running JDK 17
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

        testManager.checkJdkSourceLevelCompatibility(session);

        verify(testLogger)
                .error(
                        eq(
                                "Project requires --source {} which needs JDK <= {}, but the running JDK {} no longer supports it."),
                        eq(6),
                        eq(11),
                        eq(17));
    }

    @Test
    void getToolchainFromBuildContextChecksCompatibility() {
        // Verify getToolchainFromBuildContext calls compatibility check for jdk type
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

        Optional<Toolchain> result = testManager.getToolchainFromBuildContext(session, "jdk");

        // Should return empty (no auto-selection) but emit error
        assertTrue(result.isEmpty());
        verify(testLogger)
                .error(
                        "Project requires --source {} which needs JDK <= {}, but the running JDK {} no longer supports it.",
                        6,
                        11,
                        17);
    }

    @Test
    void getToolchainFromBuildContextReturnsExplicitToolchain() {
        // When an explicit toolchain is stored via storeToolchainToBuildContext,
        // it takes precedence — no compatibility check needed
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
    void getToolchainFromBuildContextNonJdkTypeNoCheck() {
        // Compatibility check should only apply to "jdk" type
        Map<String, Object> context = new ConcurrentHashMap<>();
        SessionData data = mock(SessionData.class);

        when(session.getService(Lookup.class)).thenReturn(lookup);
        when(lookup.lookupOptional(Project.class)).thenReturn(Optional.of(project));
        when(session.getData()).thenReturn(data);
        when(data.computeIfAbsent(any(), any())).thenReturn(context);

        Optional<Toolchain> result = manager.getToolchainFromBuildContext(session, "otherType");
        assertTrue(result.isEmpty());
    }

    @Test
    void getProjectRequiredSourceLevelTargetVersionTakesPrecedence() {
        // targetVersion in sources should take precedence over properties
        when(session.getService(Lookup.class)).thenReturn(lookup);
        when(lookup.lookupOptional(Project.class)).thenReturn(Optional.of(project));
        Model model = Model.newBuilder()
                .properties(Map.of("maven.compiler.release", "11"))
                .build(Build.newBuilder()
                        .sources(List.of(Source.newBuilder().targetVersion("8").build()))
                        .build())
                .build();
        when(project.getModel()).thenReturn(model);

        assertEquals(8, manager.getProjectRequiredSourceLevel(session));
    }

    @Test
    void getProjectRequiredSourceLevelNoProject() {
        when(session.getService(Lookup.class)).thenReturn(lookup);
        when(lookup.lookupOptional(Project.class)).thenReturn(Optional.empty());

        assertEquals(-1, manager.getProjectRequiredSourceLevel(session));
    }
}
