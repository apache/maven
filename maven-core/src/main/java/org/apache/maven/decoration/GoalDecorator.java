/* Created on Apr 5, 2004 */
package org.apache.maven.decoration;



/**
 * Represents a decorator which executes around the main execution of a goal, but which
 * fits outside the DAG, since it (a) cannot have prereqs, and (b) should decorate the execution
 * of all prereqs of the goal, to encapsulate the entire process.
 *
 * @author <a href="mailto:jdcasey@commonjava.org">John Casey</a>
 */
public interface GoalDecorator
{

    /** The goal to which this decorator is bound. */
    String getGoalToDecorate();
    
    /** The goal to execute when this decorator is triggered by execution of the bound goal target. 
     */
    String getDecoratorGoal();
    
}
