package org.apache.maven.bootstrap.download;

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

import org.apache.maven.bootstrap.model.Dependency;
import org.apache.maven.bootstrap.model.Model;
import org.apache.maven.bootstrap.model.Repository;
import org.apache.maven.bootstrap.util.FileUtils;
import org.apache.maven.bootstrap.util.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class OnlineArtifactDownloader
    extends AbstractArtifactResolver
{
    public static final String SNAPSHOT_SIGNATURE = "-SNAPSHOT";

    private boolean useTimestamp = true;

    private boolean ignoreErrors = false;

    private String proxyHost;

    private String proxyPort;

    private String proxyUserName;

    private String proxyPassword;

    private static final String REPO_URL = "http://repo1.maven.org/maven2";

    private Map downloadedArtifacts = new HashMap();

    private List remoteRepositories;

    public OnlineArtifactDownloader( Repository localRepository )
        throws Exception
    {
        super( localRepository );
    }

    public void setProxy( String host, String port, String userName, String password )
    {
        proxyHost = host;
        proxyPort = port;
        proxyUserName = userName;
        proxyPassword = password;
        System.out.println( "Using the following proxy : " + proxyHost + "/" + proxyPort );
    }

    public void downloadDependencies( Collection dependencies )
        throws DownloadFailedException
    {
        for ( Iterator j = dependencies.iterator(); j.hasNext(); )
        {
            Dependency dep = (Dependency) j.next();

            if ( isAlreadyBuilt( dep ) )
            {
                continue;
            }

            String dependencyConflictId = dep.getDependencyConflictId();
            if ( !downloadedArtifacts.containsKey( dependencyConflictId ) )
            {
                File destinationFile = getLocalRepository().getArtifactFile( dep );
                // The directory structure for this project may
                // not exists so create it if missing.
                File directory = destinationFile.getParentFile();

                if ( !directory.exists() )
                {
                    directory.mkdirs();
                }

                if ( !getRemoteArtifact( dep, destinationFile ) && !destinationFile.exists() )
                {
                    throw new DownloadFailedException( dep );
                }

                downloadedArtifacts.put( dependencyConflictId, dep );
            }
            else
            {
                Dependency d = (Dependency) downloadedArtifacts.get( dependencyConflictId );
                dep.setResolvedVersion( d.getResolvedVersion() );
            }
        }
    }

    public boolean isOnline()
    {
        return true;
    }

    private static boolean isSnapshot( Dependency dep )
    {
        // Assume managed snapshot
        if ( dep == null || dep.getGroupId().startsWith( "org.apache.maven" ) )
        {
            if ( !dep.getArtifactId().equals( "maven-parent" ) )
            {
                return false;
            }
        }

        // Assume managed snapshot
        if ( dep.getVersion() == null )
        {
            return false;
        }

        return dep.getVersion().indexOf( SNAPSHOT_SIGNATURE ) >= 0;
    }

    private boolean getRemoteArtifact( Dependency dep, File destinationFile )
    {
        boolean fileFound = false;

        List repositories = new ArrayList();
        repositories.addAll( getRemoteRepositories() );
        repositories.addAll( dep.getRepositories() );

        for ( Iterator i = dep.getChain().iterator(); i.hasNext(); )
        {
            repositories.addAll( ( (Model) i.next() ).getRepositories() );
        }

        for ( Iterator i = repositories.iterator(); i.hasNext(); )
        {
            Repository remoteRepo = (Repository) i.next();

            boolean snapshot = isSnapshot( dep );
            if ( snapshot && !remoteRepo.isSnapshots() )
            {
                continue;
            }
            if ( !snapshot && !remoteRepo.isReleases() )
            {
                continue;
            }

            // The username and password parameters are not being used here.
            String url = remoteRepo.getBasedir() + "/" + remoteRepo.getArtifactPath( dep );

            // Attempt to retrieve the artifact and set the checksum if retrieval
            // of the checksum file was successful.
            try
            {
                String version = dep.getVersion();
                if ( snapshot )
                {
                    String filename = "maven-metadata-" + remoteRepo.getId() + ".xml";
                    File localFile = getLocalRepository().getMetadataFile( dep.getGroupId(), dep.getArtifactId(),
                                                                           dep.getVersion(), dep.getType(),
                                                                           "maven-metadata-local.xml" );
                    File remoteFile = getLocalRepository().getMetadataFile( dep.getGroupId(), dep.getArtifactId(),
                                                                            dep.getVersion(), dep.getType(), filename );
                    String metadataPath = remoteRepo.getMetadataPath( dep.getGroupId(), dep.getArtifactId(),
                                                                      dep.getVersion(), dep.getType(),
                                                                      "maven-metadata.xml" );
                    String metaUrl = remoteRepo.getBasedir() + "/" + metadataPath;
                    log( "Downloading " + metaUrl );
                    try
                    {
                        HttpUtils.getFile( metaUrl, remoteFile, ignoreErrors, true, proxyHost, proxyPort, proxyUserName,
                                           proxyPassword, false );
                    }
                    catch ( IOException e )
                    {
                        log( "WARNING: remote metadata version not found, using local: " + e.getMessage() );
                        remoteFile.delete();
                    }

                    File file = localFile;
                    if ( remoteFile.exists() )
                    {
                        if ( !localFile.exists() )
                        {
                            file = remoteFile;
                        }
                        else
                        {
                            RepositoryMetadata localMetadata = RepositoryMetadata.read( localFile );

                            RepositoryMetadata remoteMetadata = RepositoryMetadata.read( remoteFile );

                            if ( remoteMetadata.getLastUpdatedUtc() > localMetadata.getLastUpdatedUtc() )
                            {
                                file = remoteFile;
                            }
                            else
                            {
                                file = localFile;
                            }
                        }
                    }

                    if ( file.exists() )
                    {
                        log( "Using metadata: " + file );

                        RepositoryMetadata metadata = RepositoryMetadata.read( file );

                        if ( !file.equals( localFile ) )
                        {
                            version = metadata.constructVersion( version );
                        }
                        log( "Resolved version: " + version );
                        dep.setResolvedVersion( version );
                        if ( !version.endsWith( "SNAPSHOT" ) )
                        {
                            String ver =
                                version.substring( version.lastIndexOf( "-", version.lastIndexOf( "-" ) - 1 ) + 1 );
                            String extension = url.substring( url.length() - 4 );
                            url = getSnapshotMetadataFile( url, ver + extension );
                        }
                        else if ( destinationFile.exists() )
                        {
                            // It's already there
                            return true;
                        }
                    }
                }
                if ( !"pom".equals( dep.getType() ) )
                {
                    String name = dep.getArtifactId() + "-" + dep.getResolvedVersion() + ".pom";
                    File file = getLocalRepository().getMetadataFile( dep.getGroupId(), dep.getArtifactId(),
                                                                      dep.getVersion(), dep.getType(), name );

                    file.getParentFile().mkdirs();

                    if ( !file.exists() || version.indexOf( "SNAPSHOT" ) >= 0 )
                    {
                        String filename = dep.getArtifactId() + "-" + version + ".pom";
                        String metadataPath = remoteRepo.getMetadataPath( dep.getGroupId(), dep.getArtifactId(),
                                                                          dep.getVersion(), dep.getType(), filename );
                        String metaUrl = remoteRepo.getBasedir() + "/" + metadataPath;
                        log( "Downloading " + metaUrl );

                        try
                        {
                            HttpUtils.getFile( metaUrl, file, ignoreErrors, false, proxyHost, proxyPort, proxyUserName,
                                               proxyPassword, false );
                        }
                        catch ( IOException e )
                        {
                            log( "Couldn't find POM - ignoring: " + e.getMessage() );
                        }
                    }
                }

                destinationFile = getLocalRepository().getArtifactFile( dep );
                if ( !destinationFile.exists() )
                {
                    log( "Downloading " + url );
                    HttpUtils.getFile( url, destinationFile, ignoreErrors, useTimestamp, proxyHost, proxyPort,
                                       proxyUserName, proxyPassword, true );
                    if ( dep.getVersion().indexOf( "SNAPSHOT" ) >= 0 )
                    {
                        String name = StringUtils.replace( destinationFile.getName(), version, dep.getVersion() );
                        FileUtils.copyFile( destinationFile, new File( destinationFile.getParentFile(), name ) );
                    }
                }

                // Artifact was found, continue checking additional remote repos (if any)
                // in case there is a newer version (i.e. snapshots) in another repo
                fileFound = true;
            }
            catch ( FileNotFoundException e )
            {
                log( "Artifact not found at [" + url + "]" );
                // Ignore
            }
            catch ( Exception e )
            {
                // If there are additional remote repos, then ignore exception
                // as artifact may be found in another remote repo. If there
                // are no more remote repos to check and the artifact wasn't found in
                // a previous remote repo, then artifactFound is false indicating
                // that the artifact could not be found in any of the remote repos
                //
                // arguably, we need to give the user better control (another command-
                // line switch perhaps) of what to do in this case? Maven already has
                // a command-line switch to work in offline mode, but what about when
                // one of two or more remote repos is unavailable? There may be multiple
                // remote repos for redundancy, in which case you probably want the build
                // to continue. There may however be multiple remote repos because some
                // artifacts are on one, and some are on another. In this case, you may
                // want the build to break.
                //
                // print a warning, in any case, so user catches on to mistyped
                // hostnames, or other snafus
                log( "Error retrieving artifact from [" + url + "]: " + e );
            }
        }

        return fileFound;
    }

    private static String getSnapshotMetadataFile( String filename, String s )
    {
        int index = filename.lastIndexOf( "SNAPSHOT" );
        return filename.substring( 0, index ) + s;
    }

    private void log( String message )
    {
        System.out.println( message );
    }

    public List getRemoteRepositories()
    {
        if ( remoteRepositories == null )
        {
            remoteRepositories = new ArrayList();
        }

        if ( remoteRepositories.isEmpty() )
        {
            // TODO: use super POM?
            remoteRepositories.add( new Repository( "central", REPO_URL, Repository.LAYOUT_DEFAULT, false, true ) );
            // TODO: use maven root POM?
            remoteRepositories.add( new Repository( "apache.snapshots", "http://people.apache.org/repo/m2-snapshot-repository/",
                                                    Repository.LAYOUT_DEFAULT, true, false ) );
        }

        return remoteRepositories;
    }

    public void setRemoteRepositories( List remoteRepositories )
    {
        this.remoteRepositories = remoteRepositories;
    }
}
