package org.apache.maven.project.artifact;

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

import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
public class DefaultProjectArtifactsCacheTest
{

    private ProjectArtifactsCache cache;

    @BeforeEach
    public void setUp()
        throws Exception
    {
        cache = new DefaultProjectArtifactsCache();
    }

    @Test
    public void testProjectDependencyOrder() throws Exception
    {
        ProjectArtifactsCache.Key project1 = new ProjectArtifactsCache.Key(){};

        Set<Artifact> artifacts = new LinkedHashSet<>( 4 );
        artifacts.add( new DefaultArtifact( "g", "a1", "v", "compile", "jar", "", null ) );
        artifacts.add( new DefaultArtifact( "g", "a2", "v", "compile", "jar", "", null ) );
        artifacts.add( new DefaultArtifact( "g", "a3", "v", "compile", "jar", "", null ) );
        artifacts.add( new DefaultArtifact( "g", "a4", "v", "compile", "jar", "", null ) );

        cache.put( project1, artifacts );

        assertArrayEquals( artifacts.toArray( new Artifact[0] ),
                           cache.get( project1 ).getArtifacts().toArray( new Artifact[0] ) );

        ProjectArtifactsCache.Key project2 = new ProjectArtifactsCache.Key(){};

        Set<Artifact> reversedArtifacts = new LinkedHashSet<>( 4 );
        artifacts.add( new DefaultArtifact( "g", "a4", "v", "compile", "jar", "", null ) );
        artifacts.add( new DefaultArtifact( "g", "a3", "v", "compile", "jar", "", null ) );
        artifacts.add( new DefaultArtifact( "g", "a2", "v", "compile", "jar", "", null ) );
        artifacts.add( new DefaultArtifact( "g", "a1", "v", "compile", "jar", "", null ) );

        cache.put( project2, reversedArtifacts );

        assertArrayEquals( reversedArtifacts.toArray( new Artifact[0] ),
                           cache.get( project2 ).getArtifacts().toArray( new Artifact[0] ) );
    }
}
