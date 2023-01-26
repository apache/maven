package org.apache.maven.cli;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.transport.GetTask;
import org.eclipse.aether.spi.connector.transport.Transporter;
import org.eclipse.aether.spi.connector.transport.TransporterProvider;
import org.eclipse.aether.transfer.NoTransporterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Optional;

/**
 * Helper class to verify connection to a remote repository.
 */
public class RemoteRepositoryConnectionVerifier {
    private static final Artifact APACHE_MAVEN_ARTIFACT = MavenStatusCommand.APACHE_MAVEN_ARTIFACT;
    private final Logger logger;
    private final TransporterProvider transporterProvider;

    public RemoteRepositoryConnectionVerifier( final PlexusContainer container ) throws ComponentLookupException
    {
        this.logger = LoggerFactory.getILoggerFactory().getLogger( RemoteRepositoryConnectionVerifier.class.getName() );
        this.transporterProvider = container.lookup(TransporterProvider.class);
    }

    private boolean isCentralOrMirrorOfCentral( final RemoteRepository remoteRepository ) {
        return "central".equals( remoteRepository.getId() ) ||
                remoteRepository.getMirroredRepositories().stream()
                        .map( RemoteRepository::getId )
                        .anyMatch( "central"::equals );
    }

    public Optional<String> verifyConnectionToRemoteRepository( final RepositorySystemSession session,
                                                                final ArtifactRepository artifactRepository )
    {
        final RemoteRepository repository = RepositoryUtils.toRepo( artifactRepository );

        final String artifactPath;

        if ( isCentralOrMirrorOfCentral( repository ) ) {
            // We can be sure the Apache Maven artifact should be resolvable.
            artifactPath = artifactRepository.getLayout().pathOf( RepositoryUtils.toArtifact( APACHE_MAVEN_ARTIFACT ) );
        } else {
            // We cannot be sure about any artifact that lives here.
            artifactPath = "/";
        }

        try {
            final Transporter transporter = transporterProvider.newTransporter( session, repository );
            final Optional<String> maybeIssue = verifyConnectionUsingTransport( transporter, repository, artifactPath );

            if ( !maybeIssue.isPresent() ) {
                logger.info( "Connection check for {} [{}] completed", repository.getId(), repository.getUrl() );
            }

            return maybeIssue;
        } catch ( final NoTransporterException nte ) {
            final String message = String.format(
                    "There is no compatible transport for remote repository %s with location %s",
                    repository.getId(),
                    repository.getUrl()
            );
            return Optional.of( message );
        }
    }

    private Optional<String> verifyConnectionUsingTransport(
            final Transporter transporter,
            final RemoteRepository remoteRepository,
            final String artifactPath
    ) {
        try {
            final GetTask task = new GetTask( URI.create( artifactPath ) );
            transporter.get( task );
            return Optional.empty();
        } catch ( final Exception e ) {
            return classifyException( remoteRepository, e );
        }
    }

    private Optional<String> classifyException( final RemoteRepository remoteRepository, final Exception e ) {
        final String message = e.getMessage();
        final String repositoryUrl = remoteRepository.getUrl();

        final boolean resourceMissing = StringUtils.contains( message, "resource missing" );

        if ( isCentralOrMirrorOfCentral( remoteRepository ) && resourceMissing ) {
            final String issue = String.format( "Connection to %s possible, but expected artifact %s cannot be resolved",
                    repositoryUrl, APACHE_MAVEN_ARTIFACT );
            return Optional.of( issue );

        } else if ( resourceMissing ) {
            // We tried to resolve the artifact from a repository that does not necessarily host it.
            logger.warn( "Connection to {} possible, but artifact {} not found", repositoryUrl, APACHE_MAVEN_ARTIFACT );
            return Optional.empty();

        } else if ( StringUtils.contains( message, "authentication failed" ) ) {
            final String issue = String.format( "Connection to %s possible, but authentication failed", repositoryUrl );
            return Optional.of(issue);

        }

        logger.error( "Error connecting to repository {} [{}]", remoteRepository.getId(), repositoryUrl, e );
        return Optional.of( "Unknown issue: " + e.getMessage() );
    }
}
