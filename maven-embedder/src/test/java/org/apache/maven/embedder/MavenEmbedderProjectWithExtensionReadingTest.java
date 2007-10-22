package org.apache.maven.embedder;

import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.PluginManagerException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.artifact.handler.ArtifactHandler;

import java.io.File;
import java.util.Map;
import java.util.HashMap;

/** @author Jason van Zyl */
public class MavenEmbedderProjectWithExtensionReadingTest
    extends MavenEmbedderTest
{
    public void testProjectWithExtensionsReading()
        throws Exception
    {
        MavenExecutionRequest request = new DefaultMavenExecutionRequest().setShowErrors( true )
            .setPomFile( new File( basedir, "src/test/resources/pom2.xml" ).getAbsolutePath() );

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        MavenExecutionResult result = new ExtendableMavenEmbedder( classLoader ).readProjectWithDependencies( request );

        assertNoExceptions( result );

        // sources, test sources, and the junit jar..

        assertEquals( 3, result.getProject().getTestClasspathElements().size() );
    }

    private class ExtendableMavenEmbedder
        extends MavenEmbedder
    {

        public ExtendableMavenEmbedder( ClassLoader classLoader )
            throws MavenEmbedderException
        {
            super( new DefaultConfiguration()
                .setClassLoader( classLoader )
                .setMavenEmbedderLogger( new MavenEmbedderConsoleLogger() ) );
        }

        protected Map getPluginExtensionComponents( Plugin plugin )
            throws PluginManagerException
        {
            Map toReturn = new HashMap();

            MyArtifactHandler handler = new MyArtifactHandler();

            toReturn.put( "mkleint", handler );

            return toReturn;
        }

        protected void verifyPlugin( Plugin plugin,
                                     MavenProject project )
        {
            //ignore don't want to actually verify in test
        }
    }

    private class MyArtifactHandler
        implements ArtifactHandler
    {

        public String getExtension()
        {
            return "jar";
        }

        public String getDirectory()
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        public String getClassifier()
        {
            return null;
        }

        public String getPackaging()
        {
            return "mkleint";
        }

        public boolean isIncludesDependencies()
        {
            return false;
        }

        public String getLanguage()
        {
            return "java";
        }

        public boolean isAddedToClasspath()
        {
            return true;
        }
    }    
}
