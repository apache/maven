package org.apache.maven.plugin.source;

import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.archiver.Archiver;

import java.io.File;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @version $Id$
 */
public class SourceBundlerTest
    extends PlexusTestCase
{
    public void testNormalProject()
        throws Exception
    {
        SourceBundler sourceBundler = new SourceBundler();

        Archiver archiver = (Archiver) lookup( Archiver.ROLE, "jar" );

        File outputFile = getTestFile( "target/source-bundler-test/normal.jar" );

        File sourceDirectories[] = {
            getTestFile( "src/test/projects/normal/src/main/java" ),
            getTestFile( "src/test/projects/normal/src/main/resources" ),
            getTestFile( "src/test/projects/normal/src/test/java" ),
            getTestFile( "src/test/projects/normal/src/test/resources" ),
        };

        if ( outputFile.exists() )
        {
            assertTrue( "Could not delete output file: " + outputFile.getAbsolutePath(), outputFile.delete() );
        }

        sourceBundler.makeSourceBundle( outputFile, sourceDirectories, archiver );

        assertTrue( "Missing output file: " + outputFile.getAbsolutePath(), outputFile.isFile() );
    }
}
