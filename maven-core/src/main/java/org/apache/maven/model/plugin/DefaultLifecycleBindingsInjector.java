package org.apache.maven.model.plugin;

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
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.lifecycle.LifeCyclePluginAnalyzer;
import org.apache.maven.api.model.Build;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.Plugin;
import org.apache.maven.api.model.PluginContainer;
import org.apache.maven.api.model.PluginExecution;
import org.apache.maven.api.model.PluginManagement;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.building.ModelProblem.Severity;
import org.apache.maven.model.building.ModelProblem.Version;
import org.apache.maven.model.building.ModelProblemCollectorRequest;
import org.apache.maven.model.merge.MavenModelMerger;

/**
 * Handles injection of plugin executions induced by the lifecycle bindings for a packaging.
 *
 * @author Benjamin Bentmann
 */
@Named
@Singleton
public class DefaultLifecycleBindingsInjector
    implements LifecycleBindingsInjector
{

    private final LifecycleBindingsMerger merger = new LifecycleBindingsMerger();

    private final LifeCyclePluginAnalyzer lifecycle;

    @Inject
    public DefaultLifecycleBindingsInjector( LifeCyclePluginAnalyzer lifecycle )
    {
        this.lifecycle = lifecycle;
    }

    public void injectLifecycleBindings( org.apache.maven.model.Model model, ModelBuildingRequest request,
                                         ModelProblemCollector problems )
    {
        String packaging = model.getPackaging();

        Collection<org.apache.maven.model.Plugin> defaultPlugins =
                lifecycle.getPluginsBoundByDefaultToAllLifecycles( packaging );

        if ( defaultPlugins == null )
        {
            problems.add( new ModelProblemCollectorRequest( Severity.ERROR, Version.BASE )
                    .setMessage( "Unknown packaging: " + packaging )
                    .setLocation( model.getLocation( "packaging" ) ) );
        }
        else if ( !defaultPlugins.isEmpty() )
        {
            List<Plugin> plugins = defaultPlugins.stream()
                    .map( org.apache.maven.model.Plugin::getDelegate )
                    .collect( Collectors.toList() );
            Model lifecycleModel = Model.newBuilder()
                            .build( Build.newBuilder().plugins( plugins ).build() )
                            .build();
            model.update( merger.merge( model.getDelegate(), lifecycleModel ) );
        }
    }

    /**
     *  The domain-specific model merger for lifecycle bindings
     */
    protected static class LifecycleBindingsMerger
        extends MavenModelMerger
    {

        private static final String PLUGIN_MANAGEMENT = "plugin-management";

        public Model merge( Model target, Model source )
        {
            Build targetBuild = target.getBuild();
            if ( targetBuild == null )
            {
                targetBuild = Build.newInstance();
            }

            Map<Object, Object> context =
                Collections.singletonMap( PLUGIN_MANAGEMENT, target.getBuild().getPluginManagement() );

            Build.Builder builder = Build.newBuilder( target.getBuild() );
            mergePluginContainer_Plugins( builder, targetBuild, source.getBuild(), false, context );

            return target.withBuild( builder.build() );
        }

        @SuppressWarnings( { "checkstyle:methodname" } )
        @Override
        protected void mergePluginContainer_Plugins( PluginContainer.Builder builder,
                                                     PluginContainer target, PluginContainer source,
                                                     boolean sourceDominant, Map<Object, Object> context )
        {
            List<Plugin> src = source.getPlugins();
            if ( !src.isEmpty() )
            {
                List<Plugin> tgt = target.getPlugins();

                Map<Object, Plugin> merged = new LinkedHashMap<>( ( src.size() + tgt.size() ) * 2 );

                for ( Plugin element : tgt )
                {
                    Object key = getPluginKey().apply( element );
                    merged.put( key, element );
                }

                Map<Object, Plugin> added = new LinkedHashMap<>();

                for ( Plugin element : src )
                {
                    Object key = getPluginKey().apply( element );
                    Plugin existing = merged.get( key );
                    if ( existing != null )
                    {
                        element = mergePlugin( existing, element, sourceDominant, context );
                    }
                    else
                    {
                        added.put( key, element );
                    }
                    merged.put( key, element );
                }

                if ( !added.isEmpty() )
                {
                    PluginManagement pluginMgmt = (PluginManagement) context.get( PLUGIN_MANAGEMENT );
                    if ( pluginMgmt != null )
                    {
                        for ( Plugin managedPlugin : pluginMgmt.getPlugins() )
                        {
                            Object key = getPluginKey().apply( managedPlugin );
                            Plugin addedPlugin = added.get( key );
                            if ( addedPlugin != null )
                            {
                                Plugin plugin = mergePlugin( managedPlugin, addedPlugin,
                                        sourceDominant, Collections.emptyMap() );
                                merged.put( key, plugin );
                            }
                        }
                    }
                }

                List<Plugin> result = new ArrayList<>( merged.values() );

                builder.plugins( result );
            }
        }

        @Override
        protected void mergePluginExecution_Priority( PluginExecution.Builder builder, PluginExecution target,
                                                      PluginExecution source, boolean sourceDominant,
                                                      Map<Object, Object> context )
        {
            if ( target.getPriority() > source.getPriority() )
            {
                builder.priority( source.getPriority() );
                builder.location( "priority", source.getLocation( "priority" ) );
            }
        }
        //mergePluginExecution_Priority( builder, target, source, sourceDominant, context );

    }

}
