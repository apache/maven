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
import java.util.Iterator;
import java.util.List;

/**
 * This is a test set for <a href="http://jira.codehaus.org/browse/MNG-3023">MNG-3023</a>
 * 
 * @author Mark Hobson
 * @author jdcasey
 * @version $Id$
 */
public class MavenITmng3023ReactorDependencyResolutionTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng3023ReactorDependencyResolutionTest()
    {
        super( "(2.1.0-M1,)" );
    }

    /**
     * Test that reactor projects are included in dependency resolution.
     * 
     * In this pass, the dependency artifact should be missing from the local repository, and since
     * the 'compile' phase has not been called, the dependency project artifact should not have a
     * file associated with it. Therefore, the dependency artifact should fail to resolve, and the
     * build should fail.
     */
    public void testitMNG3023A() throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3023" );

        // First pass. Make sure the dependency cannot be resolved.
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        
        verifier.deleteArtifact( "org.apache.maven.its.mng3023", "dependency", "1", "jar" );
        
        try
        {
            verifier.executeGoal( "initialize" );
            fail( "Expected failure to resolve dependency artifact without at least calling 'compile' phase." );
        }
        catch ( VerificationException e )
        {
            // expected.
        }
        finally
        {
            verifier.resetStreams();
        }
    }

    /**
     * Test that reactor projects are included in dependency resolution.
     * 
     * I this pass, the dependency artifact should have the file $(basedir)/dependency/target/classes
     * (a directory) associated with it, since the 'compile' phase has run. This location should be
     * present in the compile classpath output from the maven-it-plugin-dependency-resolution:compile
     * mojo execution.
     */
    public void testitMNG3023B()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3023" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        
        verifier.deleteArtifact( "org.apache.maven.its.mng3023", "dependency", "1", "jar" );
        
        verifier.executeGoal( "compile" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        List compileClassPath = verifier.loadLines( "consumer/target/compile.classpath", "UTF-8" );
        assertTrue( find( "dependency/target/classes", compileClassPath ) );
        assertFalse( find( "dependency-1.jar", compileClassPath ) );
    }

    /**
     * Test that reactor projects are included in dependency resolution.
     * 
     * I this pass, the dependency should have been installed, so the dependency artifact should have
     * a file of .../dependency-1.jar associated with it, since the 'install' phase has run. This 
     * location should be present in the compile classpath output from the 
     * maven-it-plugin-dependency-resolution:compile mojo execution.
     * 
     * Afterwards, the a separate Maven call to the 'initialize' phase should succeed, since the
     * dependency artifact has been installed locally. This second execution should use the jar file
     * from the local repository in its classpath output.
     */
    public void testitMNG3023C()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3023" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        
        verifier.deleteArtifact( "org.apache.maven.its.mng3023", "dependency", "1", "jar" );
        
        verifier.executeGoal( "install" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        List compileClassPath = verifier.loadLines( "consumer/target/compile.classpath", "UTF-8" );
        assertTrue( find( "dependency-1.jar", compileClassPath ) );
        assertFalse( find( "dependency/target/classes", compileClassPath ) );
        
        verifier.executeGoal( "initialize" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        
        compileClassPath = verifier.loadLines( "consumer/target/compile.classpath", "UTF-8" );
        assertTrue( find( "dependency-1.jar", compileClassPath ) );
        assertFalse( find( "dependency/target/classes", compileClassPath ) );
    }

    private boolean find( String pathSubstr, List classPath )
    {
        for ( Iterator it = classPath.iterator(); it.hasNext(); )
        {
            String path = (String) it.next();

            if ( path.indexOf( pathSubstr ) > -1 )
            {
                return true;
            }
        }

        return false;
    }
}
