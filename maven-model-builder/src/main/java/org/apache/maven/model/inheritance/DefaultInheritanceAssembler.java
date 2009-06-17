package org.apache.maven.model.inheritance;

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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.Model;
import org.apache.maven.model.ModelBuildingRequest;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginContainer;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.model.Reporting;
import org.apache.maven.model.merge.MavenModelMerger;
import org.codehaus.plexus.component.annotations.Component;

/**
 * Handles inheritance of model values.
 * 
 * @author Benjamin Bentmann
 */
@Component( role = InheritanceAssembler.class )
public class DefaultInheritanceAssembler
    implements InheritanceAssembler
{

    private InheritanceModelMerger merger = new InheritanceModelMerger();

    public void assembleModelInheritance( Model child, Model parent, ModelBuildingRequest request )
    {
        Map<Object, Object> hints = new HashMap<Object, Object>();
        hints.put( MavenModelMerger.CHILD_PATH_ADJUSTMENT, getChildPathAdjustment( child, parent ) );
        merger.merge( child, parent, false, hints );
    }

    /**
     * Calculates the relative path from the base directory of the parent to the parent directory of the base directory
     * of the child. The general idea is to adjust inherited URLs to match the project layout (in SCM). This calculation
     * is only a heuristic based on our conventions. In detail, the algo relies on the following assumptions. The parent
     * uses aggregation and refers to the child via the modules section. The module path to the child is considered to
     * point at the POM rather than its base directory if the path ends with ".xml" (ignoring case). The name of the
     * child's base directory matches the artifact id of the child. Note that for the sake of independence from the user
     * environment, the filesystem is intentionally not used for the calculation.
     * 
     * @param child The child model, must not be <code>null</code>.
     * @param parent The parent model, may be <code>null</code>.
     * @return The path adjustment, can be empty but never <code>null</code>.
     */
    private String getChildPathAdjustment( Model child, Model parent )
    {
        String adjustment = "";

        if ( parent != null )
        {
            String childArtifactId = child.getArtifactId();

            for ( String module : parent.getModules() )
            {
                module = module.replace( '\\', '/' );

                if ( module.regionMatches( true, module.length() - 4, ".xml", 0, 4 ) )
                {
                    module = module.substring( 0, module.lastIndexOf( '/' ) + 1 );
                }

                String moduleName = module;
                if ( moduleName.endsWith( "/" ) )
                {
                    moduleName = moduleName.substring( 0, moduleName.length() - 1 );
                }

                int lastSlash = moduleName.lastIndexOf( '/' );

                moduleName = moduleName.substring( lastSlash + 1 );

                if ( moduleName.equals( childArtifactId ) && lastSlash >= 0 )
                {
                    adjustment = module.substring( 0, lastSlash );
                    break;
                }
            }
        }

        return adjustment;
    }

    private static class InheritanceModelMerger
        extends MavenModelMerger
    {

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
                    if ( element.isInherited() )
                    {
                        // NOTE: Enforce recursive merge to trigger merging/inheritance logic for executions as well
                        Plugin plugin = new Plugin();
                        plugin.setGroupId( element.getGroupId() );
                        plugin.setArtifactId( element.getArtifactId() );
                        mergePlugin( plugin, element, sourceDominant, context );

                        merged.put( key, plugin );
                    }
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

        @Override
        protected void mergeReporting_Plugins( Reporting target, Reporting source, boolean sourceDominant,
                                               Map<Object, Object> context )
        {
            List<ReportPlugin> src = source.getPlugins();
            if ( !src.isEmpty() )
            {
                List<ReportPlugin> tgt = target.getPlugins();
                Map<Object, ReportPlugin> merged =
                    new LinkedHashMap<Object, ReportPlugin>( ( src.size() + tgt.size() ) * 2 );

                for ( Iterator<ReportPlugin> it = src.iterator(); it.hasNext(); )
                {
                    ReportPlugin element = it.next();
                    Object key = getReportPluginKey( element );
                    if ( element.isInherited() )
                    {
                        // NOTE: Enforce recursive merge to trigger merging/inheritance logic for executions as well
                        ReportPlugin plugin = new ReportPlugin();
                        plugin.setGroupId( element.getGroupId() );
                        plugin.setArtifactId( element.getArtifactId() );
                        mergeReportPlugin( plugin, element, sourceDominant, context );

                        merged.put( key, plugin );
                    }
                }

                for ( Iterator<ReportPlugin> it = tgt.iterator(); it.hasNext(); )
                {
                    ReportPlugin element = it.next();
                    Object key = getReportPluginKey( element );
                    ReportPlugin existing = merged.get( key );
                    if ( existing != null )
                    {
                        mergeReportPlugin( element, existing, sourceDominant, context );
                    }
                    merged.put( key, element );
                }

                target.setPlugins( new ArrayList<ReportPlugin>( merged.values() ) );
            }
        }
    }

}
