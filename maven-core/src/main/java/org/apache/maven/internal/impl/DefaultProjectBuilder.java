package org.apache.maven.internal.impl;

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

import javax.inject.Inject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.api.Artifact;
import org.apache.maven.api.ArtifactCoordinate;
import org.apache.maven.api.Node;
import org.apache.maven.api.Project;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.services.DependencyCollectorResult;
import org.apache.maven.api.services.ProjectBuilder;
import org.apache.maven.api.services.ProjectBuilderException;
import org.apache.maven.api.services.BuilderProblem;
import org.apache.maven.api.services.BuilderProblemSeverity;
import org.apache.maven.api.services.ProjectBuilderRequest;
import org.apache.maven.api.services.ProjectBuilderResult;
import org.apache.maven.api.services.Source;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.building.ModelProblem;
import org.apache.maven.model.building.ModelSource;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingResult;

public class DefaultProjectBuilder implements ProjectBuilder
{

    private final org.apache.maven.project.ProjectBuilder builder;

    @Inject
    public DefaultProjectBuilder( org.apache.maven.project.ProjectBuilder builder )
    {
        this.builder = builder;
    }

    @SuppressWarnings( "MethodLength" )
    @Nonnull
    @Override
    public ProjectBuilderResult build( ProjectBuilderRequest request )
            throws ProjectBuilderException, IllegalArgumentException
    {
        DefaultSession session = ( DefaultSession ) request.getSession();
        try
        {
            List<ArtifactRepository> repositories =
                    session.toArtifactRepositories( session.getRemoteRepositories() );
            ProjectBuildingRequest req = new DefaultProjectBuildingRequest()
                    .setRepositorySession( session.getSession() )
                    .setRemoteRepositories( repositories )
                    .setPluginArtifactRepositories( repositories )
                    .setProcessPlugins( request.isProcessPlugins() );
            ProjectBuildingResult res;
            if ( request.getPath().isPresent() )
            {
                Path path = request.getPath().get();
                res = builder.build( path.toFile(), req );
            }
            else if ( request.getSource().isPresent() )
            {
                Source source = request.getSource().get();
                ModelSource modelSource = new ModelSource()
                {
                    @Override
                    public InputStream getInputStream() throws IOException
                    {
                        return source.getInputStream();
                    }

                    @Override
                    public String getLocation()
                    {
                        return source.getLocation();
                    }
                };
                res = builder.build( modelSource, req );
            }
            else if ( request.getArtifact().isPresent() )
            {
                Artifact a = request.getArtifact().get();
                org.eclipse.aether.artifact.Artifact aetherArtifact = session.toArtifact( a );
                org.apache.maven.artifact.Artifact artifact = RepositoryUtils.toArtifact( aetherArtifact );
                res = builder.build( artifact, request.isAllowStubModel(), req );
            }
            else if ( request.getCoordinate().isPresent() )
            {
                ArtifactCoordinate c = request.getCoordinate().get();
                org.apache.maven.artifact.Artifact artifact = new DefaultArtifact(
                        c.getGroupId(), c.getArtifactId(), c.getVersion().asString(), null,
                        c.getExtension(), c.getClassifier(), null );
                res = builder.build( artifact, request.isAllowStubModel(), req );
            }
            else
            {
                throw new IllegalArgumentException( "Invalid request" );
            }
            return new ProjectBuilderResult()
            {
                @Nonnull
                @Override
                public String getProjectId()
                {
                    return res.getProjectId();
                }

                @Nonnull
                @Override
                public Optional<Path> getPomFile()
                {
                    return Optional.ofNullable( res.getPomFile() ).map( File::toPath );
                }

                @Nonnull
                @Override
                public Optional<Project> getProject()
                {
                    return Optional.ofNullable( res.getProject() )
                            .map( session::getProject );
                }

                @Nonnull
                @Override
                public Collection<BuilderProblem> getProblems()
                {
                    return new MappedCollection<>( res.getProblems(), this::toProblem );
                }

                private BuilderProblem toProblem( ModelProblem problem )
                {
                    return new BuilderProblem()
                    {
                        @Override
                        public String getSource()
                        {
                            return problem.getSource();
                        }

                        @Override
                        public int getLineNumber()
                        {
                            return problem.getLineNumber();
                        }

                        @Override
                        public int getColumnNumber()
                        {
                            return problem.getColumnNumber();
                        }

                        @Override
                        public String getLocation()
                        {
                            StringBuilder buffer = new StringBuilder( 256 );

                            if ( getSource().length() > 0 )
                            {
                                if ( buffer.length() > 0 )
                                {
                                    buffer.append( ", " );
                                }
                                buffer.append( getSource() );
                            }

                            if ( getLineNumber() > 0 )
                            {
                                if ( buffer.length() > 0 )
                                {
                                    buffer.append( ", " );
                                }
                                buffer.append( "line " ).append( getLineNumber() );
                            }

                            if ( getColumnNumber() > 0 )
                            {
                                if ( buffer.length() > 0 )
                                {
                                    buffer.append( ", " );
                                }
                                buffer.append( "column " ).append( getColumnNumber() );
                            }

                            return buffer.toString();
                        }

                        @Override
                        public Exception getException()
                        {
                            return problem.getException();
                        }

                        @Override
                        public String getMessage()
                        {
                            return problem.getMessage();
                        }

                        @Override
                        public BuilderProblemSeverity getSeverity()
                        {
                            return BuilderProblemSeverity.valueOf( problem.getSeverity().name() );
                        }
                    };
                }

                @Nonnull
                @Override
                public Optional<DependencyCollectorResult> getDependencyResolverResult()
                {
                    return Optional.ofNullable( res.getDependencyResolutionResult() )
                            .map( r -> new DependencyCollectorResult()
                            {
                                @Override
                                public List<Exception> getExceptions()
                                {
                                    return r.getCollectionErrors();
                                }

                                @Override
                                public Node getRoot()
                                {
                                    return session.getNode( r.getDependencyGraph() );
                                }

//                                @Override
//                                public List<ArtifactResolverResult> getArtifactResults()
//                                {
//                                    return Collections.emptyList();
//                                }
                            } );
                }
            };
        }
        catch ( ProjectBuildingException e )
        {
            throw new ProjectBuilderException( "Unable to build project", e );
        }
    }
}
