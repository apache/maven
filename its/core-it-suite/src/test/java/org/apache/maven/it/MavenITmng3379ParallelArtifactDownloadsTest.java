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

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

/**
 * This is a test set for <a href="http://jira.codehaus.org/browse/MNG-3379">MNG-3379</a>.
 * 
 * @author Benjamin Bentmann
 * @version $Id$
 */
public class MavenITmng3379ParallelArtifactDownloadsTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng3379ParallelArtifactDownloadsTest()
    {
        super();
    }

    /**
     * Tests that parallel downloads of artifacts from both the same and from different group ids don't corrupt
     * the local repo.
     */
    public void testitMNG3379()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3379" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteArtifacts( "org.apache.maven.its.mng3379.a" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng3379.b" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng3379.c" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng3379.c" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        assertArtifactPresent( verifier, "org.apache.maven.its.mng3379.a", "x", "0.1", "", "jar" );
        assertArtifactPresent( verifier, "org.apache.maven.its.mng3379.a", "x", "0.1", "", "pom" );
        assertArtifactPresent( verifier, "org.apache.maven.its.mng3379.a", "x", "0.1", "tests", "jar" );
        assertArtifactPresent( verifier, "org.apache.maven.its.mng3379.a", "x", "0.1", "sources", "jar" );
        assertArtifactPresent( verifier, "org.apache.maven.its.mng3379.a", "x", "0.1", "test-javadoc", "jar" );

        assertArtifactPresent( verifier, "org.apache.maven.its.mng3379.b", "x", "0.1", "", "jar" );
        assertArtifactPresent( verifier, "org.apache.maven.its.mng3379.b", "x", "0.1", "", "pom" );
        assertArtifactPresent( verifier, "org.apache.maven.its.mng3379.b", "x", "0.1", "tests", "jar" );
        assertArtifactPresent( verifier, "org.apache.maven.its.mng3379.b", "x", "0.1", "sources", "jar" );
        assertArtifactPresent( verifier, "org.apache.maven.its.mng3379.b", "x", "0.1", "test-javadoc", "jar" );

        assertArtifactPresent( verifier, "org.apache.maven.its.mng3379.c", "x", "0.1", "", "jar" );
        assertArtifactPresent( verifier, "org.apache.maven.its.mng3379.c", "x", "0.1", "", "pom" );
        assertArtifactPresent( verifier, "org.apache.maven.its.mng3379.c", "x", "0.1", "tests", "jar" );
        assertArtifactPresent( verifier, "org.apache.maven.its.mng3379.c", "x", "0.1", "sources", "jar" );
        assertArtifactPresent( verifier, "org.apache.maven.its.mng3379.c", "x", "0.1", "test-javadoc", "jar" );

        assertArtifactPresent( verifier, "org.apache.maven.its.mng3379.d", "x", "0.1", "", "jar" );
        assertArtifactPresent( verifier, "org.apache.maven.its.mng3379.d", "x", "0.1", "", "pom" );
        assertArtifactPresent( verifier, "org.apache.maven.its.mng3379.d", "x", "0.1", "tests", "jar" );
        assertArtifactPresent( verifier, "org.apache.maven.its.mng3379.d", "x", "0.1", "sources", "jar" );
        assertArtifactPresent( verifier, "org.apache.maven.its.mng3379.d", "x", "0.1", "test-javadoc", "jar" );
    }

    private void assertArtifactPresent( Verifier verifier, String gid, String aid, String ver, String cls, String ext )
    {
        StringBuffer buffer = new StringBuffer( 256 );
        buffer.append( verifier.localRepo );
        buffer.append( '/' ).append( gid.replace( '.', '/' ) );
        buffer.append( '/' ).append( aid );
        buffer.append( '/' ).append( ver );
        buffer.append( '/' ).append( aid ).append( '-' ).append( ver );
        if ( cls != null && cls.length() > 0 )
        {
            buffer.append( '-' ).append( cls );
        }
        buffer.append( '.' ).append( ext );
        File file = new File( buffer.toString() );
        assertTrue( file.getAbsolutePath(), file.isFile() );
    }

}
