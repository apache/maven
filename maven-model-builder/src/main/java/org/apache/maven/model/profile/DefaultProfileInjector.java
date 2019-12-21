package org.apache.maven.model.profile;

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

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.model.Build;
import org.apache.maven.model.BuildBase;
import org.apache.maven.model.Model;
import org.apache.maven.model.ModelBase;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginContainer;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.Profile;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.model.ReportSet;
import org.apache.maven.model.Reporting;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.merge.MavenModelMerger;

/**
 * Handles profile injection into the model.
 *
 * @author Benjamin Bentmann
 */
@Named
@Singleton
@SuppressWarnings( { "checkstyle:methodname" } )
public class DefaultProfileInjector
    implements ProfileInjector
{

    private ProfileModelMerger merger = new ProfileModelMerger();

    @Override
    public void injectProfile( Model model, Profile profile, ModelBuildingRequest request,
                               ModelProblemCollector problems )
    {
        if ( profile != null )
        {
            merger.mergeModelBase( model, profile );

            if ( profile.getBuild() != null )
            {
                if ( model.getBuild() == null )
                {
                    model.setBuild( new Build() );
                }
                merger.mergeBuildBase( model.getBuild(), profile.getBuild() );
            }
        }
    }

    /**
     * ProfileModelMerger
     */
    protected static class ProfileModelMerger
        extends MavenModelMerger
    {

        public void mergeModelBase( ModelBase target, ModelBase source )
        {
            mergeModelBase( target, source, true, Collections.emptyMap() );
        }

        public void mergeBuildBase( BuildBase target, BuildBase source )
        {
            mergeBuildBase( target, source, true, Collections.emptyMap() );
        }

        @Override
        protected void mergePluginContainer_Plugins( PluginContainer target, PluginContainer source,
                                                     boolean sourceDominant, Map<Object, Object> context )
        {
            List<Plugin> src = source.getPlugins();
            if ( !src.isEmpty() )
            {
                List<Plugin> tgt = target.getPlugins();
                Map<Object, Plugin> master = new LinkedHashMap<>( tgt.size() * 2 );

                for ( Plugin element : tgt )
                {
                    Object key = getPluginKey().apply( element );
                    master.put( key, element );
                }

                Map<Object, List<Plugin>> predecessors = new LinkedHashMap<>();
                List<Plugin> pending = new ArrayList<>();
                for ( Plugin element : src )
                {
                    Object key = getPluginKey().apply( element );
                    Plugin existing = master.get( key );
                    if ( existing != null )
                    {
                        mergePlugin( existing, element, sourceDominant, context );

                        if ( !pending.isEmpty() )
                        {
                            predecessors.put( key, pending );
                            pending = new ArrayList<>();
                        }
                    }
                    else
                    {
                        pending.add( element );
                    }
                }

                List<Plugin> result = new ArrayList<>( src.size() + tgt.size() );
                for ( Map.Entry<Object, Plugin> entry : master.entrySet() )
                {
                    List<Plugin> pre = predecessors.get( entry.getKey() );
                    if ( pre != null )
                    {
                        result.addAll( pre );
                    }
                    result.add( entry.getValue() );
                }
                result.addAll( pending );

                target.setPlugins( result );
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
                    new LinkedHashMap<>( ( src.size() + tgt.size() ) * 2 );

                for ( PluginExecution element : tgt )
                {
                    Object key = getPluginExecutionKey().apply( element );
                    merged.put( key, element );
                }

                for ( PluginExecution element : src )
                {
                    Object key = getPluginExecutionKey().apply( element );
                    PluginExecution existing = merged.get( key );
                    if ( existing != null )
                    {
                        mergePluginExecution( existing, element, sourceDominant, context );
                    }
                    else
                    {
                        merged.put( key, element );
                    }
                }

                target.setExecutions( new ArrayList<>( merged.values() ) );
            }
        }

        @Override
        protected void mergeReporting_Plugins( Reporting target, Reporting source, boolean sourceDominant,
                                               Map<Object, Object> context )
        {
            List<ReportPlugin> src = source.getPlugins();
            if ( !src.isEmpty() )
            {
                List<ReportPlugin> tgt = target.getPlugins();
                Map<Object, ReportPlugin> merged =
                    new LinkedHashMap<>( ( src.size() + tgt.size() ) * 2 );

                for ( ReportPlugin element : tgt )
                {
                    Object key = getReportPluginKey().apply( element );
                    merged.put( key, element );
                }

                for ( ReportPlugin element : src )
                {
                    Object key = getReportPluginKey().apply( element );
                    ReportPlugin existing = merged.get( key );
                    if ( existing == null )
                    {
                        merged.put( key, element );
                    }
                    else
                    {
                        mergeReportPlugin( existing, element, sourceDominant, context );
                    }
                }

                target.setPlugins( new ArrayList<>( merged.values() ) );
            }
        }

        @Override
        protected void mergeReportPlugin_ReportSets( ReportPlugin target, ReportPlugin source, boolean sourceDominant,
                                                     Map<Object, Object> context )
        {
            List<ReportSet> src = source.getReportSets();
            if ( !src.isEmpty() )
            {
                List<ReportSet> tgt = target.getReportSets();
                Map<Object, ReportSet> merged = new LinkedHashMap<>( ( src.size() + tgt.size() ) * 2 );

                for ( ReportSet element : tgt )
                {
                    Object key = getReportSetKey().apply( element );
                    merged.put( key, element );
                }

                for ( ReportSet element : src )
                {
                    Object key = getReportSetKey().apply( element );
                    ReportSet existing = merged.get( key );
                    if ( existing != null )
                    {
                        mergeReportSet( existing, element, sourceDominant, context );
                    }
                    else
                    {
                        merged.put( key, element );
                    }
                }

                target.setReportSets( new ArrayList<>( merged.values() ) );
            }
        }

    }

}
