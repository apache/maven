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
import java.util.Properties;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4750">MNG-4750</a> and
 * <a href="https://issues.apache.org/jira/browse/MNG-4845">MNG-4845</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4750ResolvedMavenProjectDependencyArtifactsTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4750ResolvedMavenProjectDependencyArtifactsTest()
    {
        super( "[2.0.3,3.0-alpha-1),[3.0,)" );
    }

    /**
     * Verify that MavenProject.getDependencyArtifacts() returns resolved artifacts (once dependency resolution
     * was requested).
     *
     * @throws Exception in case of failure
     */
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4750" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteArtifacts( "org.apache.maven.its.mng4750" );
        verifier.addCliOption( "-s" );
        verifier.addCliOption( "settings.xml" );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", verifier.newDefaultFilterProperties() );
        verifier.executeGoal( "initialize" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        Properties props = verifier.loadProperties( "target/artifact.properties" );
        assertEquals( "1", props.getProperty( "project.dependencyArtifacts.size" ) );

        String path = props.getProperty( "project.dependencyArtifacts.0.file" );
        assertNotNull( path );
        assertTrue( path, new File( path ).isFile() );

        String version = props.getProperty( "project.dependencyArtifacts.0.version" );
        assertEquals( "0.1", version );
    }

}
