package org.apache.maven.plugin.resources;

/* ====================================================================
 *   Copyright 2001-2004 The Apache Software Foundation.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * ====================================================================
 */

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.PluginTestCase;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.embed.Embedder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;

/**
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public class ResourcesMojoTest
    extends PluginTestCase
{
    private static final String TEST_DIRECTORY = "target/tests/test-data";
    private static final String OUTPUT_DIRECTORY = "target/tests/output";

    private static final String RESOURCE_COPY_TEST_KEYSTORE = "resourceCopyTest.jks";
    
    private byte[] testFileData;
    
    public ResourcesMojoTest( String s )
    {
        super( s );
    }

    protected void setupPlugin()
        throws Exception
    {
        plugin = new ResourcesMojo();

        // have to mkdir CVS as it can't be in CVS!
        File f = new File( basedir, TEST_DIRECTORY + "/CVS" );
        f.mkdirs();
        f = new File( f, "Root" );
        PrintWriter w = new PrintWriter( new FileWriter( f ) );
        w.println( ":local:/cvs/root" );
        w.close();

        this.testFileData = "This is a test".getBytes("MS949");

        FileOutputStream outstream = new FileOutputStream( new File( basedir, TEST_DIRECTORY + "/test.bin" ) );
        outstream.write(testFileData);
        outstream.close();

        w = new PrintWriter( new FileWriter( new File( basedir, TEST_DIRECTORY + "/test.txt" ) ) );
        w.println( "test data" );
        w.close();

        // make sure the things in the output directory we test aren't there
        f = new File( basedir, OUTPUT_DIRECTORY + "/CVS/Root" );
        f.delete();
        f.getParentFile().delete();
        f = new File( basedir, OUTPUT_DIRECTORY + "/test.txt" );
        f.delete();
        f = new File( basedir, OUTPUT_DIRECTORY + "/test.bin" );
        f.delete();
        f.getParentFile().delete();
    }

    protected Map getTestParameters()
        throws Exception
    {
        Embedder embedder = new Embedder();

        //embedder.setClassLoader( Thread.currentThread().getContextClassLoader() );

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
        f = new File( directory, "test.bin" );
        assertTrue( "sanity check creation of binary file " + f, f.exists() );

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
        
        f = new File( basedir + "/" + OUTPUT_DIRECTORY, "test.bin" );
        assertTrue( "check creation of binary resource " + f, f.exists() );
        
        FileInputStream testInput = new FileInputStream(f);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int read = -1;
        byte[] buffer = new byte[512];
        
        while((read = testInput.read(buffer)) > -1)
        {
            baos.write(buffer, 0, read);
        }
        testInput.close();
        
        assertTrue(Arrays.equals(testFileData, baos.toByteArray()));
        fail("Sanity check!");
    }
}
