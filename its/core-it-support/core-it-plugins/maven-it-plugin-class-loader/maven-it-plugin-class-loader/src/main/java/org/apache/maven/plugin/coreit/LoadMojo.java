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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 * Loads classes and/or resources from the plugin class path and records the results in a properties file.
 * 
 * @goal load
 * @phase initialize
 * 
 * @author Benjamin Bentmann
 * @version $Id$
 */
public class LoadMojo
    extends AbstractMojo
{

    /**
     * The path to the properties file used to track the results of the class/resource loading via the plugin class
     * loader.
     * 
     * @parameter expression="${clsldr.pluginClassLoaderOutput}"
     */
    private File pluginClassLoaderOutput;

    /**
     * The path to the properties file used to track the results of the class/resource loading via the thread's context
     * class loader.
     * 
     * @parameter expression="${clsldr.contextClassLoaderOutput}"
     */
    private File contextClassLoaderOutput;

    /**
     * The comma separated set of classes to load. For each specified qualified class name <code>QCN</code> that was
     * successfully loaded, the generated properties files will contain a key named <code>QCN</code>. The value of this
     * key will be the hash code of the requested class. In addition, a key named <code>QCN.methods</code> holds the
     * comma separated list of all public methods declared directly in that class, in alphabetic order and possibly with
     * duplicates to account for overloaded methods.
     * 
     * @parameter expression="${clsldr.classNames}"
     */
    private String classNames;

    /**
     * The comma separated set of resources to load. For each specified absolute resource path <code>ARP</code> that was
     * successfully loaded, the generated properties files will contain a key named <code>ARP</code> whose value gives
     * the URL to the resource. In addition, the keys <code>ARP.count</code>, <code>ARP.0</code>, <code>ARP.1</code>
     * etc. will enumerate all URLs matching the resource name.
     * 
     * @parameter expression="${clsldr.resourcePaths}"
     */
    private String resourcePaths;

    /**
     * Runs this mojo.
     * 
     * @throws MojoExecutionException If the output file could not be created.
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( pluginClassLoaderOutput != null )
        {
            execute( pluginClassLoaderOutput, getClass().getClassLoader() );
        }
        if ( contextClassLoaderOutput != null )
        {
            execute( contextClassLoaderOutput, Thread.currentThread().getContextClassLoader() );
        }
    }

    /**
     * Loads the classes/resources.
     * 
     * @param outputFile The path to the properties file to generate, must not be <code>null</code>.
     * @param classLoader The class loader to use, must not be <code>null</code>.
     * @throws MojoExecutionException If the output file could not be created.
     */
    private void execute( File outputFile, ClassLoader classLoader )
        throws MojoExecutionException
    {
        getLog().info( "[MAVEN-CORE-IT-LOG] Using class loader " + classLoader );

        Properties loaderProperties = new Properties();

        if ( classNames != null && classNames.length() > 0 )
        {
            String[] names = classNames.split( "," );
            for ( int i = 0; i < names.length; i++ )
            {
                String name = names[i];
                getLog().info( "[MAVEN-CORE-IT-LOG] Loading class " + name );

                // test ClassLoader.loadClass()
                try
                {
                    Class type = classLoader.loadClass( name );
                    loaderProperties.setProperty( name, "" + type.hashCode() );

                    Method[] methods = type.getDeclaredMethods();
                    List methodNames = new ArrayList();
                    for ( int j = 0; j < methods.length; j++ )
                    {
                        if ( Modifier.isPublic( methods[j].getModifiers() ) )
                        {
                            methodNames.add( methods[j].getName() );
                        }
                    }
                    Collections.sort( methodNames );
                    StringBuffer buffer = new StringBuffer( 1024 );
                    for ( Iterator it = methodNames.iterator(); it.hasNext(); )
                    {
                        if ( buffer.length() > 0 )
                        {
                            buffer.append( ',' );
                        }
                        buffer.append( it.next() );
                    }

                    loaderProperties.setProperty( name + ".methods", buffer.toString() );
                }
                catch ( ClassNotFoundException e )
                {
                    // ignore, will be reported by means of missing keys in the properties file
                }
            }
        }

        if ( resourcePaths != null && resourcePaths.length() > 0 )
        {
            String[] paths = resourcePaths.split( "," );
            for ( int i = 0; i < paths.length; i++ )
            {
                String path = paths[i];
                getLog().info( "[MAVEN-CORE-IT-LOG] Loading resource " + path );

                // test ClassLoader.getResource()
                URL url = classLoader.getResource( path );
                if ( url != null )
                {
                    loaderProperties.setProperty( path, url.toString() );
                }

                // test ClassLoader.getResources()
                try
                {
                    List urls = Collections.list( classLoader.getResources( path ) );
                    loaderProperties.setProperty( path + ".count", "" + urls.size() );
                    for ( int j = 0; j < urls.size(); j++ )
                    {
                        loaderProperties.setProperty( path + "." + j, urls.get( j ).toString() );
                    }
                }
                catch ( IOException e )
                {
                    throw new MojoExecutionException( "Resources could not be enumerated: " + path, e );
                }
            }
        }

        getLog().info( "[MAVEN-CORE-IT-LOG] Creating output file " + outputFile );

        OutputStream out = null;
        try
        {
            outputFile.getParentFile().mkdirs();
            out = new FileOutputStream( outputFile );
            loaderProperties.store( out, "MAVEN-CORE-IT-LOG" );
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
