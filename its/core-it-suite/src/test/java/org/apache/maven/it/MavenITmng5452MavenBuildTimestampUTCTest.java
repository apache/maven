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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-5452">MNG-5452</a>
 * Make sure that the maven.build.timestamp is in UTC.
 */
public class MavenITmng5452MavenBuildTimestampUTCTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng5452MavenBuildTimestampUTCTest()
    {
        super( "[3.2.2,)" );
    }

    @Test
    public void testMavenBuildTimestampIsUsingUTC()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-5452-maven-build-timestamp-utc" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.addCliArgument( "process-resources" );
        verifier.execute();
        //
        // We have a timestamp format = yyyyMMdd:HHmm:z, where the final element is the timezone which should be UTC
        //
        Properties filteredProperties = verifier.loadProperties( "target/classes/filtered.properties" );
        String timestamp = filteredProperties.getProperty( "timestamp" );
        assertNotNull( timestamp );
        assertTrue( timestamp.endsWith( "UTC" ) );
    }
}
