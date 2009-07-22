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

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This is a sample integration test. The IT tests typically
 * operate by having a sample project in the
 * /src/test/resources folder along with a junit test like
 * this one. The junit test uses the verifier (which uses
 * the invoker) to invoke a new instance of Maven on the
 * project in the resources folder. It then checks the
 * results. This is a non-trivial example that shows two
 * phases. See more information inline in the code.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 *
 */
public class MavenITmng2883LegacyRepoOfflineTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng2883LegacyRepoOfflineTest()
    {
        super( "(,3.0-alpha-1)" );
    }

    public void testParentUnresolvable()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-2883" );
        testDir = new File( testDir, "parent" );

        Verifier verifier;

        verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng2883" );

        File settings = verifier.filterFile( "../settings-template.xml", "settings.xml", "UTF-8", 
                                             verifier.newDefaultFilterProperties() );
        List cliOptions = new ArrayList();

        // used to inject the remote repository
        cliOptions.add( "-s" );
        cliOptions.add( settings.getName() );

        verifier.setCliOptions( cliOptions );

        // execute once just to make sure this test works at all!
        try
        {
            verifier.setLogFileName( "log-parent-a.txt" );
            // this will ensure that all relevant plugins are present.
            verifier.executeGoal( "validate" );
        }
        catch ( VerificationException e )
        {
            throw new VerificationException( "Build should succeed the first time through when NOT in offline mode!", e );
        }

        // the centerpiece of these tests!
        cliOptions.add( "-o" );

        verifier.setCliOptions( cliOptions );

        // re-run in offline mode, should still succeed by using local repo
        verifier.setLogFileName( "log-parent-b.txt" );
        verifier.executeGoal( "validate" );

        // clear out the parent POM if it's in the local repository.
        verifier.deleteArtifacts( "org.apache.maven.its.mng2883" );

        try
        {
            verifier.setLogFileName( "log-parent-c.txt" );
            verifier.executeGoal( "validate" );

            fail( "Build should fail with unresolvable parent POM." );
        }
        catch ( VerificationException e )
        {
            // expected
        }

        List missingMessages = new ArrayList();
        missingMessages.add( " is offline" );
        missingMessages.add( "org.apache.maven.its.mng2883:parent:pom:1.0-SNAPSHOT" );

        List lines = verifier.loadFile( new File( testDir, verifier.getLogFileName() ), false );

        for ( Iterator it = lines.iterator(); it.hasNext(); )
        {
            String line = (String) it.next();
            for ( Iterator messageIt = missingMessages.iterator(); messageIt.hasNext(); )
            {
                String message = (String) messageIt.next();

                if ( line.indexOf( message ) > -1 )
                {
                    messageIt.remove();
                }
            }
        }

        if ( !missingMessages.isEmpty() )
        {
            StringBuffer buffer = new StringBuffer();

            buffer.append( "The following key messages were missing from build output:\n\n" );

            for ( Iterator it = missingMessages.iterator(); it.hasNext(); )
            {
                String message = (String) it.next();
                if ( buffer.length() < 1 )
                {
                    buffer.append( "\n" );
                }
                buffer.append( '\'' ).append( message ).append( '\'' );
            }

            fail( buffer.toString() );
        }
    }

    public void testDependencyUnresolvable()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-2883" );
        testDir = new File( testDir, "dependency" );

        Verifier verifier;

        verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng2883" );

        List cliOptions = new ArrayList();

        File settings = verifier.filterFile( "../settings-template.xml", "settings.xml", "UTF-8", 
                                             verifier.newDefaultFilterProperties() );

        // used to inject the remote repository
        cliOptions.add( "-s" );
        cliOptions.add( settings.getName() );

        verifier.setCliOptions( cliOptions );

        // execute once just to make sure this test works at all!
        try
        {
            verifier.setLogFileName( "log-dep-a.txt" );
            // this will ensure that all relevant plugins are present.
            verifier.executeGoal( "validate" );
        }
        catch ( VerificationException e )
        {
            throw new VerificationException( "Build should succeed the first time through when NOT in offline mode!", e );
        }

        // the centerpiece of these tests!
        cliOptions.add( "-o" );

        verifier.setCliOptions( cliOptions );

        // re-run in offline mode, should still succeed by using local repo
        verifier.setLogFileName( "log-dep-b.txt" );
        verifier.executeGoal( "validate" );

        // clear out the dependency if it's in the local repository.
        verifier.deleteArtifacts( "org.apache.maven.its.mng2883" );

        try
        {
            verifier.setLogFileName( "log-dep-c.txt" );
            verifier.executeGoal( "validate" );

            fail( "Build should fail with unresolvable dependency artifact." );
        }
        catch ( VerificationException e )
        {
            // expected
        }

        List missingMessages = new ArrayList();

        // FIXME: We need a more prominent diagnosis including system being in offline mode for 2.0.x.
        missingMessages.add( "offline mode." );
        missingMessages.add( "org.apache.maven.its.mng2883:dep:jar:1.0-SNAPSHOT" );

        List lines = verifier.loadFile( new File( testDir, verifier.getLogFileName() ), false );

        for ( Iterator it = lines.iterator(); it.hasNext(); )
        {
            String line = (String) it.next();
            for ( Iterator messageIt = missingMessages.iterator(); messageIt.hasNext(); )
            {
                String message = (String) messageIt.next();

                if ( line.indexOf( message ) > -1 )
                {
                    messageIt.remove();
                }
            }
        }

        if ( !missingMessages.isEmpty() )
        {
            StringBuffer buffer = new StringBuffer();

            buffer.append( "The following key messages were missing from build output:\n\n" );

            for ( Iterator it = missingMessages.iterator(); it.hasNext(); )
            {
                String message = (String) it.next();
                if ( buffer.length() < 1 )
                {
                    buffer.append( "\n" );
                }
                buffer.append( '\'' ).append( message ).append( '\'' );
            }

            fail( buffer.toString() );
        }
    }

    public void testPluginUnresolvable()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-2883" );
        testDir = new File( testDir, "plugin" );

        Verifier verifier;

        verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng2883" );

        List cliOptions = new ArrayList();

        File settings = verifier.filterFile( "../settings-template.xml", "settings.xml", "UTF-8", 
                                             verifier.newDefaultFilterProperties() );

        // used to inject the remote repository
        cliOptions.add( "-s" );
        cliOptions.add( settings.getName() );

        verifier.setCliOptions( cliOptions );

        verifier.setLogFileName( "log-plugin-a.txt" );
        verifier.executeGoal( "org.apache.maven.its.mng2883:plugin:1.0-SNAPSHOT:run" );

        // the centerpiece of these tests!
        cliOptions.add( "-o" );

        verifier.setCliOptions( cliOptions );

        // re-run in offline mode, should still succeed by using local repo
        verifier.setLogFileName( "log-plugin-b.txt" );
        verifier.executeGoal( "org.apache.maven.its.mng2883:plugin:1.0-SNAPSHOT:run" );

        // clear out the dependency if it's in the local repository.
        verifier.deleteArtifacts( "org.apache.maven.its.mng2883" );

        try
        {
            verifier.setLogFileName( "log-plugin-c.txt" );
            verifier.executeGoal( "org.apache.maven.its.mng2883:plugin:1.0-SNAPSHOT:run" );

            fail( "Build should fail with unresolvable plugin artifact." );
        }
        catch ( VerificationException e )
        {
            // expected
        }

        List missingMessages = new ArrayList();
        missingMessages.add( " is offline" );
        missingMessages.add( "org.apache.maven.its.mng2883:plugin" );

        List lines = verifier.loadFile( new File( testDir, verifier.getLogFileName() ), false );

        for ( Iterator it = lines.iterator(); it.hasNext(); )
        {
            String line = (String) it.next();
            for ( Iterator messageIt = missingMessages.iterator(); messageIt.hasNext(); )
            {
                String message = (String) messageIt.next();

                if ( line.indexOf( message ) > -1 )
                {
                    messageIt.remove();
                }
            }
        }

        if ( !missingMessages.isEmpty() )
        {
            StringBuffer buffer = new StringBuffer();

            buffer.append( "The following key messages were missing from build output:\n\n" );

            for ( Iterator it = missingMessages.iterator(); it.hasNext(); )
            {
                String message = (String) it.next();
                if ( buffer.length() < 1 )
                {
                    buffer.append( "\n" );
                }
                buffer.append( '\'' ).append( message ).append( '\'' );
            }

            fail( buffer.toString() );
        }
    }

}
