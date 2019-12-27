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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import java.io.File;

/**
 * Sets the main artifact's file. This is the essence of the Maven JAR Plugin and all the other packaging plugins.
 * Creating the actual file for the main artifact is a specific plugin job and not related to the Maven core.
 * 
 * @goal set
 * @phase package
 * 
 * @author Benjamin Bentmann
 *
 */
public class SetMojo
    extends AbstractMojo
{

    /**
     * The current Maven project.
     * 
     * @parameter default-value="${project}"
     * @readonly
     * @required
     */
    private MavenProject project;

    /**
     * The path to the file to set as the main artifact, relative to the project base directory. The plugin will not
     * validate this path.
     * 
     * @parameter property="artifact.mainFile"
     * @required
     */
    private String mainFile;

    /**
     * Runs this mojo.
     * 
     * @throws MojoFailureException If the artifact file has not been set.
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        getLog().info( "[MAVEN-CORE-IT-LOG] Setting main artifact file: " + mainFile );

        if ( mainFile == null || mainFile.length() <= 0 )
        {
            throw new MojoFailureException( "Path name for main artifact file has not been specified" );
        }

        /*
         * NOTE: We do not want to test path translation here, so resolve relative paths manually.
         */
        File artifactFile = new File( mainFile );
        if ( !artifactFile.isAbsolute() )
        {
            artifactFile = new File( project.getBasedir(), mainFile );
        }

        if ( !artifactFile.exists() )
        {
            getLog().warn( "[MAVEN-CORE-IT-LOG] Main artifact file does not exist: " + artifactFile );
        }

        Artifact artifact = project.getArtifact();
        artifact.setFile( artifactFile );

        getLog().info( "[MAVEN-CORE-IT-LOG] Set main artifact file: " + artifactFile );
    }

}
