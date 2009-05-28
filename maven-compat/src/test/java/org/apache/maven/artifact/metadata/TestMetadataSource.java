package org.apache.maven.artifact.metadata;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

@Component(role = ArtifactMetadataSource.class)
public class TestMetadataSource
    implements ArtifactMetadataSource
{
    @Requirement
    private ArtifactFactory factory;
    
    public ResolutionGroup retrieve( Artifact artifact, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories )
        throws ArtifactMetadataRetrievalException
    {
        Set dependencies = new HashSet();

        if ( "g".equals( artifact.getArtifactId() ) )
        {
            Artifact a = null;
            try
            {
                a = factory.createBuildArtifact( "org.apache.maven", "h", "1.0", "jar" );
                dependencies.add( a );
            }
            catch ( Exception e )
            {
                throw new ArtifactMetadataRetrievalException( "Error retrieving metadata", e, a );
            }
        }
        
        if ( "i".equals( artifact.getArtifactId() ) )
        {
            Artifact a = null;
            try
            {
                a = factory.createBuildArtifact( "org.apache.maven", "j", "1.0-SNAPSHOT", "jar" );
                dependencies.add( a );
            }
            catch ( Exception e )
            {
                throw new ArtifactMetadataRetrievalException( "Error retrieving metadata", e, a );
            }
        }
        

        return new ResolutionGroup( artifact, dependencies, remoteRepositories );
    }

    public List<ArtifactVersion> retrieveAvailableVersions( Artifact artifact, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories )
        throws ArtifactMetadataRetrievalException
    {
        throw new UnsupportedOperationException( "Cannot get available versions in this test case" );
    }

    public List<ArtifactVersion> retrieveAvailableVersionsFromDeploymentRepository( Artifact artifact, ArtifactRepository localRepository, ArtifactRepository remoteRepository )
        throws ArtifactMetadataRetrievalException
    {
        throw new UnsupportedOperationException( "Cannot get available versions in this test case" );
    }

    public Artifact retrieveRelocatedArtifact( Artifact artifact, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories )
        throws ArtifactMetadataRetrievalException
    {
        return artifact;
    }
}
