package org.apache.maven.embedder.execution;

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

import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.settings.Settings;

import java.util.List;

/**
 * Adapt a {@link MavenExecutionRequest} to a {@link Settings} object for use in the Maven core.
 * We want to make sure that what is ask for in the execution request overrides what is in the settings.
 * The CLI feeds into an execution request so if a particular value is present in the execution request
 * then we will take that over the value coming from the user settings.
 *
 * @author Jason van Zyl
 */
public class SettingsAdapter
    extends Settings
{
    private MavenExecutionRequest request;
    private Settings settings;

    public SettingsAdapter( MavenExecutionRequest request, Settings settings )
    {
        this.request = request;
        this.settings = settings;
    }

    public String getLocalRepository()
    {
        if ( request.getLocalRepositoryPath() != null )
        {
            return request.getLocalRepositoryPath().getAbsolutePath();
        }
        
        return settings.getLocalRepository();
    }

    public boolean isInteractiveMode()
    {                    
        return request.isInteractiveMode();            
    }

    public boolean isOffline()
    {
        return request.isOffline();
    }

    // These we are not setting in the execution request currently
    
    public List getProxies()
    {
        return settings.getProxies();
    }

    public List getServers()
    {
        return settings.getServers();
    }

    public List getMirrors()
    {
        return settings.getMirrors();
    }

    public List getProfiles()
    {
        return settings.getProfiles();
    }

    public List getActiveProfiles()
    {
        return settings.getActiveProfiles();
    }

    public List getPluginGroups()
    {
        return settings.getPluginGroups();
    }
}
