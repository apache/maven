package org.apache.maven.model.normalization;

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
import java.util.function.Function;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.api.model.Build;
import org.apache.maven.api.model.Dependency;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.Plugin;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.merge.MavenModelMerger;
import org.codehaus.plexus.util.StringUtils;

/**
 * Handles normalization of a model.
 *
 * @author Benjamin Bentmann
 */
@Named
@Singleton
public class DefaultModelNormalizer
    implements ModelNormalizer
{

    private DuplicateMerger merger = new DuplicateMerger();

    @Override
    public Model mergeDuplicates( Model model, ModelBuildingRequest request, ModelProblemCollector problems )
    {
        Model.Builder builder = Model.newBuilder( model );

        Build build = model.getBuild();
        if ( build != null )
        {
            List<Plugin> plugins = build.getPlugins();
            Map<Object, Plugin> normalized = new LinkedHashMap<>( plugins.size() * 2 );

            for ( Plugin plugin : plugins )
            {
                Object key = plugin.getKey();
                Plugin first = normalized.get( key );
                if ( first != null )
                {
                    plugin = merger.mergePlugin( plugin, first );
                }
                normalized.put( key, plugin );
            }

            if ( plugins.size() != normalized.size() )
            {
                builder.build( Build.newBuilder( build )
                            .plugins( normalized.values() )
                            .build() );
            }
        }

        /*
         * NOTE: This is primarily to keep backward-compat with Maven 2.x which did not validate that dependencies are
         * unique within a single POM. Upon multiple declarations, 2.x just kept the last one but retained the order of
         * the first occurrence. So when we're in lenient/compat mode, we have to deal with such broken POMs and mimic
         * the way 2.x works. When we're in strict mode, the removal of duplicates just saves other merging steps from
         * aftereffects and bogus error messages.
         */
        List<Dependency> dependencies = model.getDependencies();
        Map<String, Dependency> normalized = new LinkedHashMap<>( dependencies.size() * 2 );

        for ( Dependency dependency : dependencies )
        {
            normalized.put( dependency.getManagementKey(), dependency );
        }

        if ( dependencies.size() != normalized.size() )
        {
            builder.dependencies( normalized.values() );
        }

        return builder.build();
    }

    /**
     * DuplicateMerger
     */
    protected static class DuplicateMerger
        extends MavenModelMerger
    {

        public Plugin mergePlugin( Plugin target, Plugin source )
        {
            return super.mergePlugin( target, source, false, Collections.emptyMap() );
        }

    }

    @Override
    public Model injectDefaultValues( Model model, ModelBuildingRequest request, ModelProblemCollector problems )
    {
        Model.Builder builder = Model.newBuilder( model );

        builder.dependencies( injectList( model.getDependencies(), this::injectDependency ) );
        Build build = model.getBuild();
        if ( build != null )
        {
            Build newBuild = Build.newBuilder( build )
                    .plugins( injectList( build.getPlugins(), this::injectPlugin ) )
                    .build();
            builder.build( newBuild != build ? newBuild : null );
        }

        return builder.build();
    }

    private Plugin injectPlugin( Plugin p )
    {
        return Plugin.newBuilder( p )
                .dependencies( injectList( p.getDependencies(), this::injectDependency ) )
                .build();
    }

    private Dependency injectDependency( Dependency d )
    {
        // we cannot set this directly in the MDO due to the interactions with dependency management
        return StringUtils.isEmpty( d.getScope() ) ? Dependency.newBuilder( d ).scope( "compile" ).build() : d;
    }

    /**
     * Returns a list suited for the builders, i.e. null if not modified
     */
    private <T> List<T> injectList( List<T> list, Function<T, T> modifer )
    {
        List<T> newList = null;
        for ( int i = 0; i < list.size(); i++ )
        {
            T oldT = list.get( i );
            T newT = modifer.apply( oldT );
            if ( newT != oldT )
            {
                if ( newList == null )
                {
                    newList = new ArrayList<>( list );
                }
                newList.set( i, newT );
            }
       }
        return newList;
    }

}
