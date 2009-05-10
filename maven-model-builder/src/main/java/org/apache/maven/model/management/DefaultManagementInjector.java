package org.apache.maven.model.management;

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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Exclusion;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginContainer;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.merge.MavenModelMerger;
import org.codehaus.plexus.component.annotations.Component;

/**
 * Handles injection of plugin/dependency management into the model.
 * 
 * @author Benjamin Bentmann
 */
@Component( role = ManagementInjector.class )
public class DefaultManagementInjector
    implements ManagementInjector
{

    private ManagementModelMerger merger = new ManagementModelMerger();

    public void injectManagement( Model model )
    {
        merger.mergeManagedDependencies( model );
        merger.mergeManagedBuildPlugins( model );
    }

    private static class ManagementModelMerger
        extends MavenModelMerger
    {

        public void mergeManagedBuildPlugins( Model model )
        {
            Build build = model.getBuild();
            if ( build != null )
            {
                PluginManagement pluginManagement = build.getPluginManagement();
                if ( pluginManagement != null )
                {
                    mergePluginContainer_Plugins( build, pluginManagement, false, Collections.emptyMap() );
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

                Map<Object, Plugin> managedPlugins = new LinkedHashMap<Object, Plugin>( src.size() * 2 );

                for ( Iterator<Plugin> it = src.iterator(); it.hasNext(); )
                {
                    Plugin element = it.next();
                    Object key = getPluginKey( element );
                    managedPlugins.put( key, element );
                }

                for ( Iterator<Plugin> it = tgt.iterator(); it.hasNext(); )
                {
                    Plugin element = it.next();
                    Object key = getPluginKey( element );
                    Plugin managedPlugin = managedPlugins.get( key );
                    if ( managedPlugin != null )
                    {
                        mergePlugin( element, managedPlugin, sourceDominant, context );
                    }
                }
            }
        }

        @Override
        protected void mergePlugin_Executions( Plugin target, Plugin source, boolean sourceDominant,
                                               Map<Object, Object> context )
        {
            List<PluginExecution> src = source.getExecutions();
            if ( !src.isEmpty() )
            {
                List<PluginExecution> tgt = target.getExecutions();

                Map<Object, PluginExecution> merged =
                    new LinkedHashMap<Object, PluginExecution>( ( src.size() + tgt.size() ) * 2 );

                for ( Iterator<PluginExecution> it = src.iterator(); it.hasNext(); )
                {
                    PluginExecution element = it.next();
                    Object key = getPluginExecutionKey( element );
                    PluginExecution clone = new PluginExecution();
                    mergePluginExecution( clone, element, true, context );
                    merged.put( key, clone );
                }

                for ( Iterator<PluginExecution> it = tgt.iterator(); it.hasNext(); )
                {
                    PluginExecution element = it.next();
                    Object key = getPluginExecutionKey( element );
                    PluginExecution existing = merged.get( key );
                    if ( existing != null )
                    {
                        mergePluginExecution( element, existing, sourceDominant, context );
                    }
                    merged.put( key, element );
                }

                target.setExecutions( new ArrayList<PluginExecution>( merged.values() ) );
            }
        }

        public void mergeManagedDependencies( Model model )
        {
            DependencyManagement dependencyManagement = model.getDependencyManagement();
            if ( dependencyManagement != null )
            {
                Map<Object, Dependency> dependencies = new HashMap<Object, Dependency>();
                Map<Object, Object> context = Collections.emptyMap();

                for ( Dependency dependency : model.getDependencies() )
                {
                    Object key = getDependencyKey( dependency );
                    dependencies.put( key, dependency );
                }

                for ( Dependency managedDependency : dependencyManagement.getDependencies() )
                {
                    Object key = getDependencyKey( managedDependency );
                    Dependency dependency = dependencies.get( key );
                    if ( dependency != null )
                    {
                        mergeDependency( dependency, managedDependency, false, context );
                    }
                }
            }
        }

        @Override
        protected void mergeDependency_Exclusions( Dependency target, Dependency source, boolean sourceDominant,
                                                   Map<Object, Object> context )
        {
            List<Exclusion> tgt = target.getExclusions();
            if ( tgt.isEmpty() )
            {
                List<Exclusion> src = source.getExclusions();

                for ( Iterator<Exclusion> it = src.iterator(); it.hasNext(); )
                {
                    Exclusion element = it.next();
                    Exclusion clone = new Exclusion();
                    mergeExclusion( clone, element, true, context );
                    target.addExclusion( clone );
                }
            }
        }

    }

}
