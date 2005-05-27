package org.apache.maven.project;

import org.apache.maven.model.Goal;
import org.apache.maven.model.Plugin;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
    
    public static void mergeSupplementalPluginDefinition( Plugin main, Plugin supplemental )
    {
        if ( main.getVersion() == null && supplemental.getVersion() != null )
        {
            main.setVersion( supplemental.getVersion() );
        }

        Map supplementalGoals = supplemental.getGoalsAsMap();

        List pluginGoals = main.getGoals();

        if ( pluginGoals != null )
        {
            for ( Iterator it = pluginGoals.iterator(); it.hasNext(); )
            {
                Goal pluginGoal = (Goal) it.next();

                Goal supplementalGoal = (Goal) supplementalGoals.get( pluginGoal.getId() );

                if ( supplementalGoal != null )
                {
                    Xpp3Dom pluginGoalConfig = (Xpp3Dom) pluginGoal.getConfiguration();
                    Xpp3Dom supplementalGoalConfig = (Xpp3Dom) supplementalGoal.getConfiguration();

                    pluginGoalConfig = Xpp3Dom.mergeXpp3Dom( pluginGoalConfig, supplementalGoalConfig );

                    pluginGoal.setConfiguration( pluginGoalConfig );
                }
            }
        }

        Xpp3Dom pluginConfiguration = (Xpp3Dom) main.getConfiguration();
        Xpp3Dom supplementalConfiguration = (Xpp3Dom) supplemental.getConfiguration();

        pluginConfiguration = Xpp3Dom.mergeXpp3Dom( pluginConfiguration, supplementalConfiguration );

        main.setConfiguration( pluginConfiguration );
    }

}
