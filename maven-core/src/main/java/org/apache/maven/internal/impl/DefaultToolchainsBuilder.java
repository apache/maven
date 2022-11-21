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

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.services.BuilderProblem;
import org.apache.maven.api.services.ToolchainsBuilder;
import org.apache.maven.api.services.ToolchainsBuilderException;
import org.apache.maven.api.services.ToolchainsBuilderRequest;
import org.apache.maven.api.services.ToolchainsBuilderResult;
import org.apache.maven.api.services.Source;
import org.apache.maven.toolchain.building.DefaultToolchainsBuildingRequest;
import org.apache.maven.toolchain.building.ToolchainsBuildingException;
import org.apache.maven.toolchain.building.ToolchainsBuildingResult;
import org.apache.maven.api.toolchain.PersistedToolchains;

@Named
@Singleton
public class DefaultToolchainsBuilder implements ToolchainsBuilder
{

    private final org.apache.maven.toolchain.building.ToolchainsBuilder builder;

    @Inject
    public DefaultToolchainsBuilder( org.apache.maven.toolchain.building.ToolchainsBuilder builder )
    {
        this.builder = builder;
    }

    @Nonnull
    @Override
    public ToolchainsBuilderResult build( ToolchainsBuilderRequest request )
            throws ToolchainsBuilderException, IllegalArgumentException
    {
        DefaultSession session = ( DefaultSession ) request.getSession();
        try
        {
            DefaultToolchainsBuildingRequest req = new DefaultToolchainsBuildingRequest();
            if ( request.getGlobalToolchainsSource().isPresent() )
            {
                req.setGlobalToolchainsSource(
                        new MappedToolchainsSource( request.getGlobalToolchainsSource().get() ) );
            }
            else if ( request.getGlobalToolchainsPath().isPresent() )
            {
                req.setGlobalToolchainsSource( new org.apache.maven.building.FileSource(
                        request.getGlobalToolchainsPath().get().toFile() ) );
            }
            if ( request.getUserToolchainsSource().isPresent() )
            {
                req.setUserToolchainsSource( new MappedToolchainsSource( request.getUserToolchainsSource().get() ) );
            }
            else if ( request.getUserToolchainsPath().isPresent() )
            {
                req.setUserToolchainsSource( new org.apache.maven.building.FileSource(
                        request.getUserToolchainsPath().get().toFile() ) );
            }
            ToolchainsBuildingResult result = builder.build( req );
            return new ToolchainsBuilderResult()
            {
                @Override
                public PersistedToolchains getEffectiveToolchains()
                {
                    return result.getEffectiveToolchains().getDelegate();
                }

                @Override
                public List<BuilderProblem> getProblems()
                {
                    return new MappedList<>( result.getProblems(), MappedBuilderProblem::new );
                }
            };
        }
        catch ( ToolchainsBuildingException e )
        {
            throw new ToolchainsBuilderException( "Unable to build Toolchains", e );
        }
    }

    private Properties toProperties( Map<String, String> map )
    {
        Properties properties = new Properties();
        properties.putAll( map );
        return properties;
    }

    private static class MappedToolchainsSource implements org.apache.maven.building.Source
    {
        private final Source source;

        MappedToolchainsSource( Source source )
        {
            this.source = source;
        }

        @Override
        public InputStream getInputStream() throws IOException
        {
            return source.getInputStream();
        }

        @Override
        public String getLocation()
        {
            return source.getLocation();
        }
    }

    private static class MappedBuilderProblem implements BuilderProblem
    {
        private final org.apache.maven.building.Problem problem;

        MappedBuilderProblem( org.apache.maven.building.Problem problem )
        {
            this.problem = problem;
        }

        @Override
        public String getSource()
        {
            return problem.getSource();
        }

        @Override
        public int getLineNumber()
        {
            return problem.getLineNumber();
        }

        @Override
        public int getColumnNumber()
        {
            return problem.getColumnNumber();
        }

        @Override
        public String getLocation()
        {
            return problem.getLocation();
        }

        @Override
        public Exception getException()
        {
            return problem.getException();
        }

        @Override
        public String getMessage()
        {
            return problem.getMessage();
        }

        @Override
        public Severity getSeverity()
        {
            return Severity.valueOf( problem.getSeverity().name() );
        }
    }
}
