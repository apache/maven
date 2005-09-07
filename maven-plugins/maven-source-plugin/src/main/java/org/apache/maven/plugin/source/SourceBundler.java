package org.apache.maven.plugin.source;

/*
 * Copyright 2005 The Apache Software Foundation.
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

import org.codehaus.plexus.archiver.Archiver;

import java.io.File;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @version $Id$
 */
public class SourceBundler
{
    private final static String[] DEFAULT_INCLUDES = new String[]{"**/*",};

    private final static String[] DEFAULT_EXCLUDES = new String[]{"**/CVS/**", "**/.svn/**",};

    public void makeSourceBundle( File outputFile, File[] sourceDirectories, Archiver archiver )
        throws Exception
    {
        String[] includes = DEFAULT_INCLUDES;

        String[] excludes = DEFAULT_EXCLUDES;

        for ( int i = 0; i < sourceDirectories.length; i++ )
        {
            if ( sourceDirectories[i].exists() )
            {
                archiver.addDirectory( sourceDirectories[i], includes, excludes );
            }
        }

        archiver.setDestFile( outputFile );

        archiver.createArchive();
    }
}
