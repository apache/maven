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
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.filter.AndArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.artifact.resolver.filter.TypeArtifactFilter;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Repository;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.apache.maven.project.artifact.MavenMetadataSource;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileList;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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

    private String useScope;

    private String type;

    private boolean verbose;

    protected void doExecute()
    {
        ArtifactRepository localRepo = createLocalArtifactRepository();

        ArtifactResolver resolver = (ArtifactResolver) lookup( ArtifactResolver.ROLE );
        MavenProjectBuilder projectBuilder = (MavenProjectBuilder) lookup( MavenProjectBuilder.ROLE );
        ArtifactFactory artifactFactory = (ArtifactFactory) lookup( ArtifactFactory.ROLE );
        MavenMetadataSource metadataSource = (MavenMetadataSource) lookup( ArtifactMetadataSource.ROLE );

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
                Repository pomRepository = (Repository) i.next();

                remoteRepositories.add( createAntRemoteRepository( pomRepository ) );
            }
        }
        else
        {
            // we have to have some sort of Pom object in order to satisfy the requirements for building the
            // originating Artifact below...
            pom = createDummyPom();
        }

        if ( dependencies.isEmpty() )
        {
            log( "There were no dependencies specified", Project.MSG_WARN );
        }

        log( "Resolving dependencies...", Project.MSG_VERBOSE );

        WagonManager wagonManager = (WagonManager) lookup( WagonManager.ROLE );
        wagonManager.setDownloadMonitor( new AntDownloadMonitor() );

        ArtifactResolutionResult result;
        Set artifacts;
        try
        {
            artifacts = MavenMetadataSource.createArtifacts( artifactFactory, dependencies, null, null, null );

            Artifact pomArtifact = artifactFactory.createBuildArtifact( pom.getGroupId(), pom.getArtifactId(), pom
                .getVersion(), pom.getPackaging() );

            List listeners = Collections.EMPTY_LIST;
            if ( verbose )
            {
                listeners = Collections.singletonList( new AntResolutionListener( getProject() ) );
            }

            List remoteRepositories = getRemoteRepositories();

            RemoteRepository remoteRepository = getDefaultRemoteRepository();
            remoteRepositories.add( remoteRepository );

            List remoteArtifactRepositories = createRemoteArtifactRepositories( remoteRepositories );

            // TODO: managed dependencies
            Map managedDependencies = Collections.EMPTY_MAP;

            ArtifactFilter filter = null;
            if ( useScope != null )
            {
                filter = new ScopeArtifactFilter( useScope );
            }
            if ( type != null )
            {
                TypeArtifactFilter typeArtifactFilter = new TypeArtifactFilter( type );
                if ( filter != null )
                {
                    AndArtifactFilter andFilter = new AndArtifactFilter();
                    andFilter.add( filter );
                    andFilter.add( typeArtifactFilter );
                    filter = andFilter;
                }
                else
                {
                    filter = typeArtifactFilter;
                }
            }

            result = resolver.resolveTransitively( artifacts, pomArtifact, managedDependencies, localRepo,
                                                   remoteArtifactRepositories, metadataSource, filter, listeners );
        }
        catch ( ArtifactResolutionException e )
        {
            throw new BuildException( "Unable to resolve artifact: " + e.getMessage(), e );
        }
        catch ( ArtifactNotFoundException e )
        {
            throw new BuildException( "Dependency not found: " + e.getMessage(), e );
        }
        catch ( InvalidDependencyVersionException e )
        {
            throw new BuildException( e.getMessage(), e );
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

        if ( result.getArtifacts().isEmpty() )
        {
            fileSet.createExclude().setName( "**/**" );
        }
        else
        {
            for ( Iterator i = result.getArtifacts().iterator(); i.hasNext(); )
            {
                Artifact artifact = (Artifact) i.next();
                String filename = localRepo.pathOf( artifact );

                FileList.FileName file = new FileList.FileName();
                file.setName( filename );

                fileList.addConfiguredFile( file );

                fileSet.createInclude().setName( filename );
            }
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

    public void setVerbose( boolean verbose )
    {
        this.verbose = verbose;
    }

    public void setUseScope( String useScope )
    {
        this.useScope = useScope;
    }

    public void setType( String type )
    {
        this.type = type;
    }


}
