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

import org.apache.maven.lifecycle.session.MavenSession;

/**
 * @author jdcasey
 */
public abstract class AbstractGoalVisitor
    implements GoalVisitor
{
    protected AbstractGoalVisitor()
    {
    }

    public boolean shouldVisit( String goal, MavenSession session )
        throws GraphTraversalException
    {
        // visit all by default
        return true;
    }

    public void visitGoal( String goal, MavenSession session )
        throws GraphTraversalException
    {
        // do nothing by default
    }

    public void visitPostGoal( String goal, String postGoal, MavenSession session )
        throws GraphTraversalException
    {
        // do nothing by default
    }

    public void visitPreGoal( String goal, String preGoal, MavenSession session )
        throws GraphTraversalException
    {
        // do nothing by default
    }

    public void visitPrereq( String goal, String prereq, MavenSession session )
        throws GraphTraversalException
    {
        // do nothing by default
    }

    public void preVisit( String goal, MavenSession session )
        throws GraphTraversalException
    {
        // do nothing by default
    }

    public void postVisit( String goal, MavenSession session )
        throws GraphTraversalException
    {
        // do nothing by default
    }
}