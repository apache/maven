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

import org.apache.maven.settings.io.xpp3.SettingsXpp3Reader;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.interpolation.EnvarBasedValueSource;
import org.codehaus.plexus.util.interpolation.RegexBasedInterpolator;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
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
    implements MavenSettingsBuilder, Initializable
{
    public static final String userHome = System.getProperty( "user.home" );

    /**
     * @configuration
     */
    private String userSettingsPath;

    /**
     * @configuration
     */
    private String globalSettingsPath;

    private File userSettingsFile;

    private File globalSettingsFile;

    private Settings loadedSettings;

    // ----------------------------------------------------------------------
    // Component Lifecycle
    // ----------------------------------------------------------------------

    public void initialize()
    {
        userSettingsFile =
            getFile( userSettingsPath, "user.home", MavenSettingsBuilder.ALT_USER_SETTINGS_XML_LOCATION );

        globalSettingsFile =
            getFile( globalSettingsPath, "maven.home", MavenSettingsBuilder.ALT_GLOBAL_SETTINGS_XML_LOCATION );

        getLogger().debug(
            "Building Maven global-level settings from: '" + globalSettingsFile.getAbsolutePath() + "'" );
        getLogger().debug( "Building Maven user-level settings from: '" + userSettingsFile.getAbsolutePath() + "'" );
    }

    // ----------------------------------------------------------------------
    // MavenProfilesBuilder Implementation
    // ----------------------------------------------------------------------

    private Settings readSettings( File settingsFile )
        throws IOException, XmlPullParserException
    {
        Settings settings = null;

        if ( settingsFile.exists() && settingsFile.isFile() )
        {
            Reader reader = null;
            try
            {
                reader = ReaderFactory.newXmlReader( settingsFile );
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

    public Settings buildSettings()
        throws IOException, XmlPullParserException
    {
        return buildSettings( userSettingsFile );
    }

    public Settings buildSettings( boolean useCachedSettings )
        throws IOException, XmlPullParserException
    {
        return buildSettings( userSettingsFile, useCachedSettings );
    }

    public Settings buildSettings( File userSettingsFile )
        throws IOException, XmlPullParserException
    {
        return buildSettings( userSettingsFile, true );
    }

    public Settings buildSettings( File userSettingsFile, boolean useCachedSettings )
        throws IOException, XmlPullParserException
    {
        if ( !useCachedSettings || loadedSettings == null )
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

            setLocalRepository( userSettings );

            loadedSettings = userSettings;
        }

        return loadedSettings;
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

    private void setLocalRepository( Settings userSettings )
    {
        // try using the local repository specified on the command line...
        String localRepository = System.getProperty( MavenSettingsBuilder.ALT_LOCAL_REPOSITORY_LOCATION );

        // otherwise, use the one in settings.xml
        if ( localRepository == null || localRepository.length() < 1 )
        {
            localRepository = userSettings.getLocalRepository();
        }

        // if all of the above are missing, default to ~/.m2/repository.
        if ( localRepository == null || localRepository.length() < 1 )
        {
            File mavenUserConfigurationDirectory = new File( userHome, ".m2" );
            if ( !mavenUserConfigurationDirectory.exists() )
            {
                if ( !mavenUserConfigurationDirectory.mkdirs() )
                {
                    //throw a configuration exception
                }
            }

            localRepository = new File( mavenUserConfigurationDirectory, "repository" ).getAbsolutePath();
        }

        userSettings.setLocalRepository( localRepository );
    }

    private File getFile( String pathPattern, String basedirSysProp, String altLocationSysProp )
    {
        // -------------------------------------------------------------------------------------
        // Alright, here's the justification for all the regexp wizardry below...
        //
        // Continuum and other server-like apps may need to locate the user-level and 
        // global-level settings somewhere other than ${user.home} and ${maven.home},
        // respectively. Using a simple replacement of these patterns will allow them
        // to specify the absolute path to these files in a customized components.xml
        // file. Ideally, we'd do full pattern-evaluation against the sysprops, but this
        // is a first step. There are several replacements below, in order to normalize
        // the path character before we operate on the string as a regex input, and 
        // in order to avoid surprises with the File construction...
        // -------------------------------------------------------------------------------------

        String path = System.getProperty( altLocationSysProp );

        if ( StringUtils.isEmpty( path ) )
        {
            // TODO: This replacing shouldn't be necessary as user.home should be in the
            // context of the container and thus the value would be interpolated by Plexus
            String basedir = System.getProperty( basedirSysProp );
            if ( basedir == null )
            {
                basedir = System.getProperty( "user.dir" );
            }

            basedir = basedir.replaceAll( "\\\\", "/" );
            basedir = basedir.replaceAll( "\\$", "\\\\\\$" );

            path = pathPattern.replaceAll( "\\$\\{" + basedirSysProp + "\\}", basedir );
            path = path.replaceAll( "\\\\", "/" );
            // ---------------------------------------------------------------------------------
            // I'm not sure if this last regexp was really intended to disallow the usage of
            // network paths as user.home directory. Unfortunately it did. I removed it and 
            // have not detected any problems yet.
            // ---------------------------------------------------------------------------------
            // path = path.replaceAll( "//", "/" );

            return new File( path ).getAbsoluteFile();
        }
        else
        {
            return new File( path ).getAbsoluteFile();
        }
    }

}
