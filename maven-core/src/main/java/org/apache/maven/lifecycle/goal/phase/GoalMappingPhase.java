/* Created on Sep 21, 2004 */
package org.apache.maven.lifecycle.goal.phase;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.maven.lifecycle.goal.AbstractMavenGoalPhase;
import org.apache.maven.lifecycle.goal.GoalExecutionException;
import org.apache.maven.lifecycle.goal.MavenGoalExecutionContext;
import org.apache.maven.lifecycle.goal.phase.PluginResolutionPhase.PluginResolutionVisitor;
import org.apache.maven.lifecycle.session.MavenSession;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.util.AbstractGoalVisitor;
import org.apache.maven.util.GoalWalker;
import org.apache.maven.util.GraphTraversalException;
import org.codehaus.plexus.util.dag.CycleDetectedException;

/**
 * @author jdcasey
 */
public class GoalMappingPhase
    extends AbstractMavenGoalPhase
{

    public void execute( MavenGoalExecutionContext context ) throws GoalExecutionException
    {
        GoalMappingVisitor visitor = new GoalMappingVisitor( context.getSession().getPluginManager() );

        try
        {
            GoalWalker.walk( context.getGoalName(), context.getSession(), visitor );
        }
        catch ( GraphTraversalException e )
        {
            throw new GoalExecutionException( "Cannot resolve plugins required for goal execution chain", e );
        }

    }

    public static final class GoalMappingVisitor
        extends AbstractGoalVisitor
    {
        private PluginManager pluginManager;

        private Set visited = new HashSet();

        GoalMappingVisitor( PluginManager pluginManager )
        {
            this.pluginManager = pluginManager;
        }

        public void visitPrereq( String goal, String prereq, MavenSession session ) throws GraphTraversalException
        {
            GoalWalker.walk( prereq, session, this );
            
            try
            {
                session.addImpliedExecution( goal, prereq );
                
                visited.add( prereq );
            }
            catch ( CycleDetectedException e )
            {
                throw new GraphTraversalException( "Goal prereq causes goal-graph cycle", e );
            }
        }

        public void visitPostGoal( String goal, String postGoal, MavenSession session ) throws GraphTraversalException
        {
            GoalWalker.walk( postGoal, session, this );
        }

        public void visitPreGoal( String goal, String preGoal, MavenSession session ) throws GraphTraversalException
        {
            GoalWalker.walk( preGoal, session, this );
        }

        public void visitGoal( String goal, MavenSession session ) throws GraphTraversalException
        {
            session.addSingleExecution( goal );
            
            visited.add( goal );
        }

        public boolean shouldVisit( String goal, MavenSession session ) throws GraphTraversalException
        {
            boolean result = !visited.contains( goal );

            return result;
        }

    }
}