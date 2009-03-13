package org.apache.maven.project.path;

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

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Reporting;
import org.apache.maven.model.Resource;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Default implementation of {@link PathTranslator}.
 */
public class DefaultPathTranslator
    implements PathTranslator
{
    private static final String[] BASEDIR_EXPRESSIONS = {
        "${basedir}",
        "${pom.basedir}",
        "${project.basedir}"
    };

    public void alignToBaseDirectory( Model model, File basedir )
    {
        if ( basedir == null )
        {
            return;
        }

        Build build = model.getBuild();

        if ( build != null )
        {
            build.setDirectory( alignToBaseDirectory( build.getDirectory(), basedir ) );

            build.setSourceDirectory( alignToBaseDirectory( build.getSourceDirectory(), basedir ) );

            build.setTestSourceDirectory( alignToBaseDirectory( build.getTestSourceDirectory(), basedir ) );

            // TODO: MNG-3731
//            build.setScriptSourceDirectory( alignToBaseDirectory( build.getScriptSourceDirectory(), basedir ) );

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

            if ( build.getFilters() != null )
            {
                List filters = new ArrayList();
                for ( Iterator i = build.getFilters().iterator(); i.hasNext(); )
                {
                    String filter = (String) i.next();

                    filters.add( alignToBaseDirectory( filter, basedir ) );
                }
                build.setFilters( filters );
            }

            build.setOutputDirectory( alignToBaseDirectory( build.getOutputDirectory(), basedir ) );

            build.setTestOutputDirectory( alignToBaseDirectory( build.getTestOutputDirectory(), basedir ) );
        }

        Reporting reporting = model.getReporting();

        if ( reporting != null )
        {
            reporting.setOutputDirectory( alignToBaseDirectory( reporting.getOutputDirectory(), basedir ) );
        }
    }

    /**
     * {@inheritDoc}
     */
    public String alignToBaseDirectory( String path, File basedir )
    {
        if ( basedir == null )
        {
            return path;
        }

        if ( path == null )
        {
            return null;
        }

        String s = stripBasedirToken( path );

        File file = new File( s );
        if ( file.isAbsolute() )
        {
            // path was already absolute, just normalize file separator and we're done
            s = file.getPath();
        }
        else if ( file.getPath().startsWith( File.separator ) )
        {
            // drive-relative Windows path, don't align with project directory but with drive root
            s = file.getAbsolutePath();
        }
        else
        {
            // an ordinary relative path, align with project directory
            s = new File( new File( basedir, s ).toURI().normalize() ).getAbsolutePath();
        }

        return s;
    }

    private String stripBasedirToken( String s )
    {
        if ( s != null )
        {
            String basedirExpr = null;
            for ( int i = 0; i < BASEDIR_EXPRESSIONS.length; i++ )
            {
                basedirExpr = BASEDIR_EXPRESSIONS[i];
                if ( s.startsWith( basedirExpr ) )
                {
                    break;
                }
                else
                {
                    basedirExpr = null;
                }
            }

            if ( basedirExpr != null )
            {
                if ( s.length() > basedirExpr.length() )
                {
                    // Take out basedir expression and the leading slash
                    s = chopLeadingFileSeparator( s.substring( basedirExpr.length() ) );
                }
                else
                {
                    s = ".";
                }
            }
        }

        return s;
    }

    /**
     * Removes the leading directory separator from the specified filesystem path (if any). For platform-independent
     * behavior, this method accepts both the forward slash and the backward slash as separator.
     *
     * @param path The filesystem path, may be <code>null</code>.
     * @return The altered filesystem path or <code>null</code> if the input path was <code>null</code>.
     */
    private String chopLeadingFileSeparator( String path )
    {
        if ( path != null )
        {
            if ( path.startsWith( "/" ) || path.startsWith( "\\" ) )
            {
                path = path.substring( 1 );
            }
        }
        return path;
    }

    /**
     * {@inheritDoc}
     */
    public void unalignFromBaseDirectory( Model model, File basedir )
    {
        if ( basedir == null )
        {
            return;
        }

        Build build = model.getBuild();

        if ( build != null )
        {
            build.setDirectory( unalignFromBaseDirectory( build.getDirectory(), basedir ) );

            build.setSourceDirectory( unalignFromBaseDirectory( build.getSourceDirectory(), basedir ) );

            build.setTestSourceDirectory( unalignFromBaseDirectory( build.getTestSourceDirectory(), basedir ) );

            build.setScriptSourceDirectory( unalignFromBaseDirectory( build.getScriptSourceDirectory(), basedir ) );

            for ( Iterator i = build.getResources().iterator(); i.hasNext(); )
            {
                Resource resource = (Resource) i.next();

                resource.setDirectory( unalignFromBaseDirectory( resource.getDirectory(), basedir ) );
            }

            for ( Iterator i = build.getTestResources().iterator(); i.hasNext(); )
            {
                Resource resource = (Resource) i.next();

                resource.setDirectory( unalignFromBaseDirectory( resource.getDirectory(), basedir ) );
            }

            if ( build.getFilters() != null )
            {
                List filters = new ArrayList();
                for ( Iterator i = build.getFilters().iterator(); i.hasNext(); )
                {
                    String filter = (String) i.next();

                    filters.add( unalignFromBaseDirectory( filter, basedir ) );
                }
                build.setFilters( filters );
            }

            build.setOutputDirectory( unalignFromBaseDirectory( build.getOutputDirectory(), basedir ) );

            build.setTestOutputDirectory( unalignFromBaseDirectory( build.getTestOutputDirectory(), basedir ) );
        }

        Reporting reporting = model.getReporting();

        if ( reporting != null )
        {
            reporting.setOutputDirectory( unalignFromBaseDirectory( reporting.getOutputDirectory(), basedir ) );
        }
    }

    /**
     * {@inheritDoc}
     */
    public String unalignFromBaseDirectory( String path, File basedir )
    {
        if ( basedir == null )
        {
            return path;
        }

        if ( path == null )
        {
            return null;
        }

        path = path.trim();

        String base = basedir.getAbsolutePath();
        if ( path.startsWith( base ) )
        {
            path = chopLeadingFileSeparator( path.substring( base.length() ) );
        }

        if ( !new File( path ).isAbsolute() )
        {
            path = path.replace( '\\', '/' );
        }

        return path;
    }

}

