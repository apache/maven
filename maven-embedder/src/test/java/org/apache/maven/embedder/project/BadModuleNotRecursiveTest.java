package org.apache.maven.embedder.project;

import org.apache.maven.embedder.Configuration;
import org.apache.maven.embedder.DefaultConfiguration;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.embedder.MavenEmbedderConsoleLogger;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;

import java.io.File;
import java.util.Iterator;

import junit.framework.TestCase;

public class BadModuleNotRecursiveTest
    extends TestCase
{
    public void test()
        throws Exception
    {
        Configuration configuration = new DefaultConfiguration();
        configuration.setMavenEmbedderLogger( new MavenEmbedderConsoleLogger() );
        MavenEmbedder embedder = new MavenEmbedder( configuration );

        File pom = new File( "src/test/projects/bad-module-non-recursive/pom.xml" ).getCanonicalFile();

        System.out.println( pom.getCanonicalFile() );

        DefaultMavenExecutionRequest request = new DefaultMavenExecutionRequest();
        request.setOffline( false );
        request.setUseReactor( false );
        request.setRecursive( false );
        //request.setLoggingLevel( Logger.LEVEL_DEBUG );
        request.setPom( pom );
        request.setBaseDirectory( pom.getParentFile() );
        MavenExecutionResult result = embedder.readProjectWithDependencies( request );
        MavenProject project = result.getProject();

        if ( result.hasExceptions() )
        {
            for ( Iterator it = result.getExceptions().iterator(); it.hasNext(); )
            {
                Exception ex = (Exception) it.next();
                ex.printStackTrace();
            }
        }

        assertNotNull( project );
    }
}
