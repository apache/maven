package org.apache.maven.execution;

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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.RuntimeInfo;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.SettingsUtils;

/**
 * Adapt a {@link MavenExecutionRequest} to a {@link Settings} object for use in the Maven core.
 * We want to make sure that what is ask for in the execution request overrides what is in the settings.
 * The CLI feeds into an execution request so if a particular value is present in the execution request
 * then we will take that over the value coming from the user settings.
 *
 * @author Jason van Zyl
 */
class SettingsAdapter
    extends Settings
{

    private MavenExecutionRequest request;

    private RuntimeInfo runtimeInfo;

    SettingsAdapter( MavenExecutionRequest request )
    {
        this.request = request;

        /*
         * NOTE: Plugins like maven-release-plugin query the path to the settings.xml to pass it into a forked Maven and
         * the CLI will fail when called with a non-existing settings, so be sure to only point at actual files. Having
         * a null file should be harmless as this case matches general Maven 2.x behavior...
         */
        File userSettings = request.getUserSettingsFile();
        this.runtimeInfo = new RuntimeInfo( ( userSettings != null && userSettings.isFile() ) ? userSettings : null );
    }

    @Override
    public String getLocalRepository()
    {
        if ( request.getLocalRepositoryPath() != null )
        {
            return request.getLocalRepositoryPath().getAbsolutePath();
        }

        return null;
    }

    @Override
    public boolean isInteractiveMode()
    {
        return request.isInteractiveMode();
    }

    @Override
    public boolean isOffline()
    {
        return request.isOffline();
    }

    @Override
    public List<Proxy> getProxies()
    {
        return request.getProxies();
    }

    @Override
    public List<Server> getServers()
    {
        return request.getServers();
    }

    @Override
    public List<Mirror> getMirrors()
    {
        return request.getMirrors();
    }

    @Override
    public List<Profile> getProfiles()
    {
        List<Profile> result = new ArrayList<>();
        for ( org.apache.maven.model.Profile profile : request.getProfiles() )
        {
            result.add( SettingsUtils.convertToSettingsProfile( profile ) );
        }
        return result;
    }

    @Override
    public List<String> getActiveProfiles()
    {
        return request.getActiveProfiles();
    }

    @Override
    public List<String> getPluginGroups()
    {
        return request.getPluginGroups();
    }
}
