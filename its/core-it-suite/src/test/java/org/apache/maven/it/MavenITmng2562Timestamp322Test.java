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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-2562">MNG-2562</a>.
 */
public class MavenITmng2562Timestamp322Test
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng2562Timestamp322Test()
    {
        super( "[3.2.2,)" ); // 3.2.2+ only as we changed the timestamp format
    }

    @Test
    public void testitDefaultFormat()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-2562/default" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();

        Date now = new Date();

        Properties props = verifier.loadProperties( "target/pom.properties" );

        String timestamp1 = props.getProperty( "project.properties.timestamp1", "" );
        assertTrue( timestamp1, timestamp1.matches( "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z" ) );
        Date date = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss'Z'" ).parse( timestamp1 );
        assertTrue( now + " vs " + date, Math.abs( now.getTime() - date.getTime() ) < 24 * 60 * 60 * 1000 );

        String timestamp2 = props.getProperty( "project.properties.timestamp2", "" );
        assertEquals( timestamp1, timestamp2 );
    }

    @Test
    public void testitCustomFormat()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-2562/custom" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();

        Date now = new Date();

        Properties props = verifier.loadProperties( "target/pom.properties" );

        String timestamp1 = props.getProperty( "project.properties.timestamp", "" );
        Date date = new SimpleDateFormat( "mm:HH dd-MM-yyyy" ).parse( timestamp1 );
        assertTrue( now + " vs " + date, Math.abs( now.getTime() - date.getTime() ) < 24 * 60 * 60 * 1000 );
    }

    @Test
    public void testitSameValueAcrossModules()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-2562/reactor" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteDirectory( "child-1/target" );
        verifier.deleteDirectory( "child-2/target" );
        verifier.deleteDirectory( "child-3/target" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();

        Properties props = verifier.loadProperties( "target/pom.properties" );
        String timestamp = props.getProperty( "project.properties.timestamp", "" );

        Properties props1 = verifier.loadProperties( "child-1/target/pom.properties" );
        String timestamp1 = props1.getProperty( "project.properties.timestamp", "" );

        Properties props2 = verifier.loadProperties( "child-2/target/pom.properties" );
        String timestamp2 = props2.getProperty( "project.properties.timestamp", "" );

        Properties props3 = verifier.loadProperties( "child-3/target/pom.properties" );
        String timestamp3 = props3.getProperty( "project.properties.timestamp", "" );

        assertEquals( timestamp, timestamp1 );
        assertEquals( timestamp, timestamp2 );
        assertEquals( timestamp, timestamp3 );
    }

}
