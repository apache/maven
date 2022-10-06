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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.api.model.InputLocation;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.ModelBase;
import org.apache.maven.api.model.Plugin;
import org.apache.maven.api.model.PluginContainer;
import org.apache.maven.api.model.ReportPlugin;
import org.apache.maven.api.model.Reporting;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.merge.MavenModelMerger;
import org.codehaus.plexus.util.StringUtils;

/**
 * Handles inheritance of model values.
 *
 * @author Benjamin Bentmann
 */
@SuppressWarnings( { "checkstyle:methodname" } )
@Named
@Singleton
public class DefaultInheritanceAssembler
    implements InheritanceAssembler
{

    private static final String CHILD_DIRECTORY = "child-directory";

    private static final String CHILD_DIRECTORY_PROPERTY = "project.directory";

    private final InheritanceModelMerger merger = new InheritanceModelMerger();

    @Override
    public Model assembleModelInheritance( Model child, Model parent, ModelBuildingRequest request,
                                          ModelProblemCollector problems )
    {
        Map<Object, Object> hints = new HashMap<>();
        String childPath = child.getProperties().getOrDefault( CHILD_DIRECTORY_PROPERTY, child.getArtifactId() );
        hints.put( CHILD_DIRECTORY, childPath );
        hints.put( MavenModelMerger.CHILD_PATH_ADJUSTMENT, getChildPathAdjustment( child, parent, childPath ) );
        return merger.merge( child, parent, false, hints );
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
     * @param childDirectory The directory defined in child model, may be <code>null</code>.
     * @return The path adjustment, can be empty but never <code>null</code>.
     */
    private String getChildPathAdjustment( Model child, Model parent, String childDirectory )
    {
        String adjustment = "";

        if ( parent != null )
        {
            String childName = child.getArtifactId();

            /*
             * This logic (using filesystem, against wanted independence from the user environment) exists only for the
             * sake of backward-compat with 2.x (MNG-5000). In general, it is wrong to
             * base URL inheritance on the module directory names as this information is unavailable for POMs in the
             * repository. In other words, modules where artifactId != moduleDirName will see different effective URLs
             * depending on how the model was constructed (from filesystem or from repository).
             */
            if ( child.getProjectDirectory() != null )
            {
                childName = child.getProjectDirectory().getFileName().toString();
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

                if ( ( moduleName.equals( childName ) || ( moduleName.equals( childDirectory ) ) ) && lastSlash >= 0 )
                {
                    adjustment = module.substring( 0, lastSlash );
                    break;
                }
            }
        }

        return adjustment;
    }

    /**
     * InheritanceModelMerger
     */
    protected static class InheritanceModelMerger
        extends MavenModelMerger
    {

        @Override
        protected String extrapolateChildUrl( String parentUrl, boolean appendPath, Map<Object, Object> context )
        {
            Object childDirectory = context.get( CHILD_DIRECTORY );
            Object childPathAdjustment = context.get( CHILD_PATH_ADJUSTMENT );

            if ( StringUtils.isBlank( parentUrl ) || childDirectory == null || childPathAdjustment == null
                || !appendPath )
            {
                return parentUrl;
            }

            // append childPathAdjustment and childDirectory to parent url
            return appendPath( parentUrl, childDirectory.toString(), childPathAdjustment.toString() );
        }

        private String appendPath( String parentUrl, String childPath, String pathAdjustment )
        {
            StringBuilder url = new StringBuilder( parentUrl.length() + pathAdjustment.length() + childPath.length()
                + ( ( pathAdjustment.length() == 0 ) ? 1 : 2 ) );

            url.append( parentUrl );
            concatPath( url, pathAdjustment );
            concatPath( url, childPath );

            return url.toString();
        }

        private void concatPath( StringBuilder url, String path )
        {
            if ( path.length() > 0 )
            {
                boolean initialUrlEndsWithSlash = url.charAt( url.length() - 1 ) == '/';
                boolean pathStartsWithSlash = path.charAt( 0 ) ==  '/';

                if ( pathStartsWithSlash )
                {
                    if ( initialUrlEndsWithSlash )
                    {
                        // 1 extra '/' to remove
                        url.setLength( url.length() - 1 );
                    }
                }
                else if ( !initialUrlEndsWithSlash )
                {
                    // add missing '/' between url and path
                    url.append( '/' );
                }

                url.append( path );

                // ensure resulting url ends with slash if initial url was
                if ( initialUrlEndsWithSlash && !path.endsWith( "/" ) )
                {
                    url.append( '/' );
                }
            }
        }

        @Override
        protected void mergeModelBase_Properties( ModelBase.Builder builder,
                                                  ModelBase target, ModelBase source, boolean sourceDominant,
                                                  Map<Object, Object> context )
        {
            Map<String, String> merged = new HashMap<>();
            if ( sourceDominant )
            {
                merged.putAll( target.getProperties() );
                putAll( merged, source.getProperties(), CHILD_DIRECTORY_PROPERTY );
            }
            else
            {
                putAll( merged, source.getProperties(), CHILD_DIRECTORY_PROPERTY );
                merged.putAll( target.getProperties() );
            }
            builder.properties( merged );
            builder.location( "properties",
                                InputLocation.merge( target.getLocation( "properties" ),
                                                     source.getLocation( "properties" ), sourceDominant ) );
        }

        private void putAll( Map<String, String> s, Map<String, String> t, Object excludeKey )
        {
            for ( Map.Entry<String, String> e : t.entrySet() )
            {
                if ( !e.getKey().equals( excludeKey ) )
                {
                    s.put( e.getKey(), e.getValue() );
                }
            }
        }

        @Override
        protected void mergePluginContainer_Plugins( PluginContainer.Builder builder,
                                                     PluginContainer target, PluginContainer source,
                                                     boolean sourceDominant, Map<Object, Object> context )
        {
            List<Plugin> src = source.getPlugins();
            if ( !src.isEmpty() )
            {
                List<Plugin> tgt = target.getPlugins();
                Map<Object, Plugin> master = new LinkedHashMap<>( src.size() * 2 );

                for ( Plugin element : src )
                {
                    if ( element.isInherited() || !element.getExecutions().isEmpty() )
                    {
                        // NOTE: Enforce recursive merge to trigger merging/inheritance logic for executions
                        Plugin plugin = Plugin.newInstance( false );
                        plugin = mergePlugin( plugin, element, sourceDominant, context );

                        Object key = getPluginKey().apply( plugin );

                        master.put( key, plugin );
                    }
                }

                Map<Object, List<Plugin>> predecessors = new LinkedHashMap<>();
                List<Plugin> pending = new ArrayList<>();
                for ( Plugin element : tgt )
                {
                    Object key = getPluginKey().apply( element );
                    Plugin existing = master.get( key );
                    if ( existing != null )
                    {
                        element = mergePlugin( element, existing, sourceDominant, context );

                        master.put( key, element );

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

                builder.plugins( result );
            }
        }

        @Override
        protected Plugin mergePlugin( Plugin target, Plugin source,
                                      boolean sourceDominant, Map<Object, Object> context )
        {
            Plugin.Builder builder = Plugin.newBuilder( target );
            if ( source.isInherited() )
            {
                mergeConfigurationContainer( builder, target, source, sourceDominant, context );
            }
            mergePlugin_GroupId( builder, target, source, sourceDominant, context );
            mergePlugin_ArtifactId( builder, target, source, sourceDominant, context );
            mergePlugin_Version( builder, target, source, sourceDominant, context );
            mergePlugin_Extensions( builder, target, source, sourceDominant, context );
            mergePlugin_Executions( builder, target, source, sourceDominant, context );
            mergePlugin_Dependencies( builder, target, source, sourceDominant, context );
            return builder.build();
        }

        @Override
        protected void mergeReporting_Plugins( Reporting.Builder builder,
                                               Reporting target, Reporting source, boolean sourceDominant,
                                               Map<Object, Object> context )
        {
            List<ReportPlugin> src = source.getPlugins();
            if ( !src.isEmpty() )
            {
                List<ReportPlugin> tgt = target.getPlugins();
                Map<Object, ReportPlugin> merged =
                    new LinkedHashMap<>( ( src.size() + tgt.size() ) * 2 );

                for ( ReportPlugin element :  src )
                {
                    if ( element.isInherited() )
                    {
                        // NOTE: Enforce recursive merge to trigger merging/inheritance logic for executions as well
                        ReportPlugin plugin = ReportPlugin.newInstance();
                        plugin = mergeReportPlugin( plugin, element, sourceDominant, context );

                        merged.put( getReportPluginKey().apply( element ), plugin );
                    }
                }

                for ( ReportPlugin element : tgt )
                {
                    Object key = getReportPluginKey().apply( element );
                    ReportPlugin existing = merged.get( key );
                    if ( existing != null )
                    {
                        element = mergeReportPlugin( element, existing, sourceDominant, context );
                    }
                    merged.put( key, element );
                }

                builder.plugins( merged.values() );
            }
        }
    }

}
