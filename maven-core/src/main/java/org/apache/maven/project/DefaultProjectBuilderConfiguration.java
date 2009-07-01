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

import java.util.List;
import java.util.Properties;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelEventListener;

@Deprecated
public class DefaultProjectBuilderConfiguration
    extends DefaultProjectBuildingRequest
    implements ProjectBuilderConfiguration
{

    public DefaultProjectBuilderConfiguration()
    {
        setProcessPlugins( false );
        setValidationLevel( ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_2_0 );
    }

    public ProjectBuilderConfiguration setLocalRepository( ArtifactRepository localRepository )
    {
        super.setLocalRepository( localRepository );
        return this;
    }

    public ProjectBuilderConfiguration setRemoteRepositories( List<ArtifactRepository> remoteRepositories )
    {
        super.setRemoteRepositories( remoteRepositories );
        return this;
    }

    public ProjectBuilderConfiguration setExecutionProperties( Properties executionProperties )
    {
        super.setExecutionProperties( executionProperties );
        return this;
    }

    public ProjectBuilderConfiguration setModelEventListeners( List<ModelEventListener> listeners )
    {
        super.setModelEventListeners( listeners );
        return this;
    }

    public ProjectBuilderConfiguration setProcessPlugins( boolean processPlugins )
    {
        super.setProcessPlugins( processPlugins );
        return this;
    }

    public ProjectBuilderConfiguration setValidationLevel( int validationLevel )
    {
        super.setValidationLevel( validationLevel );
        return this;
    }

}
