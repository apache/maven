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

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.Properties;

/**
 * Builds the local POMs.
 *
 * @author Benjamin Bentmann
 * @goal local-pom
 */
public class BuildLocalPomMojo
    extends AbstractPomMojo
{

    /**
     * The properties file to dump the POM info to.
     *
     * @parameter default-value="target/pom.properties"
     */
    private File propertiesFile;

    /**
     * The local repository.
     *
     * @parameter default-value="${localRepository}"
     * @readonly
     * @required
     */
    private ArtifactRepository localRepository;

    /**
     * The POM files to build.
     *
     * @parameter
     */
    private File[] files;

    /**
     * Runs this mojo.
     *
     * @throws MojoExecutionException If the artifact file has not been set.
     */
    public void execute()
        throws MojoExecutionException
    {
        Properties props = new Properties();

        getLog().info( "[MAVEN-CORE-IT-LOG] Building local POMs" );

        if ( files != null )
        {
            for ( File file : files )
            {
                getLog().info( "[MAVEN-CORE-IT-LOG] Building " + file );

                try
                {
                    MavenProject project = builder.build( file, localRepository, null );

                    dump( props, file.getName() + ".", project );
                }
                catch ( Exception e )
                {
                    getLog().warn( "Failed to build local POM for " + file, e );
                }
            }
        }

        store( props, propertiesFile );
    }

}
