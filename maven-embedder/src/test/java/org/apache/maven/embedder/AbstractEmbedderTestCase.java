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
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.Os;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineUtils;

public abstract class AbstractEmbedderTestCase
    extends PlexusTestCase
{
    protected MavenEmbedder maven;

    private List defaultPathList;

    /**
     * The file extensions used to resolve command names to executables. Each extension must have a leading period.
     */
    private List commandExtensions;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        Configuration configuration = new DefaultConfiguration().setClassLoader( classLoader ).setMavenEmbedderLogger( new MavenEmbedderConsoleLogger() );
        configuration.setUserSettingsFile( MavenEmbedder.DEFAULT_USER_SETTINGS_FILE );

        maven = new MavenEmbedder( configuration );
        
        // Some help with detecting executables on the command line. We want, in some cases, to use tools that are available on the command line
        // but if they are not present on the machine we don't want tests to fail. Case in point would be using SVN via the SCM plugin. We'll
        // run it if we can, pass through gracefully otherwise.

        Properties env = CommandLineUtils.getSystemEnvVars( !Os.isFamily( Os.FAMILY_WINDOWS ) );

        String defaultPath = env.getProperty( "PATH" );

        if ( defaultPath == null )
        {
            defaultPathList = Collections.EMPTY_LIST;
        }
        else
        {
            StringTokenizer tokenizer = new StringTokenizer( defaultPath, File.pathSeparator );

            defaultPathList = new LinkedList();

            while ( tokenizer.hasMoreElements() )
            {
                String element = (String) tokenizer.nextElement();

                defaultPathList.add( element );
            }
        }        

        String pathExt = env.getProperty( "PATHEXT" );

        if ( pathExt == null )
        {
            commandExtensions = Collections.EMPTY_LIST;
        }
        else
        {
            commandExtensions = Arrays.asList( pathExt.split( "\\" + File.pathSeparatorChar + "+" ) );
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

        for ( Iterator it = path.iterator(); it.hasNext(); )
        {
            String s = (String) it.next();

            for ( Iterator ite = commandExtensions.iterator(); ite.hasNext(); )
            {
                String ext = (String) ite.next();

                f = new File( s, executable + ext );

                if ( f.isFile() )
                {
                    return f;
                }
            }

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
