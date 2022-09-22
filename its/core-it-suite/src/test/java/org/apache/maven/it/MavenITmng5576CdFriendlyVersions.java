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
import java.util.List;
import java.util.Properties;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-5576">MNG-5576</a>.
 *
 * @author Jason van Zyl
 */
public class MavenITmng5576CdFriendlyVersions
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng5576CdFriendlyVersions()
    {
        super( "[3.2,)" );
    }

    /**
     * Verifies that property references with dotted notation work within
     * POM interpolation.
     *
     * @throws Exception in case of failure
     */
    public void testContinuousDeliveryFriendlyVersionsAreWarningFreeWithoutBuildConsumer()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-5576-cd-friendly-versions" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.addCliOption( "-Dchangelist=changelist" );
        verifier.addCliOption( "-Dmaven.experimental.buildconsumer=false" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        Properties props = verifier.loadProperties( "target/pom.properties" );
        assertEquals( "1.0.0.changelist", props.getProperty( "project.version" ) );

        List<String> lines = verifier.loadFile( new File( testDir, "log.txt" ), false );
        for( String line : lines )
        {
            assertFalse( line, line.contains( "WARNING" ) );
        }
    }

    /**
     * Verifies that property references with dotted notation work within
     * POM interpolation.
     *
     * @throws Exception in case of failure
     */
    public void testContinuousDeliveryFriendlyVersionsAreWarningFreeWithBuildConsumer()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-5576-cd-friendly-versions" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setLogFileName( "log-bc.txt" );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.addCliOption( "-Dchangelist=changelist" );
        verifier.addCliOption( "-Dmaven.experimental.buildconsumer=true" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        Properties props = verifier.loadProperties( "target/pom.properties" );
        assertEquals( "1.0.0.changelist", props.getProperty( "project.version" ) );

        List<String> lines = verifier.loadFile( new File( testDir, "log-bc.txt" ), false );
        for( String line : lines )
        {
            assertFalse( line, line.contains( "WARNING" ) );
        }
    }

}
