package org.apache.maven.artifact.router;

import java.util.Collection;

import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.resolution.ArtifactDescriptorRequest;
import org.sonatype.aether.resolution.ArtifactRequest;
import org.sonatype.aether.resolution.MetadataRequest;
import org.sonatype.aether.resolution.VersionRangeRequest;
import org.sonatype.aether.resolution.VersionRequest;

public interface RoutingRequestConverter
{

    void convert( final VersionRangeRequest request, final RepositorySystemSession session );

    void convert( VersionRequest request, RepositorySystemSession session );

    void convert( ArtifactDescriptorRequest request, RepositorySystemSession session );

    void convert( ArtifactRequest request, RepositorySystemSession session );

    void convert( Collection<? extends ArtifactRequest> requests, RepositorySystemSession session );

    Collection<? extends MetadataRequest> mapMetadataRequests( Collection<? extends MetadataRequest> requests,
                                                   RepositorySystemSession session );

}
