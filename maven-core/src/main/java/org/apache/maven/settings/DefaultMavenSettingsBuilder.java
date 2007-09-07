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

import org.apache.maven.context.BuildContextManager;
import org.apache.maven.context.SystemBuildContext;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.settings.io.xpp3.SettingsXpp3Reader;
import org.apache.maven.settings.io.xpp3.SettingsXpp3Writer;
import org.apache.maven.settings.validation.SettingsValidationResult;
import org.apache.maven.settings.validation.SettingsValidator;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.interpolation.EnvarBasedValueSource;
import org.codehaus.plexus.util.interpolation.PropertiesBasedValueSource;
import org.codehaus.plexus.util.interpolation.RegexBasedInterpolator;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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

    private BuildContextManager manager;

    /**
     * @since 2.1
     */
    public Settings buildSettings( File userSettingsFile, File globalSettingsFile )
        throws IOException, XmlPullParserException
    {
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

        validateSettings( globalSettings, globalSettingsFile );

        validateSettings( userSettings, userSettingsFile );

        SettingsUtils.merge( userSettings, globalSettings, TrackableBase.GLOBAL_LEVEL );

        userSettings = interpolate( userSettings );

        return userSettings;
    }

    private Settings interpolate( Settings settings )
        throws IOException, XmlPullParserException
    {
        List activeProfiles = settings.getActiveProfiles();

        StringWriter writer = new StringWriter();
        
        new SettingsXpp3Writer().write( writer, settings );

        String serializedSettings = writer.toString();

        SystemBuildContext sysContext = SystemBuildContext.getSystemBuildContext( manager, true );

        RegexBasedInterpolator interpolator = new RegexBasedInterpolator();

        interpolator.addValueSource( new PropertiesBasedValueSource( sysContext.getSystemProperties() ) );

        interpolator.addValueSource( new EnvarBasedValueSource() );

        serializedSettings = interpolator.interpolate( serializedSettings, "settings" );

        Settings result = new SettingsXpp3Reader().read( new StringReader( serializedSettings ) );

        result.setActiveProfiles( activeProfiles );

        return result;
    }

    private Settings readSettings( File settingsFile )
        throws IOException, XmlPullParserException
    {
        if ( settingsFile == null )
        {
            getLogger().debug( "Settings file is null. Returning." );
            return null;
        }

        Settings settings = null;

        if ( settingsFile.exists() && settingsFile.isFile() )
        {
            getLogger().debug( "Settings file is a proper file. Reading." );

            FileReader reader = null;
            try
            {
                reader = new FileReader( settingsFile );

                SettingsXpp3Reader modelReader = new SettingsXpp3Reader();

                settings = modelReader.read( reader );

                RuntimeInfo rtInfo = new RuntimeInfo( settings );

                rtInfo.addLocation( settingsFile.getAbsolutePath() );

                settings.setRuntimeInfo( rtInfo );
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
        }

        return settings;
    }

    private void validateSettings( Settings settings, File location )
        throws IOException
    {
        SettingsValidationResult validationResult = validator.validate( settings );

        if ( validationResult.getMessageCount() > 0 )
        {
            throw new IOException( "Failed to validate Settings file at " + location + "\n" + validationResult.render( "\n" ) );
        }
    }

    /**
      * @return a <code>Settings</code> object from the user and global settings file.
      * @throws IOException if any
      * @throws XmlPullParserException if any
      * @deprecated Use {@link org.apache.maven.settings.MavenSettingsBuilder#buildSettings(java.io.File,java.io.File)} instead.
     */
    public Settings buildSettings()
        throws IOException, XmlPullParserException
    {
        String mavenHome = System.getProperty( "maven.home" );
        String userHome = System.getProperty( "user.home" );

        return buildSettings( new File( userHome, ".m2/settings.xml" ), new File( mavenHome, "conf/settings.xml" ) );
    }
}
