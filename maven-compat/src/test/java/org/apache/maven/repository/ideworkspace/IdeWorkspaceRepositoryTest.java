package org.apache.maven.repository.ideworkspace;

import java.io.File;
import java.util.ArrayList;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.repository.DelegatingLocalArtifactRepository;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.PlexusTestCase;

public class IdeWorkspaceRepositoryTest
    extends PlexusTestCase
{
    
    private RepositorySystem repositorySystem;
    private ArtifactRepository localRepository;

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        repositorySystem = lookup( RepositorySystem.class );
        localRepository = repositorySystem.createLocalRepository( new File( "target/IdeWorkspaceRepositoryTest" ).getCanonicalFile() );
    }
    
    @Override
    protected void tearDown()
        throws Exception
    {
        repositorySystem = null;
        localRepository = null;
        
        super.tearDown();
    }

    public void testResolveFromWorkspace()
        throws Exception
    {

        Artifact artifact =
            repositorySystem.createArtifact( TestIdeWorkspaceRepository.GROUP_ID, TestIdeWorkspaceRepository.ARTIFACT_ID,
                                   TestIdeWorkspaceRepository.VERSION, "jar" );

        ArtifactResolutionRequest request = new ArtifactResolutionRequest();
        request.setArtifact( artifact );
        request.setLocalRepository( localRepository );
        request.setRemoteRepositories( new ArrayList<ArtifactRepository>() );

        repositorySystem.resolve( request );

        assertTrue( artifact.isResolved() );
        assertEquals( TestIdeWorkspaceRepository.ARTIFACT_FILE, artifact.getFile() );
        assertSame( localRepository, request.getLocalRepository() );
    }

    public void testDelegatingLocalRepo()
        throws Exception
    {
        Artifact artifact =
            repositorySystem.createArtifact( TestIdeWorkspaceRepository.GROUP_ID, TestIdeWorkspaceRepository.ARTIFACT_ID,
                                   TestIdeWorkspaceRepository.VERSION, "jar" );
        
        DelegatingLocalArtifactRepository delegatingLocalArtifactRepository = new DelegatingLocalArtifactRepository( localRepository );

        ArtifactResolutionRequest request = new ArtifactResolutionRequest();
        request.setArtifact( artifact );
        request.setLocalRepository( delegatingLocalArtifactRepository );
        request.setRemoteRepositories( new ArrayList<ArtifactRepository>() );

        repositorySystem.resolve( request );

        assertTrue( artifact.isResolved() );
        assertEquals( TestIdeWorkspaceRepository.ARTIFACT_FILE, artifact.getFile() );
        
        // make sure we restore original repository
        assertSame( delegatingLocalArtifactRepository, request.getLocalRepository() );
        assertNull( delegatingLocalArtifactRepository.getIdeWorspace() );
    }
}
