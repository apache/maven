package org.apache.maven.rtinfo.internal;

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

import org.apache.maven.rtinfo.RuntimeInformation;
import org.codehaus.plexus.testing.PlexusTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.inject.Inject;

@PlexusTest
public class DefaultRuntimeInformationTest
{
    @Inject
    RuntimeInformation rtInfo;

    @Test
    public void testGetMavenVersion()
    {
        String mavenVersion = rtInfo.getMavenVersion();
        assertNotNull( mavenVersion );
        assertTrue( mavenVersion.length() > 0 );
    }

    @Test
    public void testIsMavenVersion()
    {
        assertTrue( rtInfo.isMavenVersion( "2.0" ) );
        assertFalse( rtInfo.isMavenVersion( "9.9" ) );

        assertTrue( rtInfo.isMavenVersion( "[2.0.11,2.1.0),[3.0,)" ) );
        assertFalse( rtInfo.isMavenVersion( "[9.0,)" ) );

        assertThrows(
                IllegalArgumentException.class,
                () -> rtInfo.isMavenVersion( "[3.0," ),
                "Bad version range wasn't rejected" );

        assertThrows(
                IllegalArgumentException.class,
                () -> rtInfo.isMavenVersion( "" ),
                "Bad version range wasn't rejected" );

        assertThrows(
                NullPointerException.class,
                () -> rtInfo.isMavenVersion( null ),
                "Bad version range wasn't rejected" );
    }

}
