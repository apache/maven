package org.apache.maven.it;

import java.io.File;
import java.util.List;

import org.apache.maven.it.util.ResourceExtractor;

public class MavenITmng5783PluginDependencyFiltering
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng5783PluginDependencyFiltering()
    {
        super( "[3.0,)" );
    }

    public void testSLF4j()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-5783-plugin-dependency-filtering" );
        Verifier verifier = newVerifier( new File( testDir, "plugin" ).getAbsolutePath(), "remote" );
        verifier.executeGoal( "install" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier = newVerifier( new File( testDir, "slf4j" ).getAbsolutePath(), "remote" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        // Note that plugin dependencies always include plugin itself and plexus-utils

        List<String> dependencies = verifier.loadLines( "target/dependencies.txt", "UTF-8" );
        if ( matchesVersionRange( "(,3.7.0)" ) )
        {
            assertEquals( 3, dependencies.size() );
        }
        else
        {
            assertEquals( 2, dependencies.size() );
        }
        assertEquals( "mng-5783-plugin-dependency-filtering:mng-5783-plugin-dependency-filtering-plugin:maven-plugin:0.1",
                      dependencies.get( 0 ) );
        assertEquals( "org.slf4j:slf4j-api:jar:1.7.5", dependencies.get( 1 ) );
        if ( matchesVersionRange( "(,3.7.0)" ) )
        {
            assertEquals( "org.codehaus.plexus:plexus-utils:jar:1.1", dependencies.get( 2 ) );
        }
    }
}
