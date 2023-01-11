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
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Collects user-specified artifacts. This mimics in part the Maven Assembly Plugin.
 *
 * @author Benjamin Bentmann
 *
  */
@Mojo( name = "collect" )
public class CollectMojo
    extends AbstractMojo
{

    /**
     * The local repository.
     */
    @Parameter( defaultValue = "${localRepository}", readonly = true, required = true )
    private ArtifactRepository localRepository;

    /**
     * The remote repositories of the current Maven project.
     */
    @Parameter( defaultValue = "${project.remoteArtifactRepositories}", readonly = true, required = true )
    private List remoteRepositories;

    /**
     * The artifact collector.
     *
     */
    @Component
    private ArtifactCollector collector;

    /**
     * The artifact factory.
     *
     */
    @Component
    private ArtifactFactory factory;

    /**
     * The metadata source.
     *
     */
    @Component
    private ArtifactMetadataSource metadataSource;

    /**
     * The dependencies to resolve.
     *
     */
    @Parameter
    private Dependency[] dependencies;

    /**
     * Runs this mojo.
     *
     * @throws MojoFailureException If the artifact file has not been set.
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        getLog().info( "[MAVEN-CORE-IT-LOG] Collecting artifacts" );

        try
        {
            Artifact origin = factory.createArtifact( "it", "it", "0.1", null, "pom" );

            Set artifacts = new LinkedHashSet();

            if ( dependencies != null )
            {
                for ( Dependency dependency : dependencies )
                {
                    Artifact artifact =
                        factory.createArtifactWithClassifier( dependency.getGroupId(), dependency.getArtifactId(),
                                                              dependency.getVersion(), dependency.getType(),
                                                              dependency.getClassifier() );

                    artifacts.add( artifact );

                    getLog().info( "[MAVEN-CORE-IT-LOG] Collecting " + artifact.getId() );
                }
            }

            collector.collect( artifacts, origin, localRepository, remoteRepositories, metadataSource, null,
                               Collections.EMPTY_LIST );
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Failed to collect artifacts: " + e.getMessage(), e );
        }
    }

}
