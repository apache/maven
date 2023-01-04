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
import org.apache.maven.shared.verifier.VerificationException;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-2741">MNG-2741</a>.
 *
 *
 */
public class MavenITmng2741PluginMetadataResolutionErrorMessageTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng2741PluginMetadataResolutionErrorMessageTest()
    {
        super( "[2.1.0,3.0-alpha-1),[3.0-beta-1,)" );
    }

    /**
     * Tests that plugin prefix metadata resolution errors tell the underlying transport issue.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitPrefix()
        throws Exception
    {
        testit( "prefix", "foo:bar" );
    }

    /**
     * Tests that plugin version metadata resolution errors tell the underlying transport issue.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitVersion()
        throws Exception
    {
        testit( "version", "org.apache.maven.its.mng2741:maven-it-plugin:foo" );
    }

    private void testit( String test, String goal )
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-2741" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.setLogFileName( "log-" + test + ".txt" );
        verifier.addCliArgument( "--settings" );
        verifier.addCliArgument( "settings.xml" );
        try
        {
            verifier.addCliArgument( goal );
            verifier.execute();
            fail( "Build should have failed to resolve plugin prefix" );
        }
        catch ( VerificationException e )
        {
            boolean foundCause = false;
            StringBuilder sb = new StringBuilder(  );
            List<String> lines = verifier.loadLines( verifier.getLogFileName(), "UTF-8" );
            for ( String line : lines )
            {
                sb.append( line ).append( System.lineSeparator() );
                if ( line.matches( ".*Connection refused.*" ) )
                {
                    foundCause = true;
                    break;
                }
                if ( line.matches( ".*Connection to http://localhost:54312 refused.*" ) )
                {
                    foundCause = true;
                    break;
                }
                if ( line.matches( ".*[Tt]ransfer failed for http://localhost:54312/repo/.*/maven-metadata.xml.*" ) )
                {
                    foundCause = true;
                    break;
                }
            }
            assertTrue( "Transfer error cause was not found: " +  sb, foundCause );
        }
    }

}
