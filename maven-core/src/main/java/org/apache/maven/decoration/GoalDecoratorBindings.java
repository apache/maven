/* Created on Apr 9, 2004 */
package org.apache.maven.decoration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author jdcasey
 */
public class GoalDecoratorBindings
{

    private Map preGoals = new HashMap();
    private Map postGoals = new HashMap();
    private String defaultGoal = "compiler:compile";

    public GoalDecoratorBindings()
    {
    }

    public void addPreGoal( GoalDecorator decorator )
    {
        addGoalDecorator( decorator, preGoals );
    }

    public void addPostGoal( GoalDecorator decorator )
    {
        addGoalDecorator( decorator, postGoals );
    }

    public List getPreGoals( String goal )
    {
        List result = (List) preGoals.get( goal );
        if ( result == null )
        {
            result = Collections.EMPTY_LIST;
        }
        
        return Collections.unmodifiableList( result );
    }

    public List getPostGoals( String goal )
    {
        List result = (List) postGoals.get( goal );
        if ( result == null )
        {
            result = Collections.EMPTY_LIST;
        }
        
        return Collections.unmodifiableList( result );
    }

    private void addGoalDecorator( GoalDecorator decorator, Map decoratorMap )
    {
        String goal = decorator.getGoalToDecorate();
        
        List decorators = (List) decoratorMap.get( goal );
        if ( decorators == null )
        {
            decorators = new ArrayList();
            decoratorMap.put( goal, decorators );
        }

        decorators.add( decorator );
    }

    /**
     * @param defGoal
     */
    public void setDefaultGoal( String defGoal )
    {
        this.defaultGoal = defGoal;
    }

    public String getDefaultGoal()
    {
        return defaultGoal;
    }

}
