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

import java.net.MalformedURLException;

import org.apache.maven.repository.internal.util.ConsoleRepositoryListener;
import org.apache.maven.repository.internal.util.ConsoleTransferListener;
import org.codehaus.plexus.PlexusTestCase;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.collection.CollectRequest;
import org.sonatype.aether.collection.CollectResult;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.repository.LocalRepository;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;

public class RepositorySystemTest
    extends PlexusTestCase
{
    protected RepositorySystem system;

    protected RepositorySystemSession session;

    @Override
    protected void setUp()
        throws Exception
    {
        system = lookup( RepositorySystem.class );
        session = newMavenRepositorySystemSession( system );
    }

    @Override
    protected void tearDown()
        throws Exception
    {
        session = null;
        system = null;
        super.tearDown();
    }

    public void testCollectDependencies()
        throws Exception
    {
        String artifactCoords = "ut.simple:artifact:1.0"; // TODO test extension:classifier
        Artifact artifact = new DefaultArtifact( artifactCoords );

        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot( new Dependency( artifact, "" ) );
        collectRequest.addRepository( newTestRepository() );

        CollectResult collectResult = system.collectDependencies( session, collectRequest );

        assertEquals( 1, collectResult.getRoot().getChildren().size() );
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
    }

    public static RepositorySystemSession newMavenRepositorySystemSession( RepositorySystem system )
    {
        MavenRepositorySystemSession session = new MavenRepositorySystemSession();

        LocalRepository localRepo = new LocalRepository( "target/local-repo" );
        session.setLocalRepositoryManager( system.newLocalRepositoryManager( localRepo ) );

        session.setTransferListener( new ConsoleTransferListener() );
        session.setRepositoryListener( new ConsoleRepositoryListener() );

        return session;
    }

    public static RemoteRepository newTestRepository()
        throws MalformedURLException
    {
        return new RemoteRepository( "repo", "default", getTestFile( "target/test-classes/repo" ).toURI().toURL().toString() );
    }
}
