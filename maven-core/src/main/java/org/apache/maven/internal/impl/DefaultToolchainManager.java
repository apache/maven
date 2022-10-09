package org.apache.maven.internal.impl;

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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.maven.api.Session;
import org.apache.maven.api.Toolchain;
import org.apache.maven.api.services.ToolchainManager;
import org.apache.maven.api.services.ToolchainManagerException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.toolchain.DefaultToolchainManagerPrivate;
import org.apache.maven.toolchain.MisconfiguredToolchainException;
import org.apache.maven.toolchain.ToolchainPrivate;

@Named
@Singleton
public class DefaultToolchainManager implements ToolchainManager
{
    private final DefaultToolchainManagerPrivate toolchainManagerPrivate;

    @Inject
    public DefaultToolchainManager( DefaultToolchainManagerPrivate toolchainManagerPrivate )
    {
        this.toolchainManagerPrivate = toolchainManagerPrivate;
    }

    @Override
    public List<Toolchain> getToolchains( Session session, String type, Map<String, String> requirements )
            throws ToolchainManagerException
    {
        MavenSession s = ( ( DefaultSession ) session ).getMavenSession();
        List<org.apache.maven.toolchain.Toolchain> toolchains =
                toolchainManagerPrivate.getToolchains( s, type, requirements );
        return new MappedList<>( toolchains, this::toToolchain );
    }

    @Override
    public Optional<Toolchain> getToolchainFromBuildContext( Session session, String type )
            throws ToolchainManagerException
    {
        MavenSession s = ( ( DefaultSession ) session ).getMavenSession();
        return Optional.ofNullable( toolchainManagerPrivate.getToolchainFromBuildContext( type, s ) )
                .map( this::toToolchain );
    }

    @Override
    public List<Toolchain> getToolchainsForType( Session session, String type )
            throws ToolchainManagerException
    {
        try
        {
            MavenSession s = ( (DefaultSession) session ).getMavenSession();
            ToolchainPrivate[] toolchains = toolchainManagerPrivate.getToolchainsForType( type, s );
            return new MappedList<>( Arrays.asList( toolchains ), this::toToolchain );
        }
        catch ( MisconfiguredToolchainException e )
        {
            throw new ToolchainManagerException( "Unable to get toochains for type " + type, e );
        }
    }

    @Override
    public void storeToolchainToBuildContext( Session session, Toolchain toolchain )
            throws ToolchainManagerException
    {
        MavenSession s = ( ( DefaultSession ) session ).getMavenSession();
        org.apache.maven.toolchain.ToolchainPrivate tc =
                (org.apache.maven.toolchain.ToolchainPrivate) ( (ToolchainWrapper) toolchain ).toolchain;
        toolchainManagerPrivate.storeToolchainToBuildContext( tc, s );
    }

    private Toolchain toToolchain( org.apache.maven.toolchain.Toolchain toolchain )
    {
        return new ToolchainWrapper( toolchain );
    }

    private static class ToolchainWrapper implements Toolchain
    {
        private final org.apache.maven.toolchain.Toolchain toolchain;

        ToolchainWrapper( org.apache.maven.toolchain.Toolchain toolchain )
        {
            this.toolchain = toolchain;
        }

        @Override
        public String getType()
        {
            return toolchain.getType();
        }

        @Override
        public String findTool( String toolName )
        {
            return toolchain.findTool( toolName );
        }

        @Override
        public boolean matchesRequirements( Map<String, String> requirements )
        {
            return ( (ToolchainPrivate) toolchain ).matchesRequirements( requirements );
        }
    }
}
