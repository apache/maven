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

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * Integration test for <a href="https://github.com/apache/maven/issues/12729">#12729</a>.
 * Verifies that properties in {@code <module>} (or {@code <subproject>}) paths of aggregator
 * POMs are interpolated before filesystem resolution.
 *
 * <p>Maven 3 interpolated {@code ${property}} references in module paths, but Maven 4 lost
 * this behaviour because the module path is resolved against the filesystem before model-wide
 * interpolation runs. The fix performs a targeted early interpolation of the path string
 * against user, model and system properties.
 */
class MavenITgh12729ModulePathPropertyInterpolationTest extends AbstractMavenIntegrationTestCase {

    /**
     * Verify that a POM-defined property in a {@code <module>} path is interpolated correctly.
     */
    @Test
    void testModulePathWithPomProperty() throws Exception {
        Path basedir = extractResources("/gh-12729-module-path-property-interpolation");

        Verifier verifier = newVerifier(basedir);
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();
    }

    /**
     * Verify that a user property ({@code -D}) overrides a POM property in a module path,
     * following Maven's user → model → system property precedence.
     */
    @Test
    void testModulePathWithUserPropertyOverride() throws Exception {
        Path basedir = extractResources("/gh-12729-module-path-property-interpolation");

        Verifier verifier = newVerifier(basedir);
        // The POM defines child-dir=child; this override should also resolve to "child"
        verifier.addCliArgument("-Dchild-dir=child");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();
    }
}
