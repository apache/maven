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
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

/**
 * Dumps the version info for a wagon provider to a properties file.
 *
 * @author Benjamin Bentmann
 */
@Mojo( name = "dump-version", defaultPhase = LifecyclePhase.VALIDATE )
public class DumpVersionMojo
    extends AbstractMojo
{

    /**
     * Project base directory used for manual path alignment.
     */
    @Parameter( defaultValue = "${basedir}", readonly = true )
    private File basedir;

    /**
     * The Wagon manager used to look up the wagon of interest.
     */
    @Component
    private WagonManager wagonManager;

    /**
     * The path to the properties file used to dump the auth infos.
     */
    @Parameter( property = "wagon.propertiesFile" )
    private File propertiesFile;

    /**
     * The role hint for the provider of interest.
     */
    @Parameter( property = "wagon.providerHint" )
    private String providerHint;

    /**
     * The group id for the provider of interest.
     */
    @Parameter( property = "wagon.providerGroupId" )
    private String providerGroupId;

    /**
     * The artifact id for the provider of interest.
     */
    @Parameter( property = "wagon.providerArtifactId" )
    private String providerArtifactId;

    /**
     * Runs this mojo.
     *
     * @throws MojoExecutionException If the output file could not be created.
     */
    public void execute()
        throws MojoExecutionException
    {
        Properties wagonProperties = new Properties();

        Object wagon;
        try
        {
            wagon = wagonManager.getWagon( providerHint );

            String resource = "/META-INF/maven/" + providerGroupId + "/" + providerArtifactId + "/pom.properties";
            InputStream is = wagon.getClass().getResourceAsStream( resource );
            wagonProperties.load( is );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Wagon properties could not be read: " + e.getMessage(), e );
        }
        catch ( Exception e )
        {
            getLog().info( "[MAVEN-CORE-IT-LOG] No wagon available for " + providerHint );
            wagonProperties.setProperty( "missing", "true" );
        }

        if ( !propertiesFile.isAbsolute() )
        {
            propertiesFile = new File( basedir, propertiesFile.getPath() );
        }

        getLog().info( "[MAVEN-CORE-IT-LOG] Creating output file " + propertiesFile );

        OutputStream out = null;
        try
        {
            propertiesFile.getParentFile().mkdirs();
            out = new FileOutputStream( propertiesFile );
            wagonProperties.store( out, "MAVEN-CORE-IT-LOG" );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Output file could not be created: " + propertiesFile, e );
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

        getLog().info( "[MAVEN-CORE-IT-LOG] Created output file " + propertiesFile );
    }

}
