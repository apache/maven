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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * An integration test to ensure any pomfile is only read once.
 * This is confirmed by adding a Java Agent to the DefaultModelReader and output the options, including the source location
 *
 * <a href="https://issues.apache.org/jira/browse/MNG-5669">MNG-5669</a>.
 *
 */
public class MavenITmng5669ReadPomsOnce
    extends AbstractMavenIntegrationTestCase
{

    private static final int LOG_SIZE = 233;

    public MavenITmng5669ReadPomsOnce()
    {
        super( "[4.0.0-alpha-1,)" );
    }

    @Test
    public void testWithoutBuildConsumer()
        throws Exception
    {
        // prepare JavaAgent
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-5669-read-poms-once" );
        Verifier verifier = newVerifier( testDir.getAbsolutePath(), false );
        Map<String, String> filterProperties =
            Collections.singletonMap( "${javaAgentJar}",
                                      verifier.getArtifactPath( "org.apache.maven.its", "core-it-javaagent", "2.1-SNAPSHOT", "jar" ) );
        verifier.filterFile( ".mvn/jvm.config", ".mvn/jvm.config", null, filterProperties );

        verifier.setForkJvm( true ); // pick up agent
        verifier.setMavenDebug( false );
        verifier.setAutoclean( false );
        verifier.addCliOption( "-q" );
        verifier.addCliOption( "-U" );
        verifier.addCliOption( "-Dmaven.experimental.buildconsumer=false" );
        verifier.addCliArgument( "verify");
        verifier.execute();

        List<String> logTxt = verifier.loadLines( "log.txt", "utf-8" );
        for ( String line : logTxt )
        {
            if ( line.startsWith( "Picked up JAVA_TOOL_OPTIONS:" ) )
            {
                logTxt.remove( line );
                break;
            }
        }
        assertEquals( logTxt.toString(), LOG_SIZE, logTxt.size() );

        // analyze lines. It is a Hashmap, so we can't rely on the order
        Set<String> uniqueBuildingSources = new HashSet<>( LOG_SIZE );
        final String buildSourceKey = "org.apache.maven.model.building.source=";
        final int keyLength = buildSourceKey.length();
        for ( String line : logTxt )
        {
            int start = line.indexOf( buildSourceKey );
            if ( start < 0 )
            {
                continue;
            }

            int end = line.indexOf( ", ", start );
            if ( end < 0 )
            {
                end = line.length() - 1; // is the }
            }
            uniqueBuildingSources.add( line.substring( start + keyLength, end ) );
        }
        assertEquals( uniqueBuildingSources.size(), LOG_SIZE - 1 /* minus superpom */ );
    }

    @Test
    public void testWithBuildConsumer()
        throws Exception
    {
        // prepare JavaAgent
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-5669-read-poms-once" );
        Verifier verifier = newVerifier( testDir.getAbsolutePath(), false );
        Map<String, String> filterProperties =
            Collections.singletonMap( "${javaAgentJar}",
                                      verifier.getArtifactPath( "org.apache.maven.its", "core-it-javaagent", "2.1-SNAPSHOT", "jar" ) );
        verifier.filterFile( ".mvn/jvm.config", ".mvn/jvm.config", null, filterProperties );

        verifier.setLogFileName( "log-bc.txt" );
        verifier.setForkJvm( true ); // pick up agent
        verifier.setMavenDebug( false );
        verifier.setAutoclean( false );
        verifier.addCliOption( "-q" );
        verifier.addCliOption( "-U" );
        verifier.addCliOption( "-Dmaven.experimental.buildconsumer=true" );
        verifier.addCliArgument( "verify" );
        verifier.execute();

        List<String> logTxt = verifier.loadLines( "log-bc.txt", "utf-8" );
        for ( String line : logTxt )
        {
            if ( line.startsWith( "Picked up JAVA_TOOL_OPTIONS:" ) )
            {
                logTxt.remove( line );
                break;
            }
        }
        assertEquals( logTxt.toString(), LOG_SIZE + 4 /* reactor poms are read twice: file + raw (=XMLFilters) */,
                      logTxt.size() );

        // analyze lines. It is a Hashmap, so we can't rely on the order
        Set<String> uniqueBuildingSources = new HashSet<>( LOG_SIZE );
        final String buildSourceKey = "org.apache.maven.model.building.source=";
        final int keyLength = buildSourceKey.length();
        for ( String line : logTxt )
        {
            int start = line.indexOf( buildSourceKey );
            if ( start < 0 )
            {
                continue;
            }

            int end = line.indexOf( ", ", start );
            if ( end < 0 )
            {
                end = line.length() - 1; // is the }
            }
            uniqueBuildingSources.add( line.substring( start + keyLength, end ) );
        }
        assertEquals( uniqueBuildingSources.size(), LOG_SIZE - 1 /* minus superpom */ );
    }

}
