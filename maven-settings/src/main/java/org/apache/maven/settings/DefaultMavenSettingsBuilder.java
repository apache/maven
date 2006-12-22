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

/**
 * @author jdcasey
 * @version $Id$
 */
public class DefaultMavenSettingsBuilder
    extends AbstractLogEnabled
    implements MavenSettingsBuilder
{
    // ----------------------------------------------------------------------
    // MavenProfilesBuilder Implementation
    // ----------------------------------------------------------------------

    /**
     * @since 2.1
     */
    public Settings buildSettings( File userSettingsFile, File globalSettingsFile )
        throws IOException, XmlPullParserException
    {
        Settings globalSettings = readSettings( globalSettingsFile );

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

        SettingsUtils.merge( userSettings, globalSettings, TrackableBase.GLOBAL_LEVEL );

        activateDefaultProfiles( userSettings );

        return userSettings;
    }

    /**
     * @deprecated
     */
    public Settings buildSettings()
        throws IOException, XmlPullParserException
    {
        return buildSettings( new File( new File( System.getProperty( "user.home" ) ), ".m2/settings.xml" ) );
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
}
