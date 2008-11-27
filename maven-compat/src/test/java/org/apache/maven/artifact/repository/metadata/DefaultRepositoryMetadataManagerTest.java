package org.apache.maven.artifact.repository.metadata;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Collections;
import java.util.Date;

import junit.framework.TestCase;

import org.apache.maven.artifact.manager.DefaultUpdateCheckManager;
import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.testutils.MockManager;
import org.apache.maven.artifact.testutils.TestFileManager;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.easymock.MockControl;

public class DefaultRepositoryMetadataManagerTest
    extends TestCase
{

    private MockManager mockManager = new MockManager();

    private TestFileManager testFileManager = new TestFileManager(
                                                                   "DefaultRepositoryMetadataManager.test.",
                                                                   "" );

    private MockControl wagonManagerCtl;

    private WagonManager wagonManager;

	private DefaultUpdateCheckManager updateCheckManager;

    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();

        wagonManagerCtl = MockControl.createControl( WagonManager.class );
        mockManager.add( wagonManagerCtl );

        wagonManager = (WagonManager) wagonManagerCtl.getMock();
        
        updateCheckManager = new DefaultUpdateCheckManager( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) );
    }

    @Override
    public void tearDown()
        throws Exception
    {
        testFileManager.cleanUp();

        super.tearDown();
    }

    public void testResolveAlways_MarkTouchfileOnResourceNotFoundException()
        throws RepositoryMetadataResolutionException, IOException, XmlPullParserException,
        ParseException, InterruptedException
    {
        Date start = new Date();

        // helps the lastUpdate interval be significantly different.
        Thread.sleep( 1000 );

        MockControl localRepoCtl = MockControl.createControl( ArtifactRepository.class );

        mockManager.add( localRepoCtl );

        ArtifactRepository localRepo = (ArtifactRepository) localRepoCtl.getMock();

        File dir = testFileManager.createTempDir();
        String filename = "metadata.xml";
        String path = "path/to/" + filename;

        localRepo.getBasedir();
        localRepoCtl.setReturnValue( dir.getAbsolutePath(), MockControl.ZERO_OR_MORE );

        localRepo.pathOfLocalRepositoryMetadata( null, null );
        localRepoCtl.setMatcher( MockControl.ALWAYS_MATCHER );
        localRepoCtl.setReturnValue( path, MockControl.ZERO_OR_MORE );

        localRepo.getId();
        localRepoCtl.setReturnValue( "local", MockControl.ZERO_OR_MORE );

        wagonManager.isOnline();
        wagonManagerCtl.setReturnValue( true, MockControl.ZERO_OR_MORE );

        try
        {
            wagonManager.getArtifactMetadataFromDeploymentRepository( null, null, null, null );
            wagonManagerCtl.setMatcher( MockControl.ALWAYS_MATCHER );
            wagonManagerCtl.setThrowable( new ResourceDoesNotExistException( "Test error" ) );
        }
        catch ( TransferFailedException e )
        {
            fail( "Should not happen during mock setup." );
        }
        catch ( ResourceDoesNotExistException e )
        {
            fail( "Should not happen during mock setup." );
        }

        MockControl metadataCtl = MockControl.createControl( RepositoryMetadata.class );
        mockManager.add( metadataCtl );

        RepositoryMetadata metadata = (RepositoryMetadata) metadataCtl.getMock();

        String groupId = "group";
        String artifactId = "artifact";
        String baseVersion = "1-SNAPSHOT";

        metadata.getGroupId();
        metadataCtl.setReturnValue( groupId, MockControl.ZERO_OR_MORE );

        metadata.getArtifactId();
        metadataCtl.setReturnValue( artifactId, MockControl.ZERO_OR_MORE );

        metadata.getBaseVersion();
        metadataCtl.setReturnValue( baseVersion, MockControl.ZERO_OR_MORE );

        mockManager.replayAll();

        Logger logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );

        new DefaultRepositoryMetadataManager( wagonManager, updateCheckManager, logger ).resolveAlways( metadata,
                                                                                                       localRepo,
                                                                                                       localRepo );

        // helps the lastUpdate interval be significantly different.
        Thread.sleep( 1000 );

        Date end = new Date();

        Date checkDate = updateCheckManager.readLastUpdated( metadata, localRepo, new File( dir, path ) );

        assertNotNull( checkDate );
        assertTrue( checkDate.after( start ) );
        assertTrue( checkDate.before( end ) );

        mockManager.verifyAll();
    }

    public void testResolveAlways_MarkTouchfileOnTransferFailedException()
        throws RepositoryMetadataResolutionException, IOException, XmlPullParserException,
        ParseException, InterruptedException
    {
        Date start = new Date();

        // helps the lastUpdate interval be significantly different.
        Thread.sleep( 1000 );

        MockControl localRepoCtl = MockControl.createControl( ArtifactRepository.class );

        mockManager.add( localRepoCtl );

        ArtifactRepository localRepo = (ArtifactRepository) localRepoCtl.getMock();

        File dir = testFileManager.createTempDir();
        String filename = "metadata.xml";
        String path = "path/to/" + filename;

        localRepo.getBasedir();
        localRepoCtl.setReturnValue( dir.getAbsolutePath(), MockControl.ZERO_OR_MORE );

        localRepo.pathOfLocalRepositoryMetadata( null, null );
        localRepoCtl.setMatcher( MockControl.ALWAYS_MATCHER );
        localRepoCtl.setReturnValue( path, MockControl.ZERO_OR_MORE );

        localRepo.getId();
        localRepoCtl.setReturnValue( "local", MockControl.ZERO_OR_MORE );

        localRepo.setBlacklisted( false );
        localRepoCtl.setVoidCallable( MockControl.ZERO_OR_MORE );

        wagonManager.isOnline();
        wagonManagerCtl.setReturnValue( true, MockControl.ZERO_OR_MORE );

        try
        {
            wagonManager.getArtifactMetadataFromDeploymentRepository( null, null, null, null );
            wagonManagerCtl.setMatcher( MockControl.ALWAYS_MATCHER );
            wagonManagerCtl.setThrowable( new TransferFailedException( "Test error" ) );
        }
        catch ( TransferFailedException e )
        {
            fail( "Should not happen during mock setup." );
        }
        catch ( ResourceDoesNotExistException e )
        {
            fail( "Should not happen during mock setup." );
        }

        MockControl metadataCtl = MockControl.createControl( RepositoryMetadata.class );
        mockManager.add( metadataCtl );

        RepositoryMetadata metadata = (RepositoryMetadata) metadataCtl.getMock();

        String groupId = "group";
        String artifactId = "artifact";
        String baseVersion = "1-SNAPSHOT";

        metadata.getGroupId();
        metadataCtl.setReturnValue( groupId, MockControl.ZERO_OR_MORE );

        metadata.getArtifactId();
        metadataCtl.setReturnValue( artifactId, MockControl.ZERO_OR_MORE );

        metadata.getBaseVersion();
        metadataCtl.setReturnValue( baseVersion, MockControl.ZERO_OR_MORE );

        mockManager.replayAll();

        Logger logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );

        try
        {
            new DefaultRepositoryMetadataManager( wagonManager, updateCheckManager, logger ).resolveAlways( metadata,
                                                                                                            localRepo,
                                                                                                            localRepo );
            fail( "Should have thrown an exception due to transfer failure" );
        }
        catch ( RepositoryMetadataResolutionException e )
        {
            assertTrue( true );
        }

        // helps the lastUpdate interval be significantly different.
        Thread.sleep( 1000 );

        Date end = new Date();

        Date checkDate = updateCheckManager.readLastUpdated( metadata, localRepo, new File( dir, path ) );

        assertNotNull( checkDate );
        assertTrue( checkDate.after( start ) );
        assertTrue( checkDate.before( end ) );

        mockManager.verifyAll();
    }

    public void testResolveAlways_MarkTouchfileOnSuccess()
        throws RepositoryMetadataResolutionException, IOException, XmlPullParserException,
        ParseException, InterruptedException
    {
        Date start = new Date();

        // helps the lastUpdate interval be significantly different.
        Thread.sleep( 1000 );

        MockControl localRepoCtl = MockControl.createControl( ArtifactRepository.class );

        mockManager.add( localRepoCtl );

        ArtifactRepository localRepo = (ArtifactRepository) localRepoCtl.getMock();

        File dir = testFileManager.createTempDir();
        String filename = "metadata.xml";
        String path = "path/to/" + filename;

        localRepo.getBasedir();
        localRepoCtl.setReturnValue( dir.getAbsolutePath(), MockControl.ZERO_OR_MORE );

        localRepo.pathOfLocalRepositoryMetadata( null, null );
        localRepoCtl.setMatcher( MockControl.ALWAYS_MATCHER );
        localRepoCtl.setReturnValue( path, MockControl.ZERO_OR_MORE );

        localRepo.getId();
        localRepoCtl.setReturnValue( "local", MockControl.ZERO_OR_MORE );

        wagonManager.isOnline();
        wagonManagerCtl.setReturnValue( true, MockControl.ZERO_OR_MORE );

        try
        {
            wagonManager.getArtifactMetadataFromDeploymentRepository( null, null, null, null );
            wagonManagerCtl.setMatcher( MockControl.ALWAYS_MATCHER );
            wagonManagerCtl.setVoidCallable();
        }
        catch ( TransferFailedException e )
        {
            fail( "Should not happen during mock setup." );
        }
        catch ( ResourceDoesNotExistException e )
        {
            fail( "Should not happen during mock setup." );
        }

        MockControl metadataCtl = MockControl.createControl( RepositoryMetadata.class );
        mockManager.add( metadataCtl );

        RepositoryMetadata metadata = (RepositoryMetadata) metadataCtl.getMock();

        String groupId = "group";
        String artifactId = "artifact";
        String baseVersion = "1-SNAPSHOT";

        metadata.getGroupId();
        metadataCtl.setReturnValue( groupId, MockControl.ZERO_OR_MORE );

        metadata.getArtifactId();
        metadataCtl.setReturnValue( artifactId, MockControl.ZERO_OR_MORE );

        metadata.getBaseVersion();
        metadataCtl.setReturnValue( baseVersion, MockControl.ZERO_OR_MORE );

        mockManager.replayAll();

        Logger logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );

        new DefaultRepositoryMetadataManager( wagonManager, updateCheckManager, logger ).resolveAlways( metadata,
                                                                                                        localRepo,
                                                                                                        localRepo );

        // helps the lastUpdate interval be significantly different.
        Thread.sleep( 1000 );

        Date end = new Date();

        Date checkDate = updateCheckManager.readLastUpdated( metadata, localRepo, new File( dir, path ) );

        assertNotNull( checkDate );
        assertTrue( checkDate.after( start ) );
        assertTrue( checkDate.before( end ) );

        mockManager.verifyAll();
    }

    public void testResolve_DontRecheckWhenTouchfileIsWritten()
        throws RepositoryMetadataResolutionException, IOException, XmlPullParserException,
        ParseException, InterruptedException
    {
        Date start = new Date();

        // helps the lastUpdate interval be significantly different.
        Thread.sleep( 1000 );

        MockControl localRepoCtl = MockControl.createControl( ArtifactRepository.class );
        mockManager.add( localRepoCtl );
        ArtifactRepository localRepo = (ArtifactRepository) localRepoCtl.getMock();

        MockControl remoteRepoCtl = MockControl.createControl( ArtifactRepository.class );
        mockManager.add( remoteRepoCtl );
        ArtifactRepository remoteRepo = (ArtifactRepository) remoteRepoCtl.getMock();

        String repoId = "remote";
        remoteRepo.getId();
        remoteRepoCtl.setReturnValue( repoId, MockControl.ZERO_OR_MORE );

        ArtifactRepositoryPolicy pol = new ArtifactRepositoryPolicy();
        pol.setEnabled( true );

        remoteRepo.getSnapshots();
        remoteRepoCtl.setReturnValue( pol, MockControl.ZERO_OR_MORE );

        remoteRepo.isBlacklisted();
        remoteRepoCtl.setReturnValue( false, MockControl.ZERO_OR_MORE );

        File dir = testFileManager.createTempDir();
        String filename = "metadata.xml";
        String path = "path/to/" + filename;

        localRepo.getBasedir();
        localRepoCtl.setReturnValue( dir.getAbsolutePath(), MockControl.ZERO_OR_MORE );

        localRepo.pathOfLocalRepositoryMetadata( null, null );
        localRepoCtl.setMatcher( MockControl.ALWAYS_MATCHER );
        localRepoCtl.setReturnValue( path, MockControl.ZERO_OR_MORE );

        localRepo.getId();
        localRepoCtl.setReturnValue( "local", MockControl.ZERO_OR_MORE );

        wagonManager.isOnline();
        wagonManagerCtl.setReturnValue( true, MockControl.ZERO_OR_MORE );

        try
        {
            wagonManager.getArtifactMetadata( null, null, null, null );
            wagonManagerCtl.setMatcher( MockControl.ALWAYS_MATCHER );
            wagonManagerCtl.setThrowable( new ResourceDoesNotExistException( "Test error" ), 1 );
        }
        catch ( TransferFailedException e )
        {
            fail( "Should not happen during mock setup." );
        }
        catch ( ResourceDoesNotExistException e )
        {
            fail( "Should not happen during mock setup." );
        }

        MockControl metadataCtl = MockControl.createControl( RepositoryMetadata.class );
        mockManager.add( metadataCtl );
        RepositoryMetadata metadata = (RepositoryMetadata) metadataCtl.getMock();

        metadata.isSnapshot();
        metadataCtl.setReturnValue( true, MockControl.ZERO_OR_MORE );

        String groupId = "group";
        String artifactId = "artifact";
        String baseVersion = "1-SNAPSHOT";

        metadata.getGroupId();
        metadataCtl.setReturnValue( groupId, MockControl.ZERO_OR_MORE );

        metadata.getArtifactId();
        metadataCtl.setReturnValue( artifactId, MockControl.ZERO_OR_MORE );

        metadata.getBaseVersion();
        metadataCtl.setReturnValue( baseVersion, MockControl.ZERO_OR_MORE );

        metadata.getKey();
        metadataCtl.setReturnValue( "Test metadata for: " + groupId + ":" + artifactId + ":" + baseVersion, MockControl.ZERO_OR_MORE );

        Metadata md = new Metadata();
        metadata.getMetadata();
        metadataCtl.setReturnValue( md, MockControl.ZERO_OR_MORE );

        metadata.setMetadata( null );
        metadataCtl.setMatcher( MockControl.ALWAYS_MATCHER );
        metadataCtl.setVoidCallable( MockControl.ZERO_OR_MORE );

        mockManager.replayAll();

        Logger logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );

        System.out.println( "Testing re-check proofing..." );

        RepositoryMetadataManager mgr = new DefaultRepositoryMetadataManager( wagonManager, updateCheckManager, logger );
        mgr.resolve( metadata, Collections.singletonList( remoteRepo ), localRepo );

        Date checkDate = updateCheckManager.readLastUpdated( metadata, remoteRepo, new File( dir, path ) );

        assertNotNull( checkDate );

        mgr.resolve( metadata, Collections.singletonList( remoteRepo ), localRepo );

        checkDate = updateCheckManager.readLastUpdated( metadata, remoteRepo, new File( dir, path ) );

        assertNotNull( checkDate );

        // helps the lastUpdate interval be significantly different.
        Thread.sleep( 1000 );

        Date end = new Date();

        assertTrue( checkDate.after( start ) );
        assertTrue( checkDate.before( end ) );

        mockManager.verifyAll();
    }

}
