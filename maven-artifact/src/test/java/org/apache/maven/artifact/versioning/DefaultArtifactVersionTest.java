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
    public void testVersionParsing()
    {
        DefaultArtifactVersion version = new DefaultArtifactVersion( "1" );
        assertEquals( "check major version", 1, version.getMajorVersion() );
        assertEquals( "check minor version", 0, version.getMinorVersion() );
        assertEquals( "check incremental version", 0, version.getIncrementalVersion() );
        assertEquals( "check build number", 0, version.getBuildNumber() );
        assertNull( "check qualifier", version.getQualifier() );

        version = new DefaultArtifactVersion( "1.2" );
        assertEquals( "check major version", 1, version.getMajorVersion() );
        assertEquals( "check minor version", 2, version.getMinorVersion() );
        assertEquals( "check incremental version", 0, version.getIncrementalVersion() );
        assertEquals( "check build number", 0, version.getBuildNumber() );
        assertNull( "check qualifier", version.getQualifier() );

        version = new DefaultArtifactVersion( "1.2.3" );
        assertEquals( "check major version", 1, version.getMajorVersion() );
        assertEquals( "check minor version", 2, version.getMinorVersion() );
        assertEquals( "check incremental version", 3, version.getIncrementalVersion() );
        assertEquals( "check build number", 0, version.getBuildNumber() );
        assertNull( "check qualifier", version.getQualifier() );

        version = new DefaultArtifactVersion( "1.2.3-1" );
        assertEquals( "check major version", 1, version.getMajorVersion() );
        assertEquals( "check minor version", 2, version.getMinorVersion() );
        assertEquals( "check incremental version", 3, version.getIncrementalVersion() );
        assertEquals( "check build number", 1, version.getBuildNumber() );
        assertNull( "check qualifier", version.getQualifier() );

        version = new DefaultArtifactVersion( "1.2.3-alpha-1" );
        assertEquals( "check major version", 1, version.getMajorVersion() );
        assertEquals( "check minor version", 2, version.getMinorVersion() );
        assertEquals( "check incremental version", 3, version.getIncrementalVersion() );
        assertEquals( "check build number", 0, version.getBuildNumber() );
        assertEquals( "check qualifier", "alpha-1", version.getQualifier() );

        version = new DefaultArtifactVersion( "1.2-alpha-1" );
        assertEquals( "check major version", 1, version.getMajorVersion() );
        assertEquals( "check minor version", 2, version.getMinorVersion() );
        assertEquals( "check incremental version", 0, version.getIncrementalVersion() );
        assertEquals( "check build number", 0, version.getBuildNumber() );
        assertEquals( "check qualifier", "alpha-1", version.getQualifier() );

        version = new DefaultArtifactVersion( "1.2-alpha-1-20050205.060708-1" );
        assertEquals( "check major version", 1, version.getMajorVersion() );
        assertEquals( "check minor version", 2, version.getMinorVersion() );
        assertEquals( "check incremental version", 0, version.getIncrementalVersion() );
        assertEquals( "check build number", 0, version.getBuildNumber() );
        assertEquals( "check qualifier", "alpha-1-20050205.060708-1", version.getQualifier() );

        version = new DefaultArtifactVersion( "RELEASE" );
        assertEquals( "check major version", 0, version.getMajorVersion() );
        assertEquals( "check minor version", 0, version.getMinorVersion() );
        assertEquals( "check incremental version", 0, version.getIncrementalVersion() );
        assertEquals( "check build number", 0, version.getBuildNumber() );
        assertEquals( "check qualifier", "RELEASE", version.getQualifier() );

        version = new DefaultArtifactVersion( "1.0.1b" );
        assertEquals( "check major version", 0, version.getMajorVersion() );
        assertEquals( "check minor version", 0, version.getMinorVersion() );
        assertEquals( "check incremental version", 0, version.getIncrementalVersion() );
        assertEquals( "check build number", 0, version.getBuildNumber() );
        assertEquals( "check qualifier", "1.0.1b", version.getQualifier() );

        version = new DefaultArtifactVersion( "1.0RC2" );
        assertEquals( "check major version", 0, version.getMajorVersion() );
        assertEquals( "check minor version", 0, version.getMinorVersion() );
        assertEquals( "check incremental version", 0, version.getIncrementalVersion() );
        assertEquals( "check build number", 0, version.getBuildNumber() );
        assertEquals( "check qualifier", "1.0RC2", version.getQualifier() );

        version = new DefaultArtifactVersion( "1.7.3.0" );
        assertEquals( "check major version", 0, version.getMajorVersion() );
        assertEquals( "check minor version", 0, version.getMinorVersion() );
        assertEquals( "check incremental version", 0, version.getIncrementalVersion() );
        assertEquals( "check build number", 0, version.getBuildNumber() );
        assertEquals( "check qualifier", "1.7.3.0", version.getQualifier() );

        version = new DefaultArtifactVersion( "0.09" );
        assertEquals( "check major version", 0, version.getMajorVersion() );
        assertEquals( "check minor version", 0, version.getMinorVersion() );
        assertEquals( "check incremental version", 0, version.getIncrementalVersion() );
        assertEquals( "check build number", 0, version.getBuildNumber() );
        assertEquals( "check qualifier", "0.09", version.getQualifier() );

        version = new DefaultArtifactVersion( "02" );
        assertEquals( "check major version", 0, version.getMajorVersion() );
        assertEquals( "check minor version", 0, version.getMinorVersion() );
        assertEquals( "check incremental version", 0, version.getIncrementalVersion() );
        assertEquals( "check build number", 0, version.getBuildNumber() );
        assertEquals( "check qualifier", "02", version.getQualifier() );

        version = new DefaultArtifactVersion( "PATCH-1193602" );
        assertEquals( "check major version", 0, version.getMajorVersion() );
        assertEquals( "check minor version", 0, version.getMinorVersion() );
        assertEquals( "check incremental version", 0, version.getIncrementalVersion() );
        assertEquals( "check build number", 0, version.getBuildNumber() );
        assertEquals( "check qualifier", "PATCH-1193602", version.getQualifier() );

        version = new DefaultArtifactVersion( "2.0-1" );
        assertEquals( "check major version", 2, version.getMajorVersion() );
        assertEquals( "check minor version", 0, version.getMinorVersion() );
        assertEquals( "check incremental version", 0, version.getIncrementalVersion() );
        assertEquals( "check build number", 1, version.getBuildNumber() );
        assertNull( "check qualifier", version.getQualifier() );
        assertEquals( "check string value", "2.0-1", version.toString() );

        version = new DefaultArtifactVersion( "5.0.0alpha-2006020117" );
        assertEquals( "check major version", 0, version.getMajorVersion() );
        assertEquals( "check minor version", 0, version.getMinorVersion() );
        assertEquals( "check incremental version", 0, version.getIncrementalVersion() );
        assertEquals( "check build number", 0, version.getBuildNumber() );
        assertEquals( "check qualifier", "5.0.0alpha-2006020117", version.getQualifier() );
    }

    public void testVersionComparing()
    {
        DefaultArtifactVersion version = new DefaultArtifactVersion( "1" );
        assertTrue( version.compareTo( new DefaultArtifactVersion( "2" ) ) < 0 );

        version = new DefaultArtifactVersion( "1.5" );
        assertTrue( version.compareTo( new DefaultArtifactVersion( "2" ) ) < 0 );

        version = new DefaultArtifactVersion( "1" );
        assertEquals( 0, version.compareTo( new DefaultArtifactVersion( "1" ) ) );

        version = new DefaultArtifactVersion( "2" );
        assertTrue( version.compareTo( new DefaultArtifactVersion( "1" ) ) > 0 );

        version = new DefaultArtifactVersion( "2.5" );
        assertTrue( version.compareTo( new DefaultArtifactVersion( "1" ) ) > 0 );

        version = new DefaultArtifactVersion( "1.0" );
        assertTrue( version.compareTo( new DefaultArtifactVersion( "1" ) ) == 0 );

        version = new DefaultArtifactVersion( "1.0.0" );
        assertTrue( version.compareTo( new DefaultArtifactVersion( "1" ) ) == 0 );

        version = new DefaultArtifactVersion( "1.0" );
        assertTrue( version.compareTo( new DefaultArtifactVersion( "1.1" ) ) < 0 );

        version = new DefaultArtifactVersion( "1.2" );
        assertTrue( version.compareTo( new DefaultArtifactVersion( "1.1" ) ) > 0 );

        version = new DefaultArtifactVersion( "1.0.0" );
        assertTrue( version.compareTo( new DefaultArtifactVersion( "1.1" ) ) < 0 );

        version = new DefaultArtifactVersion( "1.2.0" );
        assertTrue( version.compareTo( new DefaultArtifactVersion( "1.1" ) ) > 0 );

        version = new DefaultArtifactVersion( "1.0-alpha-1" );
        assertTrue( version.compareTo( new DefaultArtifactVersion( "1.0" ) ) < 0 );

        version = new DefaultArtifactVersion( "1.0-alpha-1" );
        assertTrue( version.compareTo( new DefaultArtifactVersion( "1.0-alpha-2" ) ) < 0 );

        version = new DefaultArtifactVersion( "1.0-alpha-1" );
        assertTrue( version.compareTo( new DefaultArtifactVersion( "1.0-beta-1" ) ) < 0 );

        version = new DefaultArtifactVersion( "1.0" );
        assertTrue( version.compareTo( new DefaultArtifactVersion( "1.0-alpha-1" ) ) > 0 );

        version = new DefaultArtifactVersion( "1.0-alpha-2" );
        assertTrue( version.compareTo( new DefaultArtifactVersion( "1.0-alpha-1" ) ) > 0 );

        version = new DefaultArtifactVersion( "1.0-beta-1" );
        assertTrue( version.compareTo( new DefaultArtifactVersion( "1.0-alpha-1" ) ) > 0 );

        version = new DefaultArtifactVersion( "1.0-beta-1" );
        assertTrue( version.compareTo( new DefaultArtifactVersion( "1.0-SNAPSHOT" ) ) > 0 );

        version = new DefaultArtifactVersion( "1.0-SNAPSHOT" );
        assertTrue( version.compareTo( new DefaultArtifactVersion( "1.0-beta-1" ) ) < 0 );

        version = new DefaultArtifactVersion( "1.0-SNAPSHOT" );
        assertTrue( version.compareTo( new DefaultArtifactVersion( "1.0" ) ) < 0 );

        version = new DefaultArtifactVersion( "1.0" );
        assertTrue( version.compareTo( new DefaultArtifactVersion( "1.0-SNAPSHOT" ) ) > 0 );

        version = new DefaultArtifactVersion( "1.0-alpha-1-SNAPSHOT" );
        assertTrue( version.compareTo( new DefaultArtifactVersion( "1.0-alpha-1" ) ) < 0 );

        version = new DefaultArtifactVersion( "1.0-alpha-1" );
        assertTrue( version.compareTo( new DefaultArtifactVersion( "1.0-alpha-1-SNAPSHOT" ) ) > 0 );

        version = new DefaultArtifactVersion( "1.0" );
        assertTrue( version.compareTo( new DefaultArtifactVersion( "1.0-1" ) ) < 0 );

        version = new DefaultArtifactVersion( "1.0-1" );
        assertTrue( version.compareTo( new DefaultArtifactVersion( "1.0-2" ) ) < 0 );

        version = new DefaultArtifactVersion( "1.0-1" );
        assertTrue( version.compareTo( new DefaultArtifactVersion( "1.0" ) ) > 0 );

        version = new DefaultArtifactVersion( "1.0-2" );
        assertTrue( version.compareTo( new DefaultArtifactVersion( "1.0-1" ) ) > 0 );

        version = new DefaultArtifactVersion( "2.0-0" );
        assertTrue( version.compareTo( new DefaultArtifactVersion( "2.0" ) ) == 0 );

        version = new DefaultArtifactVersion( "2.0-1" );
        assertTrue( version.compareTo( new DefaultArtifactVersion( "2.0" ) ) > 0 );

        version = new DefaultArtifactVersion( "2.0-1" );
        assertTrue( version.compareTo( new DefaultArtifactVersion( "2.0.0" ) ) > 0 );

        version = new DefaultArtifactVersion( "2.0-1" );
        assertTrue( version.compareTo( new DefaultArtifactVersion( "2.0.1" ) ) < 0 );

        version = new DefaultArtifactVersion( "1.10" );
        assertTrue( version.compareTo( new DefaultArtifactVersion( "1.2" ) ) > 0 );

        // version = new DefaultArtifactVersion( "1.1-foo-10" );
        // assertTrue( version.compareTo( new DefaultArtifactVersion( "1.1-foo-2" ) ) > 0 );
    }

    public void testVersionComparingWithBuildNumberZero()
    {
        DefaultArtifactVersion v1 = new DefaultArtifactVersion( "2.0" );
        DefaultArtifactVersion v2 = new DefaultArtifactVersion( "2.0-0" );
        DefaultArtifactVersion v3 = new DefaultArtifactVersion( "2.0-alpha1" );
        DefaultArtifactVersion v4 = new DefaultArtifactVersion( "2.0-1" );

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
        DefaultArtifactVersion v1;
        DefaultArtifactVersion v2;

        // equal to itself
        v1 = new DefaultArtifactVersion( "1.3" );
        v2 = v1;
        assertTrue( v1.equals( v2 ) && v2.equals( v1 ) && ( v1.hashCode() == v2.hashCode() ) && ( v1.compareTo( v2 ) == 0 )
                    && ( v2.compareTo( v1 ) == 0 ) );

        // equal to something that means the same
        v1 = new DefaultArtifactVersion( "1" );
        v2 = new DefaultArtifactVersion( "1.0.0-0" );
        assertTrue( v1.equals( v2 ) && v2.equals( v1 ) && ( v1.hashCode() == v2.hashCode() ) && ( v1.compareTo( v2 ) == 0 )
                    && ( v2.compareTo( v1 ) == 0 ) );

        // equal with qualifier
        v1 = new DefaultArtifactVersion( "1.3-alpha1" );
        v2 = new DefaultArtifactVersion( "1.3-alpha1" );
        assertTrue( v1.equals( v2 ) && v2.equals( v1 ) && ( v1.hashCode() == v2.hashCode() ) && ( v1.compareTo( v2 ) == 0 )
                    && ( v2.compareTo( v1 ) == 0 ) );

        // longer qualifier with same start is *newer*
        v1 = new DefaultArtifactVersion( "1.3-alpha1" );
        v2 = new DefaultArtifactVersion( "1.3-alpha1-1" );
        assertTrue( !v1.equals( v2 ) && !v2.equals( v1 ) && ( v1.compareTo( v2 ) > 0 ) && ( v2.compareTo( v1 ) < 0 ) );

        // different qualifiers alpha compared
        v1 = new DefaultArtifactVersion( "1.3-alpha1" );
        v2 = new DefaultArtifactVersion( "1.3-beta1" );
        assertTrue( !v1.equals( v2 ) && !v2.equals( v1 ) && ( v1.compareTo( v2 ) < 0 ) && ( v2.compareTo( v1 ) > 0 ) );
    }

    public void testTransitivity()
    {
        DefaultArtifactVersion v1 = new DefaultArtifactVersion( "1" );
        DefaultArtifactVersion v2 = new DefaultArtifactVersion( "1.0-0" );
        DefaultArtifactVersion v3 = new DefaultArtifactVersion( "1.0.1" );
        DefaultArtifactVersion v4 = new DefaultArtifactVersion( "1.0-beta1" );

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
        final DefaultArtifactVersion dav = new DefaultArtifactVersion( version );

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
