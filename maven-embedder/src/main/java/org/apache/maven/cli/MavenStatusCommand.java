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

import org.apache.maven.RepositoryUtils;
import org.apache.maven.api.services.ArtifactResolverException;
import org.apache.maven.api.services.ArtifactResolverResult;
import org.apache.maven.artifact.repository.Authentication;
import org.apache.maven.bridge.MavenRepositorySystem;
import org.apache.maven.internal.impl.DefaultSession;
import org.apache.maven.settings.Mirror;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.apache.maven.api.ArtifactCoordinate;
import org.apache.maven.api.Session;
import org.apache.maven.api.services.ArtifactResolver;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequestPopulator;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.internal.aether.DefaultRepositorySystemSessionFactory;
import org.apache.maven.internal.impl.DefaultArtifactCoordinate;
import org.apache.maven.internal.impl.DefaultSessionFactory;
import org.apache.maven.repository.Proxy;
import org.apache.maven.session.scope.internal.SessionScope;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.transfer.ArtifactNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
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
    private final ArtifactResolver artifactResolver;
    private final DefaultSessionFactory defaultSessionFactory;
    private final DefaultRepositorySystemSessionFactory repoSession;
    private final Logger logger;
    private final PlexusContainer container;
    private final SessionScope sessionScope;

    public MavenStatusCommand( final PlexusContainer container ) throws ComponentLookupException
    {
        this.container = container;
        mavenExecutionRequestPopulator = container.lookup( MavenExecutionRequestPopulator.class );
        logger = LoggerFactory.getILoggerFactory().getLogger( MavenStatusCommand.class.getName() );
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

        final ArtifactRepository localRepository = cliRequest.getRequest().getLocalRepository();

        final List<String> localRepositoryIssues =
                verifyLocalRepository( Paths.get( URI.create( localRepository.getUrl() ) ) );
        final List<String> remoteRepositoryIssues =
                verifyRemoteRepositoryConnections( cliRequest.getRequest().getRemoteRepositories(), mavenExecutionRequest.getMirrors() );
        final List<String> artifactResolutionIssues = verifyArtifactResolution( mavenExecutionRequest );

        // Collect all issues into a single list
        return Stream.of( localRepositoryIssues, remoteRepositoryIssues, artifactResolutionIssues )
                .flatMap( Collection::stream )
                .collect( Collectors.toList() );
    }

    private List<String> verifyRemoteRepositoryConnections( List<ArtifactRepository> remoteRepositories,
                                                            List<Mirror> mirrors )
            throws IOException
    {
        final List<String> issues = new ArrayList<>();

        for ( ArtifactRepository remoteRepository : remoteRepositories )
        {
            // Only support HTTP and HTTPS
            final String protocol = remoteRepository.getProtocol();
            if ( !"http".equals( protocol ) && !"https".equals( protocol ) )
            {
                final String unsupportedProtocolWarning =
                        String.format( "No status checks available for protocol %s.", protocol );
                logger.info( unsupportedProtocolWarning );
                continue;
            }

            final Mirror mirror = MavenRepositorySystem.getMirror( remoteRepository, mirrors );
            //TODO get path of mirror to resolve url

            // Setup connection
            final String artifactUrl = remoteRepository.getUrl() + "/" + remoteRepository.getLayout().pathOf( RepositoryUtils.toArtifact( artifact ) );
            URL url = new URL( artifactUrl );
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            if (remoteRepository.getProxy() != null) {
                // Use proxy
                final Proxy proxy = remoteRepository.getProxy();
                final java.net.Proxy javaProxy = new java.net.Proxy(java.net.Proxy.Type.HTTP, new InetSocketAddress(proxy.getHost(), proxy.getPort()));
                Authenticator authenticator = new Authenticator() {
                    public PasswordAuthentication getPasswordAuthentication() {
                        return (new PasswordAuthentication( proxy.getUserName(), proxy.getPassword().toCharArray())); // Password might be encrypted, if so decrypt
                    }
                };
                Authenticator.setDefault(authenticator);
                connection = (HttpURLConnection) url.openConnection(javaProxy);
            }

            if (remoteRepository.getAuthentication() != null) {
                final Authentication authentication = remoteRepository.getAuthentication();
                if (authentication.getPrivateKey() != null)
                {
                    // We probably need to use a certificate, use password if there is one
                }
                else
                {
                    // Add Basic Authorization header instead
                    String credentials = authentication.getUsername() + ":" + authentication.getPassword();
                    connection.setRequestProperty( "Authorization", createAuthorizationHeader( credentials ));
                }
            }

            // Send request
            connection.setRequestMethod("GET");
            final int responseCode = connection.getResponseCode();

            // Report response
            if (responseCode == 200 || responseCode == 404)
            {
                logger.info( "Successfully connected to {}", remoteRepository.getUrl() );
            }
            else
            {
                final String issue
                        = String.format( "%s responded with code %d.", remoteRepository.getUrl(), responseCode );
                issues.add( issue );
            }

        }

        return issues;
    }

    private static String createAuthorizationHeader( String credentials )
    {
        return "Basic " + Base64.getEncoder().encodeToString( credentials.getBytes());
    }

    private List<String> verifyArtifactResolution( final MavenExecutionRequest mavenExecutionRequest )
    {
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

            resolverResult.getArtifacts().forEach((key, value) -> {
                logger.debug("Successfully resolved {} to {}", key.toString(), value.toString());
            });

            return Collections.emptyList();
        } catch (ArtifactResolverException are) {
            return extractErrorMessagesFromArtifactResolverException(are);
        } finally {
            sessionScope.exit();
        }
    }

    private List<String> extractErrorMessagesFromArtifactResolverException(final Exception exception) {
        final boolean isArtifactResolutionException = exception.getCause() instanceof ArtifactResolutionException;
        if (isArtifactResolutionException)
        {
            final ArtifactResolutionException are = (ArtifactResolutionException) exception.getCause();
            return (are).getResults().stream()
                    .map(ArtifactResult::getExceptions)
                    .flatMap(List::stream)
                    .map(ArtifactNotFoundException.class::cast)
                    .map(Throwable::getMessage)
                    .collect(Collectors.toList());
        } else {
            return Collections.singletonList(exception.getMessage());
        }
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

