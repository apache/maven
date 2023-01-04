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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-6223">MNG-6223</a>:
 * check that extensions in <code>.mvn/</code> are found when Maven is run with <code>-f path/to/dir</code>.
 * @see MavenITmng5889FindBasedir
 */
public class MavenITmng6223FindBasedir
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng6223FindBasedir()
    {
        super( "[3.5.1,)" );
    }

    /**
     * check that <code>path/to/.mvn/</code> is found when path to POM set by <code>--file path/to/dir</code>
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testMvnFileLongOptionToDir()
        throws Exception
    {
        runCoreExtensionWithOptionToDir( "--file", null );
    }

    /**
     * check that <code>path/to/.mvn/</code> is found when path to POM set by <code>-f path/to/dir</code>
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testMvnFileShortOptionToDir()
        throws Exception
    {
        runCoreExtensionWithOptionToDir( "-f", null );
    }

    /**
     * check that <code>path/to/.mvn/</code> is found when path to POM set by <code>--file path/to/module</code>
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testMvnFileLongOptionModuleToDir()
        throws Exception
    {
        runCoreExtensionWithOptionToDir( "--file", "module" );
    }

    /**
     * check that <code>path/to/.mvn/</code> is found when path to POM set by <code>-f path/to/module</code>
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testMvnFileShortOptionModuleToDir()
        throws Exception
    {
        runCoreExtensionWithOptionToDir( "-f", "module" );
    }

    private void runCoreExtensionWithOptionToDir( String option, String subdir )
        throws Exception
    {
        runCoreExtensionWithOption( option, subdir, false );
    }

    protected void runCoreExtensionWithOption( String option, String subdir, boolean pom )
            throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-5889-find.mvn" );

        File basedir = new File( testDir, "../mng-" + ( pom ? "5889" : "6223" ) + "-find.mvn" + option + ( pom ? "Pom" : "Dir" ) );
        basedir.mkdir();

        if ( subdir != null )
        {
            testDir = new File( testDir, subdir );
            basedir = new File( basedir, subdir );
            basedir.mkdirs();
        }

        Verifier verifier = newVerifier( basedir.getAbsolutePath() );
        verifier.addCliOption( "-Dexpression.outputFile=" + new File( basedir, "expression.properties" ).getAbsolutePath() );
        verifier.addCliOption( option ); // -f/--file client/pom.xml
        verifier.addCliOption( ( pom ? new File( testDir, "pom.xml" ) : testDir ).getAbsolutePath() );
        verifier.setForkJvm( true ); // force forked JVM since we need the shell script to detect .mvn/ location
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();

        Properties props = verifier.loadProperties( "expression.properties" );
        assertEquals( "ok", props.getProperty( "project.properties.jvm-config" ) );
        assertEquals( "ok", props.getProperty( "project.properties.maven-config" ) );
    }
}
