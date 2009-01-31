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

import junit.framework.TestCase;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

public class MavenIntegrationTestCaseTest
    extends TestCase
{

    public void testRemovePatternForTestWithVersionRange()
    {
        AbstractMavenIntegrationTestCase test = new AbstractMavenIntegrationTestCase( "[2.0,)" )
        {
            // test case with version range
        };
        testRemovePattern( test );
    }

    public void testRemovePatternForTestWithoutVersionRange()
    {
        AbstractMavenIntegrationTestCase test = new AbstractMavenIntegrationTestCase()
        {
            // test case without version range
        };
        testRemovePattern( test );
    }

    private static void testRemovePattern( AbstractMavenIntegrationTestCase test )
    {
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

}
