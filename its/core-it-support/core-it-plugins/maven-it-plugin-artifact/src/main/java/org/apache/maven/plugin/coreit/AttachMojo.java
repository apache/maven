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
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

import java.io.File;

/**
 * Attaches a secondary artifact to the current project. This mimics source/javadoc attachments or other assemblies.
 *
 * @goal attach
 * @phase package
 *
 * @author Benjamin Bentmann
 *
 */
public class AttachMojo
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
     * The Maven project helper.
     *
     * @component
     */
    private MavenProjectHelper helper;

    /**
     * The path to the file to attach, relative to the project base directory. The plugin will not validate this path.
     *
     * @parameter property="artifact.attachedFile"
     * @required
     */
    private String attachedFile;

    /**
     * The type of the artifact to attach.
     *
     * @parameter property="artifact.artifactType"
     */
    private String artifactType;

    /**
     * The classifier for the attached artifact. If unspecified, the default classifier for the specified artifact type
     * is used.
     *
     * @parameter property="artifact.artifactClassifier"
     */
    private String artifactClassifier;

    /**
     * Runs this mojo.
     *
     * @throws MojoFailureException If the attached file has not been set.
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        getLog().info( "[MAVEN-CORE-IT-LOG] Attaching artifact file: " + attachedFile );
        getLog().info( "[MAVEN-CORE-IT-LOG] type=" + artifactType + ", classifier=" + artifactClassifier );

        if ( attachedFile == null || attachedFile.length() <= 0 )
        {
            throw new MojoFailureException( "Path name for attached artifact file has not been specified" );
        }

        /*
         * NOTE: We do not want to test path translation here, so resolve relative paths manually.
         */
        File artifactFile = new File( attachedFile );
        if ( !artifactFile.isAbsolute() )
        {
            artifactFile = new File( project.getBasedir(), attachedFile );
        }

        if ( !artifactFile.exists() )
        {
            getLog().warn( "[MAVEN-CORE-IT-LOG] Attached artifact file does not exist: " + artifactFile );
        }

        helper.attachArtifact( project, artifactType, artifactClassifier, artifactFile );

        getLog().info( "[MAVEN-CORE-IT-LOG] Attached artifact file: " + artifactFile );
    }

}
