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
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-5482">MNG-5482</a>.
 *
 * It checks that plugins and reports causing errors because of Aether change from Sonatype to Eclipse
 * get a dedicated message to explain solution to end-users
 *
 * @author hboutemy
 */
public class MavenITmng5482AetherNotFoundTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng5482AetherNotFoundTest()
    {
        super( "[3.1-A,)" );
    }

    @Test
    public void testPluginDependency()
        throws IOException, VerificationException
    {
        check( "plugin-dependency" );
    }

    @Test
    public void testPluginSite()
        throws IOException, VerificationException
    {
        check( "plugin-site" );
    }

    /*public void testReportMpir()
        throws IOException, VerificationException
    {
        check( "report-mpir" );
    }*/

    public void check( String dir )
        throws IOException, VerificationException
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-5482/" + dir );

        Verifier verifier = newVerifier( testDir.getAbsolutePath(), "remote" );
        verifier.setAutoclean( false );

        try
        {
            verifier.addCliArgument( "validate" );
            verifier.execute();

            fail( "should throw an error during execution." );
        }
        catch ( VerificationException e )
        {
            // expected...it'd be nice if we could get the specifics of the exception right here...
        }

        List<String> lines = verifier.loadFile( new File( testDir, "log.txt" ), false );

        int msg = indexOf( lines, "Caused by: java.lang.ClassNotFoundException: org.sonatype.aether.+" );
        assertTrue( "ClassNotFoundException message was not found in output.", msg >= 0 );

        int url = indexOf( lines, ".*http://cwiki.apache.org/confluence/display/MAVEN/AetherClassNotFound.*" );
        assertTrue( "Url to ClassNotFoundAether was not found in output.", url >= 0 );
    }

    private int indexOf( List<String> logLines, String regex )
    {
        Pattern pattern = Pattern.compile( regex );

        for ( int i = 0; i < logLines.size(); i++ )
        {
            String logLine = logLines.get( i );

            if ( pattern.matcher( logLine ).matches() )
            {
                return i;
            }
        }

        return -1;
    }

}
