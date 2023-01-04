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

import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4007">MNG-4007</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng4007PlatformFileSeparatorTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4007PlatformFileSeparatorTest()
    {
        super( ALL_MAVEN_VERSIONS );
    }

    /**
     * Verify that paths to project directories use the platform-specific file separator.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitMNG4007()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4007" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();

        Properties modelProps = verifier.loadProperties( "target/model.properties" );

        assertPath( modelProps.getProperty( "project.build.directory" ) );

        assertPath( modelProps.getProperty( "project.build.outputDirectory" ) );

        assertPath( modelProps.getProperty( "project.build.testOutputDirectory" ) );

        assertPath( modelProps.getProperty( "project.build.sourceDirectory" ) );
        assertPath( modelProps.getProperty( "project.compileSourceRoots.0" ) );

        assertPath( modelProps.getProperty( "project.build.testSourceDirectory" ) );
        assertPath( modelProps.getProperty( "project.testCompileSourceRoots.0" ) );

        assertPath( modelProps.getProperty( "project.build.resources.0.directory" ) );

        assertPath( modelProps.getProperty( "project.build.testResources.0.directory" ) );

        assertPath( modelProps.getProperty( "project.build.filters.0" ) );

        /*
         * NOTE: The script source directory is deliberately excluded from the checks due to MNG-3741.
         */

        // MNG-3877
        if ( matchesVersionRange( "[3.0-alpha-3,)" ) )
        {
            assertPath( modelProps.getProperty( "project.reporting.outputDirectory" ) );
        }
    }

    private void assertPath( String actual )
    {
        /*
         * NOTE: Whether the path is absolute is another issue (MNG-3877), we are only interested in the proper
         * file separator here.
         */
        assertEquals( new File( actual ).getPath(), actual );
    }

}
