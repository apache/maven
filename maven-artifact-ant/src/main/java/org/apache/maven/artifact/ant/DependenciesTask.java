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
import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactPathFormatException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.artifact.MavenMetadataSource;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.FileList;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;

import java.util.ArrayList;
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

    private List remoteRepositories = new ArrayList();

    private String pathId;

    private String filesetId;

    public void execute()
    {
        ArtifactRepository localRepo = createLocalArtifactRepository();

        ArtifactResolver resolver = (ArtifactResolver) lookup( ArtifactResolver.ROLE );
        MavenProjectBuilder projectBuilder = (MavenProjectBuilder) lookup( MavenProjectBuilder.ROLE );
        MavenMetadataSource metadataSource = new MavenMetadataSource( resolver, projectBuilder );

        List dependencies = this.dependencies;

        Pom pom = buildPom( projectBuilder, localRepo );
        if ( pom != null )
        {
            if ( !dependencies.isEmpty() )
            {
                throw new BuildException( "You cannot specify both dependencies and a pom in the dependencies task" );
            }

            dependencies = pom.getDependencies();

            for ( Iterator i = pom.getRepositories().iterator(); i.hasNext(); )
            {
                org.apache.maven.model.Repository pomRepository = (org.apache.maven.model.Repository) i.next();

                remoteRepositories.add( createAntRemoteRepository( pomRepository ) );
            }
        }

        Set artifacts = metadataSource.createArtifacts( dependencies, null, null );

        log( "Resolving dependencies..." );

        WagonManager wagonManager = (WagonManager) lookup( WagonManager.ROLE );
        wagonManager.setDownloadMonitor( new AntDownloadMonitor() );

        ArtifactResolutionResult result;
        try
        {
            List remoteArtifactRepositories = createRemoteArtifactRepositories( getRemoteRepositories() );
            result = resolver.resolveTransitively( artifacts, remoteArtifactRepositories, localRepo, metadataSource );
        }
        catch ( ArtifactResolutionException e )
        {
            throw new BuildException( "Unable to resolve artifact", e );
        }

        if ( pathId != null && getProject().getReference( pathId ) != null )
        {
            throw new BuildException( "Reference ID " + pathId + " already exists" );
        }

        if ( filesetId != null && getProject().getReference( filesetId ) != null )
        {
            throw new BuildException( "Reference ID " + filesetId + " already exists" );
        }

        FileList fileList = new FileList();
        fileList.setDir( getLocalRepository().getLocation() );

        FileSet fileSet = new FileSet();
        fileSet.setDir( fileList.getDir( getProject() ) );

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

            fileSet.createInclude().setName( filename );
        }

        if ( pathId != null )
        {
            Path path = new Path( getProject() );
            path.addFilelist( fileList );
            getProject().addReference( pathId, path );
        }

        if ( filesetId != null )
        {
            getProject().addReference( filesetId, fileSet );
        }
    }

    private List createRemoteArtifactRepositories( List remoteRepositories )
    {
        List list = new ArrayList();
        for ( Iterator i = remoteRepositories.iterator(); i.hasNext(); )
        {
            list.add( createRemoteArtifactRepository( (RemoteRepository) i.next() ) );
        }
        return list;
    }

    public List getRemoteRepositories()
    {
        if ( remoteRepositories.isEmpty() )
        {
            // TODO: could we utilise the super POM for this?
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

    public String getPathId()
    {
        return pathId;
    }

    public void setPathId( String pathId )
    {
        this.pathId = pathId;
    }

    public String getFilesetId()
    {
        return filesetId;
    }

    public void setFilesetId( String filesetId )
    {
        this.filesetId = filesetId;
    }
}
