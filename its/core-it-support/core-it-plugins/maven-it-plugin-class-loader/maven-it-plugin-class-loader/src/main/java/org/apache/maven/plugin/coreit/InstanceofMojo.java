package org.apache.maven.plugin.coreit;

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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Checks whether objects obtained from the Maven core are assignment-compatible with types loaded from the plugin class
 * loader. In other words, checks that types shared with the core realm are imported into the plugin realm.
 *
 * @author Benjamin Bentmann
 *
  */
@Mojo( name = "instanceof", defaultPhase = LifecyclePhase.INITIALIZE )
public class InstanceofMojo
    extends AbstractMojo
{

    /**
     * The path to the properties file used to track the results of the instanceof tests.
     */
    @Parameter( property = "clsldr.instanceofPropertiesFile" )
    private File instanceofPropertiesFile;

    /**
     * The qualified name of the type to which the objects should be assignment-compatible. This type will be loaded
     * from the plugin class loader, just like as if it was imported in the plugin source code.
     */
    @Parameter( property = "clsldr.className" )
    private String className;

    /**
     * A list of expressions that denote the object instances that should be type-checked.
     */
    @Parameter
    private String[] objectExpressions;

    /**
     * A list of injected component instances that should be type-checked.
     */
    @Component
    private List<TestComponent> components;

    /**
     * The current Maven project against which expressions are evaluated.
     */
    @Parameter( defaultValue = "${project}", readonly = true )
    private Object project;

    /**
     * Runs this mojo.
     *
     * @throws MojoExecutionException If the output file could not be created.
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        Class type;
        try
        {
            getLog().info( "[MAVEN-CORE-IT-LOG] Loading class " + className );
            type = getClass().getClassLoader().loadClass( className );
            getLog().info( "[MAVEN-CORE-IT-LOG] Loaded class from " + type.getClassLoader() );
        }
        catch ( ClassNotFoundException e )
        {
            throw new MojoExecutionException( "Failed to load type " + className, e );
        }

        Properties instanceofProperties = new Properties();

        if ( objectExpressions != null && objectExpressions.length > 0 )
        {
            Map contexts = new HashMap();
            contexts.put( "project", project );
            contexts.put( "pom", project );

            for ( String expression : objectExpressions )
            {
                getLog().info( "[MAVEN-CORE-IT-LOG] Evaluating expression " + expression );
                Object object = ExpressionUtil.evaluate( expression, contexts );
                getLog().info( "[MAVEN-CORE-IT-LOG] Checking object " + object );
                if ( object != null )
                {
                    getLog().info( "[MAVEN-CORE-IT-LOG]   Loaded class " + object.getClass().getName() );
                    getLog().info( "[MAVEN-CORE-IT-LOG]   Loaded class from " + object.getClass().getClassLoader() );
                }
                instanceofProperties.setProperty( expression.replace( '/', '.' ),
                                                  Boolean.toString( type.isInstance( object ) ) );
            }
        }

        if ( components != null && !components.isEmpty() )
        {
            for ( Object object : components )
            {
                getLog().info( "[MAVEN-CORE-IT-LOG] Checking component " + object );
                getLog().info( "[MAVEN-CORE-IT-LOG]   Loaded class " + object.getClass().getName() );
                getLog().info( "[MAVEN-CORE-IT-LOG]   Loaded class from " + object.getClass().getClassLoader() );
                instanceofProperties.setProperty( object.getClass().getName(),
                                                  Boolean.toString( type.isInstance( object ) ) );
            }
        }

        getLog().info( "[MAVEN-CORE-IT-LOG] Creating output file " + instanceofPropertiesFile );

        PropertiesUtil.write( instanceofPropertiesFile, instanceofProperties );

        getLog().info( "[MAVEN-CORE-IT-LOG] Created output file " + instanceofPropertiesFile );
    }

}
