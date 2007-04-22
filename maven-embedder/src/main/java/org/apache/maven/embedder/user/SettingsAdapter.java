package org.apache.maven.embedder.user;

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
 *
 * @author Jason van Zyl
 */
public class SettingsAdapter
    extends Settings
{
    private MavenExecutionRequest request;

    public SettingsAdapter( MavenExecutionRequest request )
    {
        this.request = request;
    }

    public String getLocalRepository()
    {
        return request.getLocalRepositoryPath().getAbsolutePath();
    }

    public boolean isInteractiveMode()
    {
        return request.isInteractiveMode();
    }

    public boolean isOffline()
    {
        return request.isOffline();
    }

    public List getProxies()
    {
        return request.getProxies();
    }

    public List getServers()
    {
        return request.getServers();
    }

    public List getMirrors()
    {
        return request.getMirrors();
    }

    public List getProfiles()
    {
        return request.getProfiles();
    }

    public List getActiveProfiles()
    {
        return request.getActiveProfiles();
    }

    public List getPluginGroups()
    {
        return request.getPluginGroups();
    }
}
