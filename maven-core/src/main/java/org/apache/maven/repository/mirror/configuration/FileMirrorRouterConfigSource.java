/*
 *  Copyright (C) 2011 John Casey.
 *  
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *  
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.apache.maven.repository.mirror.configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileMirrorRouterConfigSource
    implements MirrorRouterConfigSource
{

    private static final String AUTOMIRROR_CONFIG_FILENAME = "automirror.properties";

    private File configFile;

    public FileMirrorRouterConfigSource( final File src )
    {
        if ( src != null && src.isDirectory() )
        {
            configFile = new File( src, AUTOMIRROR_CONFIG_FILENAME );
        }
        else
        {
            configFile = src;
        }
    }

    public Object getSource()
    {
        return configFile;
    }

    public boolean canRead()
    {
        return configFile != null && configFile.exists() && configFile.canRead() && !configFile.isDirectory();
    }

    public InputStream getInputStream()
        throws IOException
    {
        return new FileInputStream( configFile );
    }

}
