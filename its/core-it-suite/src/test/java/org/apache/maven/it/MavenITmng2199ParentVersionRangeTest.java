/*
 * Copyright 2014 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.maven.it;

import java.io.File;
import java.util.List;
import java.util.regex.Pattern;
import org.apache.maven.it.util.ResourceExtractor;

public class MavenITmng2199ParentVersionRangeTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng2199ParentVersionRangeTest()
    {
        super( "[3.2.2,)" );
    }

    public void testValidParentVersionRangeWithInclusiveUpperBound()
        throws Exception
    {
        Verifier verifier = null;
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-2199-parent-version-range/valid-inclusive-upper-bound" );

        try
        {
            verifier = newVerifier( testDir.getAbsolutePath(), "remote" );
            verifier.addCliOption( "-U" );
            verifier.setAutoclean( false );

            verifier.executeGoal( "verify" );
            verifier.verifyErrorFreeLog();
        }
        finally
        {
            verifier.resetStreams();
        }
    }

    public void testValidParentVersionRangeWithExclusiveUpperBound()
        throws Exception
    {
        Verifier verifier = null;
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-2199-parent-version-range/valid-exclusive-upper-bound" );

        try
        {
            verifier = newVerifier( testDir.getAbsolutePath(), "remote" );
            verifier.addCliOption( "-U" );
            verifier.setAutoclean( false );

            verifier.executeGoal( "verify" );
            verifier.verifyErrorFreeLog();
        }
        finally
        {
            verifier.resetStreams();
        }
    }

    public void testInvalidParentVersionRange()
        throws Exception
    {
        Verifier verifier = null;
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-2199-parent-version-range/invalid" );

        try
        {
            verifier = newVerifier( testDir.getAbsolutePath(), "remote" );
            verifier.setAutoclean( false );
            verifier.addCliOption( "-U" );
            verifier.executeGoal( "verify" );
            fail( "Expected 'VerificationException' not thrown." );
        }
        catch ( final VerificationException e )
        {
            final List<String> lines = verifier.loadFile( new File( testDir, "log.txt" ), false );
            int msg = indexOf( lines, ".*The requested version range.*does not specify an upper bound.*" );
            assertTrue( "Expected error message not found.", msg >= 0 );
        }
        finally
        {
            verifier.resetStreams();
        }
    }

    public void testValidParentVersionRangeInvalidVersionExpression()
        throws Exception
    {
        Verifier verifier = null;
        File testDir =
            ResourceExtractor.simpleExtractResources( getClass(), "/mng-2199-parent-version-range/expression" );

        try
        {
            verifier = newVerifier( testDir.getAbsolutePath(), "remote" );
            verifier.setAutoclean( false );
            verifier.addCliOption( "-U" );
            verifier.executeGoal( "verify" );
            fail( "Expected 'VerificationException' not thrown." );
        }
        catch ( final VerificationException e )
        {
            final List<String> lines = verifier.loadFile( new File( testDir, "log.txt" ), false );
            int msg =
                indexOf( lines,
                         ".*Version must be a constant @ org.apache.maven.its.mng2199:expression:\\$\\{project.parent.version\\}.*" );

            assertTrue( "Expected error message not found.", msg >= 0 );
        }
        finally
        {
            verifier.resetStreams();
        }
    }

    public void testValidParentVersionRangeInvalidVersionInheritance()
        throws Exception
    {
        Verifier verifier = null;
        File testDir =
            ResourceExtractor.simpleExtractResources( getClass(), "/mng-2199-parent-version-range/inherited" );

        try
        {
            verifier = newVerifier( testDir.getAbsolutePath(), "remote" );
            verifier.setAutoclean( false );
            verifier.addCliOption( "-U" );
            verifier.executeGoal( "verify" );
            fail( "Expected 'VerificationException' not thrown." );
        }
        catch ( final VerificationException e )
        {
            final List<String> lines = verifier.loadFile( new File( testDir, "log.txt" ), false );
            int msg =
                indexOf( lines,
                         ".*Version must be a constant @ org.apache.maven.its.mng2199:inherited:\\[unknown-version\\].*" );

            assertTrue( "Expected error message not found.", msg >= 0 );
        }
        finally
        {
            verifier.resetStreams();
        }
    }

    public void testValidLocalParentVersionRange()
        throws Exception
    {
        Verifier verifier = null;
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-2199-parent-version-range/local-parent" );

        try
        {
            verifier = newVerifier( testDir.getAbsolutePath(), "remote" );
            verifier.addCliOption( "-U" );
            verifier.setAutoclean( false );

            verifier.executeGoal( "verify" );
            verifier.verifyErrorFreeLog();
        }
        finally
        {
            verifier.resetStreams();
        }
    }

    private static int indexOf( final List<String> logLines, final String regex )
    {
        final Pattern pattern = Pattern.compile( regex );

        for ( int i = 0, l0 = logLines.size(); i < l0; i++ )
        {
            if ( pattern.matcher( logLines.get( i ) ).matches() )
            {
                return i;
            }
        }

        return -1;
    }
}
