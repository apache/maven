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
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.repository.Repository;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

/**
 * Loads resources from a class loader used to load a wagon provider. The wagon is merely used to access the extension
 * class loader it came from which is otherwise not accessible to a plugin.
 *
 * @author Benjamin Bentmann
 *
 */
@Mojo( name = "lookup-wagon", defaultPhase = LifecyclePhase.VALIDATE )
public class LookupWagonMojo
    extends AbstractMojo
{

    /**
     * The Wagon manager used to retrieve wagon providers.
     */
    @Component
    private WagonManager wagonManager;

    /**
     * The path to the properties file used to track the results of the wagon lookups.
     */
    @Parameter( property = "wagon.outputFile" )
    private File outputFile;

    /**
     * The URLs for which to look up wagons.
     */
    @Parameter
    private String[] urls;

    /**
     * Runs this mojo.
     *
     * @throws MojoFailureException If the attached file has not been set.
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        Properties loaderProperties = new Properties();

        if ( urls != null )
        {
            for ( int i = 0; i < urls.length; i++ )
            {
                String url = urls[i];
                getLog().info( "[MAVEN-CORE-IT-LOG] Looking up wagon for URL " + url );

                try
                {
                    Repository repo = new Repository( "repo-" + i, url );
                    Wagon wagon = wagonManager.getWagon( repo );
                    getLog().info( "[MAVEN-CORE-IT-LOG]   " + wagon );

                    loaderProperties.setProperty( url + ".hash", Integer.toString( System.identityHashCode( wagon ) ) );
                    loaderProperties.setProperty( url + ".class", wagon.getClass().getName() );
                }
                catch ( Exception e )
                {
                    getLog().warn( "[MAVEN-CORE-IT-LOG] Failed to look up wagon for URL " + url, e );
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
