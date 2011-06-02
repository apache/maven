/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ArtifactRouterConfiguration
{

    private static final RouterSource CANONICAL_SOURCE = new RouterSource( "default", "http://repository.apache.org/router/routes.json" );

    public static final String ALL_DISCOVERY_STRATEGIES = "all";

    public static final String NO_DISCOVERY_STRATEGIES = "none";
    
    private File routesFile;

    private boolean disabled = false;

    private String[] discoveryStrategies = { ALL_DISCOVERY_STRATEGIES };

    private final Set<ArtifactRouterOption> routerOptions = new HashSet<ArtifactRouterOption>();
    
    private final List<RouterSource> routerSources = new ArrayList<RouterSource>();
    
    public ArtifactRouterConfiguration()
    {
        setUpdate( true );
    }
    
    public ArtifactRouterConfiguration addSource( String id, String url, String username, String password )
    {
        routerSources.add( new RouterSource( id, url, username, password ) );
        return this;
    }
    
    public List<RouterSource> getSources()
    {
        if ( routerSources.isEmpty() )
        {
            return Collections.singletonList( CANONICAL_SOURCE );
        }
        else
        {
            return routerSources;
        }
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

    public String[] getDiscoveryStrategies()
    {
        return discoveryStrategies == null ? new String[0] : discoveryStrategies;
    }

    public ArtifactRouterConfiguration setDiscoveryStrategies( final String... strategies )
    {
        discoveryStrategies = strategies;
        return this;
    }
    
    public File getRoutesFile()
    {
        return routesFile;
    }
    
    public ArtifactRouterConfiguration setRoutesFile( File routesFile )
    {
        this.routesFile = routesFile;
        return this;
    }
    
    public ArtifactRouterConfiguration setOffline( boolean offline )
    {
        return setOption( ArtifactRouterOption.offline, offline );
    }

    private ArtifactRouterConfiguration setOption( ArtifactRouterOption option, boolean enabled )
    {
        if ( enabled )
        {
            routerOptions.add( option );
        }
        else
        {
            routerOptions.remove( option );
        }
        
        return this;
    }

    public boolean isOffline()
    {
        return this.routerOptions.contains( ArtifactRouterOption.offline );
    }
    
    public ArtifactRouterConfiguration setClear( boolean clear )
    {
        return setOption( ArtifactRouterOption.clear, clear );
    }
    
    public boolean isClear()
    {
        return routerOptions.contains( ArtifactRouterOption.clear );
    }

    public ArtifactRouterConfiguration setUpdate( boolean update )
    {
        return setOption( ArtifactRouterOption.update, update );
    }
    
    public boolean isUpdate()
    {
        return routerOptions.contains( ArtifactRouterOption.update );
    }

    public ArtifactRouterConfiguration setOptions( Set<ArtifactRouterOption> routerOptions )
    {
        if ( routerOptions.remove( ArtifactRouterOption.unset ) )
        {
            this.routerOptions.clear();
        }
        
        this.routerOptions.addAll( routerOptions );
        
        return this;
    }

}
