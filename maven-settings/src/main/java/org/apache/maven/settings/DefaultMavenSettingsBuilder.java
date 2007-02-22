package org.apache.maven.settings;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.context.BuildContextManager;
import org.apache.maven.settings.cache.SettingsCache;
import org.apache.maven.settings.io.xpp3.SettingsXpp3Reader;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.interpolation.EnvarBasedValueSource;
import org.codehaus.plexus.util.interpolation.RegexBasedInterpolator;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;
import org.apache.maven.settings.validation.SettingsValidationResult;
import org.apache.maven.settings.validation.SettingsValidator;

/**
 * @author jdcasey
 * @version $Id$
 */
public class DefaultMavenSettingsBuilder
    extends AbstractLogEnabled
    implements MavenSettingsBuilder
{
    
    private SettingsValidator validator;
    
    private BuildContextManager buildContextManager;
    
    /**
     * @since 2.1
     */
    public Settings buildSettings( File userSettingsFile, File globalSettingsFile )
        throws IOException, XmlPullParserException
    {
        SettingsCache cache = SettingsCache.read( buildContextManager, userSettingsFile, globalSettingsFile );
        
        if ( cache != null )
        {
            return cache.getSettings();
        }
        
        // NOTE: We're allowing users to hang themselves here...if the global settings file is null,
        // the default location is NOT read.
        Settings globalSettings = readSettings( globalSettingsFile );
        
        if ( userSettingsFile == null )
        {
            userSettingsFile = DEFAULT_USER_SETTINGS_FILE;
        }

        Settings userSettings = readSettings( userSettingsFile );

        if ( globalSettings == null )
        {
            globalSettings = new Settings();
        }

        if ( userSettings == null )
        {
            userSettings = new Settings();

            userSettings.setRuntimeInfo( new RuntimeInfo( userSettings ) );
        }
        
        validateSettings( globalSettings, globalSettingsFile );
        
        validateSettings( userSettings, userSettingsFile );

        SettingsUtils.merge( userSettings, globalSettings, TrackableBase.GLOBAL_LEVEL );

        activateDefaultProfiles( userSettings );
        
        cache = new SettingsCache( userSettingsFile, globalSettingsFile, userSettings );
        cache.store( buildContextManager );

        return userSettings;
    }

    /**
     * @deprecated
     */
    public Settings buildSettings()
        throws IOException, XmlPullParserException
    {
        return buildSettings( DEFAULT_USER_SETTINGS_FILE );
    }

    /**
     * @deprecated
     */
    public Settings buildSettings( File userSettingsFile )
        throws IOException, XmlPullParserException
    {
        return buildSettings( userSettingsFile, null );
    }


    private Settings readSettings( File settingsFile )
        throws IOException, XmlPullParserException
    {
        if ( settingsFile == null )
        {
            return null;
        }

        Settings settings = null;

        if ( settingsFile.exists() && settingsFile.isFile() )
        {
            FileReader reader = null;
            try
            {
                reader = new FileReader( settingsFile );
                StringWriter sWriter = new StringWriter();

                IOUtil.copy( reader, sWriter );

                String rawInput = sWriter.toString();

                try
                {
                    RegexBasedInterpolator interpolator = new RegexBasedInterpolator();
                    interpolator.addValueSource( new EnvarBasedValueSource() );

                    rawInput = interpolator.interpolate( rawInput, "settings" );
                }
                catch ( Exception e )
                {
                    getLogger().warn(
                        "Failed to initialize environment variable resolver. Skipping environment substitution in settings." );
                    getLogger().debug( "Failed to initialize envar resolver. Skipping resolution.", e );
                }

                StringReader sReader = new StringReader( rawInput );

                SettingsXpp3Reader modelReader = new SettingsXpp3Reader();

                settings = modelReader.read( sReader );

                RuntimeInfo rtInfo = new RuntimeInfo( settings );

                rtInfo.setFile( settingsFile );

                settings.setRuntimeInfo( rtInfo );
            }
            finally
            {
                IOUtil.close( reader );
            }
        }

        return settings;
    }

    private void activateDefaultProfiles( Settings settings )
    {
        List activeProfiles = settings.getActiveProfiles();

        for ( Iterator profiles = settings.getProfiles().iterator(); profiles.hasNext(); )
        {
            Profile profile = (Profile) profiles.next();
            if ( profile.getActivation() != null && profile.getActivation().isActiveByDefault()
                && !activeProfiles.contains( profile.getId() ) )
            {
                settings.addActiveProfile( profile.getId() );
            }
        }
    }
    
    private void validateSettings(Settings settings, File location) throws IOException {
        SettingsValidationResult validationResult = validator.validate( settings );

        if ( validationResult.getMessageCount() > 0 )
        {
            throw new IOException( "Failed to validate Settings file at " + location + 
                                    "\n" + validationResult.render("\n") );
        }
        
    }
}
