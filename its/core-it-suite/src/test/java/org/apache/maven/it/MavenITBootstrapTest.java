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

import org.apache.maven.shared.verifier.Verifier;
import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.junit.jupiter.api.Test;

/**
 * Core IT Bootstrapping: downloads from central repository every dependency (artifacts, plugins) required to let
 * ITs run without downloading anything later.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITBootstrapTest extends AbstractMavenIntegrationTestCase {
    public MavenITBootstrapTest() {
        super(ALL_MAVEN_VERSIONS);
    }

    /**
     * Bootstraps the integration tests by downloading required artifacts from central repository.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testBootstrap() throws Exception {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/bootstrap");

        Verifier verifier = newVerifier(testDir.getAbsolutePath(), "remote");
        verifier.setAutoclean(false);
        verifier.addCliArgument("--settings");
        verifier.addCliArgument("settings.xml");
        verifier.addCliArgument("-Dbootstrap="
                + getClass().getResource("/bootstrap.txt").toURI().getPath());

        // bootstrap plugin is bound to this phase, do not go further
        // important: maven-plugin packaging will fail at package phase, as there is no Mojo present!
        verifier.addCliArgument("process-resources");
        verifier.execute();
        verifier.verifyErrorFreeLog();
    }
}
