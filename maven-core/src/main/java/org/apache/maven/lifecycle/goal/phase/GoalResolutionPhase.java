package org.apache.maven.lifecycle.goal.phase;

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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.maven.lifecycle.goal.AbstractMavenGoalPhase;
import org.apache.maven.lifecycle.goal.GoalExecutionException;
import org.apache.maven.lifecycle.goal.MavenGoalExecutionContext;
import org.apache.maven.lifecycle.session.MavenSession;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.util.AbstractGoalVisitor;
import org.apache.maven.util.GoalWalker;
import org.apache.maven.util.GraphTraversalException;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl </a>
 * @version $Id$
 */
public class GoalResolutionPhase
    extends AbstractMavenGoalPhase
{

    public void execute( MavenGoalExecutionContext context ) throws GoalExecutionException
    {
        GoalResolutionVisitor visitor = new GoalResolutionVisitor( context.getSession().getPluginManager() );

        try
        {
            GoalWalker.walk( context.getGoalName(), context.getSession(), visitor );
        }
        catch ( GraphTraversalException e )
        {
            throw new GoalExecutionException("Cannot calculate goal execution chain", e);
        }
        
        context.setResolvedGoals(visitor.getExecutionChain());
    }

    public static final class GoalResolutionVisitor
        extends AbstractGoalVisitor
    {
        private PluginManager pluginManager;

        private List executionChain = new LinkedList();
        
        private Map prereqChains = new TreeMap();

        GoalResolutionVisitor( PluginManager pluginManager )
        {
            this.pluginManager = pluginManager;
        }

        public boolean shouldVisit( String goal, MavenSession session )
        {
            boolean result = !executionChain.contains( goal );
            
            return result;
        }

        public void visitGoal( String goal, MavenSession session )
        {
            executionChain.add( goal );
        }
        
        public void visitPostGoal( String goal, String postGoal, MavenSession session ) 
        throws GraphTraversalException
        {
            List chain = session.getExecutionChain( postGoal );
            
            executionChain.addAll( chain );
        }
        
        public void visitPreGoal( String goal, String preGoal, MavenSession session ) 
        throws GraphTraversalException
        {
            List chain = session.getExecutionChain( preGoal );
            
            executionChain.addAll( chain );
        }
        
        public void visitPrereq( String goal, String prereq, MavenSession session)
        throws GraphTraversalException
        {
            GoalWalker.walk( prereq, session, this );
        }
        
        public List getExecutionChain()
        {
            return executionChain;
        }

    }

}