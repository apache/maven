package org.apache.maven.artifact.ant;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactPathFormatException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.artifact.MavenMetadataSource;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.FileList;
import org.apache.tools.ant.types.Path;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Dependencies task, using maven-artifact.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public class DependenciesTask
    extends AbstractArtifactTask
{
    private List dependencies = new ArrayList();

    private LocalRepository localRepository;

    private List remoteRepositories = new ArrayList();

    private String pathId;

    public void execute()
    {
        ArtifactRepository localRepo = createArtifactRepository( localRepository );

        ArtifactFactory factory = (ArtifactFactory) lookup( ArtifactFactory.ROLE );

        Set artifacts = new HashSet();
        for ( Iterator i = dependencies.iterator(); i.hasNext(); )
        {
            Dependency dependency = (Dependency) i.next();
            Artifact a = factory.createArtifact( dependency.getGroupId(), dependency.getArtifactId(),
                                                 dependency.getVersion(), dependency.getScope(), dependency.getType(),
                                                 null );
            artifacts.add( a );
        }

        ArtifactResolver resolver = (ArtifactResolver) lookup( ArtifactResolver.ROLE );
        MavenMetadataSource metadataSource = new MavenMetadataSource( resolver, (MavenProjectBuilder) lookup(
            MavenProjectBuilder.ROLE ) );

        log( "Resolving dependencies..." );

        WagonManager wagonManager = (WagonManager) lookup( WagonManager.ROLE );
        wagonManager.setDownloadMonitor( new AntDownloadMonitor() );

        ArtifactResolutionResult result;
        try
        {
            result =
                resolver.resolveTransitively( artifacts, createRemoteArtifactRepositories(), localRepo, metadataSource );
        }
        catch ( ArtifactResolutionException e )
        {
            throw new BuildException( "Unable to resolve artifact", e );
        }

        if ( getProject().getReference( pathId ) != null )
        {
            throw new BuildException( "Reference ID " + pathId + " already exists" );
        }

        FileList fileList = new FileList();
        fileList.setDir( localRepository.getLocation() );

        for ( Iterator i = result.getArtifacts().values().iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();
            String filename = null;
            try
            {
                filename = localRepo.pathOf( artifact );
            }
            catch ( ArtifactPathFormatException e )
            {
                throw new BuildException( "Unable to determine path to artifact: " + artifact, e );
            }

            FileList.FileName file = new FileList.FileName();
            file.setName( filename );

            fileList.addConfiguredFile( file );
        }

        Path path = new Path( getProject() );
        path.addFilelist( fileList );
        getProject().addReference( pathId, path );
    }

    private List createRemoteArtifactRepositories()
    {
        List list = new ArrayList();
        for ( Iterator i = getRemoteRepositories().iterator(); i.hasNext(); )
        {
            list.add( createArtifactRepository( (RemoteRepository) i.next() ) );
        }
        return list;
    }

    public List getRemoteRepositories()
    {
        if ( remoteRepositories.isEmpty() )
        {
            RemoteRepository remoteRepository = new RemoteRepository();
            remoteRepository.setUrl( "http://repo1.maven.org/maven2" );
            remoteRepositories.add( remoteRepository );
        }
        return remoteRepositories;
    }

    public void addRemoteRepository( RemoteRepository remoteRepository )
    {
        remoteRepositories.add( remoteRepository );
    }

    public List getDependencies()
    {
        return dependencies;
    }

    public void addDependency( Dependency dependency )
    {
        dependencies.add( dependency );
    }

    public LocalRepository getLocalRepository()
    {
        return localRepository;
    }

    public void addLocalRepository( LocalRepository localRepository )
    {
        this.localRepository = localRepository;
    }

    public String getPathId()
    {
        return pathId;
    }

    public void setPathId( String pathId )
    {
        this.pathId = pathId;
    }
}
