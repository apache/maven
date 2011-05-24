/*
 *  Copyright (C) 2011 John Casey.
 *  
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *  
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.apache.maven.repository.internal;

import org.apache.maven.artifact.router.ArtifactRouter;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.spi.connector.ArtifactDownload;
import org.sonatype.aether.spi.connector.ArtifactUpload;
import org.sonatype.aether.spi.connector.MetadataDownload;
import org.sonatype.aether.spi.connector.MetadataUpload;
import org.sonatype.aether.spi.connector.RepositoryConnector;
import org.sonatype.aether.spi.connector.RepositoryConnectorFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public class RoutingRepositoryConnector
    implements RepositoryConnector
{

    private final ArtifactRouter router; 

    private final RepositorySystemSession session;
    
    private final RemoteRepository repository;
    
    private final List<RepositoryConnectorFactory> connectorFactories;
    
    private final List<RepositoryConnector> openConnectors = new ArrayList<RepositoryConnector>();

    public RoutingRepositoryConnector( ArtifactRouter router, RepositorySystemSession session, RemoteRepository repository,
                                       List<RepositoryConnectorFactory> connectorFactories )
    {
        this.router = router;
        this.session = session;
        this.repository = repository;
        this.connectorFactories = connectorFactories;
    }

    public void get( Collection<? extends ArtifactDownload> artifactDownloads,
                     Collection<? extends MetadataDownload> metadataDownloads )
    {
    }

    public void put( Collection<? extends ArtifactUpload> artifactUploads,
                     Collection<? extends MetadataUpload> metadataUploads )
    {
    }

    public void close()
    {
    }

}
