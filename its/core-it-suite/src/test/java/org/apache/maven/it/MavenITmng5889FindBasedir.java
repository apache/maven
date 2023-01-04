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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-5889">MNG-5889</a>:
 * check that extensions in <code>.mvn/</code> are found when Maven is run with <code>-f path/to/pom.xml</code>.
 */
public class MavenITmng5889FindBasedir
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng5889FindBasedir()
    {
        super( "[3.5.0,3.5.1)" );
    }

    /**
     * check that <code>path/to/.mvn/</code> is found when path to POM set by <code>--file path/to/pom.xml</code>
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testMvnFileLongOption()
        throws Exception
    {
        runCoreExtensionWithOption( "--file", null );
    }

    /**
     * check that <code>path/to/.mvn/</code> is found when path to POM set by <code>-f path/to/pom.xml</code>
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testMvnFileShortOption()
        throws Exception
    {
        runCoreExtensionWithOption( "-f", null );
    }

    /**
     * check that <code>path/to/.mvn/</code> is found when path to POM set by <code>--file path/to/module/pom.xml</code>
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testMvnFileLongOptionModule()
        throws Exception
    {
        runCoreExtensionWithOption( "--file", "module" );
    }

    /**
     * check that <code>path/to/.mvn/</code> is found when path to POM set by <code>-f path/to/module/pom.xml</code>
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testMvnFileShortOptionModule()
        throws Exception
    {
        runCoreExtensionWithOption( "-f", "module" );
    }

    private void runCoreExtensionWithOption( String option, String subdir )
        throws Exception
    {
        runCoreExtensionWithOption( option, subdir, true );
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
