package org.apache.maven.settings;

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

import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.settings.io.xpp3.SettingsXpp3Reader;
import org.apache.maven.settings.io.xpp3.SettingsXpp3Writer;
import org.apache.maven.settings.validation.SettingsValidationResult;
import org.apache.maven.settings.validation.SettingsValidator;
import org.codehaus.plexus.interpolation.EnvarBasedValueSource;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.PropertiesBasedValueSource;
import org.codehaus.plexus.interpolation.RegexBasedInterpolator;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

/**
 * @author jdcasey
 * @version $Id$
 */
public class DefaultMavenSettingsBuilder
    extends AbstractLogEnabled
    implements MavenSettingsBuilder
{
    private SettingsValidator validator;

    /** @since 2.1 */
    public Settings buildSettings( MavenExecutionRequest request )
        throws IOException, XmlPullParserException
    {
        File userSettingsFile = request.getUserSettingsFile();

        File globalSettingsFile = request.getGlobalSettingsFile();

        if ( ( globalSettingsFile == null ) && ( userSettingsFile == null ) )
        {
            getLogger().debug(
                "No settings files provided, and default locations are disabled for this request. Returning empty Settings instance." );
            return new Settings();
        }

        getLogger().debug( "Reading global settings from: " + globalSettingsFile );

        Settings globalSettings = readSettings( globalSettingsFile );

        if ( globalSettings == null )
        {
            globalSettings = new Settings();
        }

        getLogger().debug( "Reading user settings from: " + userSettingsFile );

        Settings userSettings = readSettings( userSettingsFile );

        if ( userSettings == null )
        {
            userSettings = new Settings();
        }

        validateSettings(
            globalSettings,
            globalSettingsFile );

        validateSettings(
            userSettings,
            userSettingsFile );

        SettingsUtils.merge(
            userSettings,
            globalSettings,
            TrackableBase.GLOBAL_LEVEL );

        userSettings = interpolate( userSettings, request );

        return userSettings;
    }

    private Settings interpolate( Settings settings, MavenExecutionRequest request )
        throws IOException, XmlPullParserException
    {
        List activeProfiles = settings.getActiveProfiles();

        StringWriter writer = new StringWriter();

        new SettingsXpp3Writer().write(
            writer,
            settings );

        String serializedSettings = writer.toString();

        RegexBasedInterpolator interpolator = new RegexBasedInterpolator();

        interpolator.addValueSource( new PropertiesBasedValueSource( request.getProperties() ) );

        interpolator.addValueSource( new EnvarBasedValueSource() );

        try
        {
            serializedSettings = interpolator.interpolate(
                serializedSettings,
                "settings" );
        }
        catch ( InterpolationException e )
        {
            IOException error = new IOException( "Failed to interpolate settings." );
            error.initCause( e );

            throw error;
        }

        Settings result = new SettingsXpp3Reader().read( new StringReader( serializedSettings ) );

        result.setActiveProfiles( activeProfiles );

        return result;
    }

    private Settings readSettings( File settingsFile )
        throws IOException, XmlPullParserException
    {
        if ( settingsFile == null )
        {
            getLogger().debug( "Settings file is null. Returning null." );

            return null;
        }

        if ( !settingsFile.exists() )
        {
            getLogger().debug( "Settings file doesn't exist. Returning null." );

            return null;
        }

        Settings settings = null;

        Reader reader = null;

        try
        {
            reader = ReaderFactory.newXmlReader( settingsFile );

            SettingsXpp3Reader modelReader = new SettingsXpp3Reader();

            settings = modelReader.read( reader );
        }
        catch ( XmlPullParserException e )
        {
            getLogger().error( "Failed to read settings from: " + settingsFile + ". Throwing XmlPullParserException..." );

            throw e;
        }
        catch ( IOException e )
        {
            getLogger().error( "Failed to read settings from: " + settingsFile + ". Throwing IOException..." );

            throw e;
        }
        finally
        {
            IOUtil.close( reader );
        }

        return settings;
    }

    private void validateSettings( Settings settings,
                                   File location )
        throws IOException
    {
        SettingsValidationResult validationResult = validator.validate( settings );

        if ( validationResult.getMessageCount() > 0 )
        {
            throw new IOException( "Failed to validate Settings file at " + location + "\n" + validationResult.render( "\n" ) );
        }
    }
}
