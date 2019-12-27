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

import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.wagon.repository.Repository;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * Loads resources from a class loader used to load a wagon provider. The wagon is merely used to access the extension
 * class loader it came from which is otherwise not accessible to a plugin.
 *
 * @author Benjamin Bentmann
 *
 * @goal load-resource
 * @phase validate
 */
public class LoadResourceMojo
    extends AbstractMojo
{

    /**
     * The Wagon manager used to retrieve wagon providers.
     *
     * @component
     */
    private WagonManager wagonManager;

    /**
     * The path to the properties file used to track the results of the resource loading via the wagon's class loader.
     *
     * @parameter property="wagon.wagonClassLoaderOutput"
     */
    private File wagonClassLoaderOutput;

    /**
     * The role hint for the wagon provider to load. The class loader of this provider will be used to load the
     * resources.
     *
     * @parameter property="wagon.wagonProtocol"
     */
    private String wagonProtocol;

    /**
     * The repository to load the wagon for, if applicable.
     *
     * @parameter property="wagon.repositoryId"
     */
    private String repositoryId;

    /**
     * The set of resources to load. For each specified absolute resource path <code>ARP</code> that was successfully
     * loaded, the generated properties files will contain a key named <code>ARP</code> whose value gives the URL to the
     * resource. In addition, the keys <code>ARP.count</code>, <code>ARP.0</code>, <code>ARP.1</code> etc. will
     * enumerate all URLs matching the resource name.
     *
     * @parameter
     */
    private String[] resourcePaths;

    /**
     * Runs this mojo.
     *
     * @throws MojoFailureException If the attached file has not been set.
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        getLog().info( "[MAVEN-CORE-IT-LOG] Looking up wagon for protocol " + wagonProtocol );

        Object wagon;
        try
        {
            if ( repositoryId != null )
            {
                wagon = wagonManager.getWagon( new Repository( repositoryId, wagonProtocol + "://host/path" ) );
            }
            else
            {
                wagon = wagonManager.getWagon( wagonProtocol );
            }
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Failed to load wagon for protocol " + wagonProtocol, e );
        }

        ClassLoader classLoader = wagon.getClass().getClassLoader();

        getLog().info( "[MAVEN-CORE-IT-LOG] Using class loader " + classLoader );

        Properties loaderProperties = new Properties();
        loaderProperties.setProperty( "wagon.class", wagon.getClass().getName() );

        if ( resourcePaths != null )
        {
            for ( String path : resourcePaths )
            {
                getLog().info( "[MAVEN-CORE-IT-LOG] Loading resource " + path );

                URL url = classLoader.getResource( path );
                getLog().info( "[MAVEN-CORE-IT-LOG]   Loaded resource from " + url );
                if ( url != null )
                {
                    loaderProperties.setProperty( path, url.toString() );
                }

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

        getLog().info( "[MAVEN-CORE-IT-LOG] Creating output file " + wagonClassLoaderOutput );

        OutputStream out = null;
        try
        {
            wagonClassLoaderOutput.getParentFile().mkdirs();
            out = new FileOutputStream( wagonClassLoaderOutput );
            loaderProperties.store( out, "MAVEN-CORE-IT-LOG" );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Output file could not be created: " + wagonClassLoaderOutput, e );
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

        getLog().info( "[MAVEN-CORE-IT-LOG] Created output file " + wagonClassLoaderOutput );
    }

}
