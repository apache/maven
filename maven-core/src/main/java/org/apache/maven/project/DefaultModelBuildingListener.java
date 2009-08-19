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

import java.util.List;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.DefaultRepositoryRequest;
import org.apache.maven.artifact.repository.RepositoryRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.AbstractModelBuildingListener;
import org.apache.maven.model.building.ModelBuildingEvent;
import org.apache.maven.plugin.version.PluginVersionResolutionException;
import org.codehaus.plexus.classworlds.realm.ClassRealm;

/**
 * Processes events from the model builder while building the effective model for a {@link MavenProject} instance.
 * 
 * @author Benjamin Bentmann
 */
class DefaultModelBuildingListener
    extends AbstractModelBuildingListener
{

    private ProjectBuildingHelper projectBuildingHelper;

    private ProjectBuildingRequest projectBuildingRequest;

    private ClassRealm projectRealm;

    private List<ArtifactRepository> remoteRepositories;

    private List<ArtifactRepository> pluginRepositories;

    public DefaultModelBuildingListener( ProjectBuildingHelper projectBuildingHelper,
                                         ProjectBuildingRequest projectBuildingRequest )
    {
        if ( projectBuildingHelper == null )
        {
            throw new IllegalArgumentException( "project realm manager missing" );
        }
        this.projectBuildingHelper = projectBuildingHelper;

        if ( projectBuildingRequest == null )
        {
            throw new IllegalArgumentException( "project building request missing" );
        }
        this.projectBuildingRequest = projectBuildingRequest;
        this.remoteRepositories = projectBuildingRequest.getRemoteRepositories();
        this.pluginRepositories = projectBuildingRequest.getPluginArtifactRepositories();
    }

    /**
     * Gets the project realm that hosts the build extensions.
     * 
     * @return The project realm or {@code null} if the project requires no extensions.
     */
    public ClassRealm getProjectRealm()
    {
        return projectRealm;
    }

    /**
     * Gets the effective remote artifact repositories for the project. The repository list is created from the
     * repositories given by {@link ProjectBuildingRequest#getRemoteRepositories()} and the repositories given in the
     * POM, i.e. {@link Model#getRepositories()}. The POM repositories themselves also contain any repositories
     * contributed by external profiles as specified in {@link ProjectBuildingRequest#getProfiles()}. Furthermore, the
     * repositories have already been mirrored.
     * 
     * @return The remote artifact repositories for the project.
     */
    public List<ArtifactRepository> getRemoteRepositories()
    {
        return remoteRepositories;
    }

    /**
     * Gets the effective remote plugin repositories for the project. The repository list is created from the
     * repositories given by {@link ProjectBuildingRequest#getPluginArtifactRepositories()} and the repositories given
     * in the POM, i.e. {@link Model#getPluginRepositories()}. The POM repositories themselves also contain any
     * repositories contributed by external profiles as specified in {@link ProjectBuildingRequest#getProfiles()}.
     * Furthermore, the repositories have already been mirrored.
     * 
     * @return The remote plugin repositories for the project.
     */
    public List<ArtifactRepository> getPluginRepositories()
    {
        return pluginRepositories;
    }

    @Override
    public void buildExtensionsAssembled( ModelBuildingEvent event )
    {
        Model model = event.getModel();

        try
        {
            remoteRepositories =
                projectBuildingHelper.createArtifactRepositories( model.getRepositories(), remoteRepositories );
        }
        catch ( Exception e )
        {
            event.getProblems().addError( "Invalid artifact repository: " + e.getMessage(), e );
        }

        try
        {
            pluginRepositories =
                projectBuildingHelper.createArtifactRepositories( model.getPluginRepositories(), pluginRepositories );
        }
        catch ( Exception e )
        {
            event.getProblems().addError( "Invalid plugin repository: " + e.getMessage(), e );
        }

        if ( event.getRequest().isProcessPlugins() )
        {
            try
            {
                RepositoryRequest repositoryRequest = new DefaultRepositoryRequest();
                repositoryRequest.setCache( projectBuildingRequest.getRepositoryCache() );
                repositoryRequest.setLocalRepository( projectBuildingRequest.getLocalRepository() );
                repositoryRequest.setRemoteRepositories( pluginRepositories );
                repositoryRequest.setOffline( projectBuildingRequest.isOffline() );

                projectRealm = projectBuildingHelper.createProjectRealm( model, repositoryRequest );
            }
            catch ( ArtifactResolutionException e )
            {
                event.getProblems().addError( "Unresolveable build extensions: " + e.getMessage(), e );
            }
            catch ( PluginVersionResolutionException e )
            {
                event.getProblems().addError( "Unresolveable build extensions: " + e.getMessage(), e );
            }

            if ( projectRealm != null )
            {
                /*
                 * Update the context class loader such that the container will search the project realm when the model
                 * builder injects the lifecycle bindings from the packaging in the next step. The context class loader
                 * will be reset by the project builder when the project is fully assembled.
                 */
                Thread.currentThread().setContextClassLoader( projectRealm );
            }
        }
    }

}
