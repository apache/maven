package org.apache.maven.artifact.manager;

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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.UnsupportedProtocolException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.events.TransferListener;
import org.apache.maven.wagon.observers.ChecksumObserver;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.proxy.ProxyInfoProvider;
import org.apache.maven.wagon.repository.Repository;
import org.apache.maven.wagon.repository.RepositoryPermissions;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.configurator.BasicComponentConfigurator;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.ComponentConfigurator;
import org.codehaus.plexus.component.repository.exception.ComponentLifecycleException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/** @plexus.component */
public class DefaultWagonManager
    extends AbstractLogEnabled
    implements WagonManager, Contextualizable, Initializable
{
    private static final String WILDCARD = "*";

    private static final String EXTERNAL_WILDCARD = "external:*";

    private static final String[] CHECKSUM_IDS = {"md5", "sha1"};

    /** have to match the CHECKSUM_IDS */
    private static final String[] CHECKSUM_ALGORITHMS = {"MD5", "SHA-1"};

    private static final String MAVEN_ARTIFACT_PROPERTIES = "META-INF/maven/org.apache.maven.artifact/maven-artifact/pom.properties";

    private static int anonymousMirrorIdSeed = 0;
    
    private PlexusContainer container;

    // TODO: proxies, authentication and mirrors are via settings, and should come in via an alternate method - perhaps
    // attached to ArtifactRepository before the method is called (so AR would be composed of WR, not inherit it)
    private Map<String,ProxyInfo> proxies = new HashMap<String,ProxyInfo>();

    private Map<String,AuthenticationInfo> authenticationInfoMap = new HashMap<String,AuthenticationInfo>();

    private Map<String,RepositoryPermissions> serverPermissionsMap = new HashMap<String,RepositoryPermissions>();

    //used LinkedMap to preserve the order.
    private Map<String,ArtifactRepository> mirrors = new LinkedHashMap<String,ArtifactRepository>();

    /** Map( String, XmlPlexusConfiguration ) with the repository id and the wagon configuration */
    private Map<String,XmlPlexusConfiguration> serverConfigurationMap = new HashMap<String,XmlPlexusConfiguration>();

    private TransferListener downloadMonitor;

    private boolean online = true;

    private boolean interactive = true;

    private RepositoryPermissions defaultRepositoryPermissions;

    // Components

    /** @plexus.requirement */
    private ArtifactRepositoryFactory repositoryFactory;

    /** @plexus.requirement role="org.apache.maven.wagon.Wagon" */
    private Map wagons;

    /** encapsulates access to Server credentials */
    private CredentialsDataSource credentialsDataSource;

    /** @plexus.requirement */
    private UpdateCheckManager updateCheckManager;

    private String httpUserAgent;

    // TODO: this leaks the component in the public api - it is never released back to the container
    public Wagon getWagon( Repository repository )
        throws UnsupportedProtocolException, WagonConfigurationException
    {
        String protocol = repository.getProtocol();

        if ( protocol == null )
        {
            throw new UnsupportedProtocolException( "The repository " + repository + " does not specify a protocol" );
        }

        Wagon wagon = getWagon( protocol );

        configureWagon( wagon, repository.getId(), protocol );

        return wagon;
    }

    public Wagon getWagon( String protocol )
        throws UnsupportedProtocolException
    {
        if ( protocol == null )
        {
            throw new UnsupportedProtocolException( "Unspecified protocol" );
        }

        String hint = protocol.toLowerCase( java.util.Locale.ENGLISH );
        Wagon wagon = (Wagon) wagons.get( hint );

        if ( wagon == null )
        {
            throw new UnsupportedProtocolException(
                "Cannot find wagon which supports the requested protocol: " + protocol );
        }

        wagon.setInteractive( interactive );

        return wagon;
    }

    public void putArtifact( File source,
                             Artifact artifact,
                             ArtifactRepository deploymentRepository )
        throws TransferFailedException
    {
        putRemoteFile( deploymentRepository, source, deploymentRepository.pathOf( artifact ), downloadMonitor );
    }

    public void putArtifactMetadata( File source,
                                     ArtifactMetadata artifactMetadata,
                                     ArtifactRepository repository )
        throws TransferFailedException
    {
        getLogger().info( "Uploading " + artifactMetadata );
        putRemoteFile( repository, source, repository.pathOfRemoteRepositoryMetadata( artifactMetadata ), null );
    }

    private void putRemoteFile( ArtifactRepository repository,
                                File source,
                                String remotePath,
                                TransferListener downloadMonitor )
        throws TransferFailedException
    {
        failIfNotOnline();

        String protocol = repository.getProtocol();

        Wagon wagon;
        try
        {
            wagon = getWagon( protocol );

            configureWagon( wagon, repository );
        }
        catch ( UnsupportedProtocolException e )
        {
            throw new TransferFailedException( "Unsupported Protocol: '" + protocol + "': " + e.getMessage(), e );
        }

        if ( downloadMonitor != null )
        {
            wagon.addTransferListener( downloadMonitor );
        }

        Map<String,ChecksumObserver> checksums = new HashMap<String,ChecksumObserver>( 2 );

        Map<String,String> sums = new HashMap<String,String>( 2 );

        // TODO: configure these on the repository
        for ( int i = 0; i < CHECKSUM_IDS.length; i++ )
        {
            checksums.put( CHECKSUM_IDS[i], addChecksumObserver( wagon, CHECKSUM_ALGORITHMS[i] ) );
        }

        try
        {
            try
            {
                Repository artifactRepository = new Repository( repository.getId(), repository.getUrl() );

                if ( serverPermissionsMap.containsKey( repository.getId() ) )
                {
                    RepositoryPermissions perms = serverPermissionsMap.get( repository.getId() );

                    getLogger().debug(
                        "adding permissions to wagon connection: " + perms.getFileMode() + " " + perms.getDirectoryMode() );

                    artifactRepository.setPermissions( perms );
                }
                else
                {
                    getLogger().debug( "not adding permissions to wagon connection" );
                }

                wagon.connect( artifactRepository, getAuthenticationInfo( repository.getId() ), new ProxyInfoProvider()
                {
                    public ProxyInfo getProxyInfo( String protocol )
                    {
                        return getProxy( protocol );
                    }
                });

                wagon.put( source, remotePath );
            }
            catch ( CredentialsDataSourceException e )
            {
                String err = "Problem with server credentials: " + e.getMessage();
                getLogger().error( err );
                throw new TransferFailedException( err );
            }
            finally
            {
                if ( downloadMonitor != null )
                {
                    wagon.removeTransferListener( downloadMonitor );
                }
            }

            // Pre-store the checksums as any future puts will overwrite them
            for (String extension : checksums.keySet()) {
                ChecksumObserver observer = checksums.get(extension);
                sums.put(extension, observer.getActualChecksum());
            }

            // We do this in here so we can checksum the artifact metadata too, otherwise it could be metadata itself
            for (String extension : checksums.keySet()) {
                // TODO: shouldn't need a file intermediatary - improve wagon to take a stream
                File temp = File.createTempFile("maven-artifact", null);
                temp.deleteOnExit();
                FileUtils.fileWrite(temp.getAbsolutePath(), "UTF-8", sums.get(extension));

                wagon.put(temp, remotePath + "." + extension);
            }
        }
        catch ( ConnectionException e )
        {
            throw new TransferFailedException( "Connection failed: " + e.getMessage(), e );
        }
        catch ( AuthenticationException e )
        {
            throw new TransferFailedException( "Authentication failed: " + e.getMessage(), e );
        }
        catch ( AuthorizationException e )
        {
            throw new TransferFailedException( "Authorization failed: " + e.getMessage(), e );
        }
        catch ( ResourceDoesNotExistException e )
        {
            throw new TransferFailedException( "Resource to deploy not found: " + e.getMessage(), e );
        }
        catch ( IOException e )
        {
            throw new TransferFailedException( "Error creating temporary file for deployment: " + e.getMessage(), e );
        }
        finally
        {
            // Remove every checksum listener
            for (String aCHECKSUM_IDS : CHECKSUM_IDS) {
                TransferListener checksumListener = checksums.get(aCHECKSUM_IDS);
                if (checksumListener != null) {
                    wagon.removeTransferListener(checksumListener);
                }
            }

            disconnectWagon( wagon );

            releaseWagon( protocol, wagon );
        }
    }

    private ChecksumObserver addChecksumObserver( Wagon wagon,
                                                  String algorithm )
        throws TransferFailedException
    {
        try
        {
            ChecksumObserver checksumObserver = new ChecksumObserver( algorithm );
            wagon.addTransferListener( checksumObserver );
            return checksumObserver;
        }
        catch ( NoSuchAlgorithmException e )
        {
            throw new TransferFailedException( "Unable to add checksum for unsupported algorithm " + algorithm, e );
        }
    }


    // NOTE: It is not possible that this method throws TransferFailedException under current conditions.
    // FIXME: Change the throws clause to reflect the fact that we're never throwing TransferFailedException
    public void getArtifact( Artifact artifact,
                             List<ArtifactRepository> remoteRepositories  )
        throws TransferFailedException, ResourceDoesNotExistException
	{
    	getArtifact( artifact, remoteRepositories, true );
	}

    public void getArtifact( Artifact artifact,
                             List<ArtifactRepository> remoteRepositories,
                             boolean force )
        throws TransferFailedException, ResourceDoesNotExistException
    {
        for (ArtifactRepository repository : remoteRepositories) {
            try
            {
                getArtifact( artifact, repository, force );

                if (artifact.isResolved())
                {
                	break;
                }
            }
            catch ( ResourceDoesNotExistException e )
            {
                // This one we will eat when looking through remote repositories
                // because we want to cycle through them all before squawking.

                getLogger().debug( "Unable to get resource '" + artifact.getId() + "' from repository " +
                    repository.getId() + " (" + repository.getUrl() + ")", e );
            }
            catch ( TransferFailedException e )
            {
                getLogger().debug( "Unable to get resource '" + artifact.getId() + "' from repository " +
                    repository.getId() + " (" + repository.getUrl() + ")", e );
            }
        }

        // if it already exists locally we were just trying to force it - ignore the update
        if ( !artifact.getFile().exists() )
        {
            throw new ResourceDoesNotExistException( "Unable to download the artifact from any repository" );
        }
    }

    public void getArtifact( Artifact artifact,
                             ArtifactRepository repository )
        throws TransferFailedException,
               ResourceDoesNotExistException
    {
        getArtifact( artifact, repository, true );
    }

    public void getArtifact( Artifact artifact,
                             ArtifactRepository repository,
                             boolean force )
        throws TransferFailedException, ResourceDoesNotExistException
    {
        String remotePath = repository.pathOf( artifact );

        ArtifactRepositoryPolicy policy = artifact.isSnapshot() ? repository.getSnapshots() : repository.getReleases();

        if ( !policy.isEnabled() )
        {
            getLogger().debug( "Skipping disabled repository " + repository.getId() );
        }
        else if ( repository.isBlacklisted() )
        {
            getLogger().debug( "Skipping blacklisted repository " + repository.getId() );
        }
        // If the artifact is a snapshot, we need to determine whether it's time to check this repository for an update:
        // 1. If it's forced, then check
        // 2. If the updateInterval has been exceeded since the last check for this artifact on this repository, then check.
        else if ( artifact.isSnapshot() && ( force || updateCheckManager.isUpdateRequired( artifact, repository ) ) )
        {
            getLogger().debug( "Trying repository " + repository.getId() );

            try
            {
                getRemoteFile( getMirrorRepository( repository ), artifact.getFile(), remotePath, downloadMonitor,
                               policy.getChecksumPolicy(), false );
            }
            finally
            {
            	updateCheckManager.touch( artifact, repository );
            }

            getLogger().debug( "  Artifact resolved" );

            artifact.setResolved( true );
        }

        // XXX: This is not really intended for the long term - unspecified POMs should be converted to failures
        //      meaning caching would be unnecessary. The code for this is here instead of the MavenMetadataSource
        //      to keep the logic related to update checks enclosed, and so to keep the rules reasonably consistent
        //      with release metadata
        else if ( "pom".equals( artifact.getType() ) && !artifact.getFile().exists() )
        {
            // if POM is not present locally, try and get it if it's forced, out of date, or has not been attempted yet  
            if ( force || updateCheckManager.isPomUpdateRequired( artifact, repository ) )
            {
                getLogger().debug( "Trying repository " + repository.getId() );

                try
                {
                    getRemoteFile( getMirrorRepository( repository ), artifact.getFile(), remotePath, downloadMonitor,
                                   policy.getChecksumPolicy(), false );
                }
                catch ( ResourceDoesNotExistException e )
                {
                    // cache the POM failure
                    updateCheckManager.touch( artifact, repository );
                    
                    throw e;
                }

                getLogger().debug( "  Artifact resolved" );

                artifact.setResolved( true );
            }
            else
            {
                // cached failure - pass on the failure
                throw new ResourceDoesNotExistException( "Failure was cached in the local repository" );
            }
        }
        
        // If it's not a snapshot artifact, then we don't care what the force flag says. If it's on the local
        // system, it's resolved. Releases are presumed to be immutable, so release artifacts are not ever updated.
        // NOTE: This is NOT the case for metadata files on relese-only repositories. This metadata may contain information
        // about successive releases, so it should be checked using the same updateInterval/force characteristics as snapshot
        // artifacts, above.

        // don't write touch-file for release artifacts.
        else if ( !artifact.isSnapshot() )
        {
            getLogger().debug( "Trying repository " + repository.getId() );

            getRemoteFile( getMirrorRepository( repository ), artifact.getFile(), remotePath, downloadMonitor, policy.getChecksumPolicy(), false );

            getLogger().debug( "  Artifact resolved" );

            artifact.setResolved( true );
        }
    }

    public void getArtifactMetadata( ArtifactMetadata metadata,
                                     ArtifactRepository repository,
                                     File destination,
                                     String checksumPolicy )
        throws TransferFailedException, ResourceDoesNotExistException
    {
        String remotePath = repository.pathOfRemoteRepositoryMetadata( metadata );

        getRemoteFile( getMirrorRepository( repository ), destination, remotePath, null, checksumPolicy, true );
    }

    public void getArtifactMetadataFromDeploymentRepository( ArtifactMetadata metadata, ArtifactRepository repository,
                                                             File destination, String checksumPolicy )
        throws TransferFailedException, ResourceDoesNotExistException
    {
        String remotePath = repository.pathOfRemoteRepositoryMetadata( metadata );

        getRemoteFile( repository, destination, remotePath, null, checksumPolicy, true );
    }

    private void getRemoteFile( ArtifactRepository repository,
                                File destination,
                                String remotePath,
                                TransferListener downloadMonitor,
                                String checksumPolicy,
                                boolean force )
        throws TransferFailedException, ResourceDoesNotExistException
    {
        // TODO: better excetpions - transfer failed is not enough?

        failIfNotOnline();

        String protocol = repository.getProtocol();

        Wagon wagon;

        try
        {
            wagon = getWagon( protocol );

            configureWagon( wagon, repository );
        }
        catch ( UnsupportedProtocolException e )
        {
            throw new TransferFailedException( "Unsupported Protocol: '" + protocol + "': " + e.getMessage(), e );
        }

        if ( downloadMonitor != null )
        {
            wagon.addTransferListener( downloadMonitor );
        }

        File temp = new File( destination + ".tmp" );

        temp.deleteOnExit();

        boolean downloaded = false;

        try
        {
            wagon.connect( new Repository( repository.getId(), repository.getUrl() ),
                           getAuthenticationInfo( repository.getId() ), new ProxyInfoProvider()
            {
                public ProxyInfo getProxyInfo( String protocol )
                {
                    return getProxy( protocol );
                }
            });

            boolean firstRun = true;
            boolean retry = true;

            // this will run at most twice. The first time, the firstRun flag is turned off, and if the retry flag
            // is set on the first run, it will be turned off and not re-set on the second try. This is because the
            // only way the retry flag can be set is if ( firstRun == true ).
            while ( firstRun || retry )
            {
                ChecksumObserver md5ChecksumObserver = null;
                ChecksumObserver sha1ChecksumObserver = null;
                try
                {
                    // TODO: configure on repository
                    int i = 0;

                    md5ChecksumObserver = addChecksumObserver( wagon, CHECKSUM_ALGORITHMS[i++] );
                    sha1ChecksumObserver = addChecksumObserver( wagon, CHECKSUM_ALGORITHMS[i++] );

                    // reset the retry flag.
                    retry = false;

                    // This should take care of creating destination directory now on
                    if ( destination.exists() && !force )
                    {
                        try
                        {
                            downloaded = wagon.getIfNewer( remotePath, temp, destination.lastModified() );

                            if ( !downloaded )
                            {
                                // prevent additional checks of this artifact until it expires again
                                destination.setLastModified( System.currentTimeMillis() );
                            }
                        }
                        catch ( UnsupportedOperationException e )
                        {
                            // older wagons throw this. Just get() instead
                            wagon.get( remotePath, temp );

                            downloaded = true;
                        }
                    }
                    else
                    {
                        wagon.get( remotePath, temp );
                        downloaded = true;
                    }
                }
                finally
                {
                    wagon.removeTransferListener( md5ChecksumObserver );
                    wagon.removeTransferListener( sha1ChecksumObserver );
                }

                if ( downloaded )
                {
                    // keep the checksum files from showing up on the download monitor...
                    if ( downloadMonitor != null )
                    {
                        wagon.removeTransferListener( downloadMonitor );
                    }

                    // try to verify the SHA-1 checksum for this file.
                    try
                    {
                        verifyChecksum( sha1ChecksumObserver, destination, temp, remotePath, ".sha1", wagon );
                    }
                    catch ( ChecksumFailedException e )
                    {
                        // if we catch a ChecksumFailedException, it means the transfer/read succeeded, but the checksum
                        // doesn't match. This could be a problem with the server (ibiblio HTTP-200 error page), so we'll
                        // try this up to two times. On the second try, we'll handle it as a bona-fide error, based on the
                        // repository's checksum checking policy.
                        if ( firstRun )
                        {
                            getLogger().warn( "*** CHECKSUM FAILED - " + e.getMessage() + " - RETRYING" );
                            retry = true;
                        }
                        else
                        {
                            handleChecksumFailure( checksumPolicy, e.getMessage(), e.getCause() );
                        }
                    }
                    catch ( ResourceDoesNotExistException sha1TryException )
                    {
                        getLogger().debug( "SHA1 not found, trying MD5", sha1TryException );

                        // if this IS NOT a ChecksumFailedException, it was a problem with transfer/read of the checksum
                        // file...we'll try again with the MD5 checksum.
                        try
                        {
                            verifyChecksum( md5ChecksumObserver, destination, temp, remotePath, ".md5", wagon );
                        }
                        catch ( ChecksumFailedException e )
                        {
                            // if we also fail to verify based on the MD5 checksum, and the checksum transfer/read
                            // succeeded, then we need to determine whether to retry or handle it as a failure.
                            if ( firstRun )
                            {
                                retry = true;
                            }
                            else
                            {
                                handleChecksumFailure( checksumPolicy, e.getMessage(), e.getCause() );
                            }
                        }
                        catch ( ResourceDoesNotExistException md5TryException )
                        {
                            // this was a failed transfer, and we don't want to retry.
                            handleChecksumFailure( checksumPolicy, "Error retrieving checksum file for " + remotePath,
                                md5TryException );
                        }
                    }

                    // reinstate the download monitor...
                    if ( downloadMonitor != null )
                    {
                        wagon.addTransferListener( downloadMonitor );
                    }
                }

                // unset the firstRun flag, so we don't get caught in an infinite loop...
                firstRun = false;
            }
        }
        catch ( ConnectionException e )
        {
            throw new TransferFailedException( "Connection failed: " + e.getMessage(), e );
        }
        catch ( AuthenticationException e )
        {
            throw new TransferFailedException( "Authentication failed: " + e.getMessage(), e );
        }
        catch ( AuthorizationException e )
        {
            throw new TransferFailedException( "Authorization failed: " + e.getMessage(), e );
        }
        catch ( CredentialsDataSourceException e )
        {
            throw new TransferFailedException( "Retrieving credentials failed: " + e.getMessage(), e );
        }
        finally
        {
            // Remove remaining TransferListener instances (checksum handlers removed in above finally clause)
            if ( downloadMonitor != null )
            {
                wagon.removeTransferListener( downloadMonitor );
            }

            disconnectWagon( wagon );

            releaseWagon( protocol, wagon );
        }

        if ( downloaded )
        {
            if ( !temp.exists() )
            {
                throw new ResourceDoesNotExistException( "Downloaded file does not exist: " + temp );
            }

            // The temporary file is named destination + ".tmp" and is done this way to ensure
            // that the temporary file is in the same file system as the destination because the
            // File.renameTo operation doesn't really work across file systems.
            // So we will attempt to do a File.renameTo for efficiency and atomicity, if this fails
            // then we will use a brute force copy and delete the temporary file.

            if ( !temp.renameTo( destination ) )
            {
                try
                {
                    FileUtils.copyFile( temp, destination );

                    temp.delete();
                }
                catch ( IOException e )
                {
                    throw new TransferFailedException(
                        "Error copying temporary file to the final destination: " + e.getMessage(), e );
                }
            }
        }
    }

    public ArtifactRepository getMirrorRepository( ArtifactRepository repository )
    {
        ArtifactRepository mirror = getMirror( repository );
        if ( mirror != null )
        {
            String id = mirror.getId();
            if ( id == null )
            {
                // TODO: this should be illegal in settings.xml
                id = repository.getId();
            }

            getLogger().info( "Using mirror: " + mirror.getId() + " for repository: " + repository.getId() + "\n(mirror url: " + mirror.getUrl() + ")" );
            repository = repositoryFactory.createArtifactRepository( id, mirror.getUrl(),
                                                                     repository.getLayout(), repository.getSnapshots(),
                                                                     repository.getReleases() );
        }
        return repository;
    }

    private void failIfNotOnline()
        throws TransferFailedException
    {
        if ( !isOnline() )
        {
            throw new TransferFailedException( "System is offline." );
        }
    }

    private void handleChecksumFailure( String checksumPolicy,
                                        String message,
                                        Throwable cause )
        throws ChecksumFailedException
    {
        if ( ArtifactRepositoryPolicy.CHECKSUM_POLICY_FAIL.equals( checksumPolicy ) )
        {
            throw new ChecksumFailedException( message, cause );
        }
        else if ( !ArtifactRepositoryPolicy.CHECKSUM_POLICY_IGNORE.equals( checksumPolicy ) )
        {
            // warn if it is set to anything other than ignore
            getLogger().warn( "*** CHECKSUM FAILED - " + message + " - IGNORING" );
        }
        // otherwise it is ignore
    }

    private void verifyChecksum( ChecksumObserver checksumObserver,
                                 File destination,
                                 File tempDestination,
                                 String remotePath,
                                 String checksumFileExtension,
                                 Wagon wagon )
        throws ResourceDoesNotExistException, TransferFailedException, AuthorizationException
    {
        try
        {
            // grab it first, because it's about to change...
            String actualChecksum = checksumObserver.getActualChecksum();

            File tempChecksumFile = new File( tempDestination + checksumFileExtension + ".tmp" );
            tempChecksumFile.deleteOnExit();
            wagon.get( remotePath + checksumFileExtension, tempChecksumFile );

            String expectedChecksum = FileUtils.fileRead( tempChecksumFile, "UTF-8" );

            // remove whitespaces at the end
            expectedChecksum = expectedChecksum.trim();

            // check for 'MD5 (name) = CHECKSUM'
            if ( expectedChecksum.startsWith( "MD5" ) )
            {
                int lastSpacePos = expectedChecksum.lastIndexOf( ' ' );
                expectedChecksum = expectedChecksum.substring( lastSpacePos + 1 );
            }
            else
            {
                // remove everything after the first space (if available)
                int spacePos = expectedChecksum.indexOf( ' ' );

                if ( spacePos != -1 )
                {
                    expectedChecksum = expectedChecksum.substring( 0, spacePos );
                }
            }
            if ( expectedChecksum.equalsIgnoreCase( actualChecksum ) )
            {
                File checksumFile = new File( destination + checksumFileExtension );
                if ( checksumFile.exists() )
                {
                    checksumFile.delete();
                }
                FileUtils.copyFile( tempChecksumFile, checksumFile );
                tempChecksumFile.delete();
            }
            else
            {
                throw new ChecksumFailedException( "Checksum failed on download: local = '" + actualChecksum +
                    "'; remote = '" + expectedChecksum + "'" );
            }
        }
        catch ( IOException e )
        {
            throw new ChecksumFailedException( "Invalid checksum file", e );
        }
    }


    private void disconnectWagon( Wagon wagon )
    {
        try
        {
            wagon.disconnect();
        }
        catch ( ConnectionException e )
        {
            getLogger().error( "Problem disconnecting from wagon - ignoring: " + e.getMessage() );
        }
    }

    private void releaseWagon( String protocol,
                               Wagon wagon )
    {
        try
        {
            container.release( wagon );
        }
        catch ( ComponentLifecycleException e )
        {
            getLogger().error( "Problem releasing wagon - ignoring: " + e.getMessage() );
            getLogger().debug( "", e );
        }
    }

    public ProxyInfo getProxy( String protocol )
    {
        return proxies.get( protocol );
    }

    public AuthenticationInfo getAuthenticationInfo( String id )
        throws CredentialsDataSourceException
    {
        return credentialsDataSource == null
            ? authenticationInfoMap.get( id )
            : credentialsDataSource.get( id );
    }

    /**
     * This method finds a matching mirror for the selected repository. If there is an exact match, this will be used.
     * If there is no exact match, then the list of mirrors is examined to see if a pattern applies.
     *
     * @param originalRepository See if there is a mirror for this repository.
     * @return the selected mirror or null if none are found.
     */
    public ArtifactRepository getMirror( ArtifactRepository originalRepository )
    {
        ArtifactRepository selectedMirror = mirrors.get( originalRepository.getId() );
        if ( null == selectedMirror )
        {
            // Process the patterns in order. First one that matches wins.
            Set<String> keySet = mirrors.keySet();
            if ( keySet != null )
            {
                for (String pattern : keySet) {
                    if (matchPattern(originalRepository, pattern)) {
                        selectedMirror = mirrors.get(pattern);
                    }
                }
            }

        }
        return selectedMirror;
    }

    /**
     * This method checks if the pattern matches the originalRepository.
     * Valid patterns:
     * * = everything
     * external:* = everything not on the localhost and not file based.
     * repo,repo1 = repo or repo1
     * *,!repo1 = everything except repo1
     *
     * @param originalRepository to compare for a match.
     * @param pattern used for match. Currently only '*' is supported.
     * @return true if the repository is a match to this pattern.
     */
    public boolean matchPattern( ArtifactRepository originalRepository, String pattern )
    {
        boolean result = false;
        String originalId = originalRepository.getId();

        // simple checks first to short circuit processing below.
        if ( WILDCARD.equals( pattern ) || pattern.equals( originalId ) )
        {
            result = true;
        }
        else
        {
            // process the list
            String[] repos = pattern.split( "," );
            for (String repo : repos) {
                // see if this is a negative match
                if (repo.length() > 1 && repo.startsWith("!")) {
                    if (originalId.equals(repo.substring(1))) {
                        // explicitly exclude. Set result and stop processing.
                        result = false;
                        break;
                    }
                }
                // check for exact match
                else if (originalId.equals(repo)) {
                    result = true;
                    break;
                }
                // check for external:*
                else if (EXTERNAL_WILDCARD.equals(repo) && isExternalRepo(originalRepository)) {
                    result = true;
                    // don't stop processing in case a future segment explicitly excludes this repo
                } else if (WILDCARD.equals(repo)) {
                    result = true;
                    // don't stop processing in case a future segment explicitly excludes this repo
                }
            }
        }
        return result;
    }

    /**
     * Checks the URL to see if this repository refers to an external repository
     *
     * @param originalRepository
     * @return true if external.
     */
    public boolean isExternalRepo( ArtifactRepository originalRepository )
    {
        try
        {
            URL url = new URL( originalRepository.getUrl() );
            return !( url.getHost().equals( "localhost" ) || url.getHost().equals( "127.0.0.1" ) || url.getProtocol().equals("file" ) );
        }
        catch ( MalformedURLException e )
        {
            // bad url just skip it here. It should have been validated already, but the wagon lookup will deal with it
            return false;
        }
    }

    /**
     * Set the proxy used for a particular protocol.
     *
     * @param protocol the protocol (required)
     * @param host the proxy host name (required)
     * @param port the proxy port (required)
     * @param username the username for the proxy, or null if there is none
     * @param password the password for the proxy, or null if there is none
     * @param nonProxyHosts the set of hosts not to use the proxy for. Follows Java system property format:
     *            <code>*.foo.com|localhost</code>.
     * @todo [BP] would be nice to configure this via plexus in some way
     */
    public void addProxy( String protocol,
                          String host,
                          int port,
                          String username,
                          String password,
                          String nonProxyHosts )
    {
        ProxyInfo proxyInfo = new ProxyInfo();
        proxyInfo.setHost( host );
        proxyInfo.setType( protocol );
        proxyInfo.setPort( port );
        proxyInfo.setNonProxyHosts( nonProxyHosts );
        proxyInfo.setUserName( username );
        proxyInfo.setPassword( password );

        proxies.put( protocol, proxyInfo );
    }

    public void contextualize( Context context )
        throws ContextException
    {
        container = (PlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );
    }

    /** @todo I'd rather not be setting this explicitly. */
    public void setDownloadMonitor( TransferListener downloadMonitor )
    {
        this.downloadMonitor = downloadMonitor;
    }

    // We are leaving this method here so that we can attempt to use the new maven-artifact
    // library from the 2.0.x code so that we aren't maintaining two lines of code
    // for the artifact management.
    public void addAuthenticationInfo( String repositoryId,
                                       String username,
                                       String password,
                                       String privateKey,
                                       String passphrase
    )
    {
        AuthenticationInfo authInfo = new AuthenticationInfo();
        authInfo.setUserName( username );
        authInfo.setPassword( password );
        authInfo.setPrivateKey( privateKey );
        authInfo.setPassphrase( passphrase );

        authenticationInfoMap.put( repositoryId, authInfo );
    }

    // This is the new way of handling authentication that will allow us to help users setup
    // authentication requirements.
    public void addAuthenticationCredentials( String repositoryId,
                                              String username,
                                              String password,
                                              String privateKey,
                                              String passphrase
    )
        throws CredentialsDataSourceException
    {
        AuthenticationInfo authInfo = new AuthenticationInfo();
        authInfo.setUserName( username );
        authInfo.setPassword( password );
        authInfo.setPrivateKey( privateKey );
        authInfo.setPassphrase( passphrase );

        if ( credentialsDataSource == null )
        {
            authenticationInfoMap.put( repositoryId, authInfo );
        }
        else
        {
            credentialsDataSource.set( new CredentialsChangeRequest( repositoryId, authInfo, null ) );
        }
    }

    public void addPermissionInfo( String repositoryId,
                                   String filePermissions,
                                   String directoryPermissions )
    {
        RepositoryPermissions permissions = new RepositoryPermissions();

        boolean addPermissions = false;

        if ( filePermissions != null )
        {
            permissions.setFileMode( filePermissions );
            addPermissions = true;
        }

        if ( directoryPermissions != null )
        {
            permissions.setDirectoryMode( directoryPermissions );
            addPermissions = true;
        }

        if ( addPermissions )
        {
            serverPermissionsMap.put( repositoryId, permissions );
        }
    }

    public void addMirror( String id,
                           String mirrorOf,
                           String url )
    {
        if ( id == null )
        {
            id = "mirror-" + anonymousMirrorIdSeed++;
            getLogger().warn( "You are using a mirror that doesn't declare an <id/> element. Using \'" + id + "\' instead:\nId: " + id + "\nmirrorOf: " + mirrorOf + "\nurl: " + url + "\n" );
        }
        
        ArtifactRepository mirror = new DefaultArtifactRepository( id, url, null );

        mirrors.put( mirrorOf, mirror );
    }

    public void setOnline( boolean online )
    {
        this.online = online;
    }

    public boolean isOnline()
    {
        return online;
    }

    public void setInteractive( boolean interactive )
    {
        this.interactive = interactive;
    }

    public void findAndRegisterWagons( PlexusContainer container )
    {
        try
        {
            Map wagons = container.lookupMap( Wagon.ROLE );

            registerWagons( wagons.keySet(), container );
        }
        catch ( ComponentLookupException e )
        {
            // no wagons found in the extension
        }
    }

    /** @deprecated Wagons are discovered in plugin and extension realms now. */
    @Deprecated
    public void registerWagons( Collection wagons,
                                PlexusContainer extensionContainer )
    {
    }

    /**
     * Applies the server configuration to the wagon
     *
     * @param wagon      the wagon to configure
     * @param repository the repository that has the configuration
     * @throws WagonConfigurationException wraps any error given during configuration of the wagon instance
     */
    private void configureWagon( Wagon wagon,
                                 ArtifactRepository repository )
        throws WagonConfigurationException
    {
        configureWagon( wagon, repository.getId(), repository.getProtocol() );
    }

    private void configureWagon( Wagon wagon,
                                 String repositoryId,
                                 String protocol )
        throws WagonConfigurationException
    {
        PlexusConfiguration config = (PlexusConfiguration) serverConfigurationMap.get( repositoryId ); 
        if ( protocol.startsWith( "http" ) || protocol.startsWith( "dav" ) )
        {
            config = updateUserAgentForHttp( wagon, config );
        }
        
        if ( config != null )
        {
            ComponentConfigurator componentConfigurator = null;

            try
            {
                componentConfigurator = new BasicComponentConfigurator();

                componentConfigurator.configureComponent( wagon, config, container.getContainerRealm() );
            }
            catch ( ComponentConfigurationException e )
            {
                throw new WagonConfigurationException( repositoryId, "Unable to apply wagon configuration.", e );
            }
            finally
            {
                if ( componentConfigurator != null )
                {
                    try
                    {
                        container.release( componentConfigurator );
                    }
                    catch ( ComponentLifecycleException e )
                    {
                        getLogger().error( "Problem releasing configurator - ignoring: " + e.getMessage() );
                    }
                }

            }
        }
    }

    // TODO: Remove this, once the maven-shade-plugin 1.2 release is out, allowing configuration of httpHeaders in the components.xml
    private PlexusConfiguration updateUserAgentForHttp( Wagon wagon, PlexusConfiguration config )
    {
        if ( config == null )
        {
            config = new XmlPlexusConfiguration( "configuration" );
        }
        
        if ( httpUserAgent != null )
        {
            try
            {
                wagon.getClass().getMethod( "setHttpHeaders", new Class[]{ Properties.class } );
                
                PlexusConfiguration headerConfig = config.getChild( "httpHeaders", true );
                PlexusConfiguration[] children = headerConfig.getChildren( "property" );
                boolean found = false;
                for ( int i = 0; i < children.length; i++ )
                {
                    PlexusConfiguration c = children[i].getChild( "name", false );
                    if ( c != null && "User-Agent".equals( c.getValue( null ) ) )
                    {
                        found = true;
                        break;
                    }
                }
                if ( !found )
                {
                    XmlPlexusConfiguration propertyConfig = new XmlPlexusConfiguration( "property" );
                    headerConfig.addChild( propertyConfig );
                    
                    XmlPlexusConfiguration nameConfig = new XmlPlexusConfiguration( "name" );
                    nameConfig.setValue( "User-Agent" );
                    propertyConfig.addChild( nameConfig );
                    
                    XmlPlexusConfiguration versionConfig = new XmlPlexusConfiguration( "value" );
                    versionConfig.setValue( httpUserAgent );
                    propertyConfig.addChild( versionConfig );
                }
            }
            catch ( SecurityException e )
            {
                // forget it. this method is public, if it exists.
            }
            catch ( NoSuchMethodException e )
            {
                // forget it.
            }
        }
        
        return config;
    }

    public void addConfiguration( String repositoryId,
                                  Xpp3Dom configuration )
    {
        if ( ( repositoryId == null ) || ( configuration == null ) )
        {
            throw new IllegalArgumentException( "arguments can't be null" );
        }

        final XmlPlexusConfiguration xmlConf = new XmlPlexusConfiguration( configuration );

        serverConfigurationMap.put( repositoryId, xmlConf );
    }

    public void setDefaultRepositoryPermissions( RepositoryPermissions defaultRepositoryPermissions )
    {
        this.defaultRepositoryPermissions = defaultRepositoryPermissions;
    }

    public void registerCredentialsDataSource( CredentialsDataSource cds )
    {
        credentialsDataSource = cds;
    }

    public void setUpdateCheckManager( UpdateCheckManager updateCheckManager )
    {
        this.updateCheckManager = updateCheckManager;        
    }

    // TODO: Remove this, once the maven-shade-plugin 1.2 release is out, allowing configuration of httpHeaders in the components.xml
    public void initialize()
        throws InitializationException
    {
        if ( httpUserAgent == null )
        {
            InputStream resourceAsStream = null;
            try
            {
                Properties properties = new Properties();
                resourceAsStream = getClass().getClassLoader().getResourceAsStream( MAVEN_ARTIFACT_PROPERTIES );

                if ( resourceAsStream != null )
                {
                    try
                    {
                        properties.load( resourceAsStream );

                        httpUserAgent =
                            "maven-artifact/" + properties.getProperty( "version" ) + " (Java "
                                + System.getProperty( "java.version" ) + "; " + System.getProperty( "os.name" ) + " "
                                + System.getProperty( "os.version" ) + ")";
                    }
                    catch ( IOException e )
                    {
                        getLogger().warn(
                                          "Failed to load Maven artifact properties from:\n" + MAVEN_ARTIFACT_PROPERTIES
                                              + "\n\nUser-Agent HTTP header may be incorrect for artifact resolution." );
                    }
                }
            }
            finally
            {
                IOUtil.close( resourceAsStream );
            }
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public void setHttpUserAgent( String userAgent )
    {
        this.httpUserAgent = userAgent;
    }
    
    /**
     * {@inheritDoc}
     */
    public String getHttpUserAgent()
    {
        return httpUserAgent;
    }
}
