package org.apache.maven.artifact.manager;

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
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.UnsupportedProtocolException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.events.TransferListener;

import java.io.File;
import java.util.List;

/**
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka </a>
 * @version $Id$
 */
public interface WagonManager
{
    String ROLE = WagonManager.class.getName();

    Wagon getWagon( String protocol )
        throws UnsupportedProtocolException;

    void getArtifact( Artifact artifact, List remoteRepositories, File destination )
        throws TransferFailedException, ResourceDoesNotExistException;

    public void getArtifact( Artifact artifact, ArtifactRepository repository, File destination )
        throws TransferFailedException, ResourceDoesNotExistException;

    void putArtifact( File source, Artifact artifact, ArtifactRepository deploymentRepository )
        throws TransferFailedException;

    public void putArtifactMetadata( File source, ArtifactMetadata artifactMetadata, ArtifactRepository repository )
        throws TransferFailedException;

    public void getArtifactMetadata( ArtifactMetadata metadata, ArtifactRepository remoteRepository, File destination )
        throws TransferFailedException, ResourceDoesNotExistException;

    void addProxy( String protocol, String host, int port, String username, String password, String nonProxyHosts );

    void addAuthenticationInfo( String repositoryId, String username, String password, String privateKey,
                                String passphrase );

    void addMirror( String id, String mirrorOf, String url );

    void setDownloadMonitor( TransferListener downloadMonitor );
}