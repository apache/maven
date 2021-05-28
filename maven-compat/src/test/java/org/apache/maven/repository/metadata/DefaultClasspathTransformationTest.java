package org.apache.maven.repository.metadata;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

import javax.inject.Inject;

import org.apache.maven.artifact.ArtifactScopeEnum;
import org.codehaus.plexus.testing.PlexusTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 *
 * @author <a href="mailto:oleg@codehaus.org">Oleg Gusakov</a>
 *
 */
@PlexusTest
public class DefaultClasspathTransformationTest
{
    @Inject
    ClasspathTransformation transform;

    MetadataGraph graph;

    MetadataGraphVertex v1;
    MetadataGraphVertex v2;
    MetadataGraphVertex v3;
    MetadataGraphVertex v4;
    //------------------------------------------------------------------------------------------
	@BeforeEach
    public void setUp() throws Exception
    {
        graph = new MetadataGraph( 4, 3 );
        /*
         *       v2
         *   v1<
         *       v3-v4
         *
         */
        v1 = graph.addVertex(new ArtifactMetadata("g","a1","1.0"));
        graph.setEntry(v1);
        v2 = graph.addVertex(new ArtifactMetadata("g","a2","1.0"));
        v3 = graph.addVertex(new ArtifactMetadata("g","a3","1.0"));
        v4 = graph.addVertex(new ArtifactMetadata("g","a4","1.0"));

        // v1-->v2
        graph.addEdge(v1, v2, new MetadataGraphEdge( "1.1", true, null, null, 2, 1 ) );
        graph.addEdge(v1, v2, new MetadataGraphEdge( "1.2", true, null, null, 2, 2 ) );

        // v1-->v3
        graph.addEdge(v1, v3, new MetadataGraphEdge( "1.1", true, null, null, 2, 1 ) );
        graph.addEdge(v1, v3, new MetadataGraphEdge( "1.2", true, null, null, 4, 2 ) );

        // v3-->v4
        graph.addEdge(v3, v4, new MetadataGraphEdge( "1.1", true, ArtifactScopeEnum.runtime, null, 2, 2 ) );
        graph.addEdge(v3, v4, new MetadataGraphEdge( "1.2", true, ArtifactScopeEnum.test, null, 2, 2 ) );
    }
    //------------------------------------------------------------------------------------------
	@Test
    public void testCompileClasspathTransform()
    throws Exception
    {
        ClasspathContainer res;

        res = transform.transform( graph, ArtifactScopeEnum.compile, false );

        assertNotNull( res, "null classpath container after compile transform" );
        assertNotNull( res.getClasspath(), "null classpath after compile transform" );
        assertEquals( 3, res.getClasspath().size(), "compile classpath should have 3 entries" );
    }
    //------------------------------------------------------------------------------------------
	@Test
    public void testRuntimeClasspathTransform()
    throws Exception
    {
        ClasspathContainer res;

        res = transform.transform( graph, ArtifactScopeEnum.runtime, false );

        assertNotNull( res, "null classpath container after runtime transform" );
        assertNotNull( res.getClasspath(), "null classpath after runtime transform" );
        assertEquals( 4, res.getClasspath().size(), "runtime classpath should have 4 entries" );

        ArtifactMetadata md = res.getClasspath().get(3);
        assertEquals("1.1", md.getVersion(), "runtime artifact version should be 1.1" );
    }
    //------------------------------------------------------------------------------------------
	@Test
    public void testTestClasspathTransform()
    throws Exception
    {
        ClasspathContainer res;

        res = transform.transform( graph, ArtifactScopeEnum.test, false );

        assertNotNull( res, "null classpath container after test transform" );
        assertNotNull( res.getClasspath(), "null classpath after test transform" );
        assertEquals( 4, res.getClasspath().size(), "test classpath should have 4 entries" );

        ArtifactMetadata md = res.getClasspath().get(3);
        assertEquals("1.2", md.getVersion(), "test artifact version should be 1.2" );
    }
    //------------------------------------------------------------------------------------------
    //------------------------------------------------------------------------------------------
}
