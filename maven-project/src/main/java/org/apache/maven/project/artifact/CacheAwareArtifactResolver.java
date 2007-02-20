package org.apache.maven.project.artifact;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.DefaultArtifactResolver;
import org.apache.maven.context.BuildContextManager;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.build.ProjectBuildCache;

import java.io.File;
import java.util.List;

public class CacheAwareArtifactResolver
    extends DefaultArtifactResolver
{
    
    private ArtifactResolver delegate;
    
    private BuildContextManager buildContextManager;

    public void resolve( Artifact artifact, List remoteRepositories, ArtifactRepository localRepository )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        resolveFromCache( artifact );
        
        if ( !artifact.isResolved() )
        {
            delegate.resolve( artifact, remoteRepositories, localRepository );
        }
    }

    public void resolveAlways( Artifact artifact, List remoteRepositories, ArtifactRepository localRepository )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        resolveFromCache( artifact );
        
        if ( !artifact.isResolved() )
        {
            delegate.resolveAlways( artifact, remoteRepositories, localRepository );
        }
    }

    private void resolveFromCache( Artifact artifact )
    {
        ProjectBuildCache cache = ProjectBuildCache.read( buildContextManager );
        
        if ( "pom".equals( artifact.getType() ) )
        {
            File pomFile = cache.getCachedModelFile( artifact );
            
            if ( pomFile != null )
            {
                artifact.setFile( pomFile );
                artifact.setResolved( true );
            }
        }
        // currently, artifacts with classifiers are not really supported as the main project artifact...
        else if ( artifact.getClassifier() == null )
        {
            MavenProject project = cache.getCachedProject( artifact );
            ArtifactHandler handler = artifact.getArtifactHandler();
            
            if ( project != null && handler.getPackaging().equals( project.getPackaging() ) )
            {
                File projectArtifactFile = project.getArtifact().getFile();
                
                if ( projectArtifactFile != null )
                {
                    artifact.setFile( projectArtifactFile );
                    artifact.setResolved( true );
                }
            }
        }
    }

}
