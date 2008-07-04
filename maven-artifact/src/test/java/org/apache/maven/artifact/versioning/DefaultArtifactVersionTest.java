package org.apache.maven.artifact.versioning;

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

/**
 * Test DefaultArtifactVersion.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public class DefaultArtifactVersionTest
    extends TestCase
{
    private ArtifactVersion newArtifactVersion( String version )
    {
        return new DefaultArtifactVersion( version );
    }

    private void checkVersionParsing( String version, int major, int minor, int incremental, int buildnumber,
                                      String qualifier )
    {
        ArtifactVersion artifactVersion = newArtifactVersion( version );
        String parsed = "'" + version + "' parsed as ('" + artifactVersion.getMajorVersion() + "', '"
            + artifactVersion.getMinorVersion() + "', '" + artifactVersion.getIncrementalVersion() + "', '"
            + artifactVersion.getBuildNumber() + "', '" + artifactVersion.getQualifier() + "'), ";
        assertEquals( parsed + "check major version", major, artifactVersion.getMajorVersion() );
        assertEquals( parsed + "check minor version", minor, artifactVersion.getMinorVersion() );
        assertEquals( parsed + "check incremental version", incremental, artifactVersion.getIncrementalVersion() );
        assertEquals( parsed + "check build number", buildnumber, artifactVersion.getBuildNumber() );
        assertEquals( parsed + "check qualifier", qualifier, artifactVersion.getQualifier() );
        assertEquals( "check " + version + " string value", version, artifactVersion.toString() );
    }

    public void testVersionParsing()
    {
        checkVersionParsing( "1" , 1, 0, 0, 0, null );
        checkVersionParsing( "1.2" , 1, 2, 0, 0, null );
        checkVersionParsing( "1.2.3" , 1, 2, 3, 0, null );
        checkVersionParsing( "1.2.3-1" , 1, 2, 3, 1, null );
        checkVersionParsing( "1.2.3-alpha-1" , 1, 2, 3, 0, "alpha-1" );
        checkVersionParsing( "1.2-alpha-1" , 1, 2, 0, 0, "alpha-1" );
        checkVersionParsing( "1.2-alpha-1-20050205.060708-1" , 1, 2, 0, 0, "alpha-1-20050205.060708-1" );
        checkVersionParsing( "RELEASE" , 0, 0, 0, 0, "RELEASE" );
        checkVersionParsing( "2.0-1" , 2, 0, 0, 1, null );

        // 0 at the beginning of a number has a special handling
        checkVersionParsing( "02" , 0, 0, 0, 0, "02" );
        checkVersionParsing( "0.09" , 0, 0, 0, 0, "0.09" );
        checkVersionParsing( "0.2.09" , 0, 0, 0, 0, "0.2.09" );
        checkVersionParsing( "2.0-01" , 2, 0, 0, 0, "01" );

        // version schemes not really supported: fully transformed as qualifier
        checkVersionParsing( "1.0.1b" , 0, 0, 0, 0, "1.0.1b" );
        checkVersionParsing( "1.0M2" , 0, 0, 0, 0, "1.0M2" );
        checkVersionParsing( "1.0RC2" , 0, 0, 0, 0, "1.0RC2" );
        checkVersionParsing( "1.7.3.0" , 0, 0, 0, 0, "1.7.3.0" );
        checkVersionParsing( "1.7.3.0-1" , 0, 0, 0, 0, "1.7.3.0-1" );
        checkVersionParsing( "PATCH-1193602" , 0, 0, 0, 0, "PATCH-1193602" );
        checkVersionParsing( "5.0.0alpha-2006020117" , 0, 0, 0, 0, "5.0.0alpha-2006020117" );
        checkVersionParsing( "1.0.0.-SNAPSHOT", 0, 0, 0, 0, "1.0.0.-SNAPSHOT" );
        checkVersionParsing( "1..0-SNAPSHOT", 0, 0, 0, 0, "1..0-SNAPSHOT" );
        checkVersionParsing( "1.0.-SNAPSHOT", 0, 0, 0, 0, "1.0.-SNAPSHOT" );
        checkVersionParsing( ".1.0-SNAPSHOT", 0, 0, 0, 0, ".1.0-SNAPSHOT" );

        checkVersionParsing( "1.2.3.200705301630" , 0, 0, 0, 0, "1.2.3.200705301630" );
        checkVersionParsing( "1.2.3-200705301630" , 1, 2, 3, 0, "200705301630" );
    }

    public void testVersionComparing()
    {
        assertVersionEqual( "1", "1" );
        assertVersionOlder( "1", "2" );
        assertVersionOlder( "1.5", "2" );
        assertVersionOlder( "1", "2.5" );
        assertVersionEqual( "1", "1.0" );
        assertVersionEqual( "1", "1.0.0" );
        assertVersionOlder( "1.0", "1.1" );
        assertVersionOlder( "1.1", "1.2" );
        assertVersionOlder( "1.0.0", "1.1" );
        assertVersionOlder( "1.1", "1.2.0" );
        assertVersionOlder( "1.2", "1.10" );

        assertVersionOlder( "1.0-alpha-1", "1.0" );
        assertVersionOlder( "1.0-alpha-1", "1.0-alpha-2" );
        //assertVersionOlder( "1.0-alpha-2", "1.0-alpha-15" );
        assertVersionOlder( "1.0-alpha-1", "1.0-beta-1" );

        assertVersionOlder( "1.0-SNAPSHOT", "1.0-beta-1" );
        assertVersionOlder( "1.0-SNAPSHOT", "1.0" );
        assertVersionOlder( "1.0-alpha-1-SNAPSHOT", "1.0-alpha-1" );

        assertVersionOlder( "1.0", "1.0-1" );
        assertVersionOlder( "1.0-1", "1.0-2" );
        assertVersionEqual( "2.0-0", "2.0" );
        assertVersionOlder( "2.0", "2.0-1" );
        assertVersionOlder( "2.0.0", "2.0-1" );
        assertVersionOlder( "2.0-1", "2.0.1" );

        assertVersionOlder( "2.0.1-klm", "2.0.1-lmn" );
        assertVersionOlder( "2.0.1-xyz", "2.0.1" );

        assertVersionOlder( "2.0.1", "2.0.1-123" );
        assertVersionOlder( "2.0.1-xyz", "2.0.1-123" );
        //assertVersionOlder( "1.1-foo-2", "1.1-foo-10" );
    }

    public void testVersionSnapshotComparing()
    {
        assertVersionEqual( "1-SNAPSHOT", "1-SNAPSHOT" );
        assertVersionOlder( "1-SNAPSHOT", "2-SNAPSHOT" );
        assertVersionOlder( "1.5-SNAPSHOT", "2-SNAPSHOT" );
        assertVersionOlder( "1-SNAPSHOT", "2.5-SNAPSHOT" );
        assertVersionEqual( "1-SNAPSHOT", "1.0-SNAPSHOT" );
        assertVersionEqual( "1-SNAPSHOT", "1.0.0-SNAPSHOT" );
        assertVersionOlder( "1.0-SNAPSHOT", "1.1-SNAPSHOT" );
        assertVersionOlder( "1.1-SNAPSHOT", "1.2-SNAPSHOT" );
        assertVersionOlder( "1.0.0-SNAPSHOT", "1.1-SNAPSHOT" );
        assertVersionOlder( "1.1-SNAPSHOT", "1.2.0-SNAPSHOT" );

        //assertVersionOlder( "1.0-alpha-1-SNAPSHOT", "1.0-SNAPSHOT" );
        assertVersionOlder( "1.0-alpha-1-SNAPSHOT", "1.0-alpha-2-SNAPSHOT" );
        assertVersionOlder( "1.0-alpha-1-SNAPSHOT", "1.0-beta-1-SNAPSHOT" );

        assertVersionOlder( "1.0-SNAPSHOT-SNAPSHOT", "1.0-beta-1-SNAPSHOT" );
        assertVersionOlder( "1.0-SNAPSHOT-SNAPSHOT", "1.0-SNAPSHOT" );
        assertVersionOlder( "1.0-alpha-1-SNAPSHOT-SNAPSHOT", "1.0-alpha-1-SNAPSHOT" );

        //assertVersionOlder( "1.0-SNAPSHOT", "1.0-1-SNAPSHOT" );
        //assertVersionOlder( "1.0-1-SNAPSHOT", "1.0-2-SNAPSHOT" );
        //assertVersionEqual( "2.0-0-SNAPSHOT", "2.0-SNAPSHOT" );
        //assertVersionOlder( "2.0-SNAPSHOT", "2.0-1-SNAPSHOT" );
        //assertVersionOlder( "2.0.0-SNAPSHOT", "2.0-1-SNAPSHOT" );
        assertVersionOlder( "2.0-1-SNAPSHOT", "2.0.1-SNAPSHOT" );

        assertVersionOlder( "2.0.1-klm-SNAPSHOT", "2.0.1-lmn-SNAPSHOT" );
        // assertVersionOlder( "2.0.1-xyz-SNAPSHOT", "2.0.1-SNAPSHOT" );
        //assertVersionOlder( "2.0.1-SNAPSHOT", "2.0.1-123-SNAPSHOT" );
        //assertVersionOlder( "2.0.1-xyz-SNAPSHOT", "2.0.1-123-SNAPSHOT" );
    }


    public void testSnapshotVsReleases()
    {
        assertVersionOlder( "1.0-RC1", "1.0-SNAPSHOT" );
        //assertVersionOlder( "1.0-rc1", "1.0-SNAPSHOT" );
        //assertVersionOlder( "1.0-rc-1", "1.0-SNAPSHOT" );
    }

    private void assertVersionOlder( String left, String right )
    {
        assertTrue( left + " should be older than " + right,
                    newArtifactVersion( left ).compareTo( newArtifactVersion( right ) ) < 0 );
        assertTrue( right + " should be newer than " + left,
                    newArtifactVersion( right ).compareTo( newArtifactVersion( left ) ) > 0 );
    }

    private void assertVersionEqual( String left, String right )
    {
        assertTrue( left + " should be equal to " + right,
                    newArtifactVersion( left ).compareTo( newArtifactVersion( right ) ) == 0 );
        assertTrue( right + " should be equal to " + left,
                    newArtifactVersion( right ).compareTo( newArtifactVersion( left ) ) == 0 );
    }

    public void testVersionComparingWithBuildNumberZero()
    {
        ArtifactVersion v1 = newArtifactVersion( "2.0" );
        ArtifactVersion v2 = newArtifactVersion( "2.0-0" );
        ArtifactVersion v3 = newArtifactVersion( "2.0-alpha1" );
        ArtifactVersion v4 = newArtifactVersion( "2.0-1" );

        // v1 and v2 are equal
        assertTrue( v1.compareTo( v2 ) == 0 );
        assertTrue( v2.compareTo( v1 ) == 0 );

        // v1 is newer than v3
        assertTrue( v1.compareTo( v3 ) > 0 );
        assertTrue( v3.compareTo( v1 ) < 0 );

        // ergo, v2 should also be newer than v3
        assertTrue( v2.compareTo( v3 ) > 0 );
        assertTrue( v3.compareTo( v1 ) < 0 );

        // nonzero build numbers still respected
        assertTrue( v1.compareTo( v4 ) < 0 ); // build number one is always newer
        assertTrue( v4.compareTo( v1 ) > 0 );
        assertTrue( v2.compareTo( v4 ) < 0 ); // same results as v1
        assertTrue( v4.compareTo( v2 ) > 0 );
        assertTrue( v3.compareTo( v4 ) < 0 ); // qualifier is always older
        assertTrue( v4.compareTo( v3 ) > 0 );
    }

    public void testCompareToEqualsHashCodeConsistency()
    {
        ArtifactVersion v1;
        ArtifactVersion v2;

        // equal to itself
        v1 = newArtifactVersion( "1.3" );
        v2 = v1;
        assertTrue( v1.equals( v2 ) && v2.equals( v1 ) && ( v1.hashCode() == v2.hashCode() )
                        && ( v1.compareTo( v2 ) == 0 ) && ( v2.compareTo( v1 ) == 0 ) );

        // equal to something that means the same
        v1 = newArtifactVersion( "1" );
        v2 = newArtifactVersion( "1.0.0-0" );
        assertTrue( v1.equals( v2 ) && v2.equals( v1 ) && ( v1.hashCode() == v2.hashCode() )
                        && ( v1.compareTo( v2 ) == 0 ) && ( v2.compareTo( v1 ) == 0 ) );

        // equal with qualifier
        v1 = newArtifactVersion( "1.3-alpha1" );
        v2 = newArtifactVersion( "1.3-alpha1" );
        assertTrue( v1.equals( v2 ) && v2.equals( v1 ) && ( v1.hashCode() == v2.hashCode() )
                        && ( v1.compareTo( v2 ) == 0 ) && ( v2.compareTo( v1 ) == 0 ) );

        // longer qualifier with same start is *newer*
        v1 = newArtifactVersion( "1.3-alpha1" );
        v2 = newArtifactVersion( "1.3-alpha1-1" );
        assertTrue( !v1.equals( v2 ) && !v2.equals( v1 ) && ( v1.compareTo( v2 ) > 0 ) && ( v2.compareTo( v1 ) < 0 ) );

        // different qualifiers alpha compared
        v1 = newArtifactVersion( "1.3-alpha1" );
        v2 = newArtifactVersion( "1.3-beta1" );
        assertTrue( !v1.equals( v2 ) && !v2.equals( v1 ) && ( v1.compareTo( v2 ) < 0 ) && ( v2.compareTo( v1 ) > 0 ) );
    }

    public void testTransitivity()
    {
        ArtifactVersion v1 = newArtifactVersion( "1" );
        ArtifactVersion v2 = newArtifactVersion( "1.0-0" );
        ArtifactVersion v3 = newArtifactVersion( "1.0.1" );
        ArtifactVersion v4 = newArtifactVersion( "1.0-beta1" );

        // v1 and v2 are equal
        assertTrue( v1.equals( v2 ) && v2.equals( v1 ) && ( v1.compareTo( v2 ) == 0 ) && ( v2.compareTo( v1 ) == 0 ) );

        // v1 is older than v3
        assertTrue( !v1.equals( v3 ) && !v3.equals( v1 ) && ( v1.compareTo( v3 ) < 0 ) && ( v3.compareTo( v1 ) > 0 ) );

        // ergo, v2 is older than v3
        assertTrue( !v2.equals( v3 ) && !v3.equals( v2 ) && ( v2.compareTo( v3 ) < 0 ) && ( v3.compareTo( v2 ) > 0 ) );

        // v1 is newer than v4
        assertTrue( !v1.equals( v4 ) && !v4.equals( v1 ) && ( v1.compareTo( v4 ) > 0 ) && ( v4.compareTo( v1 ) < 0 ) );

        // ergo, v2 is newer than v4
        assertTrue( !v2.equals( v4 ) && !v4.equals( v2 ) && ( v2.compareTo( v4 ) > 0 ) && ( v4.compareTo( v2 ) < 0 ) );
    }

    private void testInterfaceCompare( String version )
    {
        final ArtifactVersion dav = newArtifactVersion( version );

        // create an anonymous instance to compare the big daddy to
        ArtifactVersion av = new ArtifactVersion()
        {
            public int getMajorVersion()
            {
                return dav.getMajorVersion();
            }

            public int getMinorVersion()
            {
                return dav.getMinorVersion();
            }

            public int getIncrementalVersion()
            {
                return dav.getIncrementalVersion();
            }

            public int getBuildNumber()
            {
                return dav.getBuildNumber();
            }

            public String getQualifier()
            {
                return dav.getQualifier();
            }

            // required by interface but unused for our test
            public int compareTo( Object o )
            {
                return 0; /* bogus */
            }

            public void parseVersion( String s )
            { /* bogus */
            }
        };

        assertTrue( dav.equals( av ) );
        assertTrue( dav.compareTo( av ) == 0 );
    }

    public void testInterfaceCompares()
    {
        testInterfaceCompare( "1" );
        testInterfaceCompare( "1.2" );
        testInterfaceCompare( "1.2.3" );
        testInterfaceCompare( "1.2.3-4" );
        testInterfaceCompare( "1.2.3-four" );
        testInterfaceCompare( "1-2" );
        testInterfaceCompare( "1-two" );
    }
}
