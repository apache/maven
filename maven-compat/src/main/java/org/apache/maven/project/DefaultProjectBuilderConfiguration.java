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

import java.util.Date;
import java.util.Properties;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.profiles.ProfileManager;

/**
 * DefaultProjectBuilderConfiguration
 */
@Deprecated
public class DefaultProjectBuilderConfiguration
    implements ProjectBuilderConfiguration
{

    private ProfileManager globalProfileManager;

    private ArtifactRepository localRepository;

    private Properties userProperties;

    private Properties executionProperties = System.getProperties();

    private Date buildStartTime;

    public DefaultProjectBuilderConfiguration()
    {
    }

    public ProjectBuilderConfiguration setGlobalProfileManager( ProfileManager globalProfileManager )
    {
        this.globalProfileManager = globalProfileManager;
        return this;
    }

    public ProfileManager getGlobalProfileManager()
    {
        return globalProfileManager;
    }

    public ProjectBuilderConfiguration setLocalRepository( ArtifactRepository localRepository )
    {
        this.localRepository = localRepository;
        return this;
    }

    public ArtifactRepository getLocalRepository()
    {
        return localRepository;
    }

    public ProjectBuilderConfiguration setUserProperties( Properties userProperties )
    {
        this.userProperties = userProperties;
        return this;
    }

    public Properties getUserProperties()
    {
        if ( userProperties == null )
        {
            userProperties = new Properties();
        }

        return userProperties;
    }

    public Properties getExecutionProperties()
    {
        return executionProperties;
    }

    public ProjectBuilderConfiguration setExecutionProperties( Properties executionProperties )
    {
        this.executionProperties = executionProperties;
        return this;
    }

    public Date getBuildStartTime()
    {
        return buildStartTime;
    }

    public ProjectBuilderConfiguration setBuildStartTime( Date buildStartTime )
    {
        this.buildStartTime = buildStartTime;
        return this;
    }

}
