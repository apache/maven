package org.apache.maven.bootstrap.compile;

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

import org.apache.maven.bootstrap.util.DirectoryScanner;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl </a>
 * @author <a href="mailto:michal.maczka@dimatics.com">Michal Maczka </a>
 * @version $Id$
 */
public abstract class AbstractCompiler
    implements Compiler
{
    private static String PS = System.getProperty( "path.separator" );

    public String getPathString( List pathElements )
        throws Exception
    {
        StringBuffer sb = new StringBuffer();

        for ( Iterator it = pathElements.iterator(); it.hasNext(); )
        {
            String element = (String) it.next();

            sb.append( element ).append( PS );
        }

        return sb.toString();
    }

    protected String[] getSourceFiles( CompilerConfiguration config )
    {
        Set sources = new HashSet();

        Set sourceFiles = config.getSourceFiles();
        if ( sourceFiles != null && !sourceFiles.isEmpty() )
        {
            for ( Iterator it = sourceFiles.iterator(); it.hasNext(); )
            {
                File sourceFile = (File) it.next();
                sources.add( sourceFile.getAbsolutePath() );
            }
        }
        else
        {
            for ( Iterator it = config.getSourceLocations().iterator(); it.hasNext(); )
            {
                String sourceLocation = (String) it.next();

                DirectoryScanner scanner = new DirectoryScanner();

                scanner.setBasedir( sourceLocation );

                Set includes = config.getIncludes();
                if ( includes != null && !includes.isEmpty() )
                {
                    String[] inclStrs = (String[]) includes.toArray( new String[includes.size()] );
                    scanner.setIncludes( inclStrs );
                }
                else
                {
                    scanner.setIncludes( new String[]{"**/*.java"} );
                }

                Set excludes = config.getExcludes();
                if ( excludes != null && !excludes.isEmpty() )
                {
                    String[] exclStrs = (String[]) excludes.toArray( new String[excludes.size()] );
                    scanner.setIncludes( exclStrs );
                }

                scanner.scan();

                String[] sourceDirectorySources = scanner.getIncludedFiles();

                for ( int j = 0; j < sourceDirectorySources.length; j++ )
                {
                    File f = new File( sourceLocation, sourceDirectorySources[j] );

                    sources.add( f.getPath() );
                }
            }
        }

        String[] result = null;

        if ( sources.isEmpty() )
        {
            result = new String[0];
        }
        else
        {
            result = (String[]) sources.toArray( new String[sources.size()] );
        }

        return result;
    }

    protected String makeClassName( String fileName, String sourceDir )
        throws IOException
    {
        File origFile = new File( fileName );
        String canonical = null;

        if ( origFile.exists() )
        {
            canonical = origFile.getCanonicalPath().replace( '\\', '/' );
        }

        String str = fileName;
        str = str.replace( '\\', '/' );

        if ( sourceDir != null )
        {
            String prefix = new File( sourceDir ).getCanonicalPath().replace( '\\', '/' );

            if ( canonical != null )
            {
                if ( canonical.startsWith( prefix ) )
                {
                    String result = canonical.substring( prefix.length() + 1, canonical.length() - 5 );

                    result = result.replace( '/', '.' );

                    return result;
                }
            }
            else
            {
                File t = new File( sourceDir, fileName );

                if ( t.exists() )
                {
                    str = t.getCanonicalPath().replace( '\\', '/' );

                    String result = str.substring( prefix.length() + 1, str.length() - 5 ).replace( '/', '.' );

                    return result;
                }
            }
        }

        if ( fileName.endsWith( ".java" ) )
        {
            fileName = fileName.substring( 0, fileName.length() - 5 );
        }

        fileName = fileName.replace( '\\', '.' );

        return fileName.replace( '/', '.' );
    }

    protected String[] toStringArray( List arguments )
    {
        String[] args = new String[arguments.size()];

        for ( int i = 0; i < arguments.size(); i++ )
        {
            args[i] = (String) arguments.get( i );
        }

        return args;
    }
}