package org.apache.maven.plugin;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.PluginTestCase;
import org.apache.maven.plugin.ResourcesPlugin;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.embed.Embedder;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;

/**
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public class ResourcesPluginTest
    extends PluginTestCase
{
    private static final String TEST_DIRECTORY = "target/tests/test-data";
    private static final String OUTPUT_DIRECTORY = "target/tests/output";

    public ResourcesPluginTest( String s )
    {
        super( s );
    }

    protected void setupPlugin()
        throws Exception
    {
        plugin = new ResourcesPlugin();

        // have to mkdir CVS as it can't be in CVS!
        File f = new File( basedir, TEST_DIRECTORY + "/CVS" );
        f.mkdirs();
        f = new File( f, "Root" );
        PrintWriter w = new PrintWriter( new FileWriter( f ) );
        w.println( ":local:/cvs/root" );
        w.close();

        w = new PrintWriter( new FileWriter( new File( basedir, TEST_DIRECTORY + "/test.txt" ) ) );
        w.println( "test data" );
        w.close();

        // make sure the things in the output directory we test aren't there
        f = new File( basedir, OUTPUT_DIRECTORY + "/CVS/Root" );
        f.delete();
        f.getParentFile().delete();
        f = new File( basedir, OUTPUT_DIRECTORY + "/test.txt" );
        f.delete();
        f.getParentFile().delete();
    }

    protected Map getTestParameters()
        throws Exception
    {
        Embedder embedder = new Embedder();

        embedder.setClassLoader( Thread.currentThread().getContextClassLoader() );

        embedder.start();

        MavenProjectBuilder builder = (MavenProjectBuilder) embedder.lookup( MavenProjectBuilder.ROLE );

        // TODO: here it would be much nicer to just use resources from some test project.xml file for
        //       testing the standard resources elements
        // MavenProject project = builder.build( new File( basedir, "project.xml" ) );

        Map parameters = new HashMap();

        File directory = new File( basedir, TEST_DIRECTORY );
        assertEquals( "sanity check name of directory " + directory, "test-data", directory.getName() );

        File f = new File( directory, "CVS/Root" );
        assertTrue( "sanity check creation of CVS file " + f, f.exists() );
        f = new File( directory, "test.txt" );
        assertTrue( "sanity check creation of file " + f, f.exists() );

        Resource r = new Resource();
        r.setDirectory( TEST_DIRECTORY );
        parameters.put( "resources", Collections.singletonList( r ) ); 

        f = new File( basedir, OUTPUT_DIRECTORY );
        assertFalse( "sanity check no output directory" + f, f.exists() );

        parameters.put( "outputDirectory", OUTPUT_DIRECTORY );

        return parameters;
    }

    protected void validatePluginExecution()
        throws Exception
    {
        File f = new File( basedir + "/" + OUTPUT_DIRECTORY, "CVS/Root" );
        assertFalse( "check no creation of CVS file " + f, f.exists() );
        assertFalse( "check no creation of CVS directory " + f, f.getParentFile().exists() );
        f = new File( basedir + "/" + OUTPUT_DIRECTORY, "test.txt" );
        assertTrue( "check creation of resource " + f, f.exists() );
    }
}
