package org.apache.maven;

import java.util.Collection;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.RepositoryRequest;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.project.MavenProject;

public interface ProjectDependenciesResolver
{
    public Set<Artifact> resolve( MavenProject project, Collection<String> scopes, RepositoryRequest repositoryRequest )
        throws ArtifactResolutionException, ArtifactNotFoundException;
}
