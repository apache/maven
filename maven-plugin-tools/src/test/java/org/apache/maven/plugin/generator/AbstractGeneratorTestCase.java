package org.apache.maven.plugin.generator;

/*
 * Copyright 2001-2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import junit.framework.TestCase;

import java.io.File;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public abstract class AbstractGeneratorTestCase
    extends TestCase
{
    protected Generator generator;

    protected String basedir;

    protected void setUp()
        throws Exception
    {
        basedir = System.getProperty( "basedir" );
    }

    public void testGenerator()
        throws Exception
    {
        setupGenerator();

        String sourceDirectory = new File( basedir, "src/test/resources/source" ).getPath();

        String destinationDirectory = new File( basedir, "target" ).getPath();

        String pom = new File( basedir, "src/test/resources/source/pom.xml" ).getPath();

        generator.execute( sourceDirectory, destinationDirectory, pom );

        validate();
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    protected void setupGenerator()
        throws Exception
    {
        String generatorClassName = getClass().getName();

        generatorClassName = generatorClassName.substring( 0, generatorClassName.length() - 4 );

        try
        {
            Class generatorClass = Thread.currentThread().getContextClassLoader().loadClass( generatorClassName );

            generator = (Generator) generatorClass.newInstance();
        }
        catch ( Exception e )
        {
            throw new Exception(
                "Cannot find " + generatorClassName + "! Make sure your test case is named in the form ${generatorClassName}Test " +
                "or override the setupPlugin() method to instantiate the mojo yourself." );
        }
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    protected void validate()
        throws Exception
    {
        // empty
    }
}
