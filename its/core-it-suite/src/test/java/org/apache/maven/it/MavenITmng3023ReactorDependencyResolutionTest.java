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
import java.util.List;

import org.apache.maven.it.util.ResourceExtractor;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3023">MNG-3023</a>
 *
 * @author Mark Hobson
 * @author jdcasey
 *
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
     * In this pass, the dependency artifact should be missing from the local repository, and since the 'compile' phase
     * has not been called, i.e. the output directory for the compiled classes has not been created yet, the
     * dependency project artifact should not have a file associated with it. Therefore, the dependency artifact
     * should fail to resolve, and the build should fail.
     *
     * @throws Exception in case of failure
     */
    public void testitMNG3023A() throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3023" );

        // First pass. Make sure the dependency cannot be resolved.
        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.setLogFileName( "log-a.txt" );

        verifier.deleteDirectory( "dependency/dependency-classes" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng3023" );

        try
        {
            verifier.executeGoal( "validate" );
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
     * I this pass, the dependency artifact should have the file $(basedir)/dependency/dependency-classes
     * (a directory) associated with it, since the 'compile' phase has run. This location should be
     * present in the compile classpath output from the maven-it-plugin-dependency-resolution:compile
     * mojo execution.
     *
     * @throws Exception in case of failure
     */
    public void testitMNG3023B()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3023" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.setLogFileName( "log-b.txt" );
        // The IT doesn't actually run the compiler but merely mimics its effect, i.e. the creation of the output dir
        new File( testDir, "dependency/dependency-classes" ).mkdirs();
        verifier.deleteDirectory( "consumer/target" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng3023" );

        verifier.executeGoal( "initialize" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        List<String> compileClassPath = verifier.loadLines( "consumer/target/compile.classpath", "UTF-8" );
        assertTrue( compileClassPath.toString(), compileClassPath.contains( "dependency-classes" ) );
        assertFalse( compileClassPath.toString(), compileClassPath.contains( "dependency-1.jar" ) );
    }

    /**
     * Test that reactor projects are included in dependency resolution.
     *
     * I this pass, the dependency should have been installed, so the dependency artifact should have
     * a file of .../dependency-1.jar associated with it, since the 'install' phase has run. This
     * location should be present in the compile classpath output from the
     * maven-it-plugin-dependency-resolution:compile mojo execution.
     *
     * Afterwards, the projects are cleaned and a separate Maven call to the 'initialize' phase should succeed, since
     * the dependency artifact has been installed locally. This second execution should use the jar file
     * from the local repository in its classpath output.
     *
     * @throws Exception in case of failure
     */
    public void testitMNG3023C()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3023" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );

        verifier.deleteArtifacts( "org.apache.maven.its.mng3023" );

        verifier.deleteDirectory( "consumer/target" );
        verifier.setLogFileName( "log-c-1.txt" );
        verifier.executeGoal( "generate-sources" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        List<String> compileClassPath = verifier.loadLines( "consumer/target/compile.classpath", "UTF-8" );
        assertTrue( compileClassPath.toString(), compileClassPath.contains( "dependency-1.jar" ) );
        assertFalse( compileClassPath.toString(), compileClassPath.contains( "dependency-classes" ) );

        verifier.deleteDirectory( "dependency/dependency-classes" );
        verifier.deleteDirectory( "consumer/target" );
        verifier.setLogFileName( "log-c-2.txt" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        compileClassPath = verifier.loadLines( "consumer/target/compile.classpath", "UTF-8" );
        assertTrue( compileClassPath.toString(), compileClassPath.contains( "dependency-1.jar" ) );
        assertFalse( compileClassPath.toString(), compileClassPath.contains( "dependency-classes" ) );
    }

}
