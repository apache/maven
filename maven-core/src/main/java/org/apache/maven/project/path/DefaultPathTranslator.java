package org.apache.maven.project.path;

/*
 * Copyright 2001-2004 The Apache Software Foundation.
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
import java.util.List;

public class DefaultPathTranslator
    implements PathTranslator
{
    private String FILE_SEPARATOR = "/";

    public void alignToBaseDirectory( Model model, File projectFile )
    {
        // build.directory
        // build.sourceDirectory
        // build.unitTestSourceDirectory
        // build.aspectSourceDirectory
        // build.resources.resource.directory
        // unitTest.resources.resource.directory

        // build.output
        // build.testOutput

        Build build = model.getBuild();

        if ( build != null )
        {
            String s = stripBasedirToken( build.getDirectory() );

            if ( requiresBaseDirectoryAlignment( s ) )
            {
                build.setDirectory( new File( projectFile.getParentFile(), s ).getPath() );
            }

            s = stripBasedirToken( build.getSourceDirectory() );

            if ( requiresBaseDirectoryAlignment( s ) )
            {
                build.setSourceDirectory( new File( projectFile.getParentFile(), s ).getPath() );
            }

            s = stripBasedirToken( build.getUnitTestSourceDirectory() );

            if ( requiresBaseDirectoryAlignment( s ) )
            {
                build.setUnitTestSourceDirectory( new File( projectFile.getParentFile(), s ).getPath() );
            }

            s = stripBasedirToken( build.getAspectSourceDirectory() );

            if ( requiresBaseDirectoryAlignment( s ) )
            {
                build.setAspectSourceDirectory( new File( projectFile.getParentFile(), s ).getPath() );
            }

            List buildResources = build.getResources();

            for ( Iterator i = buildResources.iterator(); i.hasNext(); )
            {
                Resource resource = (Resource) i.next();

                s = stripBasedirToken( resource.getDirectory() );

                if ( requiresBaseDirectoryAlignment( s ) )
                {
                    resource.setDirectory( new File( projectFile.getParentFile(), s ).getPath() );
                }
            }

            List unitTestResources = build.getResources();

            for ( Iterator i = unitTestResources.iterator(); i.hasNext(); )
            {
                Resource resource = (Resource) i.next();

                s = stripBasedirToken( resource.getDirectory() );

                if ( requiresBaseDirectoryAlignment( s ) )
                {
                    resource.setDirectory( new File( projectFile.getParentFile(), s ).getPath() );
                }
            }

            s = stripBasedirToken( build.getOutput() );

            if ( requiresBaseDirectoryAlignment( s ) )
            {
                build.setOutput( new File( projectFile.getParentFile(), s ).getPath() );
            }

            s = stripBasedirToken( build.getTestOutput() );

            if ( requiresBaseDirectoryAlignment( s ) )
            {
                build.setTestOutput( new File( projectFile.getParentFile(), s ).getPath() );
            }
        }
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

