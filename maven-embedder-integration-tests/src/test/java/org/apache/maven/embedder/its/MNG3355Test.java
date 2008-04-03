package org.apache.maven.embedder.its;

import org.apache.maven.embedder.Configuration;
import org.apache.maven.embedder.DefaultConfiguration;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.embedder.MavenEmbedderConsoleLogger;
import org.apache.maven.embedder.MavenEmbedderLogger;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

public class MNG3355Test
    extends PlexusTestCase
{
    protected String basedir;

    protected MavenEmbedder maven;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        basedir = System.getProperty( "basedir" );

        if ( basedir == null )
        {
            basedir = new File( "." ).getCanonicalPath();
        }

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

    protected void assertNoExceptions( MavenExecutionResult result )
    {
        List exceptions = result.getExceptions();
        if ( ( exceptions == null ) || exceptions.isEmpty() )
        {
            // everything is a-ok.
            return;
        }

        System.err.println( "Encountered " + exceptions.size() + " exception(s)." );
        Iterator it = exceptions.iterator();
        while ( it.hasNext() )
        {
            Exception exception = (Exception) it.next();
            exception.printStackTrace( System.err );
        }

        fail( "Encountered Exceptions in MavenExecutionResult during " + getName() );
    }

    public void testMNG_3355()
        throws Exception
    {
        File targetDirectory = getProjectDirectory( "mng-3355" );

        List goals = new ArrayList();

        goals.add( "clean" );
        goals.add( "validate" );

        Properties userProperties = new Properties();
        userProperties.setProperty( "version", "foo" );

        MavenExecutionRequest request = new DefaultMavenExecutionRequest().setBaseDirectory( targetDirectory )
                                                                          .setLoggingLevel( MavenEmbedderLogger.LEVEL_DEBUG )
                                                                          .setUserProperties( userProperties )
                                                                          .setGoals( goals );

        MavenExecutionResult result = maven.execute( request );

        assertNoExceptions( result );

//        MavenProject project = result.getProject();
    }

    private File getProjectDirectory( String projectPath )
        throws IOException
    {
        File testDirectory = new File( basedir, "src/test/projects/" + projectPath );

        File targetDirectory = new File( basedir, "target/projects/" + projectPath );

        FileUtils.copyDirectoryStructure( testDirectory, targetDirectory );

        return targetDirectory;
    }

}
