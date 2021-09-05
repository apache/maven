package org.apache.maven.plugin.coreit;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

/**
 * Checks the thread-safe retrieval of components from active component collections.
 *
 * @author Benjamin Bentmann
 * @goal check-thread-safety
 * @phase validate
 */
public class CheckThreadSafetyMojo
    extends AbstractMojo
{

    /**
     * Project base directory used for manual path alignment.
     *
     * @parameter default-value="${basedir}"
     * @readonly
     */
    private File basedir;

    /**
     * The available components, as a map.
     *
     * @component role="org.apache.maven.plugin.coreit.Component"
     */
    private Map componentMap;

    /**
     * The available components, as a list.
     *
     * @component role="org.apache.maven.plugin.coreit.Component"
     */
    private List componentList;

    /**
     * The path to the properties file to create.
     *
     * @parameter property="collections.outputFile"
     */
    private File outputFile;

    /**
     * Runs this mojo.
     *
     * @throws MojoFailureException If the output file could not be created.
     */
    public void execute()
        throws MojoExecutionException
    {
        Properties componentProperties = new Properties();

        getLog().info( "[MAVEN-CORE-IT-LOG] Testing concurrent component access" );

        ClassLoader pluginRealm = getClass().getClassLoader();
        ClassLoader coreRealm = MojoExecutionException.class.getClassLoader();

        final Map map = componentMap;
        final List list = componentList;
        final List go = new Vector();
        final List exceptions = new Vector();

        Thread[] threads = new Thread[2];
        for ( int i = 0; i < threads.length; i++ )
        {
            // NOTE: The threads need to use different realms to trigger changes of the collections
            final ClassLoader cl = ( i % 2 ) == 0 ? pluginRealm : coreRealm;
            threads[i] = new Thread()
            {
                private final ClassLoader tccl = cl;

                public void run()
                {
                    getLog().info( "[MAVEN-CORE-IT-LOG] Thread " + this + " uses " + tccl );
                    Thread.currentThread().setContextClassLoader( tccl );
                    while ( go.isEmpty() )
                    {
                        // wait for start
                    }
                    for ( int j = 0; j < 10 * 1000; j++ )
                    {
                        try
                        {
                            for ( Object o : map.values() )
                            {
                                o.toString();
                            }
                            for ( Object aList : list )
                            {
                                aList.toString();
                            }
                        }
                        catch ( Exception e )
                        {
                            getLog().warn( "[MAVEN-CORE-IT-LOG] Thread " + this + " encountered concurrency issue", e );
                            exceptions.add( e );
                        }
                    }
                }
            };
            threads[i].start();
        }

        go.add( null );
        for ( Thread thread : threads )
        {
            try
            {
                thread.join();
            }
            catch ( InterruptedException e )
            {
                getLog().warn( "[MAVEN-CORE-IT-LOG] Interrupted while joining " + thread );
            }
        }

        componentProperties.setProperty( "components", Integer.toString( componentList.size() ) );
        componentProperties.setProperty( "exceptions", Integer.toString( exceptions.size() ) );

        if ( !outputFile.isAbsolute() )
        {
            outputFile = new File( basedir, outputFile.getPath() );
        }

        getLog().info( "[MAVEN-CORE-IT-LOG] Creating output file " + outputFile );

        OutputStream out = null;
        try
        {
            outputFile.getParentFile().mkdirs();
            out = new FileOutputStream( outputFile );
            componentProperties.store( out, "MAVEN-CORE-IT-LOG" );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Output file could not be created: " + outputFile, e );
        }
        finally
        {
            if ( out != null )
            {
                try
                {
                    out.close();
                }
                catch ( IOException e )
                {
                    // just ignore
                }
            }
        }

        getLog().info( "[MAVEN-CORE-IT-LOG] Created output file " + outputFile );
    }

}
