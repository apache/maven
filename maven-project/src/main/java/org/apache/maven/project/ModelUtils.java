package org.apache.maven.project;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.model.BuildBase;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Goal;
import org.apache.maven.model.Model;
import org.apache.maven.model.ModelBase;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginContainer;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.model.ReportSet;
import org.apache.maven.model.Reporting;
import org.apache.maven.model.Repository;
import org.apache.maven.project.inheritance.DefaultModelInheritanceAssembler;
import org.apache.maven.project.inheritance.ModelInheritanceAssembler;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class ModelUtils
{
    public static void mergePluginLists( PluginContainer childContainer, PluginContainer parentContainer,
                                         boolean handleAsInheritance )
    {
        if ( childContainer == null || parentContainer == null )
        {
            // nothing to do.
            return;
        }

        List parentPlugins = parentContainer.getPlugins();

        if ( parentPlugins != null && !parentPlugins.isEmpty() )
        {
            Map assembledPlugins = new TreeMap();

            Map childPlugins = childContainer.getPluginsAsMap();

            for ( Iterator it = parentPlugins.iterator(); it.hasNext(); )
            {
                Plugin parentPlugin = (Plugin) it.next();

                String parentInherited = parentPlugin.getInherited();

                if ( !handleAsInheritance || parentInherited == null ||
                    Boolean.valueOf( parentInherited ).booleanValue() )
                {

                    Plugin assembledPlugin = parentPlugin;

                    Plugin childPlugin = (Plugin) childPlugins.get( parentPlugin.getKey() );

                    if ( childPlugin != null )
                    {
                        assembledPlugin = childPlugin;

                        ModelUtils.mergePluginDefinitions( childPlugin, parentPlugin, handleAsInheritance );
                    }

                    if ( handleAsInheritance && parentInherited == null )
                    {
                        assembledPlugin.unsetInheritanceApplied();
                    }

                    assembledPlugins.put( assembledPlugin.getKey(), assembledPlugin );
                }
            }

            for ( Iterator it = childPlugins.values().iterator(); it.hasNext(); )
            {
                Plugin childPlugin = (Plugin) it.next();

                if ( !assembledPlugins.containsKey( childPlugin.getKey() ) )
                {
                    assembledPlugins.put( childPlugin.getKey(), childPlugin );
                }
            }

            childContainer.setPlugins( new ArrayList( assembledPlugins.values() ) );

            childContainer.flushPluginMap();
        }
    }

    public static void mergePluginDefinitions( Plugin child, Plugin parent, boolean handleAsInheritance )
    {
        if ( child == null || parent == null )
        {
            // nothing to do.
            return;
        }

        if ( parent.isExtensions() )
        {
            child.setExtensions( true );
        }

        if ( child.getVersion() == null && parent.getVersion() != null )
        {
            child.setVersion( parent.getVersion() );
        }

        // merge the lists of goals that are not attached to an <execution/>
        ModelUtils.mergeGoalContainerDefinitions( child, parent );

        // from here to the end of the method is dealing with merging of the <executions/> section.
        String parentInherited = parent.getInherited();

        boolean parentIsInherited = parentInherited == null || Boolean.valueOf( parentInherited ).booleanValue();

        List parentExecutions = parent.getExecutions();

        if ( parentExecutions != null && !parentExecutions.isEmpty() )
        {
            Map assembledExecutions = new TreeMap();

            Map childExecutions = child.getExecutionsAsMap();

            for ( Iterator it = parentExecutions.iterator(); it.hasNext(); )
            {
                PluginExecution parentExecution = (PluginExecution) it.next();

                if ( !handleAsInheritance || parentIsInherited )
                {
                    PluginExecution assembled = parentExecution;

                    PluginExecution childExecution = (PluginExecution) childExecutions.get( parentExecution.getId() );

                    if ( childExecution != null )
                    {
                        ModelUtils.mergePluginExecutionDefinitions( childExecution, parentExecution );

                        assembled = childExecution;
                    }
                    else if ( handleAsInheritance && parentInherited == null )
                    {
                        parentExecution.unsetInheritanceApplied();
                    }

                    assembledExecutions.put( assembled.getId(), assembled );
                }
            }

            for ( Iterator it = childExecutions.entrySet().iterator(); it.hasNext(); )
            {
                Map.Entry entry = (Map.Entry) it.next();

                String id = (String) entry.getKey();

                if ( !assembledExecutions.containsKey( id ) )
                {
                    assembledExecutions.put( id, entry.getValue() );
                }
            }

            child.setExecutions( new ArrayList( assembledExecutions.values() ) );

            child.flushExecutionMap();
        }

    }

    /**
     * @param child
     * @param parent
     * @deprecated
     */
    private static void mergeGoalContainerDefinitions( Plugin child, Plugin parent )
    {
        List parentGoals = parent.getGoals();

        // if the supplemental goals are non-existent, then nothing related to goals changes.
        if ( parentGoals != null && !parentGoals.isEmpty() )
        {
            Map assembledGoals = new TreeMap();

            Map childGoals = child.getGoalsAsMap();

            if ( childGoals != null )
            {
                for ( Iterator it = parentGoals.iterator(); it.hasNext(); )
                {
                    Goal parentGoal = (Goal) it.next();

                    Goal assembledGoal = parentGoal;

                    Goal childGoal = (Goal) childGoals.get( parentGoal.getId() );

                    if ( childGoal != null )
                    {
                        Xpp3Dom childGoalConfig = (Xpp3Dom) childGoal.getConfiguration();
                        Xpp3Dom parentGoalConfig = (Xpp3Dom) parentGoal.getConfiguration();

                        childGoalConfig = Xpp3Dom.mergeXpp3Dom( childGoalConfig, parentGoalConfig );

                        childGoal.setConfiguration( childGoalConfig );

                        assembledGoal = childGoal;
                    }

                    assembledGoals.put( assembledGoal.getId(), assembledGoal );
                }

                for ( Iterator it = childGoals.entrySet().iterator(); it.hasNext(); )
                {
                    Map.Entry entry = (Map.Entry) it.next();

                    String key = (String) entry.getKey();
                    Goal childGoal = (Goal) entry.getValue();

                    if ( !assembledGoals.containsKey( key ) )
                    {
                        assembledGoals.put( key, childGoal );
                    }
                }

                child.setGoals( new ArrayList( assembledGoals.values() ) );

                child.flushGoalMap();
            }
        }

        Xpp3Dom childConfiguration = (Xpp3Dom) child.getConfiguration();
        Xpp3Dom parentConfiguration = (Xpp3Dom) parent.getConfiguration();

        childConfiguration = Xpp3Dom.mergeXpp3Dom( childConfiguration, parentConfiguration );

        child.setConfiguration( childConfiguration );
    }

    private static void mergePluginExecutionDefinitions( PluginExecution child, PluginExecution parent )
    {
        List parentGoals = parent.getGoals();

        // if the supplemental goals are non-existent, then nothing related to goals changes.
        if ( parentGoals != null && !parentGoals.isEmpty() )
        {
            List goals = new ArrayList( parentGoals );
            if ( child.getGoals() != null )
            {
                goals.addAll( child.getGoals() );
            }

            child.setGoals( goals );
        }

        Xpp3Dom childConfiguration = (Xpp3Dom) child.getConfiguration();
        Xpp3Dom parentConfiguration = (Xpp3Dom) parent.getConfiguration();

        childConfiguration = Xpp3Dom.mergeXpp3Dom( childConfiguration, parentConfiguration );

        child.setConfiguration( childConfiguration );
    }

    public static void mergeModelBases( ModelBase dominant, ModelBase recessive )
    {
        mergeDependencies( dominant, recessive );
        
        dominant.setRepositories( mergeRepositoryLists( dominant.getRepositories(), recessive.getRepositories() ) );
        dominant.setPluginRepositories( mergeRepositoryLists( dominant.getPluginRepositories(), recessive.getPluginRepositories() ) );
        
        mergeReporting( dominant, recessive );
        
        mergeDependencyManagementSections( dominant, recessive );
    }
    
    private static List mergeRepositoryLists( List dominantRepositories, List recessiveRepositories )
    {
        List repositories = new ArrayList();
        
        for ( Iterator it = dominantRepositories.iterator(); it.hasNext(); )
        {
            Repository repository = (Repository) it.next();
            
            repositories.add( repository );
        }
        
        for ( Iterator it = recessiveRepositories.iterator(); it.hasNext(); )
        {
            Repository repository = (Repository) it.next();
            
            if ( !repositories.contains( repository ) )
            {
                repositories.add( repository );
            }
        }
        
        return repositories;
    }

    private static void mergeDependencies( ModelBase dominant, ModelBase recessive )
    {
        Map depsMap = new HashMap();
        
        List deps = recessive.getDependencies();
        
        if ( deps != null )
        {
            for ( Iterator it = deps.iterator(); it.hasNext(); )
            {
                Dependency dependency = (Dependency) it.next();
                depsMap.put( dependency.getManagementKey(), dependency );
            }
        }
        
        deps = dominant.getDependencies();
        
        if ( deps != null )
        {
            for ( Iterator it = deps.iterator(); it.hasNext(); )
            {
                Dependency dependency = (Dependency) it.next();
                depsMap.put( dependency.getManagementKey(), dependency );
            }
        }
        
        dominant.setDependencies( new ArrayList( depsMap.values() ) );
    }

    public static void mergeReporting( ModelBase dominant, ModelBase recessive )
    {
        // Reports :: aggregate
        Reporting dominantReporting = dominant.getReporting();
        Reporting modelReporting = recessive.getReporting();

        if ( dominantReporting != null && modelReporting != null )
        {
            if ( StringUtils.isEmpty( dominantReporting.getOutputDirectory() ) )
            {
                dominantReporting.setOutputDirectory( modelReporting.getOutputDirectory() );
            }

            Map mergedReportPlugins = new HashMap();

            Map dominantReportersByKey = dominantReporting.getReportPluginsAsMap();

            List parentReportPlugins = modelReporting.getPlugins();

            if ( parentReportPlugins != null )
            {
                for ( Iterator it = parentReportPlugins.iterator(); it.hasNext(); )
                {
                    ReportPlugin recessiveReportPlugin = (ReportPlugin) it.next();

                    String inherited = recessiveReportPlugin.getInherited();

                    if ( StringUtils.isEmpty( inherited ) || Boolean.valueOf( inherited ).booleanValue() )
                    {
                        ReportPlugin dominantReportPlugin = (ReportPlugin) dominantReportersByKey.get(
                            recessiveReportPlugin.getKey() );

                        ReportPlugin mergedReportPlugin = recessiveReportPlugin;

                        if ( dominantReportPlugin != null )
                        {
                            mergedReportPlugin = dominantReportPlugin;

                            mergeReportPlugins( dominantReportPlugin, recessiveReportPlugin );
                        }
                        else if ( StringUtils.isEmpty( inherited ) )
                        {
                            mergedReportPlugin.unsetInheritanceApplied();
                        }

                        mergedReportPlugins.put( mergedReportPlugin.getKey(), mergedReportPlugin );
                    }
                }
            }

            for ( Iterator it = dominantReportersByKey.entrySet().iterator(); it.hasNext(); )
            {
                Map.Entry entry = (Map.Entry) it.next();

                String key = (String) entry.getKey();

                if ( !mergedReportPlugins.containsKey( key ) )
                {
                    mergedReportPlugins.put( key, entry.getValue() );
                }
            }

            dominantReporting.setPlugins( new ArrayList( mergedReportPlugins.values() ) );

            dominantReporting.flushReportPluginMap();
        }
    }

    public static void mergeDependencyManagementSections( ModelBase dominant, ModelBase recessive )
    {
        DependencyManagement recessiveDepMgmt = recessive.getDependencyManagement();

        DependencyManagement dominantDepMgmt = dominant.getDependencyManagement();

        if ( recessiveDepMgmt != null )
        {
            if ( dominantDepMgmt == null )
            {
                dominant.setDependencyManagement( recessiveDepMgmt );
            }
            else
            {
                List dominantDeps = dominantDepMgmt.getDependencies();

                Map mappedDominantDeps = new TreeMap();
                for ( Iterator it = dominantDeps.iterator(); it.hasNext(); )
                {
                    Dependency dep = (Dependency) it.next();
                    mappedDominantDeps.put( dep.getManagementKey(), dep );
                }

                for ( Iterator it = recessiveDepMgmt.getDependencies().iterator(); it.hasNext(); )
                {
                    Dependency dep = (Dependency) it.next();
                    if ( !mappedDominantDeps.containsKey( dep.getManagementKey() ) )
                    {
                        dominantDepMgmt.addDependency( dep );
                    }
                }
            }
        }
    }

    public static void mergeReportPlugins( ReportPlugin dominant, ReportPlugin recessive )
    {
        if ( StringUtils.isEmpty( dominant.getVersion() ) )
        {
            dominant.setVersion( recessive.getVersion() );
        }

        Xpp3Dom dominantConfig = (Xpp3Dom) dominant.getConfiguration();
        Xpp3Dom recessiveConfig = (Xpp3Dom) recessive.getConfiguration();

        dominant.setConfiguration( Xpp3Dom.mergeXpp3Dom( dominantConfig, recessiveConfig ) );

        Map mergedReportSets = new HashMap();

        Map dominantReportSetsById = dominant.getReportSetsAsMap();

        for ( Iterator it = recessive.getReportSets().iterator(); it.hasNext(); )
        {
            ReportSet recessiveReportSet = (ReportSet) it.next();

            String inherited = recessiveReportSet.getInherited();

            if ( StringUtils.isEmpty( inherited ) || Boolean.valueOf( inherited ).booleanValue() )
            {
                ReportSet dominantReportSet = (ReportSet) dominantReportSetsById.get( recessiveReportSet.getId() );

                ReportSet merged = recessiveReportSet;

                if ( dominantReportSet != null )
                {
                    merged = dominantReportSet;

                    Xpp3Dom recessiveRSConfig = (Xpp3Dom) recessiveReportSet.getConfiguration();
                    Xpp3Dom mergedRSConfig = (Xpp3Dom) merged.getConfiguration();

                    merged.setConfiguration( Xpp3Dom.mergeXpp3Dom( mergedRSConfig, recessiveRSConfig ) );

                    List mergedReports = merged.getReports();

                    if ( mergedReports == null )
                    {
                        mergedReports = new ArrayList();

                        merged.setReports( mergedReports );
                    }

                    List recessiveRSReports = recessiveReportSet.getReports();

                    if ( recessiveRSReports != null )
                    {
                        for ( Iterator reportIterator = recessiveRSReports.iterator(); reportIterator.hasNext(); )
                        {
                            String report = (String) reportIterator.next();

                            if ( !mergedReports.contains( report ) )
                            {
                                mergedReports.add( report );
                            }
                        }
                    }
                }
                else if ( StringUtils.isEmpty( inherited ) )
                {
                    merged.unsetInheritanceApplied();
                }

                mergedReportSets.put( merged.getId(), merged );
            }
        }

        for ( Iterator rsIterator = dominantReportSetsById.entrySet().iterator(); rsIterator.hasNext(); )
        {
            Map.Entry entry = (Map.Entry) rsIterator.next();

            String key = (String) entry.getKey();

            if ( !mergedReportSets.containsKey( key ) )
            {
                mergedReportSets.put( key, entry.getValue() );
            }
        }

        dominant.setReportSets( new ArrayList( mergedReportSets.values() ) );

        dominant.flushReportSetMap();
    }

    public static void mergeBuildBases( BuildBase dominant, BuildBase recessive )
    {
        // NOTE: This assumes that the dominant build is not null.
        //If it is null, the action taken should have been external to this method.
        
        // if the parent build is null, obviously we cannot inherit from it...
        if ( recessive != null )
        {
            if ( dominant.getDefaultGoal() == null )
            {
                dominant.setDefaultGoal( recessive.getDefaultGoal() );
            }

            if ( dominant.getFinalName() == null )
            {
                dominant.setFinalName( recessive.getFinalName() );
            }

            List resources = dominant.getResources();
            if ( resources == null || resources.isEmpty() )
            {
                dominant.setResources( recessive.getResources() );
            }

            resources = dominant.getTestResources();
            if ( resources == null || resources.isEmpty() )
            {
                dominant.setTestResources( recessive.getTestResources() );
            }

            // Plugins are aggregated if Plugin.inherit != false
            ModelUtils.mergePluginLists( dominant, recessive, true );

            // Plugin management :: aggregate
            PluginManagement dominantPM = dominant.getPluginManagement();
            PluginManagement recessivePM = recessive.getPluginManagement();

            if ( dominantPM == null && recessivePM != null )
            {
                dominant.setPluginManagement( recessivePM );
            }
            else
            {
                ModelUtils.mergePluginLists( dominant.getPluginManagement(), recessive.getPluginManagement(),
                                             false );
            }
        }
    }
    
    static Model cloneModel( Model model )
    {
        // TODO: would be nice for the modello:java code to generate this as a copy constructor
        Model newModel = new Model();
        ModelInheritanceAssembler assembler = new DefaultModelInheritanceAssembler();
        assembler.assembleModelInheritance( newModel, model );
        newModel.setVersion( model.getVersion() );
        newModel.setArtifactId( model.getArtifactId() );
        return newModel;
    }
}
