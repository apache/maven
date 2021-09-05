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
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

/**
 * Resolves user-specified artifacts. This mimics in part the Maven Dependency Plugin and the Maven Surefire Plugin.
 *
 * @author Benjamin Bentmann
 *
 * @goal resolve
 */
public class ResolveMojo
    extends AbstractMojo
{

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
     * The artifact resolver.
     *
     * @component
     */
    private ArtifactResolver resolver;

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
     * The path to a properties file to store the resolved artifact paths in.
     *
     * @parameter
     */
    private File propertiesFile;

    /**
     * Runs this mojo.
     *
     * @throws MojoFailureException If the artifact could not be resolved
     */
    public void execute()
        throws MojoExecutionException
    {
        getLog().info( "[MAVEN-CORE-IT-LOG] Resolving artifacts" );

        Properties props = new Properties();

        try
        {
            if ( dependencies != null )
            {
                for ( Dependency dependency : dependencies )
                {
                    Artifact artifact =
                        factory.createArtifactWithClassifier( dependency.getGroupId(), dependency.getArtifactId(),
                                                              dependency.getVersion(), dependency.getType(),
                                                              dependency.getClassifier() );

                    getLog().info( "[MAVEN-CORE-IT-LOG] Resolving " + getId( artifact ) );

                    resolver.resolve( artifact, remoteRepositories, localRepository );

                    if ( artifact.getFile() != null )
                    {
                        props.setProperty( getId( artifact ), artifact.getFile().getPath() );
                    }

                    getLog().info( "[MAVEN-CORE-IT-LOG]   " + artifact.getFile() );
                }
            }
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Failed to resolve artifacts: " + e.getMessage(), e );
        }

        if ( propertiesFile != null )
        {
            getLog().info( "[MAVEN-CORE-IT-LOG] Creating properties file " + propertiesFile );

            try
            {
                propertiesFile.getParentFile().mkdirs();

                try ( FileOutputStream fos = new FileOutputStream( propertiesFile ) )
                {
                    props.store( fos, "MAVEN-CORE-IT" );
                }
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Failed to create properties file: " + e.getMessage(), e );
            }
        }
    }

    private String getId( Artifact artifact )
    {
        artifact.isSnapshot(); // decouple from MNG-2961
        return artifact.getId();
    }

}
