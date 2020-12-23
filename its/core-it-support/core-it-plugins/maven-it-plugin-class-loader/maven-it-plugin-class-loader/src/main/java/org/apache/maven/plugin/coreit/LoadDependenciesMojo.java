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

import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

/**
 * Loads classes and/or resources from a custom class loader that holds the project dependencies and records the results
 * in a properties file.
 *
 * @goal load-dependencies
 * @phase initialize
 * @requiresDependencyResolution compile
 *
 * @author Benjamin Bentmann
 *
 */
public class LoadDependenciesMojo
    extends AbstractLoadMojo
{

    /**
     * The project's class path to load classes/resources from.
     *
     * @parameter default-value="${project.compileClasspathElements}"
     * @readonly
     */
    private List classPath;

    /**
     * The path to the properties file used to track the results of the class/resource loading via the project class
     * loader.
     *
     * @parameter property="clsldr.projectClassLoaderOutput"
     */
    private File projectClassLoaderOutput;

    /**
     * Runs this mojo.
     *
     * @throws MojoExecutionException If the output file could not be created.
     */
    public void execute()
        throws MojoExecutionException
    {
        URL[] urls = new URL[classPath.size()];
        for ( int i = 0; i < urls.length; i++ )
        {
            try
            {
                urls[i] = new File( (String) classPath.get( i ) ).toURI().toURL();
                getLog().info( "[MAVEN-CORE-IT-LOG] Using " + urls[i] );
            }
            catch ( MalformedURLException e )
            {
                getLog().error( "[MAVEN-CORE-IT-LOG] Failed to convert to URL " + classPath.get( i ), e );
            }
        }

        ClassLoader projectClassLoader = new URLClassLoader( urls, getClass().getClassLoader() );

        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try
        {
            Thread.currentThread().setContextClassLoader( projectClassLoader );

            execute( projectClassLoaderOutput, projectClassLoader );
        }
        finally
        {
            Thread.currentThread().setContextClassLoader( contextClassLoader );
        }
    }

}
