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
import org.apache.maven.api.services.BuilderProblemSeverity;
import org.apache.maven.api.services.SettingsBuilder;
import org.apache.maven.api.services.SettingsBuilderException;
import org.apache.maven.api.services.SettingsBuilderRequest;
import org.apache.maven.api.services.SettingsBuilderResult;
import org.apache.maven.api.services.Source;
import org.apache.maven.api.settings.Settings;
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.apache.maven.settings.building.SettingsBuildingResult;
import org.apache.maven.settings.building.SettingsProblem;
import org.apache.maven.settings.building.SettingsSource;

@Named
@Singleton
public class DefaultSettingsBuilder implements SettingsBuilder
{

    private final org.apache.maven.settings.building.SettingsBuilder builder;

    @Inject
    public DefaultSettingsBuilder( org.apache.maven.settings.building.SettingsBuilder builder )
    {
        this.builder = builder;
    }

    @Nonnull
    @Override
    public SettingsBuilderResult build( SettingsBuilderRequest request )
            throws SettingsBuilderException, IllegalArgumentException
    {
        DefaultSession session = ( DefaultSession ) request.getSession();
        try
        {
            DefaultSettingsBuildingRequest req = new DefaultSettingsBuildingRequest();
            req.setUserProperties( toProperties( session.getUserProperties() ) );
            req.setSystemProperties( toProperties( session.getSystemProperties() ) );
            if ( request.getGlobalSettingsSource().isPresent() )
            {
                req.setGlobalSettingsSource( new MappedSettingsSource( request.getGlobalSettingsSource().get() ) );
            }
            if ( request.getGlobalSettingsPath().isPresent() )
            {
                req.setGlobalSettingsFile( request.getGlobalSettingsPath().get().toFile() );
            }
            if ( request.getUserSettingsSource().isPresent() )
            {
                req.setUserSettingsSource( new MappedSettingsSource( request.getUserSettingsSource().get() ) );
            }
            if ( request.getUserSettingsPath().isPresent() )
            {
                req.setUserSettingsFile( request.getUserSettingsPath().get().toFile() );
            }
            SettingsBuildingResult result = builder.build( req );
            return new SettingsBuilderResult()
            {
                @Override
                public Settings getEffectiveSettings()
                {
                    return result.getEffectiveSettings().getDelegate();
                }

                @Override
                public List<BuilderProblem> getProblems()
                {
                    return new MappedList<>( result.getProblems(), MappedBuilderProblem::new );
                }
            };
        }
        catch ( SettingsBuildingException e )
        {
            throw new SettingsBuilderException( "Unable to build settings", e );
        }
    }

    private Properties toProperties( Map<String, String> map )
    {
        Properties properties = new Properties();
        properties.putAll( map );
        return properties;
    }

    private static class MappedSettingsSource implements SettingsSource
    {
        private final Source source;

        MappedSettingsSource( Source source )
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
        private final SettingsProblem problem;

        MappedBuilderProblem( SettingsProblem problem )
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
        public BuilderProblemSeverity getSeverity()
        {
            return BuilderProblemSeverity.valueOf( problem.getSeverity().name() );
        }
    }
}
