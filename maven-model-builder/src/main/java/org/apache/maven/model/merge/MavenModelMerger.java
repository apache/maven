package org.apache.maven.model.merge;

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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.maven.model.BuildBase;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.ModelBase;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginContainer;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.Repository;
import org.apache.maven.model.RepositoryBase;
import org.apache.maven.model.Scm;
import org.apache.maven.model.Site;

/**
 * The domain-specific model merger for the Maven POM.
 * 
 * @author Benjamin Bentmann
 */
public class MavenModelMerger
    extends ModelMerger
{

    /**
     * The hint key for the child path adjustment used during inheritance for URL calculations.
     */
    public static final String CHILD_PATH_ADJUSTMENT = "child-path-adjustment";

    /**
     * The context key for the artifact id of the target model.
     */
    private static final String ARTIFACT_ID = "artifact-id";

    @Override
    protected void mergeModel( Model target, Model source, boolean sourceDominant, Map<Object, Object> context )
    {
        context.put( ARTIFACT_ID, target.getArtifactId() );

        super.mergeModel( target, source, sourceDominant, context );
    }

    @Override
    protected void mergeModel_Url( Model target, Model source, boolean sourceDominant, Map<Object, Object> context )
    {
        String src = source.getUrl();
        if ( src != null )
        {
            if ( sourceDominant )
            {
                target.setUrl( src );
            }
            else if ( target.getUrl() == null )
            {
                target.setUrl( appendPath( src, context.get( ARTIFACT_ID ).toString(),
                                           context.get( CHILD_PATH_ADJUSTMENT ).toString() ) );
            }
        }
    }

    @Override
    protected void mergeModelBase_Modules( ModelBase target, ModelBase source, boolean sourceDominant,
                                           Map<Object, Object> context )
    {
        List<String> src = source.getModules();
        if ( !src.isEmpty() && sourceDominant )
        {
            List<String> tgt = target.getModules();
            Set<String> excludes = new LinkedHashSet<String>( tgt );
            List<String> merged = new ArrayList<String>( tgt.size() + src.size() );
            merged.addAll( tgt );
            for ( String s : src )
            {
                if ( !excludes.contains( s ) )
                {
                    merged.add( s );
                }
            }
            target.setModules( merged );
        }
    }

    /*
     * TODO: The order of the merged list could be controlled by an attribute in the model association: target-first,
     * source-first, dominant-first, recessive-first
     */
    @Override
    protected void mergeModelBase_Repositories( ModelBase target, ModelBase source, boolean sourceDominant,
                                                Map<Object, Object> context )
    {
        List<Repository> src = source.getRepositories();
        if ( !src.isEmpty() )
        {
            List<Repository> tgt = target.getRepositories();
            Map<Object, Repository> merged = new LinkedHashMap<Object, Repository>( ( src.size() + tgt.size() ) * 2 );

            List<Repository> dominant, recessive;
            if ( sourceDominant )
            {
                dominant = src;
                recessive = tgt;
            }
            else
            {
                dominant = tgt;
                recessive = src;
            }

            for ( Iterator<Repository> it = dominant.iterator(); it.hasNext(); )
            {
                Repository element = it.next();
                Object key = getRepositoryKey( element );
                merged.put( key, element );
            }

            for ( Iterator<Repository> it = recessive.iterator(); it.hasNext(); )
            {
                Repository element = it.next();
                Object key = getRepositoryKey( element );
                if ( !merged.containsKey( key ) )
                {
                    merged.put( key, element );
                }
            }

            target.setRepositories( new ArrayList<Repository>( merged.values() ) );
        }
    }

    /*
     * TODO: Whether duplicates should be removed looks like an option for the generated merger.
     */
    @Override
    protected void mergeBuildBase_Filters( BuildBase target, BuildBase source, boolean sourceDominant,
                                           Map<Object, Object> context )
    {
        List<String> src = source.getFilters();
        if ( !src.isEmpty() )
        {
            List<String> tgt = target.getFilters();
            Set<String> excludes = new LinkedHashSet<String>( tgt );
            List<String> merged = new ArrayList<String>( tgt.size() + src.size() );
            merged.addAll( tgt );
            for ( String s : src )
            {
                if ( !excludes.contains( s ) )
                {
                    merged.add( s );
                }
            }
            target.setFilters( merged );
        }
    }

    @Override
    protected void mergeSite_Url( Site target, Site source, boolean sourceDominant, Map<Object, Object> context )
    {
        String src = source.getUrl();
        if ( src != null )
        {
            if ( sourceDominant )
            {
                target.setUrl( src );
            }
            else if ( target.getUrl() == null )
            {
                target.setUrl( appendPath( src, context.get( ARTIFACT_ID ).toString(),
                                           context.get( CHILD_PATH_ADJUSTMENT ).toString() ) );
            }
        }
    }

    @Override
    protected void mergeScm_Url( Scm target, Scm source, boolean sourceDominant, Map<Object, Object> context )
    {
        String src = source.getUrl();
        if ( src != null )
        {
            if ( sourceDominant )
            {
                target.setUrl( src );
            }
            else if ( target.getUrl() == null )
            {
                target.setUrl( appendPath( src, context.get( ARTIFACT_ID ).toString(),
                                           context.get( CHILD_PATH_ADJUSTMENT ).toString() ) );
            }
        }
    }

    @Override
    protected void mergeScm_Connection( Scm target, Scm source, boolean sourceDominant, Map<Object, Object> context )
    {
        String src = source.getConnection();
        if ( src != null )
        {
            if ( sourceDominant )
            {
                target.setConnection( src );
            }
            else if ( target.getConnection() == null )
            {
                target.setConnection( appendPath( src, context.get( ARTIFACT_ID ).toString(),
                                                  context.get( CHILD_PATH_ADJUSTMENT ).toString() ) );
            }
        }
    }

    @Override
    protected void mergeScm_DeveloperConnection( Scm target, Scm source, boolean sourceDominant,
                                                 Map<Object, Object> context )
    {
        String src = source.getDeveloperConnection();
        if ( src != null )
        {
            if ( sourceDominant )
            {
                target.setDeveloperConnection( src );
            }
            else if ( target.getDeveloperConnection() == null )
            {
                target.setDeveloperConnection( appendPath( src, context.get( ARTIFACT_ID ).toString(),
                                                           context.get( CHILD_PATH_ADJUSTMENT ).toString() ) );
            }
        }
    }

    @Override
    protected void mergePluginContainer_Plugins( PluginContainer target, PluginContainer source,
                                                 boolean sourceDominant, Map<Object, Object> context )
    {
        List<Plugin> src = source.getPlugins();
        if ( !src.isEmpty() )
        {
            List<Plugin> tgt = target.getPlugins();
            Map<Object, Plugin> merged = new LinkedHashMap<Object, Plugin>( ( src.size() + tgt.size() ) * 2 );

            for ( Iterator<Plugin> it = tgt.iterator(); it.hasNext(); )
            {
                Plugin element = it.next();
                Object key = getPluginKey( element );
                merged.put( key, element );
            }

            for ( Iterator<Plugin> it = src.iterator(); it.hasNext(); )
            {
                Plugin element = it.next();
                Object key = getPluginKey( element );
                Plugin existing = merged.get( key );
                if ( existing != null )
                {
                    mergePlugin( existing, element, sourceDominant, context );
                }
                else
                {
                    merged.put( key, element );
                }
            }

            target.setPlugins( new ArrayList<Plugin>( merged.values() ) );
        }
    }

    @Override
    protected Object getDependencyKey( Dependency dependency )
    {
        return dependency.getManagementKey();
    }

    @Override
    protected Object getPluginKey( Plugin object )
    {
        return object.getKey();
    }

    @Override
    protected Object getPluginExecutionKey( PluginExecution object )
    {
        return object.getId();
    }

    @Override
    protected Object getRepositoryBaseKey( RepositoryBase object )
    {
        return object.getId();
    }

    private String appendPath( String parentPath, String childPath, String pathAdjustment )
    {
        String uncleanPath = parentPath;

        if ( pathAdjustment != null )
        {
            uncleanPath += "/" + pathAdjustment;
        }

        if ( childPath != null )
        {
            uncleanPath += "/" + childPath;
        }

        String cleanedPath = "";

        int protocolIdx = uncleanPath.indexOf( "://" );

        if ( protocolIdx > -1 )
        {
            cleanedPath = uncleanPath.substring( 0, protocolIdx + 3 );
            uncleanPath = uncleanPath.substring( protocolIdx + 3 );
        }

        if ( uncleanPath.startsWith( "//" ) )
        {
            // preserve leading double slash for UNC paths like "file:////host/pom.xml"
            cleanedPath += "//";
        }
        else if ( uncleanPath.startsWith( "/" ) )
        {
            cleanedPath += "/";
        }

        return cleanedPath + resolvePath( uncleanPath );
    }

    private String resolvePath( String uncleanPath )
    {
        LinkedList<String> pathElements = new LinkedList<String>();

        StringTokenizer tokenizer = new StringTokenizer( uncleanPath, "/" );

        while ( tokenizer.hasMoreTokens() )
        {
            String token = tokenizer.nextToken();

            if ( token.equals( "" ) )
            {
                // Empty path entry ("...//.."), remove.
            }
            else if ( token.equals( ".." ) )
            {
                if ( !pathElements.isEmpty() )
                {
                    pathElements.removeLast();
                }
            }
            else
            {
                pathElements.addLast( token );
            }
        }

        StringBuffer cleanedPath = new StringBuffer();

        while ( !pathElements.isEmpty() )
        {
            cleanedPath.append( pathElements.removeFirst() );
            if ( !pathElements.isEmpty() )
            {
                cleanedPath.append( '/' );
            }
        }

        return cleanedPath.toString();
    }

}
