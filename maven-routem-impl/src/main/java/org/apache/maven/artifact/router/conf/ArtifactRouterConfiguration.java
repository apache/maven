package org.apache.maven.artifact.router.conf;

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

import org.apache.http.auth.UsernamePasswordCredentials;

import java.io.File;

public final class ArtifactRouterConfiguration
{

    private static final String CANONICAL_MIRRORS_URL = "http://repository.apache.org/router/mirrors.json";

    private static final String CANONICAL_GROUPS_URL = "http://repository.apache.org/router/groups.json";

    public static final String ALL_DISCOVERY_STRATEGIES = "all";

    public static final String NO_DISCOVERY_STRATEGIES = "none";
    
    private File selectedRoutesFile;

    private String routerMirrorsUrl;

    private UsernamePasswordCredentials routerCredentials;

    private boolean disabled = false;

    private String[] discoveryStrategies = { ALL_DISCOVERY_STRATEGIES };

    private String routerGroupsUrl;

    public ArtifactRouterConfiguration setRouterCredentials( final String user, final String password )
    {
        routerCredentials = new UsernamePasswordCredentials( user, password );
        return this;
    }

    public UsernamePasswordCredentials getRouterCredentials()
    {
        return routerCredentials;
    }

    public ArtifactRouterConfiguration setRouterMirrorsUrl( final String routerMirrorsUrl )
    {
        this.routerMirrorsUrl = routerMirrorsUrl;
        return this;
    }

    public String getRouterMirrorsUrl()
    {
        return routerMirrorsUrl;
    }

    public void setRouterGroupsUrl( String routerGroupsUrl )
    {
        this.routerGroupsUrl = routerGroupsUrl;
    }
    
    public String getRouterGroupsUrl()
    {
        return routerGroupsUrl;
    }

    public boolean isDisabled()
    {
        return disabled;
    }

    public ArtifactRouterConfiguration setDisabled( final boolean disabled )
    {
        this.disabled = disabled;
        return this;
    }

    public String getCanonicalMirrorsUrl()
    {
        return CANONICAL_MIRRORS_URL;
    }

    public String getCanonicalGroupsUrl()
    {
        return CANONICAL_GROUPS_URL;
    }

    public String[] getDiscoveryStrategies()
    {
        return discoveryStrategies == null ? new String[0] : discoveryStrategies;
    }

    public ArtifactRouterConfiguration setDiscoveryStrategies( final String... strategies )
    {
        discoveryStrategies = strategies;
        return this;
    }
    
    public File getSelectedRoutesFile()
    {
        return selectedRoutesFile;
    }
    
    public ArtifactRouterConfiguration setSelectedRoutesFile( File selectedRoutesFile )
    {
        this.selectedRoutesFile = selectedRoutesFile;
        return this;
    }

}
