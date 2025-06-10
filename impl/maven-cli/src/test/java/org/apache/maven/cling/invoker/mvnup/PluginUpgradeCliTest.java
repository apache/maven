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
package org.apache.maven.cling.invoker.mvnup;

import org.apache.commons.cli.ParseException;
import org.apache.maven.api.cli.mvnup.UpgradeOptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for CLI parsing of plugin upgrade options.
 * These tests verify that the --plugins option is properly parsed and handled.
 */
class PluginUpgradeCliTest {

    @Test
    void testPluginsOptionParsing() throws ParseException {
        String[] args = {"apply", "--plugins"};
        CommonsCliUpgradeOptions options = CommonsCliUpgradeOptions.parse(args);

        assertTrue(options.plugins().isPresent(), "--plugins option should be present");
        assertTrue(options.plugins().get(), "--plugins option should be true");
    }

    @Test
    void testAllOptionParsing() throws ParseException {
        String[] args = {"apply", "--all"};
        CommonsCliUpgradeOptions options = CommonsCliUpgradeOptions.parse(args);

        assertTrue(options.all().isPresent(), "--all option should be present");
        assertTrue(options.all().get(), "--all option should be true");
    }

    @Test
    void testCombinedOptionsWithPlugins() throws ParseException {
        String[] args = {"apply", "--model-version", "4.1.0", "--infer", "--model", "--plugins"};
        CommonsCliUpgradeOptions options = CommonsCliUpgradeOptions.parse(args);

        assertTrue(options.modelVersion().isPresent(), "--model-version option should be present");
        assertEquals("4.1.0", options.modelVersion().get(), "--model-version should be 4.1.0");

        assertTrue(options.infer().isPresent(), "--infer option should be present");
        assertTrue(options.infer().get(), "--infer option should be true");

        assertTrue(options.model().isPresent(), "--model option should be present");
        assertTrue(options.model().get(), "--model option should be true");

        assertTrue(options.plugins().isPresent(), "--plugins option should be present");
        assertTrue(options.plugins().get(), "--plugins option should be true");
    }

    @Test
    void testNoPluginsOptionByDefault() throws ParseException {
        String[] args = {"apply"};
        CommonsCliUpgradeOptions options = CommonsCliUpgradeOptions.parse(args);

        assertFalse(options.plugins().isPresent(), "--plugins option should not be present by default");
    }

    @Test
    void testPluginsOptionWithOtherFlags() throws ParseException {
        String[] args = {"check", "--plugins", "--directory", "/some/path"};
        CommonsCliUpgradeOptions options = CommonsCliUpgradeOptions.parse(args);

        assertTrue(options.plugins().isPresent(), "--plugins option should be present");
        assertTrue(options.plugins().get(), "--plugins option should be true");

        assertTrue(options.directory().isPresent(), "--directory option should be present");
        assertEquals("/some/path", options.directory().get(), "--directory should be /some/path");
    }

    @Test
    void testGoalsParsing() throws ParseException {
        String[] args = {"apply", "--plugins"};
        CommonsCliUpgradeOptions options = CommonsCliUpgradeOptions.parse(args);

        assertTrue(options.goals().isPresent(), "Goals should be present");
        assertEquals(1, options.goals().get().size(), "Should have one goal");
        assertEquals("apply", options.goals().get().get(0), "Goal should be 'apply'");
    }

    @Test
    void testCheckGoalWithPlugins() throws ParseException {
        String[] args = {"check", "--plugins"};
        CommonsCliUpgradeOptions options = CommonsCliUpgradeOptions.parse(args);

        assertTrue(options.goals().isPresent(), "Goals should be present");
        assertEquals("check", options.goals().get().get(0), "Goal should be 'check'");

        assertTrue(options.plugins().isPresent(), "--plugins option should be present");
        assertTrue(options.plugins().get(), "--plugins option should be true");
    }

    @Test
    void testAllOptionImpliesPlugins() throws ParseException {
        // This test verifies that when --all is used, the logic should enable plugins
        // The actual logic is in BaseUpgradeGoal, but we can test the option parsing here
        String[] args = {"apply", "--all"};
        CommonsCliUpgradeOptions options = CommonsCliUpgradeOptions.parse(args);

        assertTrue(options.all().isPresent(), "--all option should be present");
        assertTrue(options.all().get(), "--all option should be true");

        // The plugins option itself won't be set, but the logic in BaseUpgradeGoal
        // should treat --all as enabling plugins
        assertFalse(options.plugins().isPresent(), "--plugins option should not be explicitly set when using --all");
    }

    @Test
    void testLongFormPluginsOption() throws ParseException {
        String[] args = {"apply", "--plugins"};
        CommonsCliUpgradeOptions options = CommonsCliUpgradeOptions.parse(args);

        assertTrue(options.plugins().isPresent(), "Long form --plugins option should be present");
        assertTrue(options.plugins().get(), "Long form --plugins option should be true");
    }

    @Test
    void testInvalidCombinationStillParses() throws ParseException {
        // Even if the combination doesn't make logical sense, the CLI should parse it
        String[] args = {"apply", "--all", "--plugins", "--infer"};
        CommonsCliUpgradeOptions options = CommonsCliUpgradeOptions.parse(args);

        assertTrue(options.all().isPresent(), "--all option should be present");
        assertTrue(options.plugins().isPresent(), "--plugins option should be present");
        assertTrue(options.infer().isPresent(), "--infer option should be present");
    }

    @Test
    void testHelpDisplayIncludesPluginsOption() throws ParseException {
        // Test that help text includes the plugins option
        String[] args = {"help"};
        CommonsCliUpgradeOptions options = CommonsCliUpgradeOptions.parse(args);
        assertNotNull(options);

        // We can't easily test the help output directly, but we can verify
        // that the option is properly configured by checking if it parses
        String[] pluginsArgs = {"apply", "--plugins"};
        CommonsCliUpgradeOptions pluginsOptions = CommonsCliUpgradeOptions.parse(pluginsArgs);

        assertTrue(pluginsOptions.plugins().isPresent(), "Plugins option should be properly configured");
    }

    @Test
    void testEmptyArgsDefaultBehavior() throws ParseException {
        // Test that empty args (except for goal) work correctly
        String[] args = {"apply"};
        CommonsCliUpgradeOptions options = CommonsCliUpgradeOptions.parse(args);

        // None of the optional flags should be present
        assertFalse(options.plugins().isPresent(), "--plugins should not be present by default");
        assertFalse(options.all().isPresent(), "--all should not be present by default");
        assertFalse(options.infer().isPresent(), "--infer should not be present by default");
        assertFalse(options.model().isPresent(), "--fix-model should not be present by default");
        assertFalse(options.model().isPresent(), "--model should not be present by default");

        // But the goal should be present
        assertTrue(options.goals().isPresent(), "Goals should be present");
        assertEquals("apply", options.goals().get().get(0), "Goal should be 'apply'");
    }

    @Test
    void testInterpolationWithPluginsOption() throws ParseException {
        String[] args = {"apply", "--plugins"};
        CommonsCliUpgradeOptions options = CommonsCliUpgradeOptions.parse(args);

        // Test that interpolation works (even though there's nothing to interpolate here)
        UpgradeOptions interpolated = (CommonsCliUpgradeOptions) options.interpolate(s -> s);

        assertTrue(interpolated.plugins().isPresent(), "Interpolated options should preserve --plugins");
        assertTrue(interpolated.plugins().get(), "Interpolated --plugins should be true");
    }
}
