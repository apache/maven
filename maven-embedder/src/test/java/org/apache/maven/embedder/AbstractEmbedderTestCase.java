package org.apache.maven.embedder;

import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.FileUtils;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;

import java.io.File;
import java.util.List;
import java.util.Arrays;
import java.util.Iterator;

public abstract class AbstractEmbedderTestCase
    extends PlexusTestCase
{
    protected MavenEmbedder maven;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        Configuration configuration = new DefaultConfiguration()
            .setClassLoader( classLoader )
            .setMavenEmbedderLogger( new MavenEmbedderConsoleLogger() );

        maven = new MavenEmbedder( configuration );
    }

    protected void tearDown()
        throws Exception
    {
        maven.stop();
    }
}
