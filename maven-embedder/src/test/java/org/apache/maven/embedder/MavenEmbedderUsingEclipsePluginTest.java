package org.apache.maven.embedder;

import java.io.File;

/** @author Jason van Zyl */
public class MavenEmbedderUsingEclipsePluginTest
    extends AbstractMavenEmbedderTestCase
{
    protected String getId()
    {
        return "eclipse-from-embedder";
    }

    public void testRunningEclipsePlugin()
        throws Exception
    {
        File basedir = runWithProject( "eclipse:eclipse" );

        assertFileExists( new File( basedir, ".classpath" ) );

        assertFileExists( new File( basedir, ".project" ) );
    }
}
