package org.apache.maven.project;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginContainer;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.Repository;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/** @deprecated */
@Deprecated
public final class ModelUtils
{

    /**
     * This should be the resulting ordering of plugins after merging:
     * <p>
     * Given:
     * <pre>
     * parent: X -&gt; A -&gt; B -&gt; D -&gt; E
     * child: Y -&gt; A -&gt; C -&gt; D -&gt; F
     * </pre>
     * Result:
     * <pre>
     * X -&gt; Y -&gt; A -&gt; B -&gt; C -&gt; D -&gt; E -&gt; F
     * </pre>
     */
    public static void mergePluginLists( final PluginContainer childContainer, final PluginContainer parentContainer,
                                         final boolean handleAsInheritance )
    {
        if ( ( childContainer == null ) || ( parentContainer == null ) )
        {
            // nothing to do.
            return;
        }

        List<Plugin> parentPlugins = parentContainer.getPlugins();

        if ( ( parentPlugins != null ) && !parentPlugins.isEmpty() )
        {
            parentPlugins = new ArrayList<>( parentPlugins );

            // If we're processing this merge as an inheritance, we have to build up a list of
            // plugins that were considered for inheritance.
            if ( handleAsInheritance )
            {
                for ( final Iterator<Plugin> it = parentPlugins.iterator(); it.hasNext(); )
                {
                    final Plugin plugin = it.next();

                    final String inherited = plugin.getInherited();

                    if ( ( inherited != null ) && !Boolean.parseBoolean( inherited ) )
                    {
                        it.remove();
                    }
                }
            }

            final List<Plugin> assembledPlugins = new ArrayList<>();

            final Map<String, Plugin> childPlugins = childContainer.getPluginsAsMap();

            for ( final Plugin parentPlugin : parentPlugins )
            {
                final String parentInherited = parentPlugin.getInherited();

                // only merge plugin definition from the parent if at least one
                // of these is true:
                // 1. we're not processing the plugins in an inheritance-based merge
                // 2. the parent's <inherited/> flag is not set
                // 3. the parent's <inherited/> flag is set to true
                if ( !handleAsInheritance || ( parentInherited == null )
                    || Boolean.parseBoolean( parentInherited ) )
                {
                    final Plugin childPlugin = childPlugins.get( parentPlugin.getKey() );

                    if ( ( childPlugin != null ) && !assembledPlugins.contains( childPlugin ) )
                    {
                        mergePluginDefinitions( childPlugin, parentPlugin, handleAsInheritance );

                        // fix for MNG-2221 (assembly cache was not being populated for later reference):
                        assembledPlugins.add( childPlugin );
                    }

                    // if we're processing this as an inheritance-based merge, and
                    // the parent's <inherited/> flag is not set, then we need to
                    // clear the inherited flag in the merge result.
                    if ( handleAsInheritance && ( parentInherited == null ) )
                    {
                        parentPlugin.unsetInheritanceApplied();
                    }
                }

                // very important to use the parentPlugins List, rather than parentContainer.getPlugins()
                // since this list is a local one, and may have been modified during processing.
                final List<Plugin> results =
                    ModelUtils.orderAfterMerge( assembledPlugins, parentPlugins, childContainer.getPlugins() );

                childContainer.setPlugins( results );

                childContainer.flushPluginMap();
            }
        }
    }

    public static List<Plugin> orderAfterMerge( final List<Plugin> merged, final List<Plugin> highPrioritySource,
                                                final List<Plugin> lowPrioritySource )
    {
        final List<Plugin> results = new ArrayList<>();

        if ( !merged.isEmpty() )
        {
            results.addAll( merged );
        }

        final List<Plugin> missingFromResults = new ArrayList<>();

        final List<List<Plugin>> sources = new ArrayList<>();

        sources.add( highPrioritySource );
        sources.add( lowPrioritySource );

        for ( final List<Plugin> source : sources )
        {
            for ( final Plugin item : source )
            {
                if ( results.contains( item ) )
                {
                    if ( !missingFromResults.isEmpty() )
                    {
                        int idx = results.indexOf( item );

                        if ( idx < 0 )
                        {
                            idx = 0;
                        }

                        results.addAll( idx, missingFromResults );

                        missingFromResults.clear();
                    }
                }
                else
                {
                    missingFromResults.add( item );
                }
            }

            if ( !missingFromResults.isEmpty() )
            {
                results.addAll( missingFromResults );

                missingFromResults.clear();
            }
        }

        return results;
    }


    public static void mergePluginDefinitions( final Plugin child, final Plugin parent, final boolean handleAsInheritance )
    {
        if ( ( child == null ) || ( parent == null ) )
        {
            // nothing to do.
            return;
        }

        if ( parent.isExtensions() )
        {
            child.setExtensions( true );
        }

        if ( ( child.getVersion() == null ) && ( parent.getVersion() != null ) )
        {
            child.setVersion( parent.getVersion() );
        }

        Xpp3Dom childConfiguration = (Xpp3Dom) child.getConfiguration();
        final Xpp3Dom parentConfiguration = (Xpp3Dom) parent.getConfiguration();

        childConfiguration = Xpp3Dom.mergeXpp3Dom( childConfiguration, parentConfiguration );

        child.setConfiguration( childConfiguration );

        child.setDependencies( mergeDependencyList( child.getDependencies(), parent.getDependencies() ) );

        // from here to the end of the method is dealing with merging of the <executions/> section.
        final String parentInherited = parent.getInherited();

        final boolean parentIsInherited = ( parentInherited == null ) || Boolean.parseBoolean( parentInherited );

        final List<PluginExecution> parentExecutions = parent.getExecutions();

        if ( ( parentExecutions != null ) && !parentExecutions.isEmpty() )
        {
            final List<PluginExecution> mergedExecutions = new ArrayList<>();

            final Map<String, PluginExecution> assembledExecutions = new TreeMap<>();

            final Map<String, PluginExecution> childExecutions = child.getExecutionsAsMap();

            for ( final PluginExecution parentExecution : parentExecutions )
            {
                final String inherited = parentExecution.getInherited();

                final boolean parentExecInherited =
                    parentIsInherited && ( ( inherited == null ) || Boolean.parseBoolean( inherited ) );

                if ( !handleAsInheritance || parentExecInherited )
                {
                    PluginExecution assembled = parentExecution;

                    final PluginExecution childExecution = childExecutions.get( parentExecution.getId() );

                    if ( childExecution != null )
                    {
                        mergePluginExecutionDefinitions( childExecution, parentExecution );

                        assembled = childExecution;
                    }
                    else if ( handleAsInheritance && ( parentInherited == null ) )
                    {
                        parentExecution.unsetInheritanceApplied();
                    }

                    assembledExecutions.put( assembled.getId(), assembled );
                    mergedExecutions.add( assembled );
                }
            }

            for ( final PluginExecution childExecution : child.getExecutions() )
            {
                if ( !assembledExecutions.containsKey( childExecution.getId() ) )
                {
                    mergedExecutions.add( childExecution );
                }
            }

            child.setExecutions( mergedExecutions );

            child.flushExecutionMap();
        }

    }

    private static void mergePluginExecutionDefinitions( final PluginExecution child, final PluginExecution parent )
    {
        if ( child.getPhase() == null )
        {
            child.setPhase( parent.getPhase() );
        }

        final List<String> parentGoals = parent.getGoals();
        final List<String> childGoals = child.getGoals();

        final List<String> goals = new ArrayList<>();

        if ( ( childGoals != null ) && !childGoals.isEmpty() )
        {
            goals.addAll( childGoals );
        }

        if ( parentGoals != null )
        {
            for (  final String goal : parentGoals )
            {
                if ( !goals.contains( goal ) )
                {
                    goals.add( goal );
                }
            }
        }

        child.setGoals( goals );

        Xpp3Dom childConfiguration = (Xpp3Dom) child.getConfiguration();
        final Xpp3Dom parentConfiguration = (Xpp3Dom) parent.getConfiguration();

        childConfiguration = Xpp3Dom.mergeXpp3Dom( childConfiguration, parentConfiguration );

        child.setConfiguration( childConfiguration );
    }

    public static List<Repository> mergeRepositoryLists( final List<Repository> dominant, final List<Repository> recessive )
    {

        final List<Repository> repositories = new ArrayList<>( dominant );

        for ( final Repository repository : recessive )
        {
            if ( !repositories.contains( repository ) )
            {
                repositories.add( repository );
            }
        }

        return repositories;
    }

    public static void mergeFilterLists( final List<String> childFilters, final List<String> parentFilters )
    {
        for ( final String f : parentFilters )
        {
            if ( !childFilters.contains( f ) )
            {
                childFilters.add( f );
            }
        }
    }

    private static List<Dependency> mergeDependencyList( final List<Dependency> child, final List<Dependency> parent )
    {
        final Map<String, Dependency> depsMap = new LinkedHashMap<>();

        if ( parent != null )
        {
            for ( final Dependency dependency : parent )
            {
                depsMap.put( dependency.getManagementKey(), dependency );
            }
        }

        if ( child != null )
        {
            for ( final Dependency dependency : child )
            {
                depsMap.put( dependency.getManagementKey(), dependency );
            }
        }

        return new ArrayList<>( depsMap.values() );
    }

}
