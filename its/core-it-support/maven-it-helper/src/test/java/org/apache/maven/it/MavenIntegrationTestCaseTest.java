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
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MavenIntegrationTestCaseTest
{

    @Test
    public void testRemovePatternForTestWithVersionRange()
    {
        AbstractMavenIntegrationTestCase test = new AbstractMavenIntegrationTestCase( "[2.0,)" )
        {
            // test case with version range
        };

        assertVersionEquals( "2.1.0-M1", "2.1.0-M1", test );
        assertVersionEquals( "2.1.0-M1", "2.1.0-M1-SNAPSHOT", test );
        assertVersionEquals( "2.1.0-M1", "2.1.0-M1-RC1", test );
        assertVersionEquals( "2.1.0-M1", "2.1.0-M1-RC1-SNAPSHOT", test );
        assertVersionEquals( "2.0.10", "2.0.10", test );
        assertVersionEquals( "2.0.10", "2.0.10-SNAPSHOT", test );
        assertVersionEquals( "2.0.10", "2.0.10-RC1", test );
        assertVersionEquals( "2.0.10", "2.0.10-RC1-SNAPSHOT", test );
    }

    private static void assertVersionEquals( String expected, String version, AbstractMavenIntegrationTestCase test )
    {
        assertEquals( expected, test.removePattern( new DefaultArtifactVersion( version ) ).toString() );
    }

    @Test
    public void testRequiresMavenVersion()
    {
        System.setProperty( "maven.version", "2.1" );

        AbstractMavenIntegrationTestCase test = new AbstractMavenIntegrationTestCase( "[2.0,)" )
        {
            // test case with version range
        };

        try
        {
            test.requiresMavenVersion( "[3.0,)" );
        }
        catch ( RuntimeException e )
        {
            // expected
        }

        test.requiresMavenVersion( "[2.0,)" );
    }

}
