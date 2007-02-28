package org.apache.maven.artifact.manager;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.repository.Repository;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;

public class DefaultArtifactManagerTest
    extends PlexusTestCase
{
    private ArtifactFactory artifactFactory;

    private ArtifactManager artifactManager;

    private File localTestRepo;

    private File remoteTestRepo;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        artifactFactory = (ArtifactFactory) lookup( ArtifactFactory.ROLE );
        artifactManager = (ArtifactManager) lookup( ArtifactManager.ROLE );

        localTestRepo = setupDir( "target/test-classes/repositories/" + getName() + "/local-repository" );
        remoteTestRepo = setupDir( "target/test-classes/repositories/" + getName() + "/remote-repository" );
    }

    private File setupDir( String rootPath )
        throws IOException
    {
        File rootDir = new File( getBasedir(), rootPath );

        // Clean up from old tests.
        if ( rootDir.exists() )
        {
            FileUtils.deleteDirectory( rootDir );
        }

        // Create dir
        rootDir.mkdirs();

        return rootDir;
    }

    protected void tearDown()
        throws Exception
    {
        release( artifactManager );

        super.tearDown();
    }

    private Artifact createArtifact( String groupId, String artifactId, String version, String type )
        throws Exception
    {
        Artifact artifact = artifactFactory.createBuildArtifact( groupId, artifactId, version, type );
        ArtifactRepository repository = localRepository();
        String artifactPath = repository.pathOf( artifact );
        File f = new File( localTestRepo, artifactPath );
        artifact.setFile( f );
        return artifact;
    }

    protected ArtifactRepository localRepository()
        throws Exception
    {
        ArtifactRepositoryLayout repoLayout = (ArtifactRepositoryLayout) lookup( ArtifactRepositoryLayout.ROLE,
                                                                                 "legacy" );

        return new DefaultArtifactRepository( "local", "file://" + localTestRepo.getPath(), repoLayout );
    }

    protected ArtifactRepository remoteRepository()
        throws Exception
    {
        ArtifactRepositoryLayout repoLayout = (ArtifactRepositoryLayout) lookup( ArtifactRepositoryLayout.ROLE,
                                                                                 "legacy" );

        return new DefaultArtifactRepository( "test", "file://" + remoteTestRepo.getPath(), repoLayout,
                                              new ArtifactRepositoryPolicy(), new ArtifactRepositoryPolicy() );
    }

    public void testOnline()
        throws ResourceDoesNotExistException, Exception
    {
        artifactManager.getWagonManager().setOnline( false );

        assertFalse( artifactManager.isOnline() );

        // Attempt to get a wagon.
        artifactManager.getWagonManager().addRepository( new Repository( "test", "http://localhost:10007/" ) );

        try
        {
            Artifact artifact = createArtifact( "test", "testLib", "1.0", "jar" );

            artifactManager.getArtifact( artifact, localRepository() );
            fail( "Should have thrown TransferFailedException as we are offline." );
        }
        catch ( TransferFailedException e )
        {
            /* expected path */
            assertEquals( "System is offline.", e.getMessage() );
        }
    }

    public void testGetArtifact()
        throws ResourceDoesNotExistException, Exception
    {
        artifactManager.getWagonManager().setOnline( true );

        assertTrue( artifactManager.isOnline() );

        Artifact artifact = createArtifact( "test", "testLib", "1.0", "jar" );
        setupRemoteTestArtifact( artifact );

        artifactManager.getArtifact( artifact, remoteRepository() );

        assertTrue( artifact.isResolved() );
        assertTrue( artifact.getFile().exists() );
    }

    private void setupRemoteTestArtifact( Artifact artifact )
        throws Exception
    {
        ArtifactRepository remoterepo = remoteRepository();
        String artifactPath = remoterepo.pathOf( artifact );
        File actualRemoteFile = new File( remoteTestRepo, artifactPath );
        
        actualRemoteFile.getParentFile().mkdirs();

        FileUtils.fileWrite( actualRemoteFile.getAbsolutePath(),
                             "Virtual Cess Pool of Totally Useless and Trivial Information" );
    }
}
