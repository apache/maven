package org.apache.maven.script.ant;

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
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.tools.ant.PropertyHelper;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.util.introspection.ReflectionValueExtractor;

/**
 * Makes the ${expressions} used in Maven available to Ant as properties.
 *
 * @author <a href="mailto:kenney@apache.org">Kenney Westerhof</a>
 */
public class AntPropertyHelper
    extends PropertyHelper
{
    private static final String DEPENDENCY_PREFIX = "maven.dependency.";
    private Log log;
    private ExpressionEvaluator exprEvaluator;
    private MavenProject mavenProject;
    private Map artifactMap = new Hashtable();

    /**
     * @deprecated use the other constructor
     * @param project
     * @param l
     */
    public AntPropertyHelper( MavenProject project, Log l )
    {
        mavenProject = project;
        log = l;
    }

    /**
     * @deprecated use {@link #AntPropertyHelper(ExpressionEvaluator, Set, Log)} to resolve maven.dependency.* properties
     * @param exprEvaluator
     * @param l
     */
    public AntPropertyHelper( ExpressionEvaluator exprEvaluator, Log l )
    {
        this( exprEvaluator, Collections.EMPTY_SET, l );
    }

    /**
     * @param exprEvaluator
     * @param artifacts
     * @param l
     */
    public AntPropertyHelper( ExpressionEvaluator exprEvaluator, Set artifacts, Log l )
    {
        this.mavenProject = null;
        this.exprEvaluator = exprEvaluator;
        this.log = l;

        for ( Iterator it = artifacts.iterator(); it.hasNext(); )
        {
            Artifact artifact = (Artifact) it.next();

            String key = DEPENDENCY_PREFIX + artifact.getGroupId() + "." + artifact.getArtifactId()
                + ( artifact.getClassifier() != null ? "." + artifact.getClassifier() : "" )
                + ( artifact.getType() != null ? "." + artifact.getType() : "" ) + ".path";

            log.debug( "Storing: " + key + "=" + artifact.getFile().getPath() );

            artifactMap.put( key, artifact.getFile().getPath() );
        }
    }

    /**
     * @see org.apache.tools.ant.PropertyHelper#getPropertyHook(java.lang.String, java.lang.String, boolean)
     */
    public synchronized Object getPropertyHook( String ns, String name, boolean user )
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( "getProperty(ns=" + ns + ", name=" + name + ", user=" + user + ")" );
        }

        /* keep old behaviour */
        if ( mavenProject != null )
        {
            return getPropertyHook( ns, name, user, mavenProject );
        }


        Object val = null;

        if ( name.startsWith( DEPENDENCY_PREFIX ) )
        {
            val = (String) artifactMap.get( name );
        }

        if ( val == null )
        {
            try
            {
                val = exprEvaluator.evaluate( "${" + name + "}" );
            }
            catch ( ExpressionEvaluationException e )
            {
                if ( log.isErrorEnabled() )
                {
                    log.error( "Failed to evaluate expression", e );
                }
            }
        }

        if ( val == null )
        {
            val = super.getPropertyHook( ns, name, user );

            if ( val == null )
            {
                val = System.getProperty( name.toString() );
            }
        }

        return val;
    }

    /**
     * @deprecated added to keep backwards compatibility
     * @param ns
     * @param name
     * @param user
     * @param mavenProject
     * @return The property value.
     */
    private Object getPropertyHook( String ns, String name, boolean user, MavenProject mavenProject )
    {
        Object val = null;
        try
        {
            if ( name.startsWith( DEPENDENCY_PREFIX ) )
            {
                val = (String) artifactMap.get( name );
            }
            else if ( name.startsWith( "project." ) )
            {
                val = ReflectionValueExtractor.evaluate(
                    name,
                    mavenProject,
                    true
                );
            }
            else if ( name.equals( "basedir" ) )
            {
                val = ReflectionValueExtractor.evaluate(
                    "basedir.path",
                    mavenProject,
                    false
                );
            }
        }
        catch ( Exception e )
        {
            if ( log.isWarnEnabled() )
            {
                log.warn( "Error evaluating expression '" + name + "'", e );
            }
        }

        if ( val == null )
        {
            val = super.getPropertyHook( ns, name, user );
            if ( val == null )
            {
                val = System.getProperty( name.toString() );
            }
        }

        if ( val instanceof File )
        {
            val = ( (File) val ).getAbsoluteFile();
        }

        return val;
    }
}
