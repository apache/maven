/*
 * Copyright 2010 Red Hat, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.maven.repository.mirror.configuration;

import org.apache.http.auth.UsernamePasswordCredentials;

public class MirrorRouterConfiguration
{

    private static final String CANONICAL_ROUTER_URL = "http://repository.apache.org/router/mirrors.json";

    public static final String ALL_DISCOVERY_STRATEGIES = "all";

    public static final String NO_DISCOVERY_STRATEGIES = "none";

    private String routerUrl;

    private UsernamePasswordCredentials routerCredentials;

    private boolean disabled = false;

    private String[] discoveryStrategies = { ALL_DISCOVERY_STRATEGIES };

    public MirrorRouterConfiguration setRouterCredentials( final String user, final String password )
    {
        routerCredentials = new UsernamePasswordCredentials( user, password );
        return this;
    }

    public UsernamePasswordCredentials getRouterCredentials()
    {
        return routerCredentials;
    }

    public MirrorRouterConfiguration setRouterUrl( final String routerUrl )
    {
        this.routerUrl = routerUrl;
        return this;
    }

    public String getRouterUrl()
    {
        return routerUrl;
    }

    public boolean isDisabled()
    {
        return disabled;
    }

    public MirrorRouterConfiguration setDisabled( final boolean disabled )
    {
        this.disabled = disabled;
        return this;
    }

    public String getCanonicalRouterUrl()
    {
        return CANONICAL_ROUTER_URL;
    }

    public String[] getDiscoveryStrategies()
    {
        return discoveryStrategies == null ? new String[0] : discoveryStrategies;
    }

    public MirrorRouterConfiguration setDiscoveryStrategies( final String... strategies )
    {
        discoveryStrategies = strategies;
        return this;
    }

}
