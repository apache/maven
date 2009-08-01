package org.apache.maven.project;

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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.ArtifactFilterManager;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ResolutionErrorHandler;
import org.apache.maven.classrealm.ClassRealmManager;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Extension;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Repository;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;

/**
 * Assists the project builder. <strong>Warning:</strong> This is an internal utility class that is only public for
 * technical reasons, it is not part of the public API. In particular, this interface can be changed or deleted without
 * prior notice.
 * 
 * @author Benjamin Bentmann
 */
@Component( role = ProjectBuildingHelper.class )
public class DefaultProjectBuildingHelper
    implements ProjectBuildingHelper
{

    @Requirement
    private Logger logger;

    @Requirement
    private PlexusContainer container;

    @Requirement
    private ClassRealmManager classRealmManager;

    @Requirement
    private RepositorySystem repositorySystem;

    @Requirement
    private ResolutionErrorHandler resolutionErrorHandler;

    @Requirement
    private ArtifactFilterManager artifactFilterManager;

    public List<ArtifactRepository> createArtifactRepositories( List<Repository> pomRepositories,
                                                                List<ArtifactRepository> externalRepositories )
        throws InvalidRepositoryException
    {
        List<ArtifactRepository> artifactRepositories = new ArrayList<ArtifactRepository>();

        for ( Repository repository : pomRepositories )
        {
            artifactRepositories.add( repositorySystem.buildArtifactRepository( repository ) );
        }

        artifactRepositories = repositorySystem.getMirrors( artifactRepositories );

        if ( externalRepositories != null )
        {
            artifactRepositories.addAll( externalRepositories );
        }

        artifactRepositories = repositorySystem.getEffectiveRepositories( artifactRepositories );

        return artifactRepositories;
    }

    public ClassRealm createProjectRealm( Model model, ArtifactRepository localRepository,
                                          List<ArtifactRepository> remoteRepositories )
        throws ArtifactResolutionException
    {
        ClassRealm projectRealm = null;

        Build build = model.getBuild();

        if ( build == null )
        {
            return projectRealm;
        }

        List<Plugin> extensionPlugins = new ArrayList<Plugin>();

        for ( Plugin plugin : build.getPlugins() )
        {
            if ( plugin.isExtensions() )
            {
                extensionPlugins.add( plugin );
            }
        }

        if ( build.getExtensions().isEmpty() && extensionPlugins.isEmpty() )
        {
            return projectRealm;
        }

        projectRealm = classRealmManager.createProjectRealm( model );

        for ( Extension extension : build.getExtensions() )
        {
            Artifact artifact =
                repositorySystem.createArtifact( extension.getGroupId(), extension.getArtifactId(),
                                                 extension.getVersion(), "jar" );

            populateRealm( projectRealm, artifact, null, localRepository, remoteRepositories );
        }

        for ( Plugin plugin : extensionPlugins )
        {
            Artifact artifact = repositorySystem.createPluginArtifact( plugin );

            Set<Artifact> dependencies = new LinkedHashSet<Artifact>();
            for ( Dependency dependency : plugin.getDependencies() )
            {
                dependencies.add( repositorySystem.createDependencyArtifact( dependency ) );
            }

            populateRealm( projectRealm, artifact, dependencies, localRepository, remoteRepositories );
        }

        try
        {
            container.discoverComponents( projectRealm );
        }
        catch ( Exception e )
        {
            throw new IllegalStateException( "Failed to discover components in project realm " + projectRealm.getId(),
                                             e );
        }

        return projectRealm;
    }

    private void populateRealm( ClassRealm realm, Artifact artifact, Set<Artifact> dependencies,
                                ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories )
        throws ArtifactResolutionException
    {
        ArtifactResolutionRequest request = new ArtifactResolutionRequest();
        request.setArtifact( artifact );
        request.setArtifactDependencies( dependencies );
        request.setResolveTransitively( true );
        request.setFilter( artifactFilterManager.getCoreArtifactFilter() );
        request.setLocalRepository( localRepository );
        request.setRemoteRepostories( remoteRepositories );
        // FIXME setTransferListener

        ArtifactResolutionResult result = repositorySystem.resolve( request );

        resolutionErrorHandler.throwErrors( request, result );

        for ( Artifact resultArtifact : result.getArtifacts() )
        {
            if ( logger.isDebugEnabled() )
            {
                logger.debug( "  " + resultArtifact.getFile() );
            }

            try
            {
                realm.addURL( resultArtifact.getFile().toURI().toURL() );
            }
            catch ( MalformedURLException e )
            {
                throw new IllegalStateException( "Failed to populate project realm " + realm.getId() + " with "
                    + artifact.getFile(), e );
            }
        }
    }

}
