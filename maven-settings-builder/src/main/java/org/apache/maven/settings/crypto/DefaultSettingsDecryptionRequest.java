package org.apache.maven.settings.crypto;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;

/**
 * Collects parameters that control the decryption of settings.
 *
 * @author Benjamin Bentmann
 */
public class DefaultSettingsDecryptionRequest
    implements SettingsDecryptionRequest
{

    private List<Server> servers;

    private List<Proxy> proxies;

    /**
     * Creates an empty request.
     */
    public DefaultSettingsDecryptionRequest()
    {
        // does nothing
    }

    /**
     * Creates a new request to decrypt the specified settings.
     *
     * @param settings The settings to decrypt, must not be {@code null}.
     */
    public DefaultSettingsDecryptionRequest( Settings settings )
    {
        setServers( settings.getServers() );
        setProxies( settings.getProxies() );
    }

    /**
     * Creates a new request to decrypt the specified server.
     *
     * @param server The server to decrypt, must not be {@code null}.
     */
    public DefaultSettingsDecryptionRequest( Server server )
    {
        this.servers = new ArrayList<>( Arrays.asList( server ) );
    }

    /**
     * Creates a new request to decrypt the specified proxy.
     *
     * @param proxy The proxy to decrypt, must not be {@code null}.
     */
    public DefaultSettingsDecryptionRequest( Proxy proxy )
    {
        this.proxies = new ArrayList<>( Arrays.asList( proxy ) );
    }

    @Override
    public List<Server> getServers()
    {
        if ( servers == null )
        {
            servers = new ArrayList<>();
        }

        return servers;
    }

    @Override
    public DefaultSettingsDecryptionRequest setServers( List<Server> servers )
    {
        this.servers = servers;

        return this;
    }

    @Override
    public List<Proxy> getProxies()
    {
        if ( proxies == null )
        {
            proxies = new ArrayList<>();
        }

        return proxies;
    }

    @Override
    public DefaultSettingsDecryptionRequest setProxies( List<Proxy> proxies )
    {
        this.proxies = proxies;

        return this;
    }

}
