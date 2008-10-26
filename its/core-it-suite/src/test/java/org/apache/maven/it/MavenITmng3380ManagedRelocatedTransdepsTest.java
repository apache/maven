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

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

/**
 * expected project.getArtifacts() results:
 *
 * direct-dependency-groupId:direct-dependency-artifactId:jar:1:compile
 * transitive-dependency-new-groupId:transitive-dependency-artifactId:jar:2:compile
 * other-groupId:other-artifactId-a:jar:1:compile
 * other-groupId:other-artifactId-b:jar:1:compile
 *
 * org.apache.maven.project.MavenProject#.getArtifacts() is called with goal:
 * org.apache.maven.its:mng3380.plugin:mng-3380-test
 *
 */
public class MavenITmng3380ManagedRelocatedTransdepsTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng3380ManagedRelocatedTransdepsTest()
    {
        super("(2.0.9,)");
    }

    public void testitMNG3380()
        throws Exception 
    {

        // compute test directory
        File testDir = ResourceExtractor.simpleExtractResources(getClass(),
                "/mng-3380");

        Verifier verifier = new Verifier(testDir.getAbsolutePath());

        deleteArtifacts( verifier );

        installDependencies( testDir );

        String path = testDir.getAbsolutePath() //
                + "/consumer";

        verifier = new Verifier(path);
        verifier.executeGoal("package");

        // verify no errors so far
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }

    private void installDependencies( File testDir )
        throws Exception
    {
        // install projects
        String path = testDir.getAbsolutePath() //
                + "/other-c";
        Verifier verifier = new Verifier(path);
        verifier.executeGoal("install");

        path = testDir.getAbsolutePath() //
                + "/other-b";
        verifier = new Verifier(path);
        verifier.executeGoal("install");

        path = testDir.getAbsolutePath() //
                + "/other-a";
        verifier = new Verifier(path);
        verifier.executeGoal("install");

        path = testDir.getAbsolutePath() //
                + "/transdep-old";
        verifier = new Verifier(path);
        verifier.executeGoal("install");

        path = testDir.getAbsolutePath() //
                + "/transdep-new-1";
        verifier = new Verifier(path);
        verifier.executeGoal("install");

        path = testDir.getAbsolutePath() //
                + "/transdep-new-2";
        verifier = new Verifier(path);
        verifier.executeGoal("install");

        path = testDir.getAbsolutePath() //
                + "/direct-dep";
        verifier = new Verifier(path);
        verifier.executeGoal("install");
    }

    private void deleteArtifacts( Verifier verifier )
        throws Exception
    {
        // delete projects
        verifier.deleteArtifact( //
                "other-groupId", //
                "other-artifactId-c", //
                "1", //
                "jar");
        verifier.deleteArtifact( //
                "other-groupId", //
                "other-artifactId-c", //
                "1", //
                "pom");
        verifier.assertArtifactNotPresent( //
                "other-groupId", //
                "other-artifactId-c", //
                "1", //
                "jar");
        verifier.assertArtifactNotPresent( //
                "other-groupId", //
                "other-artifactId-c", //
                "1", //
                "pom");

        verifier.deleteArtifact( //
                "other-groupId", //
                "other-artifactId-b", //
                "1", //
                "jar");
        verifier.deleteArtifact( //
                "other-groupId", //
                "other-artifactId-b", //
                "1", //
                "pom");
        verifier.assertArtifactNotPresent( //
                "other-groupId", //
                "other-artifactId-b", //
                "1", //
                "jar");
        verifier.assertArtifactNotPresent( //
                "other-groupId", //
                "other-artifactId-b", //
                "1", //
                "pom");

        verifier.deleteArtifact( //
                "other-groupId", //
                "other-artifactId-a", //
                "1", //
                "jar");
        verifier.deleteArtifact( //
                "other-groupId", //
                "other-artifactId-a", //
                "1", //
                "pom");
        verifier.assertArtifactNotPresent( //
                "other-groupId", //
                "other-artifactId-a", //
                "1", //
                "jar");
        verifier.assertArtifactNotPresent( //
                "other-groupId", //
                "other-artifactId-a", //
                "1", //
                "pom");

        verifier.deleteArtifact( //
                "transitive-dependency-old-groupId", //
                "transitive-dependency-artifactId", //
                "1", //
                "jar");
        verifier.deleteArtifact( //
                "transitive-dependency-old-groupId", //
                "transitive-dependency-artifactId", //
                "1", //
                "pom");
        verifier.assertArtifactNotPresent( //
                "transitive-dependency-old-groupId", //
                "transitive-dependency-artifactId", //
                "1", //
                "jar");
        verifier.assertArtifactNotPresent( //
                "transitive-dependency-old-groupId", //
                "transitive-dependency-artifactId", //
                "1", //
                "pom");

        verifier.deleteArtifact( //
                "transitive-dependency-new-groupId", //
                "transitive-dependency-artifactId", //
                "1", //
                "jar");
        verifier.deleteArtifact( //
                "transitive-dependency-new-groupId", //
                "transitive-dependency-artifactId", //
                "1", //
                "pom");
        verifier.assertArtifactNotPresent( //
                "transitive-dependency-new-groupId", //
                "transitive-dependency-artifactId", //
                "1", //
                "jar");
        verifier.assertArtifactNotPresent( //
                "transitive-dependency-new-groupId", //
                "transitive-dependency-artifactId", //
                "1", //
                "pom");

        verifier.deleteArtifact( //
                "transitive-dependency-new-groupId", //
                "transitive-dependency-artifactId", //
                "2", //
                "jar");
        verifier.deleteArtifact( //
                "transitive-dependency-new-groupId", //
                "transitive-dependency-artifactId", //
                "2", //
                "pom");
        verifier.assertArtifactNotPresent( //
                "transitive-dependency-new-groupId", //
                "transitive-dependency-artifactId", //
                "2", //
                "jar");
        verifier.assertArtifactNotPresent( //
                "transitive-dependency-new-groupId", //
                "transitive-dependency-artifactId", //
                "2", //
                "pom");

        verifier.deleteArtifact( //
                "direct-dependency-groupId", //
                "direct-dependency-artifactId", //
                "1", //
                "jar");
        verifier.deleteArtifact( //
                "direct-dependency-groupId", //
                "direct-dependency-artifactId", //
                "1", //
                "pom");
        verifier.assertArtifactNotPresent( //
                "direct-dependency-groupId", //
                "direct-dependency-artifactId", //
                "1", //
                "jar");
        verifier.assertArtifactNotPresent( //
                "direct-dependency-groupId", //
                "direct-dependency-artifactId", //
                "1", //
                "pom");

        verifier.deleteArtifact( //
                "root-groupId", //
                "root-artifactId", //
                "1", //
                "jar");
        verifier.deleteArtifact( //
                "root-groupId", //
                "root-artifactId", //
                "1", //
                "pom");
        verifier.assertArtifactNotPresent( //
                "root-groupId", //
                "root-artifactId", //
                "1", //
                "jar");
        verifier.assertArtifactNotPresent( //
                "root-groupId", //
                "root-artifactId", //
                "1", //
                "pom");
    }
}
