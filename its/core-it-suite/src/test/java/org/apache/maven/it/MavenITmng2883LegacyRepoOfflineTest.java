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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-2883">MNG-2883</a>.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 */
public class MavenITmng2883LegacyRepoOfflineTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng2883LegacyRepoOfflineTest()
    {
        super( "(2.0.9,2.1.0-M1),(2.1.0-M1,3.0-alpha-1)" );
    }

    public void testParentUnresolvable()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-2883" );
        testDir = new File( testDir, "parent" );

        Verifier verifier;

        verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng2883" );

        File settings = verifier.filterFile( "../settings-template.xml", "settings.xml", "UTF-8",
                                             verifier.newDefaultFilterProperties() );

        // used to inject the remote repository
        verifier.addCliOption( "-s" );
        verifier.addCliOption( settings.getName() );

        // execute once just to make sure this test works at all!
        try
        {
            verifier.setLogFileName( "log-parent-a.txt" );
            // this will ensure that all relevant plugins are present.
            verifier.executeGoal( "validate" );
        }
        catch ( VerificationException e )
        {
            throw new VerificationException( "Build should succeed the first time through when NOT in offline mode!",
                                             e );
        }

        // the centerpiece of these tests!
        verifier.addCliOption( "-o" );

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

        List<String> missingMessages = new ArrayList<>();
        missingMessages.add( " is offline" );
        missingMessages.add( "org.apache.maven.its.mng2883:parent:pom:1.0-SNAPSHOT" );

        List<String> lines = verifier.loadFile( new File( testDir, verifier.getLogFileName() ), false );

        for ( String line : lines )
        {
            for ( Iterator<String> messageIt = missingMessages.iterator(); messageIt.hasNext(); )
            {
                String message = messageIt.next();

                if ( line.contains( message ) )
                {
                    messageIt.remove();
                }
            }
        }

        if ( !missingMessages.isEmpty() )
        {
            StringBuilder buffer = new StringBuilder();

            buffer.append( "The following key messages were missing from build output:\n\n" );

            for ( String message : missingMessages )
            {
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

        verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng2883" );

        File settings = verifier.filterFile( "../settings-template.xml", "settings.xml", "UTF-8",
                                             verifier.newDefaultFilterProperties() );

        // used to inject the remote repository
        verifier.addCliOption( "-s" );
        verifier.addCliOption( settings.getName() );

        // execute once just to make sure this test works at all!
        try
        {
            verifier.setLogFileName( "log-dep-a.txt" );
            // this will ensure that all relevant plugins are present.
            verifier.executeGoal( "validate" );
        }
        catch ( VerificationException e )
        {
            throw new VerificationException( "Build should succeed the first time through when NOT in offline mode!",
                                             e );
        }

        // the centerpiece of these tests!
        verifier.addCliOption( "-o" );

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

        List<String> missingMessages = new ArrayList<>();

        // FIXME: We need a more prominent diagnosis including system being in offline mode for 2.0.x.
        missingMessages.add( "offline mode." );
        missingMessages.add( "org.apache.maven.its.mng2883:dep:jar:1.0-SNAPSHOT" );

        List<String> lines = verifier.loadFile( new File( testDir, verifier.getLogFileName() ), false );

        for ( String line : lines )
        {
            for ( Iterator<String> messageIt = missingMessages.iterator(); messageIt.hasNext(); )
            {
                String message = (String) messageIt.next();

                if ( line.contains( message ) )
                {
                    messageIt.remove();
                }
            }
        }

        if ( !missingMessages.isEmpty() )
        {
            StringBuilder buffer = new StringBuilder();

            buffer.append( "The following key messages were missing from build output:\n\n" );

            for ( String message : missingMessages )
            {
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

        verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng2883" );

        File settings = verifier.filterFile( "../settings-template.xml", "settings.xml", "UTF-8",
                                             verifier.newDefaultFilterProperties() );

        // used to inject the remote repository
        verifier.addCliOption( "-s" );
        verifier.addCliOption( settings.getName() );

        verifier.setLogFileName( "log-plugin-a.txt" );
        verifier.executeGoal( "org.apache.maven.its.mng2883:plugin:1.0-SNAPSHOT:run" );

        // the centerpiece of these tests!
        verifier.addCliOption( "-o" );

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

        List<String> missingMessages = new ArrayList<>();
        missingMessages.add( " is offline" );
        missingMessages.add( "org.apache.maven.its.mng2883:plugin" );

        List<String> lines = verifier.loadFile( new File( testDir, verifier.getLogFileName() ), false );

        for ( String line : lines )
        {
            for ( Iterator<String> messageIt = missingMessages.iterator(); messageIt.hasNext(); )
            {
                String message = messageIt.next();

                if ( line.contains( message ) )
                {
                    messageIt.remove();
                }
            }
        }

        if ( !missingMessages.isEmpty() )
        {
            StringBuilder buffer = new StringBuilder();

            buffer.append( "The following key messages were missing from build output:\n\n" );

            for ( String message : missingMessages )
            {
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
