/**
 * 
 */
package org.apache.maven.project;

import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.metadata.ResolutionGroup;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.metadata.MetadataSource;
import org.apache.maven.project.artifact.MavenMetadataSource;
import org.codehaus.plexus.component.annotations.Component;

@Component(role=ArtifactMetadataSource.class,hint="classpath")
public class TestMetadataSource
    extends MavenMetadataSource
{
    @Override
    public ResolutionGroup retrieve( Artifact artifact, ArtifactRepository localRepository,
                                     List<ArtifactRepository> remoteRepositories )
        throws ArtifactMetadataRetrievalException
    {        
        ResolutionGroup rg = super.retrieve( artifact, localRepository, remoteRepositories );
        
        for ( Artifact a : rg.getArtifacts() )
        {
            a.setResolved( true );
        }
        
        return rg;
    }
}