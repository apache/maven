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

import org.apache.maven.api.services.ArtifactFactoryRequest;
import org.apache.maven.api.services.ArtifactResolver;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.Authentication;
import org.apache.maven.cli.configuration.ConfigurationProcessor;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequestPopulator;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.internal.aether.DefaultRepositorySystemSessionFactory;
import org.apache.maven.internal.impl.DefaultArtifactFactory;
import org.apache.maven.internal.impl.DefaultSessionFactory;
import org.apache.maven.repository.Proxy;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
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
    private final Artifact apacheMavenArtifact;
    private final MavenExecutionRequestPopulator mavenExecutionRequestPopulator;
    private final ConfigurationProcessor configurationProcessor;
    private final ArtifactResolver artifactResolver;
    private final DefaultSessionFactory defaultSessionFactory;
    private final DefaultRepositorySystemSessionFactory repoSession;
    private final Logger logger;
    private final PlexusContainer container;

    public MavenStatusCommand( PlexusContainer container ) throws ComponentLookupException
    {
        this.container = container;
        mavenExecutionRequestPopulator = container.lookup( MavenExecutionRequestPopulator.class );
        logger = LoggerFactory.getILoggerFactory().getLogger( MavenStatusCommand.class.getName() );
        configurationProcessor = container.lookup( ConfigurationProcessor.class );
        artifactResolver = container.lookup( ArtifactResolver.class );
        defaultSessionFactory = container.lookup( DefaultSessionFactory.class );
        repoSession = container.lookup( DefaultRepositorySystemSessionFactory.class );

        ArtifactHandlerManager manager = container.lookup( ArtifactHandlerManager.class );
        apacheMavenArtifact = new DefaultArtifact( "org.apache.maven", "apache-maven", "3.8.6",
                null, "pom", null, manager.getArtifactHandler( "pom" ) );
    }

    public List<String> verify( CliRequest cliRequest )
            throws Exception
    {
        // Populate the cliRequest with defaults and user settings
        final MavenExecutionRequest mavenExecutionRequest =
                mavenExecutionRequestPopulator.populateDefaults( cliRequest.request );
        configurationProcessor.process( cliRequest );

        final ArtifactRepository localRepository = cliRequest.getRequest().getLocalRepository();

        final List<String> localRepositoryIssues =
                verifyLocalRepository( Paths.get( URI.create( localRepository.getUrl() ) ) );
        final List<String> remoteRepositoryIssues =
                verifyRemoteRepositoryConnections( cliRequest.getRequest().getRemoteRepositories(), mavenExecutionRequest );
        final List<String> artifactResolutionIssues = verifyArtifactResolution();

        // Collect all issues into a single list
        return Stream.of( localRepositoryIssues, remoteRepositoryIssues, artifactResolutionIssues )
                .flatMap( Collection::stream )
                .collect( Collectors.toList() );
    }

    private List<String> verifyArtifactResolution()
    {
        final List<String> issues = new ArrayList<>();

        return issues;
    }

    // TODO: is becoming a large method
    private List<String> verifyRemoteRepositoryConnections( List<ArtifactRepository> remoteRepositories,
                                                            MavenExecutionRequest mavenExecutionRequest )
            throws IOException
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

            final String artifactPath = artifactRepository.getLayout().pathOf( apacheMavenArtifact );
            final String urlToRemoteRepository =
                    String.join( "/", artifactRepository.getUrl(), artifactPath );


            MavenSession session = new MavenSession( container, repoSession.newRepositorySession( mavenExecutionRequest ),
                    mavenExecutionRequest, new DefaultMavenExecutionResult() );

            final ArtifactFactoryRequest artifactRequest = ArtifactFactoryRequest.builder()
                    .groupId( apacheMavenArtifact.getGroupId() )
                    .artifactId( apacheMavenArtifact.getArtifactId() )
                    .classifier( apacheMavenArtifact.getClassifier() )
                    .extension( "pom" )
                    .version( apacheMavenArtifact.getVersion() )
                    .type( apacheMavenArtifact.getType() )
                    .build();

            final org.apache.maven.api.Artifact artifact = new DefaultArtifactFactory().create( artifactRequest );

//            new DefaultArtifactCoordinate( session.getSession(),  artifact);

//            new org.eclipse.aether.artifact.DefaultArtifact(
//                    apacheMavenArtifact.getGroupId(),
//                    apacheMavenArtifact.getArtifactId(),
//                    apacheMavenArtifact.getClassifier(),
//                    "pom",
//                    apacheMavenArtifact.getVersion(),
//                     );
//
//            artifactResolver.resolve(session,  )

            URL url = new URL( urlToRemoteRepository );
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod( "GET" );

            Authentication auth = artifactRepository.getAuthentication();
            if ( auth != null )
            {
                connection.setRequestProperty( "Authorization",
                        createAuthorizationHeaderValue( auth.getUsername(), auth.getPassword() ) );
            }

            if ( artifactRepository.getProxy() != null )
            {
                final Proxy proxy = artifactRepository.getProxy();
                connection.setRequestProperty( "Proxy-Authorization",
                        createAuthorizationHeaderValue( proxy.getUserName(), proxy.getPassword() ) );
            }

            int responseCode = connection.getResponseCode();

            if ( !isAuthenticated( responseCode ) )
            {
                final String authenticationIssue =
                        String.format( "Authentication failed. Remote repository responded with response code %d.",
                                responseCode );
                issues.add( authenticationIssue );
            }
            connection.disconnect();
        }

        return issues;
    }

    private static String createAuthorizationHeaderValue( String userName, String password )
    {
        final String userNamePassword = userName + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString( userNamePassword.getBytes() );
    }

    private boolean isAuthenticated( int responseCode )
    {
        // TODO: maybe 404 as well as it might be used to hide the existence of the page to
        //  a user without adequate privileges or not correctly authenticated.
        // Just catch entire 400-499 range to avoid missing errors?
        return responseCode != 401
                && responseCode != 403
                && responseCode != 407;
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

