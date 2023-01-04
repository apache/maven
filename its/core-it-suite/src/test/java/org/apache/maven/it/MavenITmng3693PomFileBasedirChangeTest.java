package org.apache.maven.it;

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

import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.apache.maven.shared.verifier.Verifier;

import java.io.File;

import org.apache.maven.shared.utils.io.FileUtils;
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
public class MavenITmng3693PomFileBasedirChangeTest
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng3693PomFileBasedirChangeTest()
    {
        super( ALL_MAVEN_VERSIONS );
    }

    @Test
    public void testitMNG3693 ()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3693" );

        File pluginDir = new File( testDir, "maven-mng3693-plugin" );
        File projectsDir = new File( testDir, "projects" );

        Verifier verifier = newVerifier( pluginDir.getAbsolutePath(), "remote" );

        verifier.executeGoal( "install" );

        verifier.verifyErrorFreeLog();

        String depPath = verifier.getArtifactPath( "org.apache.maven.its.mng3693", "dep", "1", "pom" );

        File dep = new File( depPath );
        dep = dep.getParentFile().getParentFile();

        // remove the dependency from the local repository.
        FileUtils.deleteDirectory( dep );

        verifier = newVerifier( projectsDir.getAbsolutePath() );

        verifier.executeGoal( "package" );

        verifier.verifyErrorFreeLog();

    }
}
