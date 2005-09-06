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

import org.apache.maven.model.Goal;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginContainer;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.model.ReportSet;
import org.apache.maven.model.Reporting;
import org.apache.maven.model.Repository;
import org.apache.maven.project.inheritance.DefaultModelInheritanceAssembler;
import org.apache.maven.project.inheritance.ModelInheritanceAssembler;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.util.ArrayList;
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

                        mergePluginDefinitions( childPlugin, parentPlugin, handleAsInheritance );
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

    public static void mergeReportPluginLists( Reporting child, Reporting parent, boolean handleAsInheritance )
    {
        if ( child == null || parent == null )
        {
            // nothing to do.
            return;
        }

        List parentPlugins = parent.getPlugins();

        if ( parentPlugins != null && !parentPlugins.isEmpty() )
        {
            Map assembledPlugins = new TreeMap();

            Map childPlugins = child.getReportPluginsAsMap();

            for ( Iterator it = parentPlugins.iterator(); it.hasNext(); )
            {
                ReportPlugin parentPlugin = (ReportPlugin) it.next();

                String parentInherited = parentPlugin.getInherited();

                if ( !handleAsInheritance || parentInherited == null ||
                    Boolean.valueOf( parentInherited ).booleanValue() )
                {

                    ReportPlugin assembledPlugin = parentPlugin;

                    ReportPlugin childPlugin = (ReportPlugin) childPlugins.get( parentPlugin.getKey() );

                    if ( childPlugin != null )
                    {
                        assembledPlugin = childPlugin;

                        mergeReportPluginDefinitions( childPlugin, parentPlugin, handleAsInheritance );
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
                ReportPlugin childPlugin = (ReportPlugin) it.next();

                if ( !assembledPlugins.containsKey( childPlugin.getKey() ) )
                {
                    assembledPlugins.put( childPlugin.getKey(), childPlugin );
                }
            }

            child.setPlugins( new ArrayList( assembledPlugins.values() ) );

            child.flushReportPluginMap();
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
        mergeGoalContainerDefinitions( child, parent );

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
                        mergePluginExecutionDefinitions( childExecution, parentExecution );

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

    public static void mergeReportPluginDefinitions( ReportPlugin child, ReportPlugin parent,
                                                     boolean handleAsInheritance )
    {
        if ( child == null || parent == null )
        {
            // nothing to do.
            return;
        }

        if ( child.getVersion() == null && parent.getVersion() != null )
        {
            child.setVersion( parent.getVersion() );
        }

        // from here to the end of the method is dealing with merging of the <executions/> section.
        String parentInherited = parent.getInherited();

        boolean parentIsInherited = parentInherited == null || Boolean.valueOf( parentInherited ).booleanValue();

        List parentReportSets = parent.getReportSets();

        if ( parentReportSets != null && !parentReportSets.isEmpty() )
        {
            Map assembledReportSets = new TreeMap();

            Map childReportSets = child.getReportSetsAsMap();

            for ( Iterator it = parentReportSets.iterator(); it.hasNext(); )
            {
                ReportSet parentReportSet = (ReportSet) it.next();

                if ( !handleAsInheritance || parentIsInherited )
                {
                    ReportSet assembledReportSet = parentReportSet;

                    ReportSet childReportSet = (ReportSet) childReportSets.get( parentReportSet.getId() );

                    if ( childReportSet != null )
                    {
                        mergeReportSetDefinitions( childReportSet, parentReportSet );

                        assembledReportSet = childReportSet;
                    }
                    else if ( handleAsInheritance && parentInherited == null )
                    {
                        parentReportSet.unsetInheritanceApplied();
                    }

                    assembledReportSets.put( assembledReportSet.getId(), assembledReportSet );
                }
            }

            for ( Iterator it = childReportSets.entrySet().iterator(); it.hasNext(); )
            {
                Map.Entry entry = (Map.Entry) it.next();

                String id = (String) entry.getKey();

                if ( !assembledReportSets.containsKey( id ) )
                {
                    assembledReportSets.put( id, entry.getValue() );
                }
            }

            child.setReportSets( new ArrayList( assembledReportSets.values() ) );

            child.flushReportSetMap();
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
        if ( child.getPhase() == null )
        {
            child.setPhase( parent.getPhase() );
        }

        List parentGoals = parent.getGoals();
        List childGoals = child.getGoals();

        List goals = new ArrayList();

        if ( childGoals != null && !childGoals.isEmpty() )
        {
            goals.addAll( childGoals );
        }

        if ( parentGoals != null )
        {
            for ( Iterator goalIterator = parentGoals.iterator(); goalIterator.hasNext(); )
            {
                String goal = (String) goalIterator.next();

                if ( !goals.contains( goal ) )
                {
                    goals.add( goal );
                }
            }
        }

        child.setGoals( goals );

        Xpp3Dom childConfiguration = (Xpp3Dom) child.getConfiguration();
        Xpp3Dom parentConfiguration = (Xpp3Dom) parent.getConfiguration();

        childConfiguration = Xpp3Dom.mergeXpp3Dom( childConfiguration, parentConfiguration );

        child.setConfiguration( childConfiguration );
    }

    private static void mergeReportSetDefinitions( ReportSet child, ReportSet parent )
    {
        List parentReports = parent.getReports();
        List childReports = child.getReports();

        List reports = new ArrayList();

        if ( childReports != null && !childReports.isEmpty() )
        {
            reports.addAll( childReports );
        }

        if ( parentReports != null )
        {
            for ( Iterator i = parentReports.iterator(); i.hasNext(); )
            {
                String report = (String) i.next();

                if ( !reports.contains( report ) )
                {
                    reports.add( report );
                }
            }
        }

        child.setReports( reports );

        Xpp3Dom childConfiguration = (Xpp3Dom) child.getConfiguration();
        Xpp3Dom parentConfiguration = (Xpp3Dom) parent.getConfiguration();

        childConfiguration = Xpp3Dom.mergeXpp3Dom( childConfiguration, parentConfiguration );

        child.setConfiguration( childConfiguration );
    }

    static Model cloneModel( Model model )
    {
        // TODO: would be nice for the modello:java code to generate this as a copy constructor
        Model newModel = new Model();
        ModelInheritanceAssembler assembler = new DefaultModelInheritanceAssembler();
        newModel.setModelVersion( model.getModelVersion() );
        newModel.setName( model.getName() );
        newModel.setParent( cloneParent( model.getParent() ) );
        newModel.setVersion( model.getVersion() );
        newModel.setArtifactId( model.getArtifactId() );
        newModel.setModules( cloneModules( model.getModules() ) );
        assembler.assembleModelInheritance( newModel, model );
        return newModel;
    }

    private static List cloneModules( List modules )
    {
        if ( modules == null )
        {
            return modules;
        }
        return new ArrayList( modules );
    }

    private static Parent cloneParent( Parent parent )
    {
        if ( parent == null )
        {
            return parent;
        }

        Parent newParent = new Parent();
        newParent.setArtifactId( parent.getArtifactId() );
        newParent.setGroupId( parent.getGroupId() );
        newParent.setRelativePath( parent.getRelativePath() );
        newParent.setVersion( parent.getVersion() );
        return newParent;
    }

    public static List mergeRepositoryLists( List dominant, List recessive )
    {
        List repositories = new ArrayList();

        for ( Iterator it = dominant.iterator(); it.hasNext(); )
        {
            Repository repository = (Repository) it.next();

            repositories.add( repository );
        }

        for ( Iterator it = recessive.iterator(); it.hasNext(); )
        {
            Repository repository = (Repository) it.next();

            if ( !repositories.contains( repository ) )
            {
                repositories.add( repository );
            }
        }

        return repositories;
    }
}
