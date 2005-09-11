package org.apache.maven.artifact.transform;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.metadata.LegacyArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.metadata.ArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataManager;
import org.apache.maven.artifact.repository.metadata.SnapshotArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Describes a version transformation during artifact resolution.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 * @todo try and refactor to remove abstract methods - not particular happy about current design
 */
public abstract class AbstractVersionTransformation
    extends AbstractLogEnabled
    implements ArtifactTransformation
{
    protected RepositoryMetadataManager repositoryMetadataManager;

    protected WagonManager wagonManager;

    /**
     * @todo remove in beta-2 - used for legacy handling
     */
    private static Set resolvedArtifactCache = new HashSet();

    protected String resolveVersion( Artifact artifact, ArtifactRepository localRepository, List remoteRepositories )
        throws ArtifactMetadataRetrievalException
    {
        // TODO: can we improve on this?
        ArtifactMetadata metadata;
        if ( !artifact.isSnapshot() || Artifact.LATEST_VERSION.equals( artifact.getBaseVersion() ) )
        {
            metadata = new ArtifactRepositoryMetadata( artifact );
        }
        else
        {
            metadata = new SnapshotArtifactRepositoryMetadata( artifact );
        }

        repositoryMetadataManager.resolve( metadata, remoteRepositories, localRepository );

/*
        // TODO: can this go directly into the manager? At least share with DefaultPluginMappingManager
        // TODO: use this, cache the output, select from that list instead of the next set
        Versioning versioning = new Versioning();
        for ( Iterator i = remoteRepositories.iterator(); i.hasNext(); )
        {
            ArtifactRepository repository = (ArtifactRepository) i.next();

            mergeVersioning( versioning, loadVersioningInformation( metadata, repository, localRepository ) );
        }
        mergeVersioning( versioning, loadVersioningInformation( metadata, localRepository, localRepository ) );

        String version = selectVersion( versioning, artifact.getVersion() );
*/
        Versioning versioning = null;
        for ( Iterator i = remoteRepositories.iterator(); i.hasNext(); )
        {
            ArtifactRepository repository = (ArtifactRepository) i.next();

            versioning = loadVersioningInformation( metadata, repository, localRepository, artifact );
            if ( versioning != null )
            {
                artifact.setRepository( repository );
                // TODO: merge instead (see above)
                break;
            }
        }
        Versioning v = loadVersioningInformation( metadata, localRepository, localRepository, artifact );
        if ( v != null )
        {
            versioning = v;
            // TODO: figure out way to avoid duplicated message
            if ( getLogger().isDebugEnabled() /*&& !alreadyResolved*/ )
            {
                // Locally installed file is newer, don't use the resolved version
                getLogger().debug( artifact.getArtifactId() + ": using locally installed snapshot" );
            }
        }

        String version = null;
        if ( versioning != null )
        {
            version = constructVersion( versioning, artifact.getBaseVersion() );
        }

        if ( version == null )
        {
            version = resolveLegacyVersion( artifact, localRepository, remoteRepositories );
            if ( version == null )
            {
                version = artifact.getBaseVersion();
            }
        }

        // TODO: also do this logging for other metadata?
        if ( getLogger().isDebugEnabled() )
        {
            if ( version != null && !version.equals( artifact.getBaseVersion() ) )
            {
                String message = artifact.getArtifactId() + ": resolved to version " + version;
                if ( artifact.getRepository() != null )
                {
                    message += " from repository " + artifact.getRepository().getId();
                }
                else
                {
                    message += " from local repository";
                }
                getLogger().debug( message );
            }
        }
        return version;
    }

    protected abstract String constructVersion( Versioning versioning, String baseVersion );

/* TODO
    private void mergeVersioning( Versioning dest, Versioning source )
    {
        // TODO: currently, it is first wins. We should probably compare the versions, or check timestamping?
        // This could also let us choose the newer of the locally installed version and the remotely built version
        if ( dest.getLatest() == null )
        {
            dest.setLatest( source.getLatest() );
        }
        if ( dest.getRelease() == null )
        {
            dest.setRelease( source.getRelease() );
        }
        if ( dest.getSnapshot() == null )
        {
            dest.setSnapshot( source.getSnapshot() );
        }
        for ( Iterator i = source.getVersions().iterator(); i.hasNext(); )
        {
            String version = (String) i.next();
            if ( !dest.getVersions().contains( version ) )
            {
                dest.getVersions().add( version );
            }
        }
    }
*/

    /**
     * @todo remove in beta-2 - used for legacy handling
     */
    private String resolveLegacyVersion( Artifact artifact, ArtifactRepository localRepository,
                                         List remoteRepositories )
        throws ArtifactMetadataRetrievalException
    {
        LegacyArtifactMetadata localMetadata = createLegacyMetadata( artifact );
        File f = new File( localRepository.getBasedir(),
                           localRepository.pathOfLocalRepositoryMetadata( localMetadata, null ) );
        if ( f.exists() )
        {
            try
            {
                localMetadata.readFromFile( f );
            }
            catch ( IOException e )
            {
                throw new ArtifactMetadataRetrievalException( "Error reading local metadata", e );
            }
        }

        boolean alreadyResolved = alreadyResolved( artifact );
        if ( !alreadyResolved )
        {
            boolean checkedUpdates = false;
            for ( Iterator i = remoteRepositories.iterator(); i.hasNext(); )
            {
                ArtifactRepository repository = (ArtifactRepository) i.next();

                ArtifactRepositoryPolicy policy = artifact.isSnapshot() ? repository.getSnapshots()
                    : repository.getReleases();

                if ( !policy.isEnabled() )
                {
                    getLogger().debug( "resolveMetaData: " + artifact.getId() + ": Skipping disabled repository " +
                        repository.getId() + " (" + repository.getUrl() + ")" );
                }
                else
                {
                    boolean checkForUpdates = policy.checkOutOfDate( localMetadata.getLastModified() );

                    if ( checkForUpdates )
                    {
                        checkedUpdates = true;

                        LegacyArtifactMetadata remoteMetadata;

                        try
                        {
                            remoteMetadata = createLegacyMetadata( artifact );

                            remoteMetadata.retrieveFromRemoteRepository( repository, wagonManager,
                                                                         policy.getChecksumPolicy() );

                            getLogger().warn( "Using old-style versioning metadata from remote repo for " + artifact );

                            int difference = remoteMetadata.compareTo( localMetadata );
                            if ( difference > 0 )
                            {
                                // remote is newer
                                artifact.setRepository( repository );

                                localMetadata = remoteMetadata;
                            }
                        }
                        catch ( ResourceDoesNotExistException e )
                        {
                            getLogger().debug( "resolveMetaData: Artifact version metadata for: " + artifact.getId() +
                                " could not be found on repository: " + repository.getId(), e );
                        }
                    }
                    else
                    {
                        getLogger().debug( "resolveMetaData: " + artifact.getId() + ": NOT checking for updates from " +
                            repository.getId() + " (" + repository.getUrl() + ")" );
                    }
                }
            }

            // touch the file if it was checked for updates, but don't create it if it did't exist to avoid
            // storing SNAPSHOT as the actual version which doesn't exist remotely.
            if ( checkedUpdates && localMetadata.getLastModified().getTime() > 0 )
            {
                localMetadata.storeInLocalRepository( localRepository );
            }

            resolvedArtifactCache.add( getCacheKey( artifact ) );
        }

        if ( artifact.getFile().exists() && !localMetadata.newerThanFile( artifact.getFile() ) )
        {
            if ( getLogger().isDebugEnabled() && !alreadyResolved )
            {
                // Locally installed file is newer, don't use the resolved version
                getLogger().debug( artifact.getArtifactId() + ": using locally installed snapshot" );
            }
            localMetadata = null;
        }
        return localMetadata != null ? localMetadata.constructVersion() : null;
    }

    /**
     * Select the version to use based on a merged versioning element.
     *
     * @param versioning the versioning element
     * @param defaultVersion the version to select if none is selected from versioning
     * @return the version selected
     */
//    protected abstract String selectVersion( Versioning versioning, String defaultVersion );
    protected abstract LegacyArtifactMetadata createLegacyMetadata( Artifact artifact );

    /**
     * @todo remove in beta-2 - used for legacy handling
     */
    private boolean alreadyResolved( Artifact artifact )
    {
        return resolvedArtifactCache.contains( getCacheKey( artifact ) );
    }

    /**
     * @todo remove in beta-2 - used for legacy handling
     */
    private static String getCacheKey( Artifact artifact )
    {
        // No type - one per POM
        return artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getBaseVersion();
    }

    protected Versioning loadVersioningInformation( ArtifactMetadata repoMetadata, ArtifactRepository remoteRepository,
                                                    ArtifactRepository localRepository, Artifact artifact )
        throws ArtifactMetadataRetrievalException
    {
        File metadataFile = new File( localRepository.getBasedir(),
                                      localRepository.pathOfLocalRepositoryMetadata( repoMetadata, remoteRepository ) );

        Versioning versioning = null;
        if ( metadataFile.exists() )
        {
            Metadata metadata = readMetadata( metadataFile );
            versioning = metadata.getVersioning();
        }
        return versioning;
    }

    /**
     * @todo share with DefaultPluginMappingManager.
     */
    private static Metadata readMetadata( File mappingFile )
        throws ArtifactMetadataRetrievalException
    {
        Metadata result;

        Reader fileReader = null;
        try
        {
            fileReader = new FileReader( mappingFile );

            MetadataXpp3Reader mappingReader = new MetadataXpp3Reader();

            result = mappingReader.read( fileReader );
        }
        catch ( FileNotFoundException e )
        {
            throw new ArtifactMetadataRetrievalException( "Cannot read version information from: " + mappingFile, e );
        }
        catch ( IOException e )
        {
            throw new ArtifactMetadataRetrievalException( "Cannot read version information from: " + mappingFile, e );
        }
        catch ( XmlPullParserException e )
        {
            throw new ArtifactMetadataRetrievalException( "Cannot parse version information from: " + mappingFile, e );
        }
        finally
        {
            IOUtil.close( fileReader );
        }
        return result;
    }
}
