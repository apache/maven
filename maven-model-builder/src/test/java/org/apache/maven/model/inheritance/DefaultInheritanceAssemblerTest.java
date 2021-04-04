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

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;

import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;

import org.apache.maven.model.Model;
import org.apache.maven.model.building.AbstractModelSourceTransformer;
import org.apache.maven.model.building.SimpleProblemCollector;
import org.apache.maven.model.building.TransformerContext;
import org.apache.maven.model.io.DefaultModelReader;
import org.apache.maven.model.io.DefaultModelWriter;
import org.apache.maven.model.io.ModelWriter;
import org.apache.maven.model.transform.sax.AbstractSAXFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xmlunit.matchers.CompareMatcher;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Hervé Boutemy
 */
public class DefaultInheritanceAssemblerTest
{
    private DefaultModelReader reader;

    private ModelWriter writer;

    private InheritanceAssembler assembler;

    @BeforeEach
    public void setUp()
        throws Exception
    {
        reader = new DefaultModelReader();
        reader.setTransformer( new AbstractModelSourceTransformer()
        {
            @Override
            protected AbstractSAXFilter getSAXFilter( Path pomFile, TransformerContext context,
                                                      Consumer<LexicalHandler> lexicalHandlerConsumer )
                throws TransformerConfigurationException, SAXException, ParserConfigurationException
            {
                return null;
            }
        } );
        writer = new DefaultModelWriter();
        assembler = new DefaultInheritanceAssembler();
    }

    private File getPom( String name )
    {
        return new File( "src/test/resources/poms/inheritance/" + name + ".xml" );
    }

    private Model getModel( String name )
        throws IOException
    {
        return reader.read( getPom( name ), null );
    }

    @Test
    public void testPluginConfiguration()
        throws Exception
    {
        testInheritance( "plugin-configuration" );
    }

    /**
     * Check most classical urls inheritance: directory structure where parent POM in parent directory
     * and child directory == artifactId
     * @throws IOException Model read problem
     */
    @Test
    public void testUrls()
        throws Exception
    {
        testInheritance( "urls" );
    }

    /**
     * Flat directory structure: parent &amp; child POMs in sibling directories, child directory == artifactId.
     * @throws IOException Model read problem
     */
    @Test
    public void testFlatUrls()
        throws IOException
    {
        testInheritance( "flat-urls" );
    }

    /**
     * MNG-5951 MNG-6059 child.x.y.inherit.append.path="false" test
     * @throws Exception
     */
    @Test
    public void testNoAppendUrls()
        throws Exception
    {
        testInheritance( "no-append-urls" );
    }

    /**
     * MNG-5951 special case test: inherit with partial override
     * @throws Exception
     */
    @Test
    public void testNoAppendUrls2()
        throws Exception
    {
        testInheritance( "no-append-urls2" );
    }

    /**
     * MNG-5951 special case test: child.x.y.inherit.append.path="true" in child should not reset content
     * @throws Exception
     */
    @Test
    public void testNoAppendUrls3()
        throws Exception
    {
        testInheritance( "no-append-urls3" );
    }

    /**
     * Tricky case: flat directory structure, but child directory != artifactId.
     * Model interpolation does not give same result when calculated from build or from repo...
     * This is why MNG-5000 fix in code is marked as bad practice (uses file names)
     * @throws IOException Model read problem
     */
    @Test
    public void testFlatTrickyUrls()
        throws IOException
    {
        // parent references child with artifactId (which is not directory name)
        // then relative path calculation will fail during build from disk but success when calculated from repo
        try
        {
            // build from disk expected to fail
            testInheritance( "tricky-flat-artifactId-urls", false );
            //fail( "should have failed since module reference == artifactId != directory name" );
        }
        catch ( AssertionError afe )
        {
            // expected failure: wrong relative path calculation
            assertTrue( afe.getMessage().contains(
                                "Expected text value 'http://www.apache.org/path/to/parent/child-artifact-id/' but was " +
                                        "'http://www.apache.org/path/to/parent/../child-artifact-id/'" ),
                        afe.getMessage() );
        }
        // but ok from repo: local disk is ignored
        testInheritance( "tricky-flat-artifactId-urls", true );

        // parent references child with directory name (which is not artifact id)
        // then relative path calculation will success during build from disk but fail when calculated from repo
        testInheritance( "tricky-flat-directory-urls", false );

        AssertionError afe = assertThrows(
                AssertionError.class,
                () -> testInheritance( "tricky-flat-directory-urls", true ),
                "should have failed since module reference == directory name != artifactId" );
        // expected failure
        assertTrue( afe.getMessage().contains(
                                "Expected text value 'http://www.apache.org/path/to/parent/../child-artifact-id/' but was " +
                                        "'http://www.apache.org/path/to/parent/child-artifact-id/'" ),
                    afe.getMessage() );
    }

    @Test
    public void testWithEmptyUrl()
        throws IOException
    {
            testInheritance( "empty-urls", false );
    }

    public void testInheritance( String baseName )
        throws IOException
    {
        testInheritance( baseName, false );
        testInheritance( baseName, true );
    }

    public void testInheritance( String baseName, boolean fromRepo )
        throws IOException
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
        File actual = new File( "target/test-classes/poms/inheritance/" + baseName
            + ( fromRepo ? "-build" : "-repo" ) + "-actual.xml" );
        writer.write( actual, null, child );

        // check with getPom( baseName + "-expected" )
        File expected = getPom( baseName + "-expected" );

        assertThat( actual, CompareMatcher.isIdenticalTo( expected ).ignoreComments().ignoreWhitespace() );
    }

    @Test
    public void testModulePathNotArtifactId()
        throws IOException
    {
        Model parent = getModel( "module-path-not-artifactId-parent" );

        Model child = getModel( "module-path-not-artifactId-child" );

        SimpleProblemCollector problems = new SimpleProblemCollector();

        assembler.assembleModelInheritance( child, parent, null, problems );

        File actual = new File( "target/test-classes/poms/inheritance/module-path-not-artifactId-actual.xml" );

        writer.write( actual, null, child );

        // check with getPom( "module-path-not-artifactId-effective" )
        File expected = getPom( "module-path-not-artifactId-expected" );

        assertThat( actual, CompareMatcher.isIdenticalTo(expected).ignoreComments().ignoreWhitespace() );
    }
}
