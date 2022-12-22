package org.apache.maven.cli;

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

import org.apache.maven.api.services.ArtifactResolverException;
import org.apache.maven.api.services.ArtifactResolverResult;
import org.apache.maven.internal.impl.DefaultSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.apache.maven.api.ArtifactCoordinate;
import org.apache.maven.api.Session;
import org.apache.maven.api.services.ArtifactResolver;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.cli.configuration.ConfigurationProcessor;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequestPopulator;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.internal.aether.DefaultRepositorySystemSessionFactory;
import org.apache.maven.internal.impl.DefaultArtifactCoordinate;
import org.apache.maven.internal.impl.DefaultSessionFactory;
import org.apache.maven.session.scope.internal.SessionScope;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MavenStatusCommand
{
    /**
     * In order to verify artifacts can be downloaded from the remote repositories we want to resolve an actual
     * artifact. The Apache Maven artifact was chosen as it eventually, be it by proxy, mirror or directly, will be
     * gathered from the central repository. The version is chosen arbitrarily since any listed should work.
     */
    private static final Artifact artifact = new DefaultArtifact(
            "org.apache.maven",
            "apache-maven",
            null,
            "pom",
            "3.8.6"
    );
    private final MavenExecutionRequestPopulator mavenExecutionRequestPopulator;
    private final ConfigurationProcessor configurationProcessor;
    private final ArtifactResolver artifactResolver;
    private final DefaultSessionFactory defaultSessionFactory;
    private final DefaultRepositorySystemSessionFactory repoSession;
    private final Logger logger;
    private final PlexusContainer container;
    private final SessionScope sessionScope;

    public MavenStatusCommand( PlexusContainer container ) throws ComponentLookupException
    {
        this.container = container;
        mavenExecutionRequestPopulator = container.lookup( MavenExecutionRequestPopulator.class );
        logger = LoggerFactory.getILoggerFactory().getLogger( MavenStatusCommand.class.getName() );
        configurationProcessor = container.lookup( ConfigurationProcessor.class );
        artifactResolver = container.lookup( ArtifactResolver.class );
        defaultSessionFactory = container.lookup( DefaultSessionFactory.class );
        repoSession = container.lookup( DefaultRepositorySystemSessionFactory.class );
        sessionScope = container.lookup( SessionScope.class );
    }

    public List<String> verify(CliRequest cliRequest )
            throws Exception
    {
        // Populate the cliRequest with defaults and user settings
        final MavenExecutionRequest mavenExecutionRequest =
                mavenExecutionRequestPopulator.populateDefaults( cliRequest.request );
        // TODO If an active profile defines more remote repositories, they do not yet show in mavenExecutionRequest.
        // configurationProcessor.process( cliRequest );

        final ArtifactRepository localRepository = cliRequest.getRequest().getLocalRepository();

        final List<String> localRepositoryIssues =
                verifyLocalRepository( Paths.get( URI.create( localRepository.getUrl() ) ) );
        final List<String> remoteRepositoryIssues =
                verifyRemoteRepositoryConnections();
        final List<String> artifactResolutionIssues =
                verifyArtifactResolution( cliRequest.getRequest().getRemoteRepositories(), mavenExecutionRequest );

        // Collect all issues into a single list
        return Stream.of( localRepositoryIssues, remoteRepositoryIssues, artifactResolutionIssues )
                .flatMap( Collection::stream )
                .collect( Collectors.toList() );
    }

    private List<String> verifyRemoteRepositoryConnections()
    {
        final List<String> issues = new ArrayList<>();

        return issues;
    }

    private List<String> verifyArtifactResolution( List<ArtifactRepository> remoteRepositories,
                                                   MavenExecutionRequest mavenExecutionRequest )
    {
        final List<String> issues = new ArrayList<>();

        for ( ArtifactRepository artifactRepository : remoteRepositories )
        {
            final String protocol = artifactRepository.getProtocol();
            if ( !"http".equals( protocol ) && !"https".equals( protocol ) )
            {
                final String unsupportedProtocolWarning =
                        String.format( "No status checks available for protocol %s.", protocol );
                logger.info( unsupportedProtocolWarning );
                continue;
            }

            final Session session = this.defaultSessionFactory.getSession(
                    new MavenSession(
                            container,
                            repoSession.newRepositorySession(mavenExecutionRequest),
                            mavenExecutionRequest,
                            new DefaultMavenExecutionResult()
                    )
            );

            sessionScope.enter();
            try {
                sessionScope.seed(DefaultSession.class, (DefaultSession) session);

                final ArtifactCoordinate artifactCoordinate = new DefaultArtifactCoordinate(session, artifact);
                final ArtifactResolverResult resolverResult = artifactResolver.resolve(session, Collections.singleton(artifactCoordinate));

                resolverResult.getArtifacts().keySet().forEach(entry -> {
                    logger.info("Successfully resolved {} from {}", entry.toString(), artifactRepository.getUrl());
                });

            } catch (ArtifactResolverException are) {
                final boolean isArtifactResolutionException = are.getCause() instanceof ArtifactResolutionException;
                final String message = isArtifactResolutionException ? are.getCause().getMessage() : are.getMessage();
                issues.add(message);
            } finally {
                sessionScope.exit();
            }
        }

        return issues;
    }

    private List<String> verifyLocalRepository( final Path localRepositoryPath )
    {
        final List<String> issues = new ArrayList<>();

        if ( !Files.isDirectory( localRepositoryPath ) )
        {
            issues.add( "Local repository is not a directory." );
        }

        if ( !Files.isReadable( localRepositoryPath ) )
        {
            issues.add( "No read permissions on local repository." );
        }

        if ( !Files.isWritable( localRepositoryPath ) )
        {
            issues.add( "No write permissions on local repository." );
        }

        return issues;
    }
}

