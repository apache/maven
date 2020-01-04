package org.apache.maven.settings.building;

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

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.building.FileSource;
import org.apache.maven.building.Source;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.TrackableBase;
import org.apache.maven.settings.io.SettingsParseException;
import org.apache.maven.settings.io.SettingsReader;
import org.apache.maven.settings.io.SettingsWriter;
import org.apache.maven.settings.merge.MavenSettingsMerger;
import org.apache.maven.settings.validation.SettingsValidator;
import org.codehaus.plexus.interpolation.EnvarBasedValueSource;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.PropertiesBasedValueSource;
import org.codehaus.plexus.interpolation.RegexBasedInterpolator;

/**
 * Builds the effective settings from a user settings file and/or a global settings file.
 *
 * @author Benjamin Bentmann
 */
@Named
@Singleton
public class DefaultSettingsBuilder
    implements SettingsBuilder
{

    private SettingsReader settingsReader;

    private SettingsWriter settingsWriter;

    private SettingsValidator settingsValidator;

    private final MavenSettingsMerger settingsMerger = new MavenSettingsMerger();

    @Inject
    public DefaultSettingsBuilder( SettingsReader settingsReader,
                                   SettingsWriter settingsWriter,
                                   SettingsValidator settingsValidator )
    {
        this.settingsReader = settingsReader;
        this.settingsWriter = settingsWriter;
        this.settingsValidator = settingsValidator;
    }

    public DefaultSettingsBuilder setSettingsReader( SettingsReader settingsReader )
    {
        this.settingsReader = settingsReader;
        return this;
    }

    public DefaultSettingsBuilder setSettingsWriter( SettingsWriter settingsWriter )
    {
        this.settingsWriter = settingsWriter;
        return this;
    }

    public DefaultSettingsBuilder setSettingsValidator( SettingsValidator settingsValidator )
    {
        this.settingsValidator = settingsValidator;
        return this;
    }

    @Override
    public SettingsBuildingResult build( SettingsBuildingRequest request )
        throws SettingsBuildingException
    {
        DefaultSettingsProblemCollector problems = new DefaultSettingsProblemCollector( null );

        Source globalSettingsSource =
            getSettingsSource( request.getGlobalSettingsFile(), request.getGlobalSettingsSource() );
        Settings globalSettings = readSettings( globalSettingsSource, request, problems );

        Source userSettingsSource =
            getSettingsSource( request.getUserSettingsFile(), request.getUserSettingsSource() );
        Settings userSettings = readSettings( userSettingsSource, request, problems );

        settingsMerger.merge( userSettings, globalSettings, TrackableBase.GLOBAL_LEVEL );

        problems.setSource( "" );

        userSettings = interpolate( userSettings, request, problems );

        // for the special case of a drive-relative Windows path, make sure it's absolute to save plugins from trouble
        String localRepository = userSettings.getLocalRepository();
        if ( localRepository != null && localRepository.length() > 0 )
        {
            File file = new File( localRepository );
            if ( !file.isAbsolute() && file.getPath().startsWith( File.separator ) )
            {
                userSettings.setLocalRepository( file.getAbsolutePath() );
            }
        }

        if ( hasErrors( problems.getProblems() ) )
        {
            throw new SettingsBuildingException( problems.getProblems() );
        }

        return new DefaultSettingsBuildingResult( userSettings, problems.getProblems() );
    }

    private boolean hasErrors( List<SettingsProblem> problems )
    {
        if ( problems != null )
        {
            for ( SettingsProblem problem : problems )
            {
                if ( SettingsProblem.Severity.ERROR.compareTo( problem.getSeverity() ) >= 0 )
                {
                    return true;
                }
            }
        }

        return false;
    }

    private Source getSettingsSource( File settingsFile, Source settingsSource )
    {
        if ( settingsSource != null )
        {
            return settingsSource;
        }
        else if ( settingsFile != null && settingsFile.exists() )
        {
            return new FileSource( settingsFile );
        }
        return null;
    }

    private Settings readSettings( Source settingsSource, SettingsBuildingRequest request,
                                   DefaultSettingsProblemCollector problems )
    {
        if ( settingsSource == null )
        {
            return new Settings();
        }

        problems.setSource( settingsSource.getLocation() );

        Settings settings;

        try
        {
            Map<String, ?> options = Collections.singletonMap( SettingsReader.IS_STRICT, Boolean.TRUE );

            try
            {
                settings = settingsReader.read( settingsSource.getInputStream(), options );
            }
            catch ( SettingsParseException e )
            {
                options = Collections.singletonMap( SettingsReader.IS_STRICT, Boolean.FALSE );

                settings = settingsReader.read( settingsSource.getInputStream(), options );

                problems.add( SettingsProblem.Severity.WARNING, e.getMessage(), e.getLineNumber(), e.getColumnNumber(),
                              e );
            }
        }
        catch ( SettingsParseException e )
        {
            problems.add( SettingsProblem.Severity.FATAL, "Non-parseable settings " + settingsSource.getLocation()
                + ": " + e.getMessage(), e.getLineNumber(), e.getColumnNumber(), e );
            return new Settings();
        }
        catch ( IOException e )
        {
            problems.add( SettingsProblem.Severity.FATAL, "Non-readable settings " + settingsSource.getLocation()
                + ": " + e.getMessage(), -1, -1, e );
            return new Settings();
        }

        settingsValidator.validate( settings, problems );

        return settings;
    }

    private Settings interpolate( Settings settings, SettingsBuildingRequest request,
                                  SettingsProblemCollector problems )
    {
        StringWriter writer = new StringWriter( 1024 * 4 );

        try
        {
            settingsWriter.write( writer, null, settings );
        }
        catch ( IOException e )
        {
            throw new IllegalStateException( "Failed to serialize settings to memory", e );
        }

        String serializedSettings = writer.toString();

        RegexBasedInterpolator interpolator = new RegexBasedInterpolator();

        interpolator.addValueSource( new PropertiesBasedValueSource( request.getUserProperties() ) );

        interpolator.addValueSource( new PropertiesBasedValueSource( request.getSystemProperties() ) );

        try
        {
            interpolator.addValueSource( new EnvarBasedValueSource() );
        }
        catch ( IOException e )
        {
            problems.add( SettingsProblem.Severity.WARNING, "Failed to use environment variables for interpolation: "
                + e.getMessage(), -1, -1, e );
        }

        interpolator.addPostProcessor( ( expression, value ) ->
        {
            if ( value != null )
            {
                // we're going to parse this back in as XML so we need to escape XML markup
                value = value.toString().replace( "&", "&amp;" ).replace( "<", "&lt;" ).replace( ">", "&gt;" );
                return value;
            }
            return null;
        } );

        try
        {
            serializedSettings = interpolator.interpolate( serializedSettings, "settings" );
        }
        catch ( InterpolationException e )
        {
            problems.add( SettingsProblem.Severity.ERROR, "Failed to interpolate settings: " + e.getMessage(), -1, -1,
                          e );

            return settings;
        }

        Settings result;
        try
        {
            Map<String, ?> options = Collections.singletonMap( SettingsReader.IS_STRICT, Boolean.FALSE );
            result = settingsReader.read( new StringReader( serializedSettings ), options );
        }
        catch ( IOException e )
        {
            problems.add( SettingsProblem.Severity.ERROR, "Failed to interpolate settings: " + e.getMessage(), -1, -1,
                          e );
            return settings;
        }

        return result;
    }

}
