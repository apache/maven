package org.apache.maven.util;

/* ====================================================================
 *   Copyright 2001-2004 The Apache Software Foundation.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * ====================================================================
 */

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.plugin.descriptor.MojoDescriptor;

import java.util.Iterator;
import java.util.List;

/**
 * @author jdcasey
 */
public final class GoalWalker
{
    public static void walk( String goal, MavenSession session, GoalVisitor visitor )
        throws GraphTraversalException
    {
        if ( visitor.shouldVisit( goal, session ) )
        {
            visitor.preVisit( goal, session );

            List preGoals = session.getPreGoals( goal );

            if ( preGoals != null )
            {
                for ( Iterator it = preGoals.iterator(); it.hasNext(); )
                {
                    String preGoal = (String) it.next();

                    visitor.visitPreGoal( goal, preGoal, session );
                }
            }

            PluginManager pluginManager = session.getPluginManager();

            MojoDescriptor mojoDescriptor = pluginManager.getMojoDescriptor( goal );

            if ( mojoDescriptor != null )
            {
                List prereqs = mojoDescriptor.getPrereqs();

                if ( prereqs != null )
                {
                    for ( Iterator it = prereqs.iterator(); it.hasNext(); )
                    {
                        String prereq = (String) it.next();

                        visitor.visitPrereq( goal, prereq, session );
                    }
                }
            }

            visitor.visitGoal( goal, session );

            List postGoals = session.getPostGoals( goal );

            if ( postGoals != null )
            {
                for ( Iterator it = postGoals.iterator(); it.hasNext(); )
                {
                    String postGoal = (String) it.next();

                    visitor.visitPostGoal( goal, postGoal, session );
                }
            }

            visitor.postVisit( goal, session );
        }
    }
}