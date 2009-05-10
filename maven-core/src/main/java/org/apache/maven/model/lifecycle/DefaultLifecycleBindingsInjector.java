package org.apache.maven.model.lifecycle;

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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginContainer;
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
    private LifecycleExecutor lifecycle;

    public void injectLifecycleBindings( Model model )
    {
        String packaging = model.getPackaging();

        Collection<Plugin> defaultPlugins = lifecycle.getPluginsBoundByDefaultToAllLifecycles( packaging );

        if ( !defaultPlugins.isEmpty() )
        {
            Model lifecycleModel = new Model();
            lifecycleModel.setBuild( new Build() );
            lifecycleModel.getBuild().getPlugins().addAll( defaultPlugins );

            merger.merge( model, lifecycleModel );
        }
    }

    private static class LifecycleBindingsMerger
        extends MavenModelMerger
    {

        public void merge( Model target, Model source )
        {
            if ( target.getBuild() == null )
            {
                target.setBuild( new Build() );
            }
            mergePluginContainer_Plugins( target.getBuild(), source.getBuild(), false, Collections.emptyMap() );
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

                for ( Iterator<Plugin> it = src.iterator(); it.hasNext(); )
                {
                    Plugin element = it.next();
                    Object key = getPluginKey( element );
                    merged.put( key, element );
                }

                for ( Iterator<Plugin> it = tgt.iterator(); it.hasNext(); )
                {
                    Plugin element = it.next();
                    Object key = getPluginKey( element );
                    Plugin existing = merged.get( key );
                    if ( existing != null )
                    {
                        mergePlugin( element, existing, sourceDominant, context );
                    }
                    merged.put( key, element );
                }

                target.setPlugins( new ArrayList<Plugin>( merged.values() ) );
            }
        }
    }

}
