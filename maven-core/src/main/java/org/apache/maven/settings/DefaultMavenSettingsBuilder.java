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

import java.io.File;
import java.io.FileReader;

import org.apache.maven.MavenConstants;
import org.apache.maven.settings.io.xpp3.SettingsXpp3Reader;

import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.util.IOUtil;

/**
 * @author jdcasey
 * @version $Id$
 */
public class DefaultMavenSettingsBuilder
    extends AbstractLogEnabled
    implements MavenSettingsBuilder, Initializable
{
    public static final String userHome = System.getProperty( "user.home" );

    /** @configuration */
    private String settingsPath;

    private File settingsFile;

    // ----------------------------------------------------------------------
    // Component Lifecycle
    // ----------------------------------------------------------------------

    public void initialize()
        throws Exception
    {
        settingsFile = getSettingsFile();

        getLogger().debug( "Building Maven settings from: '" + settingsFile.getAbsolutePath() + "'" );
    }

    // ----------------------------------------------------------------------
    // MavenSettingsBuilder Implementation
    // ----------------------------------------------------------------------

    // TODO: don't throw Exception.
    public Settings buildSettings()
        throws Exception
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
        
        if(settings == null)
        {
            getLogger().debug( "Settings model not found. Creating empty instance of MavenSettings." );

            settings = new Settings();
        }
        
        if(settings.getActiveProfile() == null)
        {
            File mavenUserConfigurationDirectory = new File( userHome, MavenConstants.MAVEN_USER_CONFIGURATION_DIRECTORY );
            if ( !mavenUserConfigurationDirectory.exists() )
            {
                if ( !mavenUserConfigurationDirectory.mkdirs() )
                {
                    //throw a configuration exception
                }
            }

            String localRepository =
                new File( mavenUserConfigurationDirectory, MavenConstants.MAVEN_REPOSITORY ).getAbsolutePath();
            
            settings.initializeActiveProfile( localRepository );
        }
        
        return settings;
    }

    private File getSettingsFile()
    {
        String path = settingsPath;

        // TODO: This replacing shouldn't be necessary as user.home should be in the
        // context of the container and thus the value would be interpolated by Plexus
        String userHome = System.getProperty( "user.home" );
        userHome = userHome.replaceAll( "\\\\", "/" );

        path = path.replaceAll( "\\$\\{user.home\\}", userHome );
        path = path.replaceAll( "\\\\", "/" );
        path = path.replaceAll( "//", "/" );

        return new File( path );
    }
}
