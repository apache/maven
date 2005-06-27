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
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginContainer;
import org.apache.maven.model.PluginExecution;
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
