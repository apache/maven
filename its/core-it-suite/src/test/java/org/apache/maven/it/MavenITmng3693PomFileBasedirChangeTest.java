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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3693">MNG-3693</a>:
 * it tests that changes to a project's POM file reference (MavenProject.setFile(..))
 * doesn't affect the basedir of the project instance for using that project's classes directory
 * in the classpath of another project's build...this happens when both projects are
 * built in the same reactor, and one project depends on the other.
 *
 * @author jdcasey
 */
public class MavenITmng3693PomFileBasedirChangeTest extends AbstractMavenIntegrationTestCase {

    @Test
    public void testitMNG3693() throws Exception {
        Path testDir = extractResources("mng-3693");

        Path pluginDir = testDir.resolve("maven-mng3693-plugin");
        Path projectsDir = testDir.resolve("projects");

        Verifier verifier = newVerifier(pluginDir);

        verifier.addCliArgument("install");
        verifier.execute();

        verifier.verifyErrorFreeLog();

        Path depPath = verifier.getArtifactPath("org.apache.maven.its.mng3693", "dep", "1", "pom");

        Path dep = depPath.getParent().getParent();

        // remove the dependency from the local repository.
        ItUtils.deleteDirectory(dep);

        verifier = newVerifier(projectsDir);

        verifier.addCliArgument("package");
        verifier.execute();

        verifier.verifyErrorFreeLog();
    }
}
