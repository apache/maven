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

package org.apache.maven.artifact.router.session;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.apache.maven.artifact.router.conf.ArtifactRouterConfiguration;
import org.apache.maven.artifact.router.conf.RouterSource;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Server;

public class DefaultArtifactRouterSession
    implements ArtifactRouterSession
{
    
    private ArtifactRouterConfiguration config;
    
    private final List<Server> servers;
    
    private final List<Proxy> proxies;
    
    public DefaultArtifactRouterSession( final ArtifactRouterConfiguration config, final List<Server> servers,
                                         final List<Proxy> proxies )
    {
        this.config = config;
        this.servers = servers;
        this.proxies = proxies;
    }

    public DefaultArtifactRouterSession( ArtifactRouterConfiguration config )
    {
        this.config = config;
        this.servers = Collections.emptyList();
        this.proxies = Collections.emptyList();
    }

    /**
     * {@inheritDoc}
     * @see org.apache.maven.artifact.router.session.ArtifactRouterSession#getDiscoveryStrategies()
     */
    public String getDiscoveryStrategy()
    {
        return config.getDiscoveryStrategy();
    }

    /**
     * {@inheritDoc}
     * @see org.apache.maven.artifact.router.session.ArtifactRouterSession#getProxy(java.lang.String)
     */
    public Proxy getProxy( String protocol )
    {
        if ( proxies != null )
        {
            for ( Proxy proxy : proxies )
            {
                if ( proxy.isActive() && proxy.getProtocol().equals( protocol ) )
                {
                    return proxy;
                }
            }
        }
        
        return null;
    }

    /**
     * {@inheritDoc}
     * @see org.apache.maven.artifact.router.session.ArtifactRouterSession#getServer(java.lang.String, java.lang.String)
     */
    public Server getServer( String id )
    {
        if ( servers != null )
        {
            for ( Server server : servers )
            {
                if ( server.getId().equals( id ) )
                {
                    return server;
                }
            }
        }
        
        return null;
    }

    /**
     * {@inheritDoc}
     * @see org.apache.maven.artifact.router.session.ArtifactRouterSession#getSources()
     */
    public RouterSource getSource()
    {
        return config.getSource();
    }

    public RouterSource getDefaultSource()
    {
        return config.getDefaultSource();
    }

    /**
     * {@inheritDoc}
     * @see org.apache.maven.artifact.router.session.ArtifactRouterSession#getRoutesFile()
     */
    public File getRoutesFile()
    {
        return config.getRoutesFile();
    }

    /**
     * {@inheritDoc}
     * @see org.apache.maven.artifact.router.session.ArtifactRouterSession#isClear()
     */
    public boolean isClear()
    {
        return config.isClear();
    }

    /**
     * {@inheritDoc}
     * @see org.apache.maven.artifact.router.session.ArtifactRouterSession#isDisabled()
     */
    public boolean isDisabled()
    {
        return config.isDisabled();
    }

    /**
     * {@inheritDoc}
     * @see org.apache.maven.artifact.router.session.ArtifactRouterSession#isOffline()
     */
    public boolean isOffline()
    {
        return config.isOffline();
    }

    /**
     * {@inheritDoc}
     * @see org.apache.maven.artifact.router.session.ArtifactRouterSession#isUpdate()
     */
    public boolean isUpdate()
    {
        return config.isUpdate();
    }

}
