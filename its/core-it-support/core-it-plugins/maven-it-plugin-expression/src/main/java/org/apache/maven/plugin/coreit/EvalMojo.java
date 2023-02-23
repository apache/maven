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
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Creates a properties file with the effective values of some user-defined expressions. Unlike Maven's built-in
 * expression syntax for interpolation, these expressions use forward slashes to navigate down the object graph and
 * support access to individual collection/array elements. Furthermore, the result of an expression need not be a scalar
 * value but can also be a collection/array or a bean-like object (from the Maven model). For example, the expression
 * "project/dependencies/0" would extract the first project dependency. In more detail, this example expression could
 * output the following keys to the properties file:
 *
 * <pre>
 * project.dependencies.0.groupId = org.apache.maven
 * project.dependencies.0.artifactId = maven-project
 * project.dependencies.0.type = jar
 * project.dependencies.0.version = 2.0
 * project.dependencies.0.optional = false
 * project.dependencies.0.exclusions = 2
 * project.dependencies.0.exclusions.0.groupId = plexus
 * project.dependencies.0.exclusions.0.artifactId = plexus-utils
 * project.dependencies.0.exclusions.1.groupId = plexus
 * project.dependencies.0.exclusions.1.artifactId = plexus-container-default
 * </pre>
 *
 * Expressions that reference non-existing objects or use invalid collection/array indices silently resolve to
 * <code>null</code>. For collections and arrays, the special index "*" can be used to iterate all elements.
 *
 * @author Benjamin Bentmann
 *
  */
@Mojo( name = "eval", defaultPhase = LifecyclePhase.INITIALIZE )
public class EvalMojo
    extends AbstractMojo
{

    /**
     * The project's base directory, used for manual path translation.
     */
    @Parameter( defaultValue = "${basedir", readonly = true )
    private File basedir;

    /**
     * The path to the output file for the properties with the expression values. For each expression given by the
     * parameter {@link #expressions}, a similar named properties key will be used to save the expression value. If an
     * expression evaluated to <code>null</code>, there will be no corresponding key in the properties file.
     */
    @Parameter( property = "expression.outputFile" )
    private File outputFile;

    /**
     * The set of expressions to evaluate.
     */
    @Parameter
    private String[] expressions;

    /**
     * The comma separated set of expressions to evaluate.
     */
    @Parameter( property = "expression.expressions" )
    private String expressionList;

    /**
     * The current Maven project against which expressions are evaluated.
     */
    @Parameter( defaultValue = "${project}", readonly = true )
    private Object project;

    /**
     * The forked Maven project against which expressions are evaluated.
     */
    @Parameter( defaultValue = "${executedProject}", readonly = true )
    private Object executedProject;

    /**
     * The merged user/global settings of the current build against which expressions are evaluated.
     */
    @Parameter( defaultValue = "${settings}", readonly = true )
    private Object settings;

    /**
     * The session context of the current build against which expressions are evaluated.
     */
    @Parameter( defaultValue = "${session}", readonly = true )
    private Object session;

    /**
     * The local repository of the current build against which expressions are evaluated.
     */
    @Parameter( defaultValue = "${session.request.localRepositoryPath}", readonly = true )
    private Object localRepositoryBasedir;

    /**
     * Runs this mojo.
     *
     * @throws MojoExecutionException If the output file could not be created.
     * @throws MojoFailureException   If the output file has not been set.
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( outputFile == null )
        {
            throw new MojoFailureException( "Path name for output file has not been specified" );
        }

        /*
         * NOTE: We don't want to test path translation here.
         */
        if ( !outputFile.isAbsolute() )
        {
            outputFile = new File( basedir, outputFile.getPath() ).getAbsoluteFile();
        }

        getLog().info( "[MAVEN-CORE-IT-LOG] Creating output file: " + outputFile );

        Properties expressionProperties = new Properties();

        if ( expressionList != null && expressionList.length() > 0 )
        {
            expressions = expressionList.split( "," );
        }
        if ( expressions != null && expressions.length > 0 )
        {
            Map contexts = new HashMap();
            contexts.put( "project", project );
            contexts.put( "executedProject", executedProject );
            contexts.put( "pom", project );
            contexts.put( "settings", settings );
            contexts.put( "session", session );
            contexts.put( "localRepositoryBasedir", localRepositoryBasedir );

            for ( String expression : expressions )
            {
                Map values = ExpressionUtil.evaluate( expression, contexts );
                for ( Object key : values.keySet() )
                {
                    Object value = values.get( key );
                    PropertyUtil.store( expressionProperties, key.toString().replace( '/', '.' ), value );
                }
            }
        }

        try
        {
            PropertyUtil.write( expressionProperties, outputFile );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Output file could not be created: " + outputFile, e );
        }

        getLog().info( "[MAVEN-CORE-IT-LOG] Created output file: " + outputFile );
    }

    public void setOutputFile( File outputFile )
    {
        this.outputFile = outputFile;
    }
}

