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
import org.apache.maven.plugin.PluginExecutionResponse;

import java.util.Iterator;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class GoalAttainmentPhase
    extends AbstractMavenGoalPhase
{
    public void execute( MavenGoalExecutionContext context )
        throws GoalExecutionException
    {
        PluginExecutionResponse response;

        // TODO: remove - most likely empty as there are no prereqs and I don't think the pre/postGoals are being walked
        for ( Iterator it = context.getResolvedGoals().iterator(); it.hasNext(); )
        {
            String goalName = (String) it.next();

            response = context.getSession().getPluginManager().executeMojo( context.getSession(), goalName );

            if ( response.isExecutionFailure() )
            {
                context.setExecutionFailure( goalName, response.getFailureResponse() );

                break;
            }
        }
    }
}
