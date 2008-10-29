package org.apache.maven.artifact;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.versioning.VersionRange;

import junit.framework.TestCase;

/**
 * Tests {@link ArtifactUtils}.
 * 
 * @author Benjamin Bentmann
 * @version $Id$
 */
public class ArtifactUtilsTest
    extends TestCase
{

    /**
     * Tests that the ordering of the map resembles the ordering of the input collection of artifacts.
     */
    public void testArtifactMapByArtifactIdOrdering()
        throws Exception
    {
        List list = new ArrayList();
        list.add( newArtifact( "b" ) );
        list.add( newArtifact( "a" ) );
        list.add( newArtifact( "c" ) );
        list.add( newArtifact( "e" ) );
        list.add( newArtifact( "d" ) );

        Map map = ArtifactUtils.artifactMapByArtifactId( list );
        assertNotNull( map );
        assertEquals( list, new ArrayList( map.values() ) );
    }

    /**
     * Tests that the ordering of the map resembles the ordering of the input collection of artifacts.
     */
    public void testArtifactMapByVersionlessIdOrdering()
        throws Exception
    {
        List list = new ArrayList();
        list.add( newArtifact( "b" ) );
        list.add( newArtifact( "a" ) );
        list.add( newArtifact( "c" ) );
        list.add( newArtifact( "e" ) );
        list.add( newArtifact( "d" ) );

        Map map = ArtifactUtils.artifactMapByVersionlessId( list );
        assertNotNull( map );
        assertEquals( list, new ArrayList( map.values() ) );
    }

    private Artifact newArtifact( String aid )
    {
        return new DefaultArtifact( "org.apache.maven.ut", aid, VersionRange.createFromVersion( "1.0" ), "test", "jar",
                                    "tests", null );
    }

}
