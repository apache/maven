package org.apache.maven.artifact.resolver;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;

import java.util.List;
import java.util.Set;

/**
 * I want to use it for hidding the fact that sometime artifact must be
 * downloaded. I am just asking LocalRepository for given artifact and I don't
 * care if it is alredy there or how it will get there.
 * 
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka </a>
 * @version $Id$
 */
public interface ArtifactResolver
{
    static String ROLE = ArtifactResolver.class.getName();

    Artifact resolve( Artifact artifact, List remoteRepositories, ArtifactRepository localRepository )
        throws ArtifactResolutionException;

    ArtifactResolutionResult resolveTransitively( Artifact artifact, List remoteRepositories,
        ArtifactRepository localRepository, ArtifactMetadataSource source ) throws ArtifactResolutionException;

    Set resolve( Set artifacts, List remoteRepositories, ArtifactRepository localRepository )
        throws ArtifactResolutionException;

    ArtifactResolutionResult resolveTransitively( Set artifacts, List remoteRepositories,
        ArtifactRepository localRepository, ArtifactMetadataSource source ) throws ArtifactResolutionException;

    ArtifactResolutionResult resolveTransitively( Set artifacts, List remoteRepositories,
        ArtifactRepository localRepository, ArtifactMetadataSource source, ArtifactFilter filter )
        throws ArtifactResolutionException;

    void addArtifactRequestTransformation(
        org.apache.maven.artifact.resolver.transform.ArtifactRequestTransformation requestTransformation );

}