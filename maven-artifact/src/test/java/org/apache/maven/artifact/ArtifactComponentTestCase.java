/*
 * Copyright 2001-2004 The Apache Software Foundation.
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

package org.apache.maven.artifact;

import org.codehaus.plexus.PlexusTestCase;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerNotFoundException;
import org.apache.maven.artifact.repository.ArtifactRepository;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Set;
import java.util.HashSet;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public abstract class ArtifactComponentTestCase
    extends PlexusTestCase
{
    protected ArtifactHandlerManager artifactHandlerManager;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        artifactHandlerManager = (ArtifactHandlerManager) lookup( ArtifactHandlerManager.ROLE );
    }

    protected abstract String component();

    /** Return an existing file, not a directory - causes creation to fail. */
    protected ArtifactRepository badLocalRepository() throws IOException
    {
        ArtifactRepository localRepository = new ArtifactRepository();

        String path = "target/test-classes/repositories/" + component() + "/bad-local-repository";

        File f = new File( getBasedir(), path );

        f.createNewFile();

        localRepository.setUrl( "file://" + f.getPath() );

        return localRepository;
    }

    protected ArtifactRepository localRepository()
    {
        ArtifactRepository localRepository = new ArtifactRepository();

        String path = "target/test-classes/repositories/" + component() + "/local-repository";

        File f = new File( getBasedir(), path );

        localRepository.setUrl( "file://" + f.getPath() );

        return localRepository;
    }

    protected ArtifactRepository remoteRepository()
    {
        ArtifactRepository repository = new ArtifactRepository();

        String path = "target/test-classes/repositories/" + component() + "/remote-repository";

        File f = new File( getBasedir(), path );

        repository.setUrl( "file://" + f.getPath() );

        return repository;
    }

    protected ArtifactRepository badRemoteRepository()
    {
        ArtifactRepository repository = new ArtifactRepository();

        repository.setUrl( "http://foo.bar/repository" );

        return repository;
    }

    protected void assertRemoteArtifactPresent( Artifact artifact )
        throws ArtifactHandlerNotFoundException
    {
        String path = artifactHandlerManager.path( artifact );

        File file = new File( remoteRepository().getBasedir(), path );

        if ( !file.exists() )
        {
            fail( "Remote artifact " + file + " should be present." );
        }
    }

    protected void assertLocalArtifactPresent( Artifact artifact )
        throws ArtifactHandlerNotFoundException
    {
        String path = artifactHandlerManager.path( artifact );

        File file = new File( localRepository().getBasedir(), path );

        if ( !file.exists() )
        {
            fail( "Local artifact " + file + " should be present." );
        }
    }

    protected void assertRemoteArtifactNotPresent( Artifact artifact )
        throws ArtifactHandlerNotFoundException
    {
        String path = artifactHandlerManager.path( artifact );

        File file = new File( remoteRepository().getBasedir(), path );

        if ( file.exists() )
        {
            fail( "Remote artifact " + file + " should not be present." );
        }
    }

    protected void assertLocalArtifactNotPresent( Artifact artifact )
        throws ArtifactHandlerNotFoundException
    {
        String path = artifactHandlerManager.path( artifact );

        File file = new File( localRepository().getBasedir(), path );

        if ( file.exists() )
        {
            fail( "Local artifact " + file + " should not be present." );
        }
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    protected Set remoteRepositories()
    {
        Set remoteRepositories = new HashSet();

        remoteRepositories.add( remoteRepository() );

        return remoteRepositories;
    }

    // ----------------------------------------------------------------------
    // Test artifact generation for unit tests
    // ----------------------------------------------------------------------

    protected Artifact createLocalArtifact( String artifactId, String version )
        throws Exception
    {
        Artifact artifact = createArtifact( artifactId, version );

        createArtifact( artifact, localRepository() );

        return artifact;
    }

    protected Artifact createRemoteArtifact( String artifactId, String version )
        throws Exception
    {
        Artifact artifact = createArtifact( artifactId, version );

        createArtifact( artifact, remoteRepository() );

        return artifact;
    }

    protected void createLocalArtifact( Artifact artifact )
        throws Exception
    {
        createArtifact( artifact, localRepository() );
    }

    protected void createRemoteArtifact( Artifact artifact )
        throws Exception
    {
        createArtifact( artifact, remoteRepository() );
    }

    protected void createArtifact( Artifact artifact, ArtifactRepository repository )
        throws Exception
    {
        String path = artifactHandlerManager.path( artifact );

        File artifactFile = new File( repository.getBasedir(), path );

        if ( !artifactFile.getParentFile().exists() )
        {
            artifactFile.getParentFile().mkdirs();
        }

        Writer writer = new FileWriter( artifactFile );

        writer.write( artifact.getId() );

        writer.close();
    }

    protected Artifact createArtifact( String artifactId, String version )
    {
        return createArtifact( artifactId, version, "jar" );
    }

    protected Artifact createArtifact( String artifactId, String version, String type )
    {
        return new DefaultArtifact( "maven", artifactId, version, type );
    }

    protected void deleteLocalArtifact( Artifact artifact )
        throws Exception
    {
        deleteArtifact( artifact, localRepository() );
    }

    protected void deleteArtifact( Artifact artifact, ArtifactRepository repository )
        throws Exception
    {
        String path = artifactHandlerManager.path( artifact );

        File artifactFile = new File( repository.getBasedir(), path );

        if ( artifactFile.exists() )
        {
            if ( !artifactFile.delete() )
            {
                throw new IOException( "Failure while attempting to delete artifact " + artifactFile );
            }
        }
    }
}

