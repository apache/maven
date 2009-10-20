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
import org.apache.maven.model.Model;
import org.apache.maven.model.building.AbstractModelBuildingListener;
import org.apache.maven.model.building.ModelBuildingEvent;
import org.apache.maven.plugin.PluginResolutionException;
import org.apache.maven.plugin.version.PluginVersionResolutionException;

/**
 * Processes events from the model builder while building the effective model for a {@link MavenProject} instance.
 * 
 * @author Benjamin Bentmann
 */
class DefaultModelBuildingListener
    extends AbstractModelBuildingListener
{

    private MavenProject project;

    private ProjectBuildingHelper projectBuildingHelper;

    private ProjectBuildingRequest projectBuildingRequest;

    private List<ArtifactRepository> remoteRepositories;

    private List<ArtifactRepository> pluginRepositories;

    public DefaultModelBuildingListener( MavenProject project, ProjectBuildingHelper projectBuildingHelper,
                                         ProjectBuildingRequest projectBuildingRequest )
    {
        if ( project == null )
        {
            throw new IllegalArgumentException( "project missing" );
        }
        this.project = project;

        if ( projectBuildingHelper == null )
        {
            throw new IllegalArgumentException( "project building helper missing" );
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
     * Gets the project whose model is being built.
     * 
     * @return The project, never {@code null}.
     */
    public MavenProject getProject()
    {
        return project;
    }

    @Override
    public void buildExtensionsAssembled( ModelBuildingEvent event )
    {
        Model model = event.getModel();

        try
        {
            pluginRepositories =
                projectBuildingHelper.createArtifactRepositories( model.getPluginRepositories(), pluginRepositories,
                                                                  projectBuildingRequest );
        }
        catch ( Exception e )
        {
            event.getProblems().addError( "Invalid plugin repository: " + e.getMessage(), e );
        }
        project.setPluginArtifactRepositories( pluginRepositories );

        if ( event.getRequest().isProcessPlugins() )
        {
            try
            {
                RepositoryRequest repositoryRequest = new DefaultRepositoryRequest();
                repositoryRequest.setCache( projectBuildingRequest.getRepositoryCache() );
                repositoryRequest.setLocalRepository( projectBuildingRequest.getLocalRepository() );
                repositoryRequest.setRemoteRepositories( pluginRepositories );
                repositoryRequest.setOffline( projectBuildingRequest.isOffline() );

                ProjectRealmCache.CacheRecord record =
                    projectBuildingHelper.createProjectRealm( project, model, repositoryRequest );

                project.setClassRealm( record.realm );
                project.setExtensionArtifactFilter( record.extensionArtifactFilter );
            }
            catch ( PluginResolutionException e )
            {
                event.getProblems().addError( "Unresolveable build extension: " + e.getMessage(), e );
            }
            catch ( PluginVersionResolutionException e )
            {
                event.getProblems().addError( "Unresolveable build extension: " + e.getMessage(), e );
            }

            if ( project.getClassRealm() != null )
            {
                /*
                 * Update the context class loader such that the container will search the project realm when the model
                 * builder injects the lifecycle bindings from the packaging in the next step. The context class loader
                 * will be reset by the project builder when the project is fully assembled.
                 */
                Thread.currentThread().setContextClassLoader( project.getClassRealm() );
            }
            else
            {
                /*
                 * Reset context class loader to core realm.
                 */
                Thread.currentThread().setContextClassLoader( getClass().getClassLoader() );
            }
        }

        // build the regular repos after extensions are loaded to allow for custom layouts
        try
        {
            remoteRepositories =
                projectBuildingHelper.createArtifactRepositories( model.getRepositories(), remoteRepositories,
                                                                  projectBuildingRequest );
        }
        catch ( Exception e )
        {
            event.getProblems().addError( "Invalid artifact repository: " + e.getMessage(), e );
        }
        project.setRemoteArtifactRepositories( remoteRepositories );
    }

}
