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

import org.apache.maven.model.*;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.util.*;

public final class ModelUtils
{

    /**
     * This should be the resulting ordering of plugins after merging:
     * <p/>
     * Given:
     * <p/>
     * parent: X -> A -> B -> D -> E
     * child: Y -> A -> C -> D -> F
     * <p/>
     * Result:
     * <p/>
     * X -> Y -> A -> B -> C -> D -> E -> F
     */
    public static void mergePluginLists( PluginContainer childContainer, PluginContainer parentContainer,
                                         boolean handleAsInheritance )
    {
        if ( ( childContainer == null ) || ( parentContainer == null ) )
        {
            // nothing to do.
            return;
        }

        List parentPlugins = parentContainer.getPlugins();

        if ( ( parentPlugins != null ) && !parentPlugins.isEmpty() )
        {
            parentPlugins = new ArrayList( parentPlugins );

            // If we're processing this merge as an inheritance, we have to build up a list of
            // plugins that were considered for inheritance.
            if ( handleAsInheritance )
            {
                for ( Iterator it = parentPlugins.iterator(); it.hasNext(); )
                {
                    Plugin plugin = (Plugin) it.next();

                    String inherited = plugin.getInherited();

                    if ( ( inherited != null ) && !Boolean.valueOf( inherited ).booleanValue() )
                    {
                        it.remove();
                    }
                }
            }

            List assembledPlugins = new ArrayList();

            Map childPlugins = childContainer.getPluginsAsMap();

            for ( Iterator it = parentPlugins.iterator(); it.hasNext(); )
            {
                Plugin parentPlugin = (Plugin) it.next();

                String parentInherited = parentPlugin.getInherited();

                // only merge plugin definition from the parent if at least one
                // of these is true:
                // 1. we're not processing the plugins in an inheritance-based merge
                // 2. the parent's <inherited/> flag is not set
                // 3. the parent's <inherited/> flag is set to true
                if ( !handleAsInheritance || ( parentInherited == null ) ||
                    Boolean.valueOf( parentInherited ).booleanValue() )
                {
                    Plugin childPlugin = (Plugin) childPlugins.get( parentPlugin.getKey() );

                    if ( ( childPlugin != null ) && !assembledPlugins.contains( childPlugin ) )
                    {
                        Plugin assembledPlugin = childPlugin;

                        mergePluginDefinitions( childPlugin, parentPlugin, handleAsInheritance );

                        // fix for MNG-2221 (assembly cache was not being populated for later reference):
                        assembledPlugins.add( assembledPlugin );
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
                List results =
                    ModelUtils.orderAfterMerge( assembledPlugins, parentPlugins, childContainer.getPlugins() );

                childContainer.setPlugins( results );

                childContainer.flushPluginMap();
            }
        }
    }

    public static List orderAfterMerge( List merged, List highPrioritySource, List lowPrioritySource )
    {
        List results = new ArrayList();

        if ( !merged.isEmpty() )
        {
            results.addAll( merged );
        }

        List missingFromResults = new ArrayList();

        List sources = new ArrayList();

        sources.add( highPrioritySource );
        sources.add( lowPrioritySource );

        for ( Iterator sourceIterator = sources.iterator(); sourceIterator.hasNext(); )
        {
            List source = (List) sourceIterator.next();

            for ( Iterator it = source.iterator(); it.hasNext(); )
            {
                Object item = it.next();

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


    public static void mergePluginDefinitions( Plugin child, Plugin parent, boolean handleAsInheritance )
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
        Xpp3Dom parentConfiguration = (Xpp3Dom) parent.getConfiguration();

        childConfiguration = Xpp3Dom.mergeXpp3Dom( childConfiguration, parentConfiguration );

        child.setConfiguration( childConfiguration );

        child.setDependencies( mergeDependencyList( child.getDependencies(), parent.getDependencies() ) );

        // from here to the end of the method is dealing with merging of the <executions/> section.
        String parentInherited = parent.getInherited();

        boolean parentIsInherited = ( parentInherited == null ) || Boolean.valueOf( parentInherited ).booleanValue();

        List parentExecutions = parent.getExecutions();

        if ( ( parentExecutions != null ) && !parentExecutions.isEmpty() )
        {
            List mergedExecutions = new ArrayList();

            Map assembledExecutions = new TreeMap();

            Map childExecutions = child.getExecutionsAsMap();

            for ( Iterator it = parentExecutions.iterator(); it.hasNext(); )
            {
                PluginExecution parentExecution = (PluginExecution) it.next();

                String inherited = parentExecution.getInherited();

                boolean parentExecInherited =
                    parentIsInherited && ( ( inherited == null ) || Boolean.valueOf( inherited ).booleanValue() );

                if ( !handleAsInheritance || parentExecInherited )
                {
                    PluginExecution assembled = parentExecution;

                    PluginExecution childExecution = (PluginExecution) childExecutions.get( parentExecution.getId() );

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

            for ( Iterator it = child.getExecutions().iterator(); it.hasNext(); )
            {
                PluginExecution childExecution = (PluginExecution) it.next();

                if ( !assembledExecutions.containsKey( childExecution.getId() ) )
                {
                    mergedExecutions.add( childExecution );
                }
            }

            child.setExecutions( mergedExecutions );

            child.flushExecutionMap();
        }

    }

    private static void mergePluginExecutionDefinitions( PluginExecution child, PluginExecution parent )
    {
        if ( child.getPhase() == null )
        {
            child.setPhase( parent.getPhase() );
        }

        List parentGoals = parent.getGoals();
        List childGoals = child.getGoals();

        List goals = new ArrayList();

        if ( ( childGoals != null ) && !childGoals.isEmpty() )
        {
            goals.addAll( childGoals );
        }

        if ( parentGoals != null )
        {
            for ( Iterator goalIterator = parentGoals.iterator(); goalIterator.hasNext(); )
            {
                String goal = (String) goalIterator.next();

                if ( !goals.contains( goal ) )
                {
                    goals.add( goal );
                }
            }
        }

        child.setGoals( goals );

        Xpp3Dom childConfiguration = (Xpp3Dom) child.getConfiguration();
        Xpp3Dom parentConfiguration = (Xpp3Dom) parent.getConfiguration();

        childConfiguration = Xpp3Dom.mergeXpp3Dom( childConfiguration, parentConfiguration );

        child.setConfiguration( childConfiguration );
    }

    public static List mergeRepositoryLists( List dominant, List recessive )
    {
        List repositories = new ArrayList();

        for ( Iterator it = dominant.iterator(); it.hasNext(); )
        {
            Repository repository = (Repository) it.next();

            repositories.add( repository );
        }

        for ( Iterator it = recessive.iterator(); it.hasNext(); )
        {
            Repository repository = (Repository) it.next();

            if ( !repositories.contains( repository ) )
            {
                repositories.add( repository );
            }
        }

        return repositories;
    }

    public static void mergeFilterLists( List childFilters, List parentFilters )
    {
        for ( Iterator i = parentFilters.iterator(); i.hasNext(); )
        {
            String f = (String) i.next();
            if ( !childFilters.contains( f ) )
            {
                childFilters.add( f );
            }
        }
    }

    private static List mergeDependencyList( List child, List parent )
    {
        Map depsMap = new HashMap();

        if ( parent != null )
        {
            for ( Iterator it = parent.iterator(); it.hasNext(); )
            {
                Dependency dependency = (Dependency) it.next();
                depsMap.put( dependency.getManagementKey(), dependency );
            }
        }

        if ( child != null )
        {
            for ( Iterator it = child.iterator(); it.hasNext(); )
            {
                Dependency dependency = (Dependency) it.next();
                depsMap.put( dependency.getManagementKey(), dependency );
            }
        }

        return new ArrayList( depsMap.values() );
    }

}
