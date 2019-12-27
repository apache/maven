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
import org.apache.maven.project.artifact.ProjectArtifactMetadata;

import java.io.File;

/**
 * Attaches a POM to the main artifact.
 * 
 * @goal attach-pom
 * @phase package
 * 
 * @author Benjamin Bentmann
 *
 */
public class AttachPomMojo
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
     * The path to the POM file to attach to the main artifact, relative to the project base directory. The plugin will
     * not validate this path.
     * 
     * @parameter property="artifact.pomFile" default-value="${project.file.path}"
     * @required
     */
    private String pomFile;

    /**
     * Runs this mojo.
     * 
     * @throws MojoFailureException If the artifact file has not been set.
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        getLog().info( "[MAVEN-CORE-IT-LOG] Attaching POM to main artifact: " + pomFile );

        if ( pomFile == null || pomFile.length() <= 0 )
        {
            throw new MojoFailureException( "Path name for POM file has not been specified" );
        }

        /*
         * NOTE: We do not want to test path translation here, so resolve relative paths manually.
         */
        File metadataFile = new File( pomFile );
        if ( !metadataFile.isAbsolute() )
        {
            metadataFile = new File( project.getBasedir(), pomFile );
        }

        Artifact artifact = project.getArtifact();
        artifact.addMetadata( new ProjectArtifactMetadata( artifact, metadataFile ) );

        getLog().info( "[MAVEN-CORE-IT-LOG] Attached POM to main artifact: " + metadataFile );
    }

}
