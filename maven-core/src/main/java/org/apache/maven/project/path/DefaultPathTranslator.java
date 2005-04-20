package org.apache.maven.project.path;

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

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Resource;

import java.io.File;
import java.util.Iterator;

public class DefaultPathTranslator
    implements PathTranslator
{
    private String FILE_SEPARATOR = "/";

    public void alignToBaseDirectory( Model model, File projectFile )
    {
        Build build = model.getBuild();

        File basedir = projectFile.getParentFile();

        if ( build != null )
        {
            build.setDirectory( alignToBaseDirectory( build.getDirectory(), basedir ) );

            build.setSourceDirectory( alignToBaseDirectory( build.getSourceDirectory(), basedir ) );

            build.setTestSourceDirectory( alignToBaseDirectory( build.getTestSourceDirectory(), basedir ) );

            for ( Iterator i = build.getResources().iterator(); i.hasNext(); )
            {
                Resource resource = (Resource) i.next();

                resource.setDirectory( alignToBaseDirectory( resource.getDirectory(), basedir ) );
            }

            for ( Iterator i = build.getTestResources().iterator(); i.hasNext(); )
            {
                Resource resource = (Resource) i.next();

                resource.setDirectory( alignToBaseDirectory( resource.getDirectory(), basedir ) );
            }

            build.setOutputDirectory( alignToBaseDirectory( build.getOutputDirectory(), basedir ) );

            build.setTestOutputDirectory( alignToBaseDirectory( build.getTestOutputDirectory(), basedir ) );
        }
    }

    public String alignToBaseDirectory( String path, File basedir )
    {
        String s = stripBasedirToken( path );

        if ( requiresBaseDirectoryAlignment( s ) )
        {
            s = new File( basedir, s ).getPath();
        }

        return s;
    }

    private String stripBasedirToken( String s )
    {
        if ( s != null )
        {
            s = s.trim();

            if ( s.startsWith( "${basedir}" ) )
            {
                // Take out ${basedir} and the leading slash
                s = s.substring( 11 );
            }
        }

        return s;
    }

    private boolean requiresBaseDirectoryAlignment( String s )
    {
        if ( s != null )
        {
            File f = new File( s );

            if ( s.startsWith( FILE_SEPARATOR ) || f.isAbsolute() )
            {
                return false;
            }
            else
            {
                return true;
            }
        }

        return false;
    }
}

