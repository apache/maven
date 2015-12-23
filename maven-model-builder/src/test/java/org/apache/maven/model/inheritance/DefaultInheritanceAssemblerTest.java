package org.apache.maven.model.inheritance;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.model.Model;
import org.apache.maven.model.building.SimpleProblemCollector;
import org.apache.maven.model.io.ModelParseException;
import org.apache.maven.model.io.ModelReader;
import org.apache.maven.model.io.ModelWriter;
import org.codehaus.plexus.PlexusTestCase;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;

import junit.framework.AssertionFailedError;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 * @author Hervé Boutemy
 */
public class DefaultInheritanceAssemblerTest
    extends PlexusTestCase
{
    private ModelReader reader;

    private ModelWriter writer;

    private InheritanceAssembler assembler;

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        reader = lookup( ModelReader.class );
        writer = lookup( ModelWriter.class );
        assembler = lookup( InheritanceAssembler.class );
    }

    private File getPom( String name )
    {
        return getTestFile( "src/test/resources/poms/inheritance/" + name + ".xml" );
    }

    private Model getModel( String name )
        throws ModelParseException, IOException
    {
        return reader.read( getPom( name ), null );
    }

    public void testPluginConfiguration()
        throws Exception
    {
        testInheritance( "plugin-configuration" );
    }

    /**
     * Check most classical urls inheritance: directory structure where parent POM in parent directory
     * and child directory == artifatId
     * @throws Exception
     */
    public void testUrls()
        throws Exception
    {
        testInheritance( "urls" );
    }

    /**
     * Flat directory structure: parent & child POMs in sibling directories, child directory == artifactId.
     * @throws Exception
     */
    public void testFlatUrls()
        throws Exception
    {
        testInheritance( "flat-urls" );
    }

    /**
     * MNG-5951 child.inherit.append.path="false" test
     * @throws Exception
     */
    public void testNoAppendUrls()
        throws Exception
    {
        testInheritance( "no-append-urls" );
    }

    /**
     * Tricky case: flat directory structure, but child directory != artifactId.
     * Model interpolation does not give same result when calculated from build or from repo...
     * This is why MNG-5000 fix in code is marked as bad practice (uses file names)
     * @throws Exception
     */
    public void testFlatTrickyUrls()
        throws Exception
    {
        // parent references child with artifactId (which is not directory name)
        // then relative path calculation will fail during build from disk but success when calculated from repo
        try
        {
            // build from disk expected to fail
            testInheritance( "tricky-flat-artifactId-urls", false );
            //fail( "should have failed since module reference == artifactId != directory name" );
        }
        catch ( AssertionFailedError afe )
        {
            // expected failure: wrong relative path calculation
            assertTrue( afe.getMessage(),
                        afe.getMessage().contains( "http://www.apache.org/path/to/parent/child-artifact-id/" ) );
        }
        // but ok from repo: local disk is ignored
        testInheritance( "tricky-flat-artifactId-urls", true );

        // parent references child with directory name (which is not artifact id)
        // then relative path calculation will success during build from disk but failwhen calculated from repo
        testInheritance( "tricky-flat-directory-urls", false );
        try
        {
            testInheritance( "tricky-flat-directory-urls", true );
            fail( "should have failed since module reference == directory name != artifactId" );
        }
        catch ( AssertionFailedError afe )
        {
            // expected failure
            assertTrue( afe.getMessage(),
                        afe.getMessage().contains( "http://www.apache.org/path/to/parent/child-artifact-id/" ) );
        }
    }

    public void testWithEmptyUrl() 
        throws Exception
    {
        	testInheritance( "empty-urls", false );
    }
    
    public void testInheritance( String baseName )
        throws Exception
    {
        testInheritance( baseName, false );
        testInheritance( baseName, true );
    }

    public void testInheritance( String baseName, boolean fromRepo )
        throws Exception
    {
        Model parent = getModel( baseName + "-parent" );

        Model child = getModel( baseName + "-child" );

        if ( fromRepo )
        {
            // when model is read from repo, a stream is used, then pomFile == null
            // (has consequences in inheritance algorithm since getProjectDirectory() returns null)
            parent.setPomFile( null );
            child.setPomFile( null );
        }

        SimpleProblemCollector problems = new SimpleProblemCollector();

        assembler.assembleModelInheritance( child, parent, null, problems );

        // write baseName + "-actual"
        File actual = getTestFile( "target/test-classes/poms/inheritance/" + baseName
            + ( fromRepo ? "-build" : "-repo" ) + "-actual.xml" );
        writer.write( actual, null, child );

        // check with getPom( baseName + "-expected" )
        File expected = getPom( baseName + "-expected" );
        try ( Reader control = new InputStreamReader( new FileInputStream( expected ), "UTF-8" );
              Reader test = new InputStreamReader( new FileInputStream( actual ), "UTF-8" ) )
        {
            XMLUnit.setIgnoreComments( true );
            XMLUnit.setIgnoreWhitespace( true );
            XMLAssert.assertXMLEqual( control, test );
        }
    }    

    public void testModulePathNotArtifactId()
        throws Exception
    {
        Model parent = getModel( "module-path-not-artifactId-parent" );

        Model child = getModel( "module-path-not-artifactId-child" );

        SimpleProblemCollector problems = new SimpleProblemCollector();

        assembler.assembleModelInheritance( child, parent, null, problems );

        File actual = getTestFile( "target/test-classes/poms/inheritance/module-path-not-artifactId-actual.xml" );

        writer.write( actual, null, child );

        // check with getPom( "module-path-not-artifactId-effective" )
        File expected = getPom( "module-path-not-artifactId-expected" );
        try (Reader control = new InputStreamReader( new FileInputStream( expected ), "UTF-8" );
                        Reader test = new InputStreamReader( new FileInputStream( actual ), "UTF-8" ))
        {
            XMLUnit.setIgnoreComments( true );
            XMLUnit.setIgnoreWhitespace( true );
            XMLAssert.assertXMLEqual( control, test );
        }
    }
}
