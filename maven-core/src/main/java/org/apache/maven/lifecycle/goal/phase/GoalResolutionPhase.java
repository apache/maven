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

import org.apache.maven.lifecycle.goal.AbstractMavenGoalPhase;
import org.apache.maven.lifecycle.goal.GoalExecutionException;
import org.apache.maven.lifecycle.goal.MavenGoalExecutionContext;
import org.apache.maven.model.GoalDecorator;
import org.apache.maven.model.PreGoal;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.plugin.descriptor.MojoDescriptor;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl </a>
 * @version $Id$
 */
public class GoalResolutionPhase
    extends AbstractMavenGoalPhase
{
    public void execute( MavenGoalExecutionContext context ) throws GoalExecutionException
    {
        PluginManager pluginManager = context.getSession().getPluginManager();

        try
        {
            // First, start by retrieving the currently-requested goal.
            String goal = context.getGoalName();

            List resolvedGoals = resolveTopLevel( goal, new HashSet(), new LinkedList(), context, pluginManager );

            context.setResolvedGoals( resolvedGoals );

            if ( goal.indexOf( ":" ) < 0 )
            {
                goal = context.getProject().getType() + ":" + goal;
            }

            MojoDescriptor md = pluginManager.getMojoDescriptor( goal );

            context.setMojoDescriptor( md );
        }
        catch ( Exception e )
        {
            throw new GoalExecutionException( "Error resolving goals: ", e );
        }
        finally
        {
            context.release( pluginManager );
        }
    }

    private List resolveTopLevel( String goal, Set includedGoals, List results, MavenGoalExecutionContext context,
        PluginManager pluginManager ) throws Exception
    {

        // Ensure that the plugin for this goal is installed.
        pluginManager.verifyPluginForGoal( goal );

        // Retrieve the prereqs-driven execution path for this goal, using the
        // DAG.
        List work = pluginManager.getGoals( goal );

        // Reverse the original goals list to preserve encapsulation while
        // decorating.
        Collections.reverse( work );

        return resolveWithPrereqs( work, includedGoals, results, context, pluginManager );
    }

    private List resolveWithPrereqs( List work, Set includedGoals, List results, MavenGoalExecutionContext context,
        PluginManager pluginManager ) throws Exception
    {
        if ( !work.isEmpty() )
        {
            String goal = (String) work.remove( 0 );

            MojoDescriptor descriptor = context.getMojoDescriptor( goal );

            if ( descriptor.alwaysExecute() || !includedGoals.contains( goal ) )
            {
                List preGoals = new LinkedList();
                List allPreGoals = context.getProject().getModel().getPreGoals();
                for ( Iterator it = allPreGoals.iterator(); it.hasNext(); )
                {
                    PreGoal preGoal = (PreGoal) it.next();
                    if ( goal.equals( preGoal.getName() ) )
                    {
                        preGoals.add( preGoal.getAttain() );
                    }
                }

                results = resolveGoalDecorators( goal, true, includedGoals, results, context, pluginManager );

                results = resolveWithPrereqs( work, includedGoals, results, context, pluginManager );
                includedGoals.add( goal );
                results.add( goal );

                results = resolveGoalDecorators( goal, false, includedGoals, results, context, pluginManager );
            }
        }
        return results;
    }

    private List resolveGoalDecorators( String baseGoal, boolean usePreGoals, Set includedGoals, List results,
        MavenGoalExecutionContext context, PluginManager pluginManager ) throws Exception
    {
        List decorators = null;
        if ( usePreGoals )
        {
            decorators = context.getProject().getModel().getPreGoals();
        }
        else
        {
            decorators = context.getProject().getModel().getPostGoals();
        }

        for ( Iterator it = decorators.iterator(); it.hasNext(); )
        {
            GoalDecorator decorator = (GoalDecorator) it.next();
            if ( baseGoal.equals( decorator.getName() ) )
            {
                String goal = decorator.getAttain();
                resolveTopLevel( goal, includedGoals, results, context, pluginManager );
            }
        }

        return results;
    }

}