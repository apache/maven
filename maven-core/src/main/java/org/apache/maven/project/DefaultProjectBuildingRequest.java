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
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.DefaultModelBuildingRequest;
import org.apache.maven.model.ModelBuildingRequest;
import org.apache.maven.model.ModelEventListener;
import org.apache.maven.model.Profile;

public class DefaultProjectBuildingRequest
    implements ProjectBuildingRequest
{
    private ArtifactRepository localRepository;

    private List<ArtifactRepository> remoteRepositories;

    private List<ModelEventListener> listeners;
    
    private MavenProject topProject;

    private ModelBuildingRequest modelBuildingRequest = new DefaultModelBuildingRequest().setProcessPlugins( true );

    public MavenProject getTopLevelProjectFromReactor()
    {
    	return topProject;
    }
    
    public void setTopLevelProjectForReactor(MavenProject mavenProject)
    {
    	this.topProject = mavenProject;
    }       

    public ProjectBuildingRequest setLocalRepository( ArtifactRepository localRepository )
    {
        this.localRepository = localRepository;
        return this;
    }

    public ArtifactRepository getLocalRepository()
    {
        return localRepository;
    }
        
    public List<ArtifactRepository> getRemoteRepositories()
    {
        if ( remoteRepositories == null )
        {
            remoteRepositories = new ArrayList<ArtifactRepository>();
        }
        return remoteRepositories;
    }

    public ProjectBuildingRequest setRemoteRepositories( List<ArtifactRepository> remoteRepositories )
    {
        this.remoteRepositories = remoteRepositories;
        return this;
    }
    
    public Properties getExecutionProperties()
    {
        return modelBuildingRequest.getExecutionProperties();
    }

    public ProjectBuildingRequest setExecutionProperties( Properties executionProperties )
    {
        modelBuildingRequest.setExecutionProperties( executionProperties );
        return this;
    }

    public List<ModelEventListener> getModelEventListeners()
    {
        return listeners;
    }

    public ProjectBuildingRequest setModelEventListeners( List<ModelEventListener> listeners )
    {
        this.listeners = listeners;
        return this;
    }

    public boolean isProcessPlugins()
    {
        return modelBuildingRequest.isProcessPlugins();
    }

    public ProjectBuildingRequest setProcessPlugins( boolean processPlugins )
    {
        modelBuildingRequest.setProcessPlugins( processPlugins );
        return this;
    }

    public ProjectBuildingRequest setLenientValidation( boolean lenientValidation )
    {
        modelBuildingRequest.setLenientValidation( lenientValidation );
        return this;
    }

    public boolean istLenientValidation()
    {
        return modelBuildingRequest.istLenientValidation();
    }

    public List<String> getActiveProfileIds()
    {
        return modelBuildingRequest.getActiveProfileIds();
    }

    public void setActiveProfileIds( List<String> activeProfileIds )
    {
        modelBuildingRequest.setActiveProfileIds( activeProfileIds );
    }

    public List<String> getInactiveProfileIds()
    {
        return modelBuildingRequest.getInactiveProfileIds();
    }

    public void setInactiveProfileIds( List<String> inactiveProfileIds )
    {
        modelBuildingRequest.setInactiveProfileIds( inactiveProfileIds );
    }

    public void setProfiles( List<Profile> profiles )
    {
        modelBuildingRequest.setProfiles( profiles );
    }
    
    public void addProfile( Profile profile )
    {
        modelBuildingRequest.getProfiles().add(profile);
    }

    public List<Profile> getProfiles()
    {
        return modelBuildingRequest.getProfiles();
    }

    public ModelBuildingRequest getModelBuildingRequest()
    {
        return modelBuildingRequest;
    }

}
