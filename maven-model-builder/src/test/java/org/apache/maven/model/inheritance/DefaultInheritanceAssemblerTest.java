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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import org.apache.maven.model.Model;
import org.apache.maven.model.building.SimpleProblemCollector;
import org.apache.maven.model.io.ModelParseException;
import org.apache.maven.model.io.ModelReader;
import org.apache.maven.model.io.ModelWriter;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.IOUtil;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;

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
        Model parent = getModel( "plugin-configuration-parent" );

        Model child = getModel( "plugin-configuration-child" );

        SimpleProblemCollector problems = new SimpleProblemCollector();

        assembler.assembleModelInheritance( child, parent, null, problems );

        File actual = getTestFile( "target/test-classes/poms/inheritance/plugin-configuration-actual.xml" );

        writer.write( actual, null, child );

        // check with getPom( "plugin-configuration-effective" )
        Reader control = null;
        Reader test = null;
        try
        {
            File expected = getPom( "plugin-configuration-expected" );
            control = new InputStreamReader( new FileInputStream( expected ), "UTF-8" );

            test = new InputStreamReader( new FileInputStream( actual ), "UTF-8" );

            XMLUnit.setIgnoreComments( true );
            XMLUnit.setIgnoreWhitespace( true );
            XMLAssert.assertXMLEqual( control, test );
        }
        catch ( IOException ioe )
        {
            IOUtil.close( control );
            IOUtil.close( test );
        }
    }

}
