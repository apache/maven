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
package org.apache.maven.project;

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
public final class ModelUtils {

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
    public static void mergePluginLists(
            PluginContainer childContainer, PluginContainer parentContainer, boolean handleAsInheritance) {
        if ((childContainer == null) || (parentContainer == null)) {
            // nothing to do.
            return;
        }

        List<Plugin> parentPlugins = parentContainer.getPlugins();

        if ((parentPlugins != null) && !parentPlugins.isEmpty()) {
            parentPlugins = new ArrayList<>(parentPlugins);

            // If we're processing this merge as an inheritance, we have to build up a list of
            // plugins that were considered for inheritance.
            if (handleAsInheritance) {
                for (Iterator<Plugin> it = parentPlugins.iterator(); it.hasNext(); ) {
                    Plugin plugin = it.next();

                    String inherited = plugin.getInherited();

                    if ((inherited != null) && !Boolean.parseBoolean(inherited)) {
                        it.remove();
                    }
                }
            }

            List<Plugin> assembledPlugins = new ArrayList<>();

            Map<String, Plugin> childPlugins = childContainer.getPluginsAsMap();

            for (Plugin parentPlugin : parentPlugins) {
                String parentInherited = parentPlugin.getInherited();

                // only merge plugin definition from the parent if at least one
                // of these is true:
                // 1. we're not processing the plugins in an inheritance-based merge
                // 2. the parent's <inherited/> flag is not set
                // 3. the parent's <inherited/> flag is set to true
                if (!handleAsInheritance || (parentInherited == null) || Boolean.parseBoolean(parentInherited)) {
                    Plugin childPlugin = childPlugins.get(parentPlugin.getKey());

                    if ((childPlugin != null) && !assembledPlugins.contains(childPlugin)) {
                        Plugin assembledPlugin = childPlugin;

                        mergePluginDefinitions(childPlugin, parentPlugin, handleAsInheritance);

                        // fix for MNG-2221 (assembly cache was not being populated for later reference):
                        assembledPlugins.add(assembledPlugin);
                    }

                    // if we're processing this as an inheritance-based merge, and
                    // the parent's <inherited/> flag is not set, then we need to
                    // clear the inherited flag in the merge result.
                    if (handleAsInheritance && (parentInherited == null)) {
                        parentPlugin.unsetInheritanceApplied();
                    }
                }

                // very important to use the parentPlugins List, rather than parentContainer.getPlugins()
                // since this list is a local one, and may have been modified during processing.
                List<Plugin> results =
                        ModelUtils.orderAfterMerge(assembledPlugins, parentPlugins, childContainer.getPlugins());

                childContainer.setPlugins(results);

                childContainer.flushPluginMap();
            }
        }
    }

    public static List<Plugin> orderAfterMerge(
            List<Plugin> merged, List<Plugin> highPrioritySource, List<Plugin> lowPrioritySource) {
        List<Plugin> results = new ArrayList<>();

        if (!merged.isEmpty()) {
            results.addAll(merged);
        }

        List<Plugin> missingFromResults = new ArrayList<>();

        List<List<Plugin>> sources = new ArrayList<>();

        sources.add(highPrioritySource);
        sources.add(lowPrioritySource);

        for (List<Plugin> source : sources) {
            for (Plugin item : source) {
                if (results.contains(item)) {
                    if (!missingFromResults.isEmpty()) {
                        int idx = results.indexOf(item);

                        if (idx < 0) {
                            idx = 0;
                        }

                        results.addAll(idx, missingFromResults);

                        missingFromResults.clear();
                    }
                } else {
                    missingFromResults.add(item);
                }
            }

            if (!missingFromResults.isEmpty()) {
                results.addAll(missingFromResults);

                missingFromResults.clear();
            }
        }

        return results;
    }

    public static void mergePluginDefinitions(Plugin child, Plugin parent, boolean handleAsInheritance) {
        if ((child == null) || (parent == null)) {
            // nothing to do.
            return;
        }

        if (parent.isExtensions()) {
            child.setExtensions(true);
        }

        if ((child.getVersion() == null) && (parent.getVersion() != null)) {
            child.setVersion(parent.getVersion());
        }

        Xpp3Dom childConfiguration = (Xpp3Dom) child.getConfiguration();
        Xpp3Dom parentConfiguration = (Xpp3Dom) parent.getConfiguration();

        childConfiguration = Xpp3Dom.mergeXpp3Dom(childConfiguration, parentConfiguration);

        child.setConfiguration(childConfiguration);

        child.setDependencies(mergeDependencyList(child.getDependencies(), parent.getDependencies()));

        // from here to the end of the method is dealing with merging of the <executions/> section.
        String parentInherited = parent.getInherited();

        boolean parentIsInherited = (parentInherited == null) || Boolean.parseBoolean(parentInherited);

        List<PluginExecution> parentExecutions = parent.getExecutions();

        if ((parentExecutions != null) && !parentExecutions.isEmpty()) {
            List<PluginExecution> mergedExecutions = new ArrayList<>();

            Map<String, PluginExecution> assembledExecutions = new TreeMap<>();

            Map<String, PluginExecution> childExecutions = child.getExecutionsAsMap();

            for (PluginExecution parentExecution : parentExecutions) {
                String inherited = parentExecution.getInherited();

                boolean parentExecInherited =
                        parentIsInherited && ((inherited == null) || Boolean.parseBoolean(inherited));

                if (!handleAsInheritance || parentExecInherited) {
                    PluginExecution assembled = parentExecution;

                    PluginExecution childExecution = childExecutions.get(parentExecution.getId());

                    if (childExecution != null) {
                        mergePluginExecutionDefinitions(childExecution, parentExecution);

                        assembled = childExecution;
                    } else if (handleAsInheritance && (parentInherited == null)) {
                        parentExecution.unsetInheritanceApplied();
                    }

                    assembledExecutions.put(assembled.getId(), assembled);
                    mergedExecutions.add(assembled);
                }
            }

            for (PluginExecution childExecution : child.getExecutions()) {
                if (!assembledExecutions.containsKey(childExecution.getId())) {
                    mergedExecutions.add(childExecution);
                }
            }

            child.setExecutions(mergedExecutions);

            child.flushExecutionMap();
        }
    }

    private static void mergePluginExecutionDefinitions(PluginExecution child, PluginExecution parent) {
        if (child.getPhase() == null) {
            child.setPhase(parent.getPhase());
        }

        List<String> parentGoals = parent.getGoals();
        List<String> childGoals = child.getGoals();

        List<String> goals = new ArrayList<>();

        if ((childGoals != null) && !childGoals.isEmpty()) {
            goals.addAll(childGoals);
        }

        if (parentGoals != null) {
            for (String goal : parentGoals) {
                if (!goals.contains(goal)) {
                    goals.add(goal);
                }
            }
        }

        child.setGoals(goals);

        Xpp3Dom childConfiguration = (Xpp3Dom) child.getConfiguration();
        Xpp3Dom parentConfiguration = (Xpp3Dom) parent.getConfiguration();

        childConfiguration = Xpp3Dom.mergeXpp3Dom(childConfiguration, parentConfiguration);

        child.setConfiguration(childConfiguration);
    }

    public static List<Repository> mergeRepositoryLists(List<Repository> dominant, List<Repository> recessive) {

        List<Repository> repositories = new ArrayList<>(dominant);

        for (Repository repository : recessive) {
            if (!repositories.contains(repository)) {
                repositories.add(repository);
            }
        }

        return repositories;
    }

    public static void mergeFilterLists(List<String> childFilters, List<String> parentFilters) {
        for (String f : parentFilters) {
            if (!childFilters.contains(f)) {
                childFilters.add(f);
            }
        }
    }

    private static List<Dependency> mergeDependencyList(List<Dependency> child, List<Dependency> parent) {
        Map<String, Dependency> depsMap = new LinkedHashMap<>();

        if (parent != null) {
            for (Dependency dependency : parent) {
                depsMap.put(dependency.getManagementKey(), dependency);
            }
        }

        if (child != null) {
            for (Dependency dependency : child) {
                depsMap.put(dependency.getManagementKey(), dependency);
            }
        }

        return new ArrayList<>(depsMap.values());
    }
}
