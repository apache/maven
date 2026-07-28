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
package org.apache.maven.it;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

/**
 * This is a test for <a href="https://github.com/apache/maven/issues/12522">GH-12522</a>.
 *
 * Verifies that plugins that are NOT configured with {@code <extensions>true</extensions>}
 * do not cause build failures when they ship JSR330 components whose internal dependencies
 * cannot be resolved in Maven's container.
 *
 * <p>Prior to Sisu 1.1.0, {@code @Inject List<T>} produced an unfiltered live list backed
 * by the global {@code BeanLocator}, so JSR330 components discovered from non-extension
 * plugin realms would leak into core Maven injection points (e.g.
 * {@code List<ProjectExecutionListener>} in {@code LifecycleModuleBuilder}). Lazy
 * provisioning of such components would fail when their plugin-internal dependencies
 * were not bound in Maven's container, aborting the build.</p>
 *
 * <p>Sisu 1.1.0's {@code jsr330ComponentVisibilityFollowsPlexusVisibility} feature applies
 * realm-based filtering to JSR330 bean lookups, matching the filtering already applied
 * by the Plexus compatibility layer. Non-extension plugin components are no longer
 * visible from the container realm.</p>
 *
 * <p>The reproducer uses {@code tycho-bnd-plugin} which ships a
 * {@code ProjectExecutionListener} ({@code BndProjectExecutionListener}) as a
 * {@code @Named} JSR330 component but is not configured as an extension in the POM.</p>
 */
class MavenITgh12522NonExtensionPluginTest extends AbstractMavenIntegrationTestCase {

    MavenITgh12522NonExtensionPluginTest() {
        super("[4.0.0-alpha-1,)");
    }

    /**
     * Verify that a build using a plugin with JSR330 components (tycho-bnd-plugin)
     * that is NOT marked as an extension completes successfully. With Sisu 1.1.0's
     * realm-based JSR330 filtering, the plugin's {@code ProjectExecutionListener}
     * is not visible from the container realm and should not be provisioned at all.
     */
    @Test
    @EnabledForJreRange(min = JRE.JAVA_21, disabledReason = "tycho-bnd-plugin 5.0.3 requires Java 21")
    void testNonExtensionPluginComponentsNotPickedUp() throws Exception {
        File testDir = extractResources("/gh-12522-non-extension-plugin");

        Verifier verifier = newVerifier(testDir.getAbsolutePath(), "remote");
        verifier.addCliArgument("process-classes");
        verifier.execute();
        verifier.verifyErrorFreeLog();
    }
}
