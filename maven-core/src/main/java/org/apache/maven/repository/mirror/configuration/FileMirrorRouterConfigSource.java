package org.apache.maven.repository.mirror.configuration;

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
