package org.apache.maven.lifecycle.goal.phase;

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

import org.apache.maven.lifecycle.goal.AbstractMavenGoalPhase;
import org.apache.maven.lifecycle.goal.GoalExecutionException;
import org.apache.maven.lifecycle.goal.MavenGoalExecutionContext;
import org.apache.maven.lifecycle.session.MavenSession;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.util.AbstractGoalVisitor;
import org.apache.maven.util.GoalWalker;
import org.apache.maven.util.GraphTraversalException;
import org.codehaus.plexus.util.dag.CycleDetectedException;

import java.util.HashSet;
import java.util.Set;

/**
 * @author jdcasey
 */
public class GoalMappingPhase
    extends AbstractMavenGoalPhase
{

    public void execute( MavenGoalExecutionContext context )
        throws GoalExecutionException
    {
        GoalMappingVisitor visitor = new GoalMappingVisitor();

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
        private Set visited = new HashSet();

        public void visitPrereq( String goal, String prereq, MavenSession session )
            throws GraphTraversalException
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

        public void visitPostGoal( String goal, String postGoal, MavenSession session )
            throws GraphTraversalException
        {
            GoalWalker.walk( postGoal, session, this );
        }

        public void visitPreGoal( String goal, String preGoal, MavenSession session )
            throws GraphTraversalException
        {
            GoalWalker.walk( preGoal, session, this );
        }

        public void visitGoal( String goal, MavenSession session )
        {
            session.addSingleExecution( goal );
            
            visited.add( goal );
        }

        public boolean shouldVisit( String goal, MavenSession session )
        {
            return !visited.contains( goal );
        }
    }
}