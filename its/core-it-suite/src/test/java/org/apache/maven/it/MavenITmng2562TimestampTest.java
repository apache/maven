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
import org.apache.maven.it.Verifier;

import java.io.File;
import java.util.Date;
import java.util.Properties;
import java.text.SimpleDateFormat;

public class MavenITmng2562TimestampTest
    extends AbstractMavenIntegrationTestCase
{
    
    public MavenITmng2562TimestampTest()
    {
        super( "[2.1.0-M1,)" ); // 2.1.0+ only
    }

    public void testitDefaultFormat()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-2562/default" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        Date now = new Date();

        Properties props = verifier.loadProperties( "target/pom.properties" );

        String timestamp1 = props.getProperty( "project.properties.timestamp1", "" );
        assertTrue( timestamp1, timestamp1.matches( "[0-9]{8}-[0-9]{4}" ) );
        Date date = new SimpleDateFormat( "yyyyMMdd-HHmm" ).parse( timestamp1 );
        assertTrue( now + " vs " + date, Math.abs( now.getTime() - date.getTime() ) < 24 * 60 * 60 * 1000 );

        String timestamp2 = props.getProperty( "project.properties.timestamp2", "" );
        assertEquals( timestamp1, timestamp2 );
    }

    public void testitCustomFormat()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-2562/custom" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        Date now = new Date();

        Properties props = verifier.loadProperties( "target/pom.properties" );

        String timestamp1 = props.getProperty( "project.properties.timestamp", "" );
        Date date = new SimpleDateFormat( "mm:HH dd-MM-yyyy" ).parse( timestamp1 );
        assertTrue( now + " vs " + date, Math.abs( now.getTime() - date.getTime() ) < 24 * 60 * 60 * 1000 );
    }

}
