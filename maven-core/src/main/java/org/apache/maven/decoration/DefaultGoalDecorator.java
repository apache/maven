/* Created on Apr 6, 2004 */
package org.apache.maven.decoration;



/**
 * Default implementation of a goal decorator.
 *
 * @author <a href="mailto:jdcasey@commonjava.org">John Casey</a>
 */
public class DefaultGoalDecorator implements GoalDecorator
{

    private String goal;
    private String decoratorGoal;

    public DefaultGoalDecorator( String goal, String decoratorGoal )
    {
        this.goal = goal;
        this.decoratorGoal = decoratorGoal;
    }

    public String getGoalToDecorate()
    {
        return goal;
    }

    public String getDecoratorGoal()
    {
        return decoratorGoal;
    }
    
    public String toString()
    {
        return "DefaultGoalDecorator[decorate: " + goal + 
        	" with: " + decoratorGoal + "]"; 
    }

}
