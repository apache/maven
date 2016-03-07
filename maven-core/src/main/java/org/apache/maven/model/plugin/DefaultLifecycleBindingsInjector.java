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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.maven.lifecycle.LifecycleMappingNotFoundException;
import org.apache.maven.lifecycle.LifecyclePluginAnalyzer;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginContainer;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelProblem.Severity;
import org.apache.maven.model.building.ModelProblem.Version;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.building.ModelProblemCollectorRequest;
import org.apache.maven.model.merge.MavenModelMerger;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

/**
 * Handles injection of plugin executions induced by the lifecycle bindings for a packaging.
 *
 * @author Benjamin Bentmann
 */
@Component( role = LifecycleBindingsInjector.class )
public class DefaultLifecycleBindingsInjector
    implements LifecycleBindingsInjector
{

    private LifecycleBindingsMerger merger = new LifecycleBindingsMerger();

    @Requirement
    private LifecyclePluginAnalyzer lifecyclePluginAnalyzer;

    @Override
    public void injectLifecycleBindings( Model model, ModelBuildingRequest request, ModelProblemCollector problems )
    {
        try
        {
            final Model lifecycleModel = this.lifecyclePluginAnalyzer.getLifecycleModel( model );

            if ( model.getBuild() == null )
            {
                model.setBuild( new Build() );
            }

            final PluginManagement pluginManagement = model.getBuild().getPluginManagement();

            model.getBuild().setPluginManagement( lifecycleModel.getBuild().getPluginManagement() );

            merger.merge( model, lifecycleModel );

            model.getBuild().setPluginManagement( pluginManagement );
        }
        catch ( final LifecycleMappingNotFoundException e )
        {
            problems.add( new ModelProblemCollectorRequest( Severity.ERROR, Version.BASE )
                .setException( e )
                .setLocation( model.getLocation( "packaging" ) ) );

        }
    }

    protected static class LifecycleBindingsMerger
        extends MavenModelMerger
    {

        private static final String PLUGIN_MANAGEMENT = "plugin-management";

        public void merge( Model target, Model source )
        {
            if ( target.getBuild() == null )
            {
                target.setBuild( new Build() );
            }

            Map<Object, Object> context =
                Collections.<Object, Object>singletonMap( PLUGIN_MANAGEMENT, target.getBuild().getPluginManagement() );

            mergePluginContainer_Plugins( target.getBuild(), source.getBuild(), false, context );
        }

        @Override
        protected void mergePluginContainer_Plugins( PluginContainer target, PluginContainer source,
                                                     boolean sourceDominant, Map<Object, Object> context )
        {
            List<Plugin> src = source.getPlugins();
            if ( !src.isEmpty() )
            {
                List<Plugin> tgt = target.getPlugins();

                Map<Object, Plugin> merged = new LinkedHashMap<>( ( src.size() + tgt.size() ) * 2 );

                for ( Plugin element : tgt )
                {
                    Object key = getPluginKey( element );
                    merged.put( key, element );
                }

                Map<Object, Plugin> unmanaged = new LinkedHashMap<>();

                for ( Plugin element : src )
                {
                    Object key = getPluginKey( element );
                    Plugin existing = merged.get( key );
                    if ( existing != null )
                    {
                        mergePlugin( existing, element, sourceDominant, context );
                    }
                    else
                    {
                        merged.put( key, element );
                        unmanaged.put( key, element );
                    }
                }

                if ( !unmanaged.isEmpty() )
                {
                    PluginManagement pluginMgmt = (PluginManagement) context.get( PLUGIN_MANAGEMENT );
                    if ( pluginMgmt != null )
                    {
                        for ( Plugin managedPlugin : pluginMgmt.getPlugins() )
                        {
                            Object key = getPluginKey( managedPlugin );
                            Plugin unmanagedPlugin = unmanaged.get( key );
                            if ( unmanagedPlugin != null )
                            {
                                Plugin plugin = managedPlugin.clone();
                                mergePlugin( plugin, unmanagedPlugin, sourceDominant, Collections.emptyMap() );
                                merged.put( key, plugin );
                            }
                        }
                    }
                }

                List<Plugin> result = new ArrayList<>( merged.values() );

                target.setPlugins( result );
            }
        }

        @Override
        protected void mergePluginExecution( PluginExecution target, PluginExecution source, boolean sourceDominant,
                                             Map<Object, Object> context )
        {
            super.mergePluginExecution( target, source, sourceDominant, context );

            target.setPriority( Math.min( target.getPriority(), source.getPriority() ) );
        }

    }

}
