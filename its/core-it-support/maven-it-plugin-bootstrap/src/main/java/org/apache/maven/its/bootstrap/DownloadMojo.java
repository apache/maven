package org.apache.maven.its.bootstrap;

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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.ProjectBuildingRequest;
import org.codehaus.plexus.util.StringUtils;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.ArtifactTypeRegistry;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.DependencyRequest;

/**
 * Boostrap plugin to download all required dependencies
 */
@Mojo( name = "download" )
public class DownloadMojo
        extends AbstractMojo
{

    /**
     * A list of artifacts coordinates.
     */
    @Parameter
    private List<Dependency> dependencies = new ArrayList<>();

    /**
     * A list of string of the form groupId:artifactId:version[:packaging[:classifier]].
     */
    @Parameter
    private List<String> artifacts = new ArrayList<>();

    /**
     * A file containing lines of the form groupId:artifactId:version[:packaging[:classifier]].
     */
    @Parameter
    private File file;

    @Component
    private RepositorySystem repositorySystem;

    @Component
    private MavenSession session;

    @Component
    private ArtifactHandlerManager artifactHandlerManager;

    @Override
    public void execute() throws MojoFailureException
    {
        if ( file != null && file.exists() )
        {
            try ( BufferedReader reader = new BufferedReader( new FileReader( file ) ) )
            {
                reader.lines()
                        .map( String::trim )
                        .filter( s ->  !s.isEmpty() && !s.startsWith( "#" ) )
                        .forEach( artifacts::add );
            }
            catch ( IOException e )
            {
                throw new MojoFailureException( "Unable to read dependencies: " + file, e );
            }
        }
        for ( String artifact : artifacts )
        {
            if ( artifact != null )
            {
                dependencies.add( toDependency( artifact ) );
            }
        }

        ProjectBuildingRequest projectBuildingRequest = session.getProjectBuildingRequest();
        RepositorySystemSession repositorySystemSession = projectBuildingRequest.getRepositorySession();
        List<RemoteRepository> repos = RepositoryUtils.toRepos( projectBuildingRequest.getRemoteRepositories() );
        ArtifactTypeRegistry registry = RepositoryUtils.newArtifactTypeRegistry( artifactHandlerManager );
        for ( Dependency dependency : dependencies )
        {
            try
            {
                org.eclipse.aether.graph.Dependency dep = RepositoryUtils.toDependency( dependency, registry );
                DependencyRequest request = new DependencyRequest(
                        new CollectRequest( Collections.singletonList( dep ), Collections.emptyList(), repos ),
                        null );
                System.out.println( "Resolving: " + dep.getArtifact() );
                repositorySystem.resolveDependencies( repositorySystemSession, request );
            }
            catch ( Exception e )
            {
                throw new MojoFailureException( "Unable to resolve dependency: " + dependency, e );
            }
        }
    }


    static Dependency toDependency( String artifact )
            throws MojoFailureException
    {
        Dependency coordinate = new Dependency();
        String[] tokens = StringUtils.split( artifact, ":" );
        if ( tokens.length < 3 || tokens.length > 5 )
        {
            throw new MojoFailureException( "Invalid artifact, you must specify "
                    + "groupId:artifactId:version[:packaging[:classifier]] " + artifact );
        }
        coordinate.setGroupId( tokens[0] );
        coordinate.setArtifactId( tokens[1] );
        coordinate.setVersion( tokens[2] );
        if ( tokens.length >= 4 )
        {
            coordinate.setType( tokens[3] );
        }
        if ( tokens.length == 5 )
        {
            coordinate.setClassifier( tokens[4] );
        }
        return coordinate;
    }
}
