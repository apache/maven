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
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Resolves an artifact and has an (unused) dependency on log4j.
 *
 *
 * @author Benjamin Bentmann
 *
 */
@Mojo( name = "it" )
public class ItMojo
    extends AbstractMojo
{

    /**
     */
    @Component
    private ArtifactFactory artifactFactory;

    /**
     */
    @Component
    private ArtifactResolver artifactResolver;

    @Parameter( defaultValue = "${localRepository}", required = true, readonly = true )
    private ArtifactRepository localRepository;

    @Parameter( defaultValue = "${project.remoteArtifactRepositories}", required = true, readonly = true )
    private List remoteRepositories;

    @Parameter( defaultValue = "test" )
    private String groupId;

    @Parameter( defaultValue = "test" )
    private String artifactId;

    @Parameter( defaultValue = "1.0" )
    private String version;

    /**
     * Runs this mojo.
     *
     * @throws MojoExecutionException If the output file could not be created.
     * or if the output file has not been set.
     */
    public void execute()
        throws MojoExecutionException
    {
        getLog().info( "[MAVEN-CORE-IT-LOG4J]" );

        Artifact artifact = artifactFactory.createProjectArtifact( groupId, artifactId, version );

        try
        {
            artifactResolver.resolve( artifact, remoteRepositories, localRepository );
        }
        catch ( ArtifactNotFoundException e )
        {
            getLog().info( "SUCCESS" );
        }
        catch ( ArtifactResolutionException e )
        {
            throw new MojoExecutionException( "Should have responded with ArtifactNotFoundException.", e );
        }
    }

}
