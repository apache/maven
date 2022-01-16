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
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.List;
import java.util.Properties;

/**
 * Builds the remote POMs of user-specified artifacts. This mimics in part the Maven Remote Resources Plugin.
 *
 * @author Benjamin Bentmann
 *
 * @goal remote-pom
 */
public class BuildRemotePomMojo
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
     * The remote repositories of the current Maven project.
     *
     * @parameter default-value="${project.remoteArtifactRepositories}"
     * @readonly
     * @required
     */
    private List remoteRepositories;

    /**
     * The artifact factory.
     *
     * @component
     */
    private ArtifactFactory factory;

    /**
     * The dependencies to resolve.
     *
     * @parameter
     */
    private Dependency[] dependencies;

    /**
     * Runs this mojo.
     *
     * @throws MojoExecutionException If the artifact file has not been set.
     */
    public void execute()
        throws MojoExecutionException
    {
        Properties props = new Properties();

        getLog().info( "[MAVEN-CORE-IT-LOG] Building remote POMs" );

        if ( dependencies != null )
        {
            for ( Dependency dependency : dependencies )
            {
                Artifact artifact =
                    factory.createArtifactWithClassifier( dependency.getGroupId(), dependency.getArtifactId(),
                                                          dependency.getVersion(), dependency.getType(),
                                                          dependency.getClassifier() );

                String id = artifact.getId();

                getLog().info( "[MAVEN-CORE-IT-LOG] Building " + id );

                try
                {
                    MavenProject project = builder.buildFromRepository( artifact, remoteRepositories, localRepository );

                    dump( props, id + ".", project );
                }
                catch ( Exception e )
                {
                    getLog().warn( "Failed to build remote POM for " + artifact.getId(), e );
                }

                put( props, id + ".file", artifact.getFile() );
                put( props, id + ".version", artifact.getVersion() );
            }
        }

        store( props, propertiesFile );
    }

}
