/* Created on Sep 21, 2004 */
package org.apache.maven.util;

import java.util.Iterator;
import java.util.List;

import org.apache.maven.lifecycle.session.MavenSession;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.plugin.descriptor.MojoDescriptor;

/**
 * @author jdcasey
 */
public final class GoalWalker
{

    public static void walk( String goal, MavenSession session, GoalVisitor visitor ) throws GraphTraversalException
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