package org.apache.maven.tools.plugin.generator;

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
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl </a>
 * @version $Id: AbstractGeneratorTestCase.java,v 1.1 2005/02/20 16:25:21
 *          jdcasey Exp $
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

        MojoDescriptor mojoDescriptor = new MojoDescriptor();
        mojoDescriptor.setGoal( "testGoal" );
        mojoDescriptor.setId( "test" );
        mojoDescriptor.setImplementation( "org.apache.maven.tools.plugin.generator.TestMojo" );
        mojoDescriptor.setRequiresDependencyResolution( "compile" );

        List params = new ArrayList();

        Parameter param = new Parameter();
        param.setDefaultValue( "value" );
        param.setExpression( "#project.build.directory" );
        param.setName( "dir" );
        param.setRequired( true );
        param.setType( "String" );
        param.setDescription( "Test parameter description" );

        params.add( param );

        mojoDescriptor.setParameters( params );

        Set descriptors = Collections.singleton( mojoDescriptor );

        Model model = new Model();

        model.setArtifactId( "maven-unitTesting-plugin" );

        Dependency dependency = new Dependency();
        dependency.setGroupId( "testGroup" );
        dependency.setArtifactId( "testArtifact" );
        dependency.setVersion( "0.0.0" );

        model.addDependency( dependency );

        MavenProject project = new MavenProject( model );

        generator.execute( destinationDirectory, descriptors, project );

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
            throw new Exception( "Cannot find " + generatorClassName +
                                 "! Make sure your test case is named in the form ${generatorClassName}Test " +
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