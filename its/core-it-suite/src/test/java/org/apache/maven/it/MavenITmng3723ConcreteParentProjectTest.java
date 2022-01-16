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

import java.io.File;

import org.apache.maven.it.util.ResourceExtractor;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3723">MNG-3723</a>.
 *
 * Tests that the parent MavenProject instance of the current project has been
 * transitioned into a concrete state before a mojo is executed. If this is true,
 * then injecting ${project} into the mojo and traversing to project.getParent().getBasedir()
 * should retrieve a File instance that is interpolated and exists (since we're
 * going to be using a parent-child project set in the projects directory).
 *
 * @author jdcasey
 *
 */
public class MavenITmng3723ConcreteParentProjectTest
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng3723ConcreteParentProjectTest()
    {
        super( "(2.0.8,)" ); // only test in 2.0.9+
    }

    public void testitMNG3723 ()
        throws Exception
    {
        // The testdir is computed from the location of this
        // file.
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3723" );
        File pluginDir = new File( testDir, "maven-mng3723-plugin" );
        File projectDir = new File( testDir, "projects" );

        Verifier verifier;

        verifier = newVerifier( pluginDir.getAbsolutePath(), "remote" );

        verifier.executeGoal( "install" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier = newVerifier( projectDir.getAbsolutePath() );

        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }
}
