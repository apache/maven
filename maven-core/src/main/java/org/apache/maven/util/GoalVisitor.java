/* Created on Sep 21, 2004 */
package org.apache.maven.util;

import org.apache.maven.lifecycle.session.MavenSession;
import org.apache.maven.plugin.descriptor.MojoDescriptor;

/**
 * @author jdcasey
 */
public interface GoalVisitor
{

    boolean shouldVisit( String goal, MavenSession session ) throws GraphTraversalException;
    
    void preVisit( String goal, MavenSession session ) throws GraphTraversalException;

    void visitGoal( String goal, MavenSession session ) throws GraphTraversalException;

    void visitPreGoal( String goal, String preGoal, MavenSession session ) throws GraphTraversalException;

    void visitPrereq( String goal, String prereq, MavenSession session ) throws GraphTraversalException;

    void visitPostGoal( String goal, String postGoal, MavenSession session ) throws GraphTraversalException;

    void postVisit( String goal, MavenSession session ) throws GraphTraversalException;

}