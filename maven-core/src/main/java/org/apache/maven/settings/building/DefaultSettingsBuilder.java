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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.maven.settings.Settings;
import org.apache.maven.settings.SettingsUtils;
import org.apache.maven.settings.TrackableBase;
import org.apache.maven.settings.io.SettingsParseException;
import org.apache.maven.settings.io.SettingsReader;
import org.apache.maven.settings.io.SettingsWriter;
import org.apache.maven.settings.validation.SettingsValidationResult;
import org.apache.maven.settings.validation.SettingsValidator;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.interpolation.EnvarBasedValueSource;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.PropertiesBasedValueSource;
import org.codehaus.plexus.interpolation.RegexBasedInterpolator;

/**
 * Builds the effective settings from a user settings file and/or a global settings file.
 * 
 * @author Benjamin Bentmann
 */
@Component( role = SettingsBuilder.class )
public class DefaultSettingsBuilder
    implements SettingsBuilder
{

    @Requirement
    private SettingsReader settingsReader;

    @Requirement
    private SettingsWriter settingsWriter;

    @Requirement
    private SettingsValidator settingsValidator;

    public SettingsBuildingResult build( SettingsBuildingRequest request )
        throws SettingsBuildingException
    {
        List<SettingsProblem> problems = new ArrayList<SettingsProblem>();

        Settings globalSettings = readSettings( request.getGlobalSettingsFile(), request, problems );

        Settings userSettings = readSettings( request.getUserSettingsFile(), request, problems );

        SettingsUtils.merge( userSettings, globalSettings, TrackableBase.GLOBAL_LEVEL );

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

        if ( hasErrors( problems ) )
        {
            throw new SettingsBuildingException( problems );
        }

        return new DefaultSettingsBuildingResult( userSettings, problems );
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

    private Settings readSettings( File settingsFile, SettingsBuildingRequest request, List<SettingsProblem> problems )
    {
        if ( settingsFile == null || !settingsFile.exists() )
        {
            return new Settings();
        }

        Settings settings;

        try
        {
            Map<String, ?> options = Collections.singletonMap( SettingsReader.IS_STRICT, Boolean.TRUE );

            try
            {
                settings = settingsReader.read( settingsFile, options );
            }
            catch ( SettingsParseException e )
            {
                options = Collections.singletonMap( SettingsReader.IS_STRICT, Boolean.FALSE );

                settings = settingsReader.read( settingsFile, options );

                problems.add( new DefaultSettingsProblem( e.getMessage(), SettingsProblem.Severity.WARNING,
                                                          settingsFile.getAbsolutePath(), e.getLineNumber(),
                                                          e.getColumnNumber(), e ) );
            }
        }
        catch ( SettingsParseException e )
        {
            problems.add( new DefaultSettingsProblem( "Non-parseable settings " + settingsFile + ": " + e.getMessage(),
                                                      SettingsProblem.Severity.FATAL, settingsFile.getAbsolutePath(),
                                                      e.getLineNumber(), e.getColumnNumber(), e ) );
            return new Settings();
        }
        catch ( IOException e )
        {
            problems.add( new DefaultSettingsProblem( "Non-readable settings " + settingsFile + ": " + e.getMessage(),
                                                      SettingsProblem.Severity.FATAL, settingsFile.getAbsolutePath(),
                                                      -1, -1, e ) );
            return new Settings();
        }

        SettingsValidationResult result = settingsValidator.validate( settings );

        for ( String error : result.getMessages() )
        {
            problems.add( new DefaultSettingsProblem( error, SettingsProblem.Severity.ERROR,
                                                      settingsFile.getAbsolutePath(), -1, -1, null ) );
        }

        return settings;
    }

    private Settings interpolate( Settings settings, SettingsBuildingRequest request, List<SettingsProblem> problems )
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
            problems.add( new DefaultSettingsProblem( "Failed to use environment variables for interpolation: "
                + e.getMessage(), SettingsProblem.Severity.WARNING, "", -1, -1, e ) );
        }

        try
        {
            serializedSettings = interpolator.interpolate( serializedSettings, "settings" );
        }
        catch ( InterpolationException e )
        {
            problems.add( new DefaultSettingsProblem( "Failed to interpolate settings: " + e.getMessage(),
                                                      SettingsProblem.Severity.ERROR, "", -1, -1, e ) );

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
            problems.add( new DefaultSettingsProblem( "Failed to interpolate settings: " + e.getMessage(),
                                                      SettingsProblem.Severity.ERROR, "", -1, -1, e ) );
            return settings;
        }

        return result;
    }

}
