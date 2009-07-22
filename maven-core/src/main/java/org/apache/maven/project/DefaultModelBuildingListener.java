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
import org.apache.maven.model.Model;
import org.apache.maven.model.building.AbstractModelBuildingListener;
import org.apache.maven.model.building.ModelBuildingEvent;
import org.codehaus.plexus.classworlds.realm.ClassRealm;

/**
 * Processes events from the model builder.
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

    public ClassRealm getProjectRealm()
    {
        return projectRealm;
    }

    public List<ArtifactRepository> getRemoteRepositories()
    {
        return remoteRepositories;
    }

    public List<ArtifactRepository> getPluginRepositories()
    {
        return pluginRepositories;
    }

    @Override
    public void buildExtensionsAssembled( ModelBuildingEvent event )
        throws Exception
    {
        Model model = event.getModel();

        remoteRepositories = projectBuildingHelper.createArtifactRepositories( model.getRepositories(), remoteRepositories );

        pluginRepositories = projectBuildingHelper.createArtifactRepositories( model.getPluginRepositories(), pluginRepositories );

        if ( event.getRequest().isProcessPlugins() )
        {
            projectRealm =
                projectBuildingHelper.createProjectRealm( model, projectBuildingRequest.getLocalRepository(),
                                                          pluginRepositories );

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
