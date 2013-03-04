package org.apache.maven.wrapper;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URI;

import org.apache.commons.io.FileUtils;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Zip;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Hans Dockter
 */
public class InstallerTest
{
    private File testDir = new File( "target/test-files/SystemPropertiesHandlerTest-" + System.currentTimeMillis() );

    private Installer install;

    private Downloader downloadMock;

    private PathAssembler pathAssemblerMock;

    private boolean downloadCalled;

    private File zip;

    private File distributionDir;

    private File zipStore;

    private File mavenHomeDir;

    private File zipDestination;

    private WrapperConfiguration configuration = new WrapperConfiguration();

    private Downloader download;

    private PathAssembler pathAssembler;

    private PathAssembler.LocalDistribution localDistribution;

    @Before
    public void setup()
        throws Exception
    {

        testDir.mkdirs();

        downloadCalled = false;
        configuration.setZipBase( PathAssembler.PROJECT_STRING );
        configuration.setZipPath( "someZipPath" );
        configuration.setDistributionBase( PathAssembler.MAVEN_USER_HOME_STRING );
        configuration.setDistributionPath( "someDistPath" );
        configuration.setDistribution( new URI( "http://server/maven-0.9.zip" ) );
        configuration.setAlwaysDownload( false );
        configuration.setAlwaysUnpack( false );
        distributionDir = new File( testDir, "someDistPath" );
        mavenHomeDir = new File( distributionDir, "maven-0.9" );
        zipStore = new File( testDir, "zips" );
        zipDestination = new File( zipStore, "maven-0.9.zip" );

        download = mock( Downloader.class );
        pathAssembler = mock( PathAssembler.class );
        localDistribution = mock( PathAssembler.LocalDistribution.class );

        when( localDistribution.getZipFile() ).thenReturn( zipDestination );
        when( localDistribution.getDistributionDir() ).thenReturn( distributionDir );
        when( pathAssembler.getDistribution( configuration ) ).thenReturn( localDistribution );

        install = new Installer( download, pathAssembler );

    }

    private void createTestZip( File zipDestination )
        throws Exception
    {
        File explodedZipDir = new File( testDir, "explodedZip" );
        explodedZipDir.mkdirs();
        zipDestination.getParentFile().mkdirs();
        File mavenScript = new File( explodedZipDir, "maven-0.9/bin/mvn" );
        mavenScript.getParentFile().mkdirs();
        FileUtils.write( mavenScript, "something" );

        zipTo( explodedZipDir, zipDestination );
    }

    public void testCreateDist()
        throws Exception
    {
        File homeDir = install.createDist( configuration );

        Assert.assertEquals( mavenHomeDir, homeDir );
        Assert.assertTrue( homeDir.isDirectory() );
        Assert.assertTrue( new File( homeDir, "bin/mvn" ).exists() );
        Assert.assertTrue( zipDestination.exists() );

        Assert.assertEquals( localDistribution, pathAssembler.getDistribution( configuration ) );
        Assert.assertEquals( distributionDir, localDistribution.getDistributionDir() );
        Assert.assertEquals( zipDestination, localDistribution.getZipFile() );

        // download.download(new URI("http://some/test"), distributionDir);
        // verify(download).download(new URI("http://some/test"), distributionDir);
    }

    @Test
    public void testCreateDistWithExistingDistribution()
        throws Exception
    {

        FileUtils.touch( zipDestination );
        mavenHomeDir.mkdirs();
        File someFile = new File( mavenHomeDir, "some-file" );
        FileUtils.touch( someFile );

        File homeDir = install.createDist( configuration );

        Assert.assertEquals( mavenHomeDir, homeDir );
        Assert.assertTrue( mavenHomeDir.isDirectory() );
        Assert.assertTrue( new File( homeDir, "some-file" ).exists() );
        Assert.assertTrue( zipDestination.exists() );

        Assert.assertEquals( localDistribution, pathAssembler.getDistribution( configuration ) );
        Assert.assertEquals( distributionDir, localDistribution.getDistributionDir() );
        Assert.assertEquals( zipDestination, localDistribution.getZipFile() );
    }

    @Test
    public void testCreateDistWithExistingDistAndZipAndAlwaysUnpackTrue()
        throws Exception
    {

        createTestZip( zipDestination );
        mavenHomeDir.mkdirs();
        File garbage = new File( mavenHomeDir, "garbage" );
        FileUtils.touch( garbage );
        configuration.setAlwaysUnpack( true );

        File homeDir = install.createDist( configuration );

        Assert.assertEquals( mavenHomeDir, homeDir );
        Assert.assertTrue( mavenHomeDir.isDirectory() );
        Assert.assertFalse( new File( homeDir, "garbage" ).exists() );
        Assert.assertTrue( zipDestination.exists() );

        Assert.assertEquals( localDistribution, pathAssembler.getDistribution( configuration ) );
        Assert.assertEquals( distributionDir, localDistribution.getDistributionDir() );
        Assert.assertEquals( zipDestination, localDistribution.getZipFile() );
    }

    @Test
    public void testCreateDistWithExistingZipAndDistAndAlwaysDownloadTrue()
        throws Exception
    {

        createTestZip( zipDestination );
        File garbage = new File( mavenHomeDir, "garbage" );
        FileUtils.touch( garbage );
        configuration.setAlwaysUnpack( true );

        File homeDir = install.createDist( configuration );

        Assert.assertEquals( mavenHomeDir, homeDir );
        Assert.assertTrue( mavenHomeDir.isDirectory() );
        Assert.assertTrue( new File( homeDir, "bin/mvn" ).exists() );
        Assert.assertFalse( new File( homeDir, "garbage" ).exists() );
        Assert.assertTrue( zipDestination.exists() );

        Assert.assertEquals( localDistribution, pathAssembler.getDistribution( configuration ) );
        Assert.assertEquals( distributionDir, localDistribution.getDistributionDir() );
        Assert.assertEquals( zipDestination, localDistribution.getZipFile() );

        // download.download(new URI("http://some/test"), distributionDir);
        // verify(download).download(new URI("http://some/test"), distributionDir);
    }

    public void zipTo( File directoryToZip, File zipFile )
    {
        Zip zip = new Zip();
        zip.setBasedir( directoryToZip );
        zip.setDestFile( zipFile );
        zip.setProject( new Project() );

        Zip.WhenEmpty whenEmpty = new Zip.WhenEmpty();
        whenEmpty.setValue( "create" );
        zip.setWhenempty( whenEmpty );
        zip.execute();
    }

}
