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
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * @author jdcasey
 * @version $Id: DefaultMavenSettingsBuilder.java 189510 2005-06-08 03:27:43Z jdcasey $
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

    // ----------------------------------------------------------------------
    // Component Lifecycle
    // ----------------------------------------------------------------------

    public void initialize()
    {
        userSettingsFile = getFile( userSettingsPath, "user.home", MavenSettingsBuilder.ALT_USER_SETTINGS_XML_LOCATION );
        
        globalSettingsFile = getFile( globalSettingsPath, "maven.home", MavenSettingsBuilder.ALT_GLOBAL_SETTINGS_XML_LOCATION );

        getLogger().debug( "Building Maven global-level settings from: '" + globalSettingsFile.getAbsolutePath() + "'" );
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
            FileReader reader = null;
            try
            {
                reader = new FileReader( settingsFile );

                SettingsXpp3Reader modelReader = new SettingsXpp3Reader();

                settings = modelReader.read( reader );
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
        Settings globalSettings = readSettings( globalSettingsFile );
        Settings userSettings = readSettings( userSettingsFile );

        if ( userSettings == null )
        {
            userSettings = new Settings();
        }

        SettingsUtils.merge( userSettings, globalSettings, TrackableBase.GLOBAL_LEVEL );

        if ( userSettings.getLocalRepository() == null || userSettings.getLocalRepository().length() < 1 )
        {
            File mavenUserConfigurationDirectory = new File( userHome, ".m2" );
            if ( !mavenUserConfigurationDirectory.exists() )
            {
                if ( !mavenUserConfigurationDirectory.mkdirs() )
                {
                    //throw a configuration exception
                }
            }

            String localRepository = new File( mavenUserConfigurationDirectory, "repository" ).getAbsolutePath();

            userSettings.setLocalRepository( localRepository );
        }

        return userSettings;
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

            basedir = basedir.replaceAll( "\\\\", "/" );
            
            path = pathPattern.replaceAll( "\\$\\{" + basedirSysProp + "\\}", basedir );
            path = path.replaceAll( "\\\\", "/" );
            path = path.replaceAll( "//", "/" );

            return new File( path ).getAbsoluteFile();
        }
        else
        {
            return new File( path ).getAbsoluteFile();
        }
    }
    
}
