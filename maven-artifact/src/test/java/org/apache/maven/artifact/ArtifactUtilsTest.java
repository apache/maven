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
package org.apache.maven.artifact;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.versioning.VersionRange;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests {@link ArtifactUtils}.
 *
 * @author Benjamin Bentmann
 */
public class ArtifactUtilsTest
{

    private Artifact newArtifact( String aid )
    {
        return new DefaultArtifact( "group", aid, VersionRange.createFromVersion( "1.0" ), "test", "jar", "tests",
                                    null );
    }

    @Test
    public void testIsSnapshot()
    {
        assertFalse( ArtifactUtils.isSnapshot( null ) );
        assertFalse( ArtifactUtils.isSnapshot( "" ) );
        assertFalse( ArtifactUtils.isSnapshot( "1.2.3" ) );
        assertTrue( ArtifactUtils.isSnapshot( "1.2.3-SNAPSHOT" ) );
        assertTrue( ArtifactUtils.isSnapshot( "1.2.3-snapshot" ) );
        assertTrue( ArtifactUtils.isSnapshot( "1.2.3-20090413.094722-2" ) );
        assertFalse( ArtifactUtils.isSnapshot( "1.2.3-20090413X094722-2" ) );
    }

    @Test
    public void testToSnapshotVersion()
    {
        assertEquals( "1.2.3", ArtifactUtils.toSnapshotVersion( "1.2.3" ) );
        assertEquals( "1.2.3-SNAPSHOT", ArtifactUtils.toSnapshotVersion( "1.2.3-SNAPSHOT" ) );
        assertEquals( "1.2.3-SNAPSHOT", ArtifactUtils.toSnapshotVersion( "1.2.3-20090413.094722-2" ) );
        assertEquals( "1.2.3-20090413X094722-2", ArtifactUtils.toSnapshotVersion( "1.2.3-20090413X094722-2" ) );
    }

    /**
     * Tests that the ordering of the map resembles the ordering of the input collection of artifacts.
     */
    @Test
    public void testArtifactMapByVersionlessIdOrdering()
        throws Exception
    {
        List<Artifact> list = new ArrayList<>();
        list.add( newArtifact( "b" ) );
        list.add( newArtifact( "a" ) );
        list.add( newArtifact( "c" ) );
        list.add( newArtifact( "e" ) );
        list.add( newArtifact( "d" ) );

        Map<String, Artifact> map = ArtifactUtils.artifactMapByVersionlessId( list );
        assertNotNull( map );
        assertEquals( list, new ArrayList<>( map.values() ) );
    }

}
