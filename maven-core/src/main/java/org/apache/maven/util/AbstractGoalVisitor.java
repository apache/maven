/* Created on Sep 21, 2004 */
package org.apache.maven.util;

import org.apache.maven.lifecycle.session.MavenSession;
import org.apache.maven.plugin.descriptor.MojoDescriptor;

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