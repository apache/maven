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

import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.util.List;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4842">MNG-4842</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4842ParentResolutionOfDependencyPomTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4842ParentResolutionOfDependencyPomTest()
    {
        super( "[2.0.3,3.0-alpha-1),[3.0,)" );
    }

    /**
     * Verify that resolution of parent POMs for dependency POMs treats the remote repositories of the current
     * resolution request as dominant when merging with any repositories declared in the dependency POM. This
     * variant of the test checks dependency resolution by the core.
     *
     * @throws Exception in case of failure
     */
    public void testitCore()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4842" );

        Verifier verifier = newVerifier( new File( testDir, "core" ).getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng4842" );
        verifier.addCliOption( "-s" );
        verifier.addCliOption( "settings.xml" );
        verifier.filterFile( "../settings-template.xml", "settings.xml", "UTF-8", verifier.newDefaultFilterProperties() );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        List<String> compile = verifier.loadLines( "target/compile.txt", "UTF-8" );

        assertTrue( compile.toString(), compile.contains( "dep-0.1.jar" ) );
        verifier.assertArtifactPresent( "org.apache.maven.its.mng4842", "parent", "0.1", "pom" );
    }

    /**
     * Verify that resolution of parent POMs for dependency POMs treats the remote repositories of the current
     * resolution request as dominant when merging with any repositories declared in the dependency POM. This
     * variant of the test checks manual dependency resolution by a plugin.
     *
     * @throws Exception in case of failure
     */
    public void testitPlugin()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4842" );

        Verifier verifier = newVerifier( new File( testDir, "plugin" ).getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng4842" );
        verifier.addCliOption( "-s" );
        verifier.addCliOption( "settings.xml" );
        verifier.filterFile( "../settings-template.xml", "settings.xml", "UTF-8", verifier.newDefaultFilterProperties() );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier.assertArtifactPresent( "org.apache.maven.its.mng4842", "parent", "0.1", "pom" );
    }

}
