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

import org.apache.maven.lifecycle.goal.GoalNotFoundException;
import org.apache.maven.decoration.GoalDecorator;
import org.apache.maven.decoration.GoalDecoratorBindings;
import org.apache.maven.lifecycle.goal.AbstractMavenGoalPhase;
import org.apache.maven.lifecycle.goal.MavenGoalExecutionContext;
import org.apache.maven.lifecycle.goal.MavenGoalExecutionContext;
import org.apache.maven.lifecycle.goal.GoalExecutionException;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.plugin.descriptor.MojoDescriptor;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class GoalResolutionPhase extends AbstractMavenGoalPhase
{
    public void execute( MavenGoalExecutionContext context )
        throws GoalExecutionException
    {
        PluginManager pluginManager = context.getSession().getPluginManager();

        try
        {
            // First, start by retrieving the currently-requested goal.
            MojoDescriptor goalDescriptor = context.getMojoDescriptor();

            if ( goalDescriptor == null )
            {
                throw new GoalNotFoundException( context.getGoalName() );
            }

            String goal = goalDescriptor.getId();

            List resolvedGoals = resolveTopLevel( goal, new HashSet(), new LinkedList(), context, pluginManager );

            context.setResolvedGoals( resolvedGoals );
        }
        finally
        {
            context.release( pluginManager );
        }
    }

    private List resolveTopLevel( String goal, Set includedGoals, List results, MavenGoalExecutionContext context, PluginManager pluginManager )
    {

        // Retrieve the prereqs-driven execution path for this goal, using the DAG.
        List work = pluginManager.getGoals( goal );

        // Reverse the original goals list to preserve encapsulation while decorating.
        Collections.reverse( work );

        return resolveWithPrereqs( work, includedGoals, results, context, pluginManager );
    }

    private List resolveWithPrereqs( List work, Set includedGoals, List results, MavenGoalExecutionContext context, PluginManager pluginManager )
    {
        if ( !work.isEmpty() )
        {
            String goal = (String) work.remove( 0 );

            MojoDescriptor descriptor = context.getMojoDescriptor( goal );

            if ( descriptor.alwaysExecute() || !includedGoals.contains( goal ) )
            {
                GoalDecoratorBindings bindings = context.getGoalDecoratorBindings();
                if ( bindings != null )
                {
                    List preGoals = bindings.getPreGoals( goal );

                    results = resolveGoalDecorators( preGoals, includedGoals, results, context, pluginManager );
                }

                results = resolveWithPrereqs( work, includedGoals, results, context, pluginManager );
                includedGoals.add( goal );
                results.add( goal );

                if ( bindings != null )
                {
                    List postGoals = bindings.getPostGoals( goal );
                    results = resolveGoalDecorators( postGoals, includedGoals, results, context, pluginManager );
                }
            }
        }
        return results;
    }

    private List resolveGoalDecorators( List preGoals, Set includedGoals, List results, MavenGoalExecutionContext context, PluginManager pluginManager )
    {
        for ( Iterator it = preGoals.iterator(); it.hasNext(); )
        {
            GoalDecorator decorator = (GoalDecorator) it.next();
            String goal = decorator.getDecoratorGoal();
            resolveTopLevel( goal, includedGoals, results, context, pluginManager );
        }

        return results;
    }

}
