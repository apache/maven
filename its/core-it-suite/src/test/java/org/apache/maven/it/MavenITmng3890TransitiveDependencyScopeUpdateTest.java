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
import java.util.List;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3890">MNG-3890</a>.
 * 
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng3890TransitiveDependencyScopeUpdateTest
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng3890TransitiveDependencyScopeUpdateTest()
    {
        super( ALL_MAVEN_VERSIONS );
    }

    /**
     * Test that transitive dependencies whose scope has been updated from "compile" to "provided" by a consumer
     * remain in "provided" scope when depending on this consumer with scope "compile".
     */
    public void testitMNG3890()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3890" );
        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng3890" );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", verifier.newDefaultFilterProperties() );
        verifier.addCliOption( "--settings" );
        verifier.addCliOption( "settings.xml" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        List<String> artifacts = verifier.loadLines( "target/artifacts.txt", "UTF-8" );
        assertTrue( artifacts.toString(), artifacts.contains( "org.apache.maven.its.mng3890:c:jar:0.1" ) );
        assertTrue( artifacts.toString(), artifacts.contains( "org.apache.maven.its.mng3890:b:jar:0.1" ) );
        assertFalse( artifacts.toString(), artifacts.contains( "org.apache.maven.its.mng3890:a:jar:0.1" ) );
        assertEquals( 2, artifacts.size() );
    }

}
