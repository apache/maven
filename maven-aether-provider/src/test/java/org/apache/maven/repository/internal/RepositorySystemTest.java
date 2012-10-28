package org.apache.maven.repository.internal;

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

import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.collection.CollectRequest;
import org.sonatype.aether.collection.CollectResult;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.util.artifact.DefaultArtifact;

public class RepositorySystemTest
    extends AbstractRepositoryTestCase
{
    public void testCollectDependencies()
        throws Exception
    {
        String artifactCoords = "ut.simple:artifact:1.0"; // TODO test extension:classifier
        Artifact artifact = new DefaultArtifact( artifactCoords );

        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot( new Dependency( artifact, null ) );
        collectRequest.addRepository( newTestRepository() );

        CollectResult collectResult = system.collectDependencies( session, collectRequest );

        assertEquals( 2, collectResult.getRoot().getChildren().size() );
        Dependency dep = collectResult.getRoot().getChildren().get( 0 ).getDependency();
        assertEquals( "compile", dep.getScope() );
        assertFalse( dep.isOptional() );
        assertEquals( 0, dep.getExclusions().size() );
        Artifact depArtifact = dep.getArtifact();
        assertEquals( "ut.simple", depArtifact.getGroupId() );
        assertEquals( "dependency", depArtifact.getArtifactId() );
        assertEquals( "1.0", depArtifact.getVersion() );
        assertEquals( "1.0", depArtifact.getBaseVersion() );
        assertNull( depArtifact.getFile() );
        assertFalse( depArtifact.isSnapshot() );
        assertEquals( "", depArtifact.getClassifier() );
        assertEquals( "jar", depArtifact.getExtension() );
        assertEquals( "java", depArtifact.getProperty( "language", null ) );
        assertEquals( "jar", depArtifact.getProperty( "type", null ) );
        assertEquals( "true", depArtifact.getProperty( "constitutesBuildPath", null ) );
        assertEquals( "false", depArtifact.getProperty( "includesDependencies", null ) );
        assertEquals( 4, depArtifact.getProperties().size() );

        dep = collectResult.getRoot().getChildren().get( 1 ).getDependency();
        assertEquals( "compile", dep.getScope() );
        assertFalse( dep.isOptional() );
        assertEquals( 0, dep.getExclusions().size() );
        depArtifact = dep.getArtifact();
        assertEquals( "ut.simple", depArtifact.getGroupId() );
        assertEquals( "dependency", depArtifact.getArtifactId() );
        assertEquals( "1.0", depArtifact.getVersion() );
        assertEquals( "1.0", depArtifact.getBaseVersion() );
        assertNull( depArtifact.getFile() );
        assertFalse( depArtifact.isSnapshot() );
        assertEquals( "sources", depArtifact.getClassifier() );
        assertEquals( "jar", depArtifact.getExtension() );
        assertEquals( "java", depArtifact.getProperty( "language", null ) );
        assertEquals( "jar", depArtifact.getProperty( "type", null ) ); // shouldn't it be java-sources given the classifier?
        assertEquals( "true", depArtifact.getProperty( "constitutesBuildPath", null ) ); // shouldn't it be false given the classifier?
        assertEquals( "false", depArtifact.getProperty( "includesDependencies", null ) );
        assertEquals( 4, depArtifact.getProperties().size() );
    }
}
