/* Created on Jul 14, 2004 */
package org.apache.maven.decoration;

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

import junit.framework.TestCase;

/**
 * @author jdcasey
 */
public class GoalDecoratorBindingsTest extends TestCase {
    
    public void testShouldConstructWithNoArguments() {
        new GoalDecoratorBindings();
    }
    
    public void testShouldAddOnePreGoalAndRetrievePreGoalsListOfSizeOne() {
        String targetGoal = "compiler:compile";
        
        DefaultGoalDecorator decorator = new DefaultGoalDecorator(targetGoal, "compiler:init");
        
        GoalDecoratorBindings bindings = new GoalDecoratorBindings();
        bindings.addPreGoal(decorator);
        
        assertEquals(1, bindings.getPreGoals(targetGoal).size());
    }

    public void testShouldAddOnePostGoalAndRetrievePostGoalsListOfSizeOne() {
        String targetGoal = "compiler:compile";
        
        DefaultGoalDecorator decorator = new DefaultGoalDecorator(targetGoal, "compiler:init");
        
        GoalDecoratorBindings bindings = new GoalDecoratorBindings();
        bindings.addPostGoal(decorator);
        
        assertEquals(1, bindings.getPostGoals(targetGoal).size());
    }

    public void testShouldAddTwoPreGoalsAndRetrievePreGoalsListOfSizeTwo() {
        String targetGoal = "compiler:compile";
        
        DefaultGoalDecorator decorator = new DefaultGoalDecorator(targetGoal, "compiler:init");
        DefaultGoalDecorator decorator2 = new DefaultGoalDecorator(targetGoal, "compiler:clean");
        
        GoalDecoratorBindings bindings = new GoalDecoratorBindings();
        bindings.addPreGoal(decorator);
        bindings.addPreGoal(decorator2);
        
        assertEquals(2, bindings.getPreGoals(targetGoal).size());
    }

    public void testShouldAddTwoPostGoalsAndRetrievePostGoalsListOfSizeTwo() {
        String targetGoal = "compiler:compile";
        
        DefaultGoalDecorator decorator = new DefaultGoalDecorator(targetGoal, "compiler:init");
        DefaultGoalDecorator decorator2 = new DefaultGoalDecorator(targetGoal, "compiler:clean");
        
        GoalDecoratorBindings bindings = new GoalDecoratorBindings();
        bindings.addPostGoal(decorator);
        bindings.addPostGoal(decorator2);
        
        assertEquals(2, bindings.getPostGoals(targetGoal).size());
    }
    
    public void testShouldSetAndRetrieveDefaultGoal() {
        GoalDecoratorBindings bindings = new GoalDecoratorBindings();
        bindings.setDefaultGoal("jar:jar");
        
        assertEquals("jar:jar", bindings.getDefaultGoal());
    }

    public void testShouldGetDefaultGoalOfCompiler_CompileWhenNotExplicitlySet() {
        GoalDecoratorBindings bindings = new GoalDecoratorBindings();
        
        assertEquals("compiler:compile", bindings.getDefaultGoal());
    }

}
