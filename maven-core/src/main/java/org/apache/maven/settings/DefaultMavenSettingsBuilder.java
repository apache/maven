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
    public MavenSettings buildSettings()
        throws Exception
    {
        if ( settingsFile.exists() && settingsFile.isFile() )
        {
            FileReader reader = null;
            try
            {
                reader = new FileReader( settingsFile );

                SettingsXpp3Reader modelReader = new SettingsXpp3Reader();

                Settings model = modelReader.read( reader );

                return new MavenSettings( model );
            }
            finally
            {
                IOUtil.close( reader );
            }
        }
        else
        {
            getLogger().debug( "Settings model not found. Creating empty instance of MavenSettings." );

            return new MavenSettings();
        }
    }

    private File getSettingsFile()
    {
        String path = settingsPath;

        // TODO: This replacing shouldn't be necessary as user.home should be in the
        // context of the container and thus the value would be interpolated by Plexus
        String userHome = System.getProperty( "user.home" );

        path = path.replaceAll( "\\$\\{user.home\\}", userHome );
        path = path.replaceAll( "\\\\", "/" );
        path = path.replaceAll( "//", "/" );

        return new File( path );
    }
}
