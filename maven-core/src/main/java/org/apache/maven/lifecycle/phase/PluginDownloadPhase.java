package org.apache.maven.lifecycle.phase;

/*
 * Copyright 2001-2004 The Apache Software Foundation.
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

import org.apache.maven.lifecycle.AbstractMavenLifecyclePhase;
import org.apache.maven.lifecycle.MavenLifecycleContext;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.plugin.descriptor.MojoDescriptor;

/**
 * From the name of the goal we can determine the plugin that houses the
 * goal. The goal will be in the form pluginId:goalName so we take the pluginId
 * portion of the name and with that we can determine if the plugin is installed
 * or not. If the plugin is not installed then we can use the pluginId to retrieve
 * the plugin and install it for use.
 *
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class PluginDownloadPhase
    extends AbstractMavenLifecyclePhase
{
    public void execute( MavenLifecycleContext context )
        throws Exception
    {
        PluginManager pluginManager = (PluginManager) context.lookup( PluginManager.ROLE );

        String goalName = context.getGoalName();

        // m2 install

        // install the install plugin

        // if this is a dispatcher goal then turn install in to type:install

        // then the appropriate prereqs will be called

        // would be good to let the plugin manager deal with all of this

        pluginManager.verifyPluginForGoal( goalName );

        if ( goalName.indexOf( ":" ) < 0 )
        {
            goalName = context.getProject().getType() + ":" + goalName;

            System.out.println( "goalName inside plugin download phase = " + goalName );
        }

        MojoDescriptor md = pluginManager.getMojoDescriptor( goalName );

        context.setMojoDescriptor( md );
    }
}
