package org.apache.maven.plugin.source;

import java.io.File;

import org.codehaus.plexus.archiver.Archiver;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @version $Id$
 */
public class SourceBundler
{
    private final static String[] DEFAULT_INCLUDES = new String[]{
        "**/*",
    };

    private final static String[] DEFAULT_EXCLUDES = new String[]{
        "**/CVS/**",
        "**/.svn/**",
    };

    public void makeSourceBundle( File outputFile, File[] sourceDirectories, Archiver archiver )
        throws Exception
    {
        String[] includes = DEFAULT_INCLUDES;

        String[] excludes = DEFAULT_EXCLUDES;

        for ( int i = 0; i < sourceDirectories.length; i++ )
        {
            archiver.addDirectory( sourceDirectories[ i ], includes, excludes );
        }

        archiver.setDestFile( outputFile );

        archiver.createArchive();
    }
}
