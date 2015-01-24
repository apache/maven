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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginContainer;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.model.Reporting;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelProblemCollector;
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

    @Override
    public void assembleModelInheritance( Model child, Model parent, ModelBuildingRequest request,
                                          ModelProblemCollector problems )
    {
        Map<Object, Object> hints = new HashMap<Object, Object>();
        hints.put( MavenModelMerger.CHILD_PATH_ADJUSTMENT, getChildPathAdjustment( child, parent ) );
        merger.merge( child, parent, false, hints );
    }

    /**
     * Calculates the relative path from the base directory of the parent to the parent directory of the base directory
     * of the child. The general idea is to adjust inherited URLs to match the project layout (in SCM).
     *
     * <p>This calculation is only a heuristic based on our conventions.
     * In detail, the algo relies on the following assumptions: <ul>
     * <li>The parent uses aggregation and refers to the child via the modules section</li>
     * <li>The module path to the child is considered to
     * point at the POM rather than its base directory if the path ends with ".xml" (ignoring case)</li>
     * <li>The name of the child's base directory matches the artifact id of the child.</li>
     * </ul>
     * Note that for the sake of independence from the user
     * environment, the filesystem is intentionally not used for the calculation.</p>
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
            String childName = child.getArtifactId();

            /*
             * This logic exists only for the sake of backward-compat with 2.x (MNG-5000). In generally, it is wrong to
             * base URL inheritance on the project directory names as this information is unavailable for POMs in the
             * repository. In other words, projects where artifactId != projectDirName will see different effective URLs
             * depending on how the POM was constructed.
             */
            File childDirectory = child.getProjectDirectory();
            if ( childDirectory != null )
            {
                childName = childDirectory.getName();
            }

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

                if ( moduleName.equals( childName ) && lastSlash >= 0 )
                {
                    adjustment = module.substring( 0, lastSlash );
                    break;
                }
            }
        }

        return adjustment;
    }

    protected static class InheritanceModelMerger
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
                Map<Object, Plugin> master = new LinkedHashMap<Object, Plugin>( src.size() * 2 );

                for ( Plugin element : src )
                {
                    if ( element.isInherited() || !element.getExecutions().isEmpty() )
                    {
                        // NOTE: Enforce recursive merge to trigger merging/inheritance logic for executions
                        Plugin plugin = new Plugin();
                        plugin.setLocation( "", element.getLocation( "" ) );
                        plugin.setGroupId( null );
                        mergePlugin( plugin, element, sourceDominant, context );

                        Object key = getPluginKey( element );

                        master.put( key, plugin );
                    }
                }

                Map<Object, List<Plugin>> predecessors = new LinkedHashMap<Object, List<Plugin>>();
                List<Plugin> pending = new ArrayList<Plugin>();
                for ( Plugin element : tgt )
                {
                    Object key = getPluginKey( element );
                    Plugin existing = master.get( key );
                    if ( existing != null )
                    {
                        mergePlugin( element, existing, sourceDominant, context );

                        master.put( key, element );

                        if ( !pending.isEmpty() )
                        {
                            predecessors.put( key, pending );
                            pending = new ArrayList<Plugin>();
                        }
                    }
                    else
                    {
                        pending.add( element );
                    }
                }

                List<Plugin> result = new ArrayList<Plugin>( src.size() + tgt.size() );
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
        protected void mergePlugin( Plugin target, Plugin source, boolean sourceDominant, Map<Object, Object> context )
        {
            if ( source.isInherited() )
            {
                mergeConfigurationContainer( target, source, sourceDominant, context );
            }
            mergePlugin_GroupId( target, source, sourceDominant, context );
            mergePlugin_ArtifactId( target, source, sourceDominant, context );
            mergePlugin_Version( target, source, sourceDominant, context );
            mergePlugin_Extensions( target, source, sourceDominant, context );
            mergePlugin_Dependencies( target, source, sourceDominant, context );
            mergePlugin_Executions( target, source, sourceDominant, context );
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

                for ( ReportPlugin element :  src )
                {
                    Object key = getReportPluginKey( element );
                    if ( element.isInherited() )
                    {
                        // NOTE: Enforce recursive merge to trigger merging/inheritance logic for executions as well
                        ReportPlugin plugin = new ReportPlugin();
                        plugin.setLocation( "", element.getLocation( "" ) );
                        plugin.setGroupId( null );
                        mergeReportPlugin( plugin, element, sourceDominant, context );

                        merged.put( key, plugin );
                    }
                }

                for ( ReportPlugin element : tgt )
                {
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
