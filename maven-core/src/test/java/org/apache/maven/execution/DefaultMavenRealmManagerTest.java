package org.apache.maven.execution;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.realm.DefaultMavenRealmManager;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.net.URL;
import java.util.Collections;

public class DefaultMavenRealmManagerTest
    extends PlexusTestCase
{

    private ArtifactFactory factory;

    protected void setUp() throws Exception
    {
        super.setUp();

        factory = lookup( ArtifactFactory.class );
    }

    public void test_ReuseSingleExtensionRealmFromMultipleProjectRealms_UsingTwoManagerInstances()
        throws Exception
    {
        ClassLoader cloader = Thread.currentThread().getContextClassLoader();
        URL jarResource = cloader.getResource( "org/apache/maven/execution/test-extension-1.jar" );

        Artifact ext1 = factory.createBuildArtifact( "org.group", "artifact-ext", "1", "jar" );
        Artifact ext2 = factory.createBuildArtifact( "org.group", "artifact-ext", "1", "jar" );

        assertNotSame( ext1, ext2 );

        ext1.setFile( FileUtils.toFile( jarResource ) );
        ext1.setResolved( true );

        ext2.setFile( FileUtils.toFile( jarResource ) );
        ext2.setResolved( true );

        Logger logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );
        DefaultMavenRealmManager mgr1 = new DefaultMavenRealmManager( getContainer(), logger );

        assertFalse( mgr1.hasExtensionRealm( ext1 ) );

        mgr1.createExtensionRealm( ext1, Collections.EMPTY_LIST );

        assertTrue( mgr1.hasExtensionRealm( ext1 ) );

        String pAid1 = "artifact-project1";
        String pAid2 = "artifact-project1";

        assertNotNull( ext1.getFile() );

        mgr1.importExtensionsIntoProjectRealm( "org.group", pAid1, "2", ext1 );

        ArtifactFactory result1 = getContainer().lookup( ArtifactFactory.class, "test" );

        assertNotNull( result1 );

        DefaultMavenRealmManager mgr2 = new DefaultMavenRealmManager( getContainer(), logger );

        assertNotNull( ext2.getFile() );

        assertTrue( mgr2.hasExtensionRealm( ext2 ) );

        // ext2 doesn't have a file associated with it, but it SHOULD succeed anyway.
        mgr2.importExtensionsIntoProjectRealm( "org.group", pAid2, "2", ext2 );

        ArtifactFactory result2 = getContainer().lookup( ArtifactFactory.class, "test" );

        assertNotNull( result2 );

        assertEquals( result1.getClass().getName(), result2.getClass().getName() );
    }
}
