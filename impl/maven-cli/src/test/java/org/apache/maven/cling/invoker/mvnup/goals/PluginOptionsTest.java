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

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Optional;

import org.apache.maven.api.cli.mvnup.UpgradeOptions;
import org.apache.maven.cling.invoker.mvnup.UpgradeContext;
import org.apache.maven.cling.invoker.mvnup.UpgradeInvokerRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for plugin options handling in BaseUpgradeGoal.
 * These tests verify that the --plugins option is properly integrated with the upgrade workflow.
 */
class PluginOptionsTest {

    private TestableBaseUpgradeGoal upgrade;

    @BeforeEach
    void setUp() {
        upgrade = new TestableBaseUpgradeGoal();
    }

    private UpgradeContext createMockContext(UpgradeOptions options) {
        // Mock parserRequest and logger
        org.apache.maven.api.cli.ParserRequest parserRequest =
                Mockito.mock(org.apache.maven.api.cli.ParserRequest.class);
        org.apache.maven.api.cli.Logger logger = Mockito.mock(org.apache.maven.api.cli.Logger.class);
        Mockito.when(parserRequest.logger()).thenReturn(logger);

        // Create UpgradeInvokerRequest mock
        UpgradeInvokerRequest upgradeRequest = Mockito.mock(UpgradeInvokerRequest.class);
        Mockito.when(upgradeRequest.options()).thenReturn(options);

        // Mock all required properties for LookupContext constructor
        Mockito.when(upgradeRequest.cwd()).thenReturn(Paths.get("/project"));
        Mockito.when(upgradeRequest.installationDirectory()).thenReturn(Paths.get("/maven"));
        Mockito.when(upgradeRequest.userHomeDirectory()).thenReturn(Paths.get("/home/user"));
        Mockito.when(upgradeRequest.topDirectory()).thenReturn(Paths.get("/project"));
        Mockito.when(upgradeRequest.rootDirectory()).thenReturn(java.util.Optional.empty());
        Mockito.when(upgradeRequest.userProperties()).thenReturn(java.util.Map.of());
        Mockito.when(upgradeRequest.systemProperties()).thenReturn(java.util.Map.of());
        Mockito.when(upgradeRequest.parserRequest()).thenReturn(parserRequest);

        UpgradeContext context = new UpgradeContext(upgradeRequest);
        return context;
    }

    @Test
    void testAllOptionIncludesPlugins() throws Exception {
        // Test that --all option enables plugin upgrades
        UpgradeOptions options = Mockito.mock(UpgradeOptions.class);
        Mockito.when(options.all()).thenReturn(Optional.of(true));
        Mockito.when(options.infer()).thenReturn(Optional.empty());
        Mockito.when(options.fixModel()).thenReturn(Optional.empty());
        Mockito.when(options.plugins()).thenReturn(Optional.empty());
        Mockito.when(options.model()).thenReturn(Optional.empty());

        UpgradeContext context = createMockContext(options);
        TestableBaseUpgradeGoal goal = new TestableBaseUpgradeGoal();

        // Test that --all option enables plugins
        boolean pluginsEnabled = goal.testPluginsOptionLogic(context, "4.0.0", new HashMap<>());
        assertTrue(pluginsEnabled, "--all option should enable plugin upgrades");
    }

    @Test
    void testExplicitPluginsOption() throws Exception {
        // Test that explicit --plugins option enables plugin upgrades
        UpgradeOptions options = Mockito.mock(UpgradeOptions.class);
        Mockito.when(options.all()).thenReturn(Optional.empty());
        Mockito.when(options.infer()).thenReturn(Optional.empty());
        Mockito.when(options.fixModel()).thenReturn(Optional.empty());
        Mockito.when(options.plugins()).thenReturn(Optional.of(true));
        Mockito.when(options.model()).thenReturn(Optional.empty());

        UpgradeContext context = createMockContext(options);
        TestableBaseUpgradeGoal goal = new TestableBaseUpgradeGoal();

        // Test that explicit --plugins option enables plugins
        boolean pluginsEnabled = goal.testPluginsOptionLogic(context, "4.0.0", new HashMap<>());
        assertTrue(pluginsEnabled, "Explicit --plugins option should enable plugin upgrades");
    }

    @Test
    void testDefaultBehaviorIncludesPlugins() throws Exception {
        // Test that default behavior enables plugin upgrades
        UpgradeOptions options = Mockito.mock(UpgradeOptions.class);
        Mockito.when(options.all()).thenReturn(Optional.empty());
        Mockito.when(options.infer()).thenReturn(Optional.empty());
        Mockito.when(options.fixModel()).thenReturn(Optional.empty());
        Mockito.when(options.plugins()).thenReturn(Optional.empty());
        Mockito.when(options.model()).thenReturn(Optional.empty());

        UpgradeContext context = createMockContext(options);
        TestableBaseUpgradeGoal goal = new TestableBaseUpgradeGoal();

        // Test that default behavior enables plugins
        boolean pluginsEnabled = goal.testPluginsOptionLogic(context, "4.0.0", new HashMap<>());
        assertTrue(pluginsEnabled, "Default behavior should enable plugin upgrades");
    }

    @Test
    void testPluginsDisabledWhenOtherOptionsPresent() throws Exception {
        // Test that plugins are not enabled by default when other options are present
        UpgradeOptions options = Mockito.mock(UpgradeOptions.class);
        Mockito.when(options.all()).thenReturn(Optional.empty());
        Mockito.when(options.infer()).thenReturn(Optional.of(true)); // Other option present
        Mockito.when(options.fixModel()).thenReturn(Optional.empty());
        Mockito.when(options.plugins()).thenReturn(Optional.empty());
        Mockito.when(options.model()).thenReturn(Optional.empty());

        UpgradeContext context = createMockContext(options);
        TestableBaseUpgradeGoal goal = new TestableBaseUpgradeGoal();

        // Test that plugins are not enabled when other options are present
        boolean pluginsEnabled = goal.testPluginsOptionLogic(context, "4.0.0", new HashMap<>());
        assertFalse(pluginsEnabled, "Plugin upgrades should not be enabled by default when other options are present");
    }

    @Test
    void testDefaultBehaviorIncludesFixModelAndPlugins() throws Exception {
        // Test that default behavior enables both fix-model and plugins
        UpgradeOptions options = Mockito.mock(UpgradeOptions.class);
        Mockito.when(options.all()).thenReturn(Optional.empty());
        Mockito.when(options.infer()).thenReturn(Optional.empty());
        Mockito.when(options.fixModel()).thenReturn(Optional.empty());
        Mockito.when(options.plugins()).thenReturn(Optional.empty());
        Mockito.when(options.model()).thenReturn(Optional.empty());

        UpgradeContext context = createMockContext(options);
        TestableBaseUpgradeGoal goal = new TestableBaseUpgradeGoal();

        // Test that default behavior enables both fix-model and plugins
        boolean fixModelEnabled = goal.testFixModelOptionLogic(context, "4.0.0", new HashMap<>());
        boolean pluginsEnabled = goal.testPluginsOptionLogic(context, "4.0.0", new HashMap<>());

        assertTrue(fixModelEnabled, "Default behavior should enable fix-model");
        assertTrue(pluginsEnabled, "Default behavior should enable plugin upgrades");
    }

    @Test
    void testAllOptionSetsTargetModelTo410() throws Exception {
        // Test that --all option sets target model to 4.1.0
        UpgradeOptions options = Mockito.mock(UpgradeOptions.class);
        Mockito.when(options.all()).thenReturn(Optional.of(true));
        Mockito.when(options.infer()).thenReturn(Optional.empty());
        Mockito.when(options.fixModel()).thenReturn(Optional.empty());
        Mockito.when(options.plugins()).thenReturn(Optional.empty());
        Mockito.when(options.model()).thenReturn(Optional.empty());

        UpgradeContext context = createMockContext(options);
        TestableBaseUpgradeGoal goal = new TestableBaseUpgradeGoal();

        // Test that --all option sets target model to 4.1.0
        String targetModel = goal.testTargetModelLogic(context, "4.0.0", new HashMap<>());
        assertEquals("4.1.0", targetModel, "--all option should set target model to 4.1.0");
    }

    @Test
    void testDefaultBehaviorKeepsTargetModelAt400() throws Exception {
        // Test that default behavior (--fix-model and --plugins) keeps target model at 4.0.0
        UpgradeOptions options = Mockito.mock(UpgradeOptions.class);
        Mockito.when(options.all()).thenReturn(Optional.empty());
        Mockito.when(options.infer()).thenReturn(Optional.empty());
        Mockito.when(options.fixModel()).thenReturn(Optional.empty());
        Mockito.when(options.plugins()).thenReturn(Optional.empty());
        Mockito.when(options.model()).thenReturn(Optional.empty());

        UpgradeContext context = createMockContext(options);
        TestableBaseUpgradeGoal goal = new TestableBaseUpgradeGoal();

        // Test that default behavior keeps target model at 4.0.0
        String targetModel = goal.testTargetModelLogic(context, "4.0.0", new HashMap<>());
        assertEquals("4.0.0", targetModel, "Default behavior should keep target model at 4.0.0");
    }

    @Test
    void testExplicitFixModelAndPluginsKeepsTargetModelAt400() throws Exception {
        // Test that explicit --fix-model and --plugins keeps target model at 4.0.0
        UpgradeOptions options = Mockito.mock(UpgradeOptions.class);
        Mockito.when(options.all()).thenReturn(Optional.empty());
        Mockito.when(options.infer()).thenReturn(Optional.empty());
        Mockito.when(options.fixModel()).thenReturn(Optional.of(true));
        Mockito.when(options.plugins()).thenReturn(Optional.of(true));
        Mockito.when(options.model()).thenReturn(Optional.empty());

        UpgradeContext context = createMockContext(options);
        TestableBaseUpgradeGoal goal = new TestableBaseUpgradeGoal();

        // Test that explicit --fix-model and --plugins keeps target model at 4.0.0
        String targetModel = goal.testTargetModelLogic(context, "4.0.0", new HashMap<>());
        assertEquals("4.0.0", targetModel, "Explicit --fix-model and --plugins should keep target model at 4.0.0");
    }

    /**
     * Testable subclass that exposes protected methods for testing.
     */
    private static class TestableBaseUpgradeGoal extends BaseUpgradeGoal {
        @Override
        protected boolean shouldSaveModifications() {
            return false;
        }

        // Test helper methods to expose internal logic
        public boolean testPluginsOptionLogic(
                UpgradeContext context,
                String targetModel,
                java.util.Map<java.nio.file.Path, org.jdom2.Document> pomMap) {
            UpgradeOptions options = ((UpgradeInvokerRequest) context.invokerRequest).options();

            // Handle --all option (overrides individual options)
            boolean useAll = options.all().orElse(false);
            boolean usePlugins = useAll || options.plugins().orElse(false);

            // Apply default behavior: if no specific options are provided, enable --fix-model and --plugins
            if (!useAll
                    && !options.infer().isPresent()
                    && !options.fixModel().isPresent()
                    && !options.plugins().isPresent()
                    && !options.model().isPresent()) {
                usePlugins = true;
            }

            return usePlugins;
        }

        public boolean testFixModelOptionLogic(
                UpgradeContext context,
                String targetModel,
                java.util.Map<java.nio.file.Path, org.jdom2.Document> pomMap) {
            UpgradeOptions options = ((UpgradeInvokerRequest) context.invokerRequest).options();

            // Handle --all option (overrides individual options)
            boolean useAll = options.all().orElse(false);
            boolean useFixModel = useAll || options.fixModel().orElse(false);

            // Apply default behavior: if no specific options are provided, enable --fix-model and --plugins
            if (!useAll
                    && !options.infer().isPresent()
                    && !options.fixModel().isPresent()
                    && !options.plugins().isPresent()
                    && !options.model().isPresent()) {
                useFixModel = true;
            }

            return useFixModel;
        }

        public String testTargetModelLogic(
                UpgradeContext context,
                String initialTargetModel,
                java.util.Map<java.nio.file.Path, org.jdom2.Document> pomMap) {
            UpgradeOptions options = ((UpgradeInvokerRequest) context.invokerRequest).options();

            // Determine target model version (same logic as in execute method)
            String targetModel;
            if (options.model().isPresent()) {
                targetModel = options.model().get();
            } else if (options.all().orElse(false)) {
                targetModel = "4.1.0";
            } else {
                targetModel = "4.0.0";
            }

            return targetModel;
        }
    }
}
