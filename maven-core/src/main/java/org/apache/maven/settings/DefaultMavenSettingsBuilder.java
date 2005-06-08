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

import org.apache.maven.settings.Settings;
import org.apache.maven.settings.io.xpp3.SettingsXpp3Reader;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

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
    private String settingsPath;

    private File settingsFile;

    // ----------------------------------------------------------------------
    // Component Lifecycle
    // ----------------------------------------------------------------------

    public void initialize()
    {
        settingsFile = getSettingsFile();

        getLogger().debug( "Building Maven settings from: '" + settingsFile.getAbsolutePath() + "'" );
    }

    // ----------------------------------------------------------------------
    // MavenProfilesBuilder Implementation
    // ----------------------------------------------------------------------

    public Settings buildSettings()
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
            catch ( FileNotFoundException e )
            {
                // Not possible - just ignore
                getLogger().warn( "Settings file disappeared - ignoring", e );
            }
            finally
            {
                IOUtil.close( reader );
            }
        }

        if ( settings == null )
        {
            getLogger().debug( "Settings model not found. Creating empty instance of MavenSettings." );

            settings = new Settings();
        }
        
        if( settings.getLocalRepository() == null || settings.getLocalRepository().length() < 1 )
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
            
            settings.setLocalRepository( localRepository );
        }

        return settings;
    }

    private File getSettingsFile()
    {
        String path = System.getProperty( MavenSettingsBuilder.ALT_SETTINGS_XML_LOCATION );
        
        if( StringUtils.isEmpty( path ) )
        {
            // TODO: This replacing shouldn't be necessary as user.home should be in the
            // context of the container and thus the value would be interpolated by Plexus
            String userHome = System.getProperty( "user.home" );

            return new File( userHome, settingsPath ).getAbsoluteFile();
        }
        else
        {
            return new File( path ).getAbsoluteFile();
        }
    }
}
