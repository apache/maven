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
import java.util.Objects;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.AbstractModelBuildingListener;
import org.apache.maven.model.building.ModelBuildingEvent;
import org.apache.maven.model.building.ModelProblem.Severity;
import org.apache.maven.model.building.ModelProblem.Version;
import org.apache.maven.model.building.ModelProblemCollectorRequest;
import org.apache.maven.plugin.PluginManagerException;
import org.apache.maven.plugin.PluginResolutionException;
import org.apache.maven.plugin.version.PluginVersionResolutionException;

/**
 * Processes events from the model builder while building the effective model for a {@link MavenProject} instance.
 *
 * @author Benjamin Bentmann
 */
public class DefaultModelBuildingListener
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
        this.project = Objects.requireNonNull( project, "project cannot be null" );
        this.projectBuildingHelper =
            Objects.requireNonNull( projectBuildingHelper, "projectBuildingHelper cannot be null" );
        this.projectBuildingRequest =
            Objects.requireNonNull( projectBuildingRequest, "projectBuildingRequest cannot be null" );
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
            event.getProblems().add( new ModelProblemCollectorRequest( Severity.ERROR, Version.BASE )
                    .setMessage( "Invalid plugin repository: " + e.getMessage() )
                    .setException( e ) );
        }
        project.setPluginArtifactRepositories( pluginRepositories );

        if ( event.getRequest().isProcessPlugins() )
        {
            try
            {
                ProjectRealmCache.CacheRecord record =
                    projectBuildingHelper.createProjectRealm( project, model, projectBuildingRequest );

                project.setClassRealm( record.getRealm() );
                project.setExtensionDependencyFilter( record.getExtensionArtifactFilter() );
            }
            catch ( PluginResolutionException | PluginManagerException | PluginVersionResolutionException e )
            {
                event.getProblems().add( new ModelProblemCollectorRequest( Severity.ERROR, Version.BASE )
                        .setMessage( "Unresolvable build extension: " + e.getMessage() )
                        .setException( e ) );
            }

            projectBuildingHelper.selectProjectRealm( project );
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
            event.getProblems().add( new ModelProblemCollectorRequest( Severity.ERROR, Version.BASE )
                    .setMessage( "Invalid artifact repository: " + e.getMessage() )
                    .setException( e ) );
        }
        project.setRemoteArtifactRepositories( remoteRepositories );
    }

}
