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

/**
 * @author jdcasey
 */
public interface GoalVisitor
{
    boolean shouldVisit( String goal, MavenSession session )
        throws GraphTraversalException;
    
    void preVisit( String goal, MavenSession session )
        throws GraphTraversalException;

    void visitGoal( String goal, MavenSession session )
        throws GraphTraversalException;

    void visitPreGoal( String goal, String preGoal, MavenSession session )
        throws GraphTraversalException;

    void visitPrereq( String goal, String prereq, MavenSession session )
        throws GraphTraversalException;

    void visitPostGoal( String goal, String postGoal, MavenSession session )
        throws GraphTraversalException;

    void postVisit( String goal, MavenSession session )
        throws GraphTraversalException;
}