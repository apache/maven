package org.apache.maven.settings;

/* ====================================================================
 *   Copyright 2001-2004 The Apache Software Foundation.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * ====================================================================
 */

import org.apache.maven.settings.io.xpp3.SettingsXpp3Reader;
import org.codehaus.plexus.logging.AbstractLogEnabled;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * @author jdcasey
 */
public class DefaultMavenSettingsBuilder
    extends AbstractLogEnabled
    implements MavenSettingsBuilder
{

    private static final String DEFAULT_SETTINGS_PATH = "${user.home}/.m2/settings.xml";
    
    private String settingsPath = DEFAULT_SETTINGS_PATH;

    // TODO: don't throw Exception.
    public MavenSettings buildSettings() throws Exception
    {
        MavenSettings settings = null;
        
        File modelFile = getSettingsFile();
        if ( modelFile.exists() && modelFile.isFile() )
        {
            SettingsXpp3Reader modelReader = new SettingsXpp3Reader();
            FileReader reader = null;
            try
            {
                reader = new FileReader( modelFile );

                Settings model = modelReader.read( reader );
                settings = new MavenSettings( model );
            }
            finally
            {
                if ( reader != null )
                {
                    try
                    {
                        reader.close();
                    }
                    catch ( IOException e )
                    {
                    }
                }
            }
        }

        if ( settings == null )
        {
            getLogger().debug( "Settings model not found. Creating empty instance of MavenSettings." );
            settings = new MavenSettings();
        }

        return settings;
    }

    private File getSettingsFile()
    {
        String userDir = System.getProperty( "user.home" );
        
        String path = settingsPath;
        
        path = path.replaceAll( "\\$\\{user.home\\}", userDir );
        path = path.replaceAll( "\\\\", "/" );
        path = path.replaceAll( "//", "/" );

        File userModelFile = new File( path );
        
        getLogger().debug( "Using userModel configured from: " + userModelFile );
        
        return userModelFile;
    }

}
