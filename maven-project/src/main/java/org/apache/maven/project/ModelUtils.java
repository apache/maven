package org.apache.maven.project;

import org.apache.maven.model.Goal;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginContainer;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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

public final class ModelUtils
{

    public static void mergePluginLists( PluginContainer childContainer, PluginContainer parentContainer,
                                        boolean handleAsInheritance )
    {
        if( childContainer == null || parentContainer == null )
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

                if ( !handleAsInheritance || parentInherited == null
                    || Boolean.valueOf( parentInherited ).booleanValue() )
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
        if( child == null || parent == null )
        {
            // nothing to do.
            return;
        }
        
        if ( child.getVersion() == null && parent.getVersion() != null )
        {
            child.setVersion( parent.getVersion() );
        }

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

                    String parentInherited = parentGoal.getInherited();

                    if ( !handleAsInheritance || parentInherited == null
                        || Boolean.valueOf( parentInherited ).booleanValue() )
                    {
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

                        if ( handleAsInheritance && parentInherited == null )
                        {
                            assembledGoal.unsetInheritanceApplied();
                        }

                        assembledGoals.put( assembledGoal.getId(), assembledGoal );
                    }
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

}
