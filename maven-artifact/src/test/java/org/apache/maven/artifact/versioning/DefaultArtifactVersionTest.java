package org.apache.maven.artifact.versioning;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
    }

    public void testVersionComparing()
    {
        DefaultArtifactVersion version = new DefaultArtifactVersion( "1" );
        assertTrue( version.compareTo( new DefaultArtifactVersion( "2" ) ) < 0 );

        version = new DefaultArtifactVersion( "1.5" );
        assertTrue( version.compareTo( new DefaultArtifactVersion( "2" ) ) < 0 );

        version = new DefaultArtifactVersion( "1" );
        assertTrue( version.compareTo( new DefaultArtifactVersion( "1" ) ) == 0 );

        version = new DefaultArtifactVersion( "2" );
        assertTrue( version.compareTo( new DefaultArtifactVersion( "1" ) ) > 0 );

        version = new DefaultArtifactVersion( "2.5" );
        assertTrue( version.compareTo( new DefaultArtifactVersion( "1" ) ) > 0 );

        version = new DefaultArtifactVersion( "1.0" );
        assertTrue( version.compareTo( new DefaultArtifactVersion( "1" ) ) > 0 );

        version = new DefaultArtifactVersion( "1.0.0" );
        assertTrue( version.compareTo( new DefaultArtifactVersion( "1" ) ) > 0 );

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
    }
}
