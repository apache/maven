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

/**
 * The usage of a <code>${revision}</code> for the version in the pom file and furthermore
 * defining the property in the pom file and overwrite it via command line.
 * <a href="https://issues.apache.org/jira/browse/MNG-5895">MNG-5895</a>.
 *
 * This will result in a failure without the fix for this issue.
 *
 * @author Karl Heinz Marbaise khmarbaise@apache.org
 */
public class MavenITmng5895CIFriendlyUsageWithPropertyTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng5895CIFriendlyUsageWithPropertyTest() {
        // The first version which contains the fix for the MNG-issue.
        // TODO: Think about it!
        super("[3.5.0-alpha-2,)");
    }

    /**
     * Check that the resulting run will not fail in case
     * of defining the property via command line which is
     * already defined inside the pom.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitShouldResolveTheDependenciesWithoutBuildConsumer() throws Exception {
        File testDir = extractResources("/mng-5895-ci-friendly-usage-with-property");

        Verifier verifier = newVerifier(testDir.getAbsolutePath(), false);
        verifier.setAutoclean(false);

        // verifier.setLogFileName( "log-only.txt" );
        verifier.addCliArgument("-Drevision=1.2");
        verifier.addCliArgument("-Dmaven.consumerpom=false");
        verifier.addCliArgument("clean");
        verifier.execute();
        verifier.addCliArgument("package");
        verifier.execute();
        verifier.verifyErrorFreeLog();
    }

    @Test
    public void testitShouldResolveTheDependenciesWithBuildConsumer() throws Exception {
        File testDir = extractResources("/mng-5895-ci-friendly-usage-with-property");

        Verifier verifier = newVerifier(testDir.getAbsolutePath(), false);
        verifier.setAutoclean(false);

        verifier.setLogFileName("log-bc.txt");
        verifier.addCliArgument("-Drevision=1.2");
        verifier.addCliArgument("-Dmaven.consumerpom=true");
        verifier.addCliArgument("clean");
        verifier.execute();
        verifier.addCliArgument("package");
        verifier.execute();
        verifier.verifyErrorFreeLog();
    }
}
