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
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

/**
 * This is a test set for <a href="http://jira.codehaus.org/browse/MNG-2690">MNG-2690</a>.
 * 
 * It checks, at the most basic level possible, that the plugin manager is intercepting things like
 * {@link NoClassDefFoundError} and ComponntLookupException, then throwing user-friendly errors when loading and
 * configuring a mojo.
 * 
 * @author jdcasey
 */
public class MavenITmng2690MojoLoadingErrorsTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng2690MojoLoadingErrorsTest()
    {
        super( "(2.1.0-M1,)" );
    }

    public void testNoClassDefFromMojoLoad()
        throws IOException, VerificationException
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-2690/noclassdef-mojo" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );

        try
        {
            verifier.executeGoal( "validate" );

            fail( "should throw an error during execution." );
        }
        catch ( VerificationException e )
        {
            // expected...it'd be nice if we could get the specifics of the exception right here...
        }
        finally
        {
            verifier.resetStreams();
        }

        List lines = verifier.loadFile( new File( testDir, "log.txt" ), false );

        boolean foundMessage = false;
        boolean foundClass = false;
        for ( Iterator it = lines.iterator(); it.hasNext(); )
        {
            String line = (String) it.next();
            if ( line.indexOf( "A required class is missing" ) > -1 )
            {
                foundMessage = true;
            }

            // trigger AFTER the required-class message is found, since the class name should come afterward.
            if ( foundMessage && line.replace( '/', '.' ).indexOf( TestCase.class.getName() ) > -1 )
            {
                foundClass = true;
                break;
            }
        }

        assertTrue( "User-friendly message was not found in output.", foundMessage );
        assertTrue( "Missing class name was not found in output.", foundClass );
    }

    public void testNoClassDefFromMojoConfiguration()
        throws IOException, VerificationException
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-2690/noclassdef-param" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );

        try
        {
            verifier.executeGoal( "validate" );

            fail( "should throw an error during execution." );
        }
        catch ( VerificationException e )
        {
            // expected...it'd be nice if we could get the specifics of the exception right here...
        }
        finally
        {
            verifier.resetStreams();
        }

        List lines = verifier.loadFile( new File( testDir, "log.txt" ), false );

        boolean foundMessage = false;
        boolean foundClass = false;
        for ( Iterator it = lines.iterator(); it.hasNext(); )
        {
            String line = (String) it.next();
            if ( line.indexOf( "A required class was missing during mojo configuration" ) > -1 )
            {
                foundMessage = true;
            }

            // trigger AFTER the required-class message is found, since the class name should come afterward.
            if ( foundMessage && line.replace( '/', '.' ).indexOf( TestCase.class.getName() ) > -1 )
            {
                foundClass = true;
                break;
            }
        }

        assertTrue( "User-friendly message was not found in output.", foundMessage );
        assertTrue( "Missing class name was not found in output.", foundClass );
    }

    public void testMojoComponentLookupException()
        throws IOException, VerificationException
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-2690/mojo-complookup" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );

        try
        {
            verifier.executeGoal( "validate" );

            fail( "should throw an error during execution." );
        }
        catch ( VerificationException e )
        {
            // expected...it'd be nice if we could get the specifics of the exception right here...
        }
        finally
        {
            verifier.resetStreams();
        }

        List lines = verifier.loadFile( new File( testDir, "log.txt" ), false );

        String compLookupMessage =
            "Unable to find the mojo 'mojo-component-lookup-exception' "
                + "(or one of its required components) in the plugin "
                + "'org.apache.maven.its.plugins:maven-it-plugin-error";

        boolean foundMessage = false;
        for ( Iterator it = lines.iterator(); it.hasNext(); )
        {
            String line = (String) it.next();
            if ( line.indexOf( compLookupMessage ) > -1 )
            {
                foundMessage = true;
                break;
            }
        }

        assertTrue( "User-friendly message was not found in output.", foundMessage );
    }

    public void testMojoRequirementComponentLookupException()
        throws IOException, VerificationException
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-2690/requirement-complookup" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );

        try
        {
            verifier.executeGoal( "validate" );

            fail( "should throw an error during execution." );
        }
        catch ( VerificationException e )
        {
            // expected...it'd be nice if we could get the specifics of the exception right here...
        }
        finally
        {
            verifier.resetStreams();
        }

        List lines = verifier.loadFile( new File( testDir, "log.txt" ), false );

        String compLookupMessage =
            "Unable to find the mojo 'requirement-component-lookup-exception' "
                + "(or one of its required components) in the plugin "
                + "'org.apache.maven.its.plugins:maven-it-plugin-error";

        boolean foundMessage = false;
        for ( Iterator it = lines.iterator(); it.hasNext(); )
        {
            String line = (String) it.next();
            if ( line.indexOf( compLookupMessage ) > -1 )
            {
                foundMessage = true;
                break;
            }
        }

        assertTrue( "User-friendly message was not found in output.", foundMessage );
    }

}
