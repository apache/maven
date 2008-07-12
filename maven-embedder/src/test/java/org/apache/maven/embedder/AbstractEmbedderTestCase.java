package org.apache.maven.embedder;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

import java.io.File;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineUtils;

public abstract class AbstractEmbedderTestCase
    extends PlexusTestCase
{
    protected MavenEmbedder maven;

    private String defaultPath;

    private List defaultPathList;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        Configuration configuration = new DefaultConfiguration().setClassLoader( classLoader ).setMavenEmbedderLogger( new MavenEmbedderConsoleLogger() );

        maven = new MavenEmbedder( configuration );
        
        // Some help with detecting executables on the command line. We want, in some cases, to use tools that are available on the command line
        // but if they are not present on the machine we don't want tests to fail. Case in point would be using SVN via the SCM plugin. We'll
        // run it if we can, pass through gracefully otherwise.

        defaultPath = CommandLineUtils.getSystemEnvVars().getProperty( "PATH" );

        if ( defaultPath == null )
        {
            defaultPathList = Collections.EMPTY_LIST;
        }
        else
        {
            String separator = System.getProperty( "path.separator" );

            StringTokenizer tokenizer = new StringTokenizer( defaultPath, separator );

            defaultPathList = new LinkedList();

            while ( tokenizer.hasMoreElements() )
            {
                String element = (String) tokenizer.nextElement();

                defaultPathList.add( element );
            }
        }        
    }

    protected void tearDown()
        throws Exception
    {
        maven.stop();
    }

    // ----------------------------------------------------------------------
    // ExecutableResolver Implementation
    // ----------------------------------------------------------------------

    public List getDefaultPath()
    {
        return defaultPathList;
    }

    public File findExecutable( String executable )
    {
        return findExecutable( executable, getDefaultPath() );
    }

    public File findExecutable( String executable, List path )
    {
        if ( StringUtils.isEmpty( executable ) )
        {
            throw new NullPointerException( "executable cannot be null" );
        }

        if ( path == null )
        {
            throw new NullPointerException( "path cannot be null" );
        }

        File f = new File( executable );

        if ( f.isAbsolute() && f.isFile() )
        {
            return f;
        }

        if ( path == null )
        {
            return null;
        }

        // TODO: Need to resolve it with defaults extension of system
        // ie. if executable is 'mvn', we must search 'mvn.bat'
        for ( Iterator it = path.iterator(); it.hasNext(); )
        {
            String s = (String) it.next();

            f = new File( s, executable );

            if ( f.isFile() )
            {
                return f;
            }
        }

        return null;
    }

    public boolean hasExecutable( String executable )
    {
        return hasExecutable( executable, getDefaultPath() );
    }

    public boolean hasExecutable( String executable, List path )
    {
        return findExecutable( executable, path ) != null;
    }
}
