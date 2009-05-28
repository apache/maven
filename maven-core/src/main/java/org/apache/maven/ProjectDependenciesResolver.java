package org.apache.maven;

import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.project.MavenProject;

public interface ProjectDependenciesResolver
{
    public Set<Artifact> resolve( MavenProject project, String scope, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories )
        throws ArtifactResolutionException, ArtifactNotFoundException;
}
