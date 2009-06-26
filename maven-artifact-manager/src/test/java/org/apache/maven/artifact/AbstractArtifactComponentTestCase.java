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
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.codehaus.plexus.PlexusTestCase;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl </a>
 * @version $Id: AbstractArtifactComponentTestCase.java,v 1.5 2004/10/23 13:33:59
 *          jvanzyl Exp $
 */
public abstract class AbstractArtifactComponentTestCase
    extends PlexusTestCase
{
    protected abstract String component();

    /**
     * Return an existing file, not a directory - causes creation to fail.
     *
     * @throws Exception
     */
    protected ArtifactRepository badLocalRepository()
        throws Exception
    {
        String path = "target/test-classes/repositories/" + component() + "/bad-local-repository";

        File f = new File( getBasedir(), path );

        f.createNewFile();

        ArtifactRepositoryLayout repoLayout = (ArtifactRepositoryLayout) lookup( ArtifactRepositoryLayout.ROLE,
                                                                                 "legacy" );

        return new DefaultArtifactRepository( "test", "file://" + f.getPath(), repoLayout );
    }

    protected String getRepositoryLayout()
    {
        return "legacy";
    }

    protected ArtifactRepository localRepository()
        throws Exception
    {
        String path = "target/test-classes/repositories/" + component() + "/local-repository";

        File f = new File( getBasedir(), path );

        ArtifactRepositoryLayout repoLayout = (ArtifactRepositoryLayout) lookup( ArtifactRepositoryLayout.ROLE,
                                                                                 "legacy" );

        return new DefaultArtifactRepository( "local", "file://" + f.getPath(), repoLayout );
    }

    protected ArtifactRepository remoteRepository()
        throws Exception
    {
        String path = "target/test-classes/repositories/" + component() + "/remote-repository";

        File f = new File( getBasedir(), path );

        ArtifactRepositoryLayout repoLayout = (ArtifactRepositoryLayout) lookup( ArtifactRepositoryLayout.ROLE,
                                                                                 "legacy" );

        return new DefaultArtifactRepository( "test", "file://" + f.getPath(), repoLayout,
                                              new ArtifactRepositoryPolicy(), new ArtifactRepositoryPolicy() );
    }

    protected ArtifactRepository badRemoteRepository()
        throws Exception
    {
        ArtifactRepositoryLayout repoLayout = (ArtifactRepositoryLayout) lookup( ArtifactRepositoryLayout.ROLE,
                                                                                 "legacy" );

        return new DefaultArtifactRepository( "test", "http://foo.bar/repository", repoLayout );
    }

    protected void assertRemoteArtifactPresent( Artifact artifact )
        throws Exception
    {
        ArtifactRepository remoteRepo = remoteRepository();

        String path = remoteRepo.pathOf( artifact );

        File file = new File( remoteRepo.getBasedir(), path );

        if ( !file.exists() )
        {
            fail( "Remote artifact " + file + " should be present." );
        }
    }

    protected void assertLocalArtifactPresent( Artifact artifact )
        throws Exception
    {
        ArtifactRepository localRepo = localRepository();

        String path = localRepo.pathOf( artifact );

        File file = new File( localRepo.getBasedir(), path );

        if ( !file.exists() )
        {
            fail( "Local artifact " + file + " should be present." );
        }
    }

    protected void assertRemoteArtifactNotPresent( Artifact artifact )
        throws Exception
    {
        ArtifactRepository remoteRepo = remoteRepository();

        String path = remoteRepo.pathOf( artifact );

        File file = new File( remoteRepo.getBasedir(), path );

        if ( file.exists() )
        {
            fail( "Remote artifact " + file + " should not be present." );
        }
    }

    protected void assertLocalArtifactNotPresent( Artifact artifact )
        throws Exception
    {
        ArtifactRepository localRepo = localRepository();

        String path = localRepo.pathOf( artifact );

        File file = new File( localRepo.getBasedir(), path );

        if ( file.exists() )
        {
            fail( "Local artifact " + file + " should not be present." );
        }
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    protected List remoteRepositories()
        throws Exception
    {
        List remoteRepositories = new ArrayList();

        remoteRepositories.add( remoteRepository() );

        return remoteRepositories;
    }

    // ----------------------------------------------------------------------
    // Test artifact generation for unit tests
    // ----------------------------------------------------------------------

    protected Artifact createLocalArtifact( String artifactId, String version, String content )
        throws Exception
    {
        Artifact artifact = createArtifact( artifactId, version );

        createArtifact( artifact, localRepository(), content );

        return artifact;
    }

    protected Artifact createRemoteArtifact( String artifactId, String version )
        throws Exception
    {
        Artifact artifact = createArtifact( artifactId, version );

        createArtifact( artifact, remoteRepository(), null );

        return artifact;
    }

    protected void createLocalArtifact( Artifact artifact )
        throws Exception
    {
        createArtifact( artifact, localRepository(), null );
    }

    protected void createRemoteArtifact( Artifact artifact )
        throws Exception
    {
        createArtifact( artifact, remoteRepository(), null );
    }

    protected void createArtifact( Artifact artifact, ArtifactRepository repository, String content )
        throws Exception
    {
        String path = repository.pathOf( artifact );

        File artifactFile = new File( repository.getBasedir(), path );

        if ( !artifactFile.getParentFile().exists() )
        {
            artifactFile.getParentFile().mkdirs();
        }

        Writer writer = new FileWriter( artifactFile );

        if( content == null )
        {
            writer.write( artifact.getId() );
        }
        else
        {
            writer.write( content );
        }

        writer.close();
        
        artifactFile = new File( repository.getBasedir(), path );
        
        artifact.setFile( artifactFile );
    }

    protected Artifact createArtifact( String artifactId, String version )
        throws Exception
    {
        return createArtifact( artifactId, version, "jar" );
    }

    protected Artifact createArtifact( String artifactId, String version, String type )
        throws Exception
    {
        return createArtifact( "org.apache.maven", artifactId, version, type );
    }

    protected Artifact createArtifact( String groupId, String artifactId, String version, String type )
        throws Exception
    {
        ArtifactFactory artifactFactory = (ArtifactFactory) lookup( ArtifactFactory.ROLE );

        return artifactFactory.createBuildArtifact( groupId, artifactId, version, type );
    }

    protected void deleteLocalArtifact( Artifact artifact )
        throws Exception
    {
        deleteArtifact( artifact, localRepository() );
    }

    protected void deleteArtifact( Artifact artifact, ArtifactRepository repository )
        throws Exception
    {
        String path = repository.pathOf( artifact );

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

