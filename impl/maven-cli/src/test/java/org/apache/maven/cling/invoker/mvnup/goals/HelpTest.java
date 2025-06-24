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

import org.apache.maven.cling.invoker.mvnup.UpgradeContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for the Help goal.
 */
class HelpTest {

    private Help help;

    @BeforeEach
    void setUp() {
        help = new Help();
    }

    private UpgradeContext createMockContext() {
        return TestUtils.createMockContext();
    }

    @Test
    void testHelpExecuteReturnsZero() throws Exception {
        UpgradeContext context = createMockContext();

        int result = help.execute(context);

        assertEquals(0, result, "Help goal should return 0 (success)");
    }

    @Test
    void testHelpExecuteDoesNotThrow() throws Exception {
        UpgradeContext context = createMockContext();

        // Should not throw any exceptions
        assertDoesNotThrow(() -> help.execute(context));
    }

    @Test
    void testHelpLogsMessages() throws Exception {
        UpgradeContext context = createMockContext();

        help.execute(context);

        // Verify that logger.info was called multiple times
        // We can't easily verify the exact content without capturing the logger output,
        // but we can verify that the method executes without errors
        Mockito.verify(context.logger, Mockito.atLeastOnce()).info(Mockito.anyString());
    }

    @Test
    void testHelpIncludesPluginsOption() throws Exception {
        UpgradeContext context = createMockContext();

        help.execute(context);

        // Verify that the plugins option is mentioned in the help output
        Mockito.verify(context.logger).info("      --plugins         Upgrade plugins known to fail with Maven 4");
    }

    @Test
    void testHelpIncludesAllOption() throws Exception {
        UpgradeContext context = createMockContext();

        help.execute(context);

        // Verify that the --all option is mentioned with correct description
        Mockito.verify(context.logger)
                .info(
                        "  -a, --all             Apply all upgrades (equivalent to --model-version 4.1.0 --infer --model --plugins)");
    }

    @Test
    void testHelpIncludesDefaultBehavior() throws Exception {
        UpgradeContext context = createMockContext();

        help.execute(context);

        // Verify that the default behavior is explained
        Mockito.verify(context.logger)
                .info("Default behavior: --model and --plugins are applied if no other options are specified");
    }

    @Test
    void testHelpIncludesForceAndYesOptions() throws Exception {
        UpgradeContext context = createMockContext();

        help.execute(context);

        // Verify that --force and --yes options are included
        Mockito.verify(context.logger).info("  -f, --force           Overwrite files without asking for confirmation");
        Mockito.verify(context.logger).info("  -y, --yes             Answer \"yes\" to all prompts automatically");
    }
}
