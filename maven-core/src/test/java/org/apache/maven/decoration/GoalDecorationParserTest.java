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

import java.io.IOException;
import java.io.StringReader;

import org.xmlpull.v1.XmlPullParserException;

import junit.framework.TestCase;

/**
 * @author jdcasey
 */
public class GoalDecorationParserTest extends TestCase {
    
    private static final String SCRIPT_WITH_ONE_PREGOAL_NO_DEFAULT_GOAL = 
        "<decorators>" +
        "<preGoal name=\"compiler:compile\" attain=\"compiler:init\"/>" +
        "</decorators>";

    private static final String SCRIPT_WITH_ONE_POSTGOAL_NO_DEFAULT_GOAL = 
        "<decorators>" +
        "<postGoal name=\"compiler:compile\" attain=\"compiler:init\"/>" +
        "</decorators>";

    private static final String SCRIPT_WITH_DEFAULT_GOAL_NO_DECORATORS = 
        "<decorators defaultGoal=\"jar:jar\"/>";

    private static final String SCRIPT_WITH_ONE_POSTGOAL_ONE_PREGOAL_AND_DEFAULT_GOAL = 
        "<decorators defaultGoal=\"jar:jar\">" +
        "<preGoal name=\"compiler:compile\" attain=\"compiler:init\"/>" +
        "<postGoal name=\"compiler:compile\" attain=\"jar:initFS\"/>" +
        "</decorators>";

    public void testShouldConstructWithNoArgs() {
        new GoalDecorationParser();
    }
    
    public void testShouldParseOnePreGoalNoPostGoalsNoDefaultGoal() throws XmlPullParserException, IOException {
        GoalDecoratorBindings bindings = parse(SCRIPT_WITH_ONE_POSTGOAL_ONE_PREGOAL_AND_DEFAULT_GOAL);
        
        assertEquals(1, bindings.getPreGoals("compiler:compile").size());
    }
    
    public void testShouldParseNoPreGoalsOnePostGoalNoDefaultGoal() throws XmlPullParserException, IOException {
        GoalDecoratorBindings bindings = parse(SCRIPT_WITH_ONE_POSTGOAL_ONE_PREGOAL_AND_DEFAULT_GOAL);
        
        assertEquals(1, bindings.getPostGoals("compiler:compile").size());
    }
    
    public void testShouldParseNoPreGoalsNoPostGoalsWithDefaultGoal() throws XmlPullParserException, IOException {
        GoalDecoratorBindings bindings = parse(SCRIPT_WITH_ONE_POSTGOAL_ONE_PREGOAL_AND_DEFAULT_GOAL);
        
        assertEquals("jar:jar", bindings.getDefaultGoal());
    }
    
    public void testShouldParseOnePreGoalOnePostGoalWithDefaultGoal() throws XmlPullParserException, IOException {
        GoalDecoratorBindings bindings = parse(SCRIPT_WITH_ONE_POSTGOAL_ONE_PREGOAL_AND_DEFAULT_GOAL);
        
        assertEquals("jar:jar", bindings.getDefaultGoal());
        assertEquals(1, bindings.getPreGoals("compiler:compile").size());
        assertEquals(1, bindings.getPostGoals("compiler:compile").size());
    }
    
    private GoalDecoratorBindings parse(String inputString) throws XmlPullParserException, IOException {
        GoalDecorationParser parser = new GoalDecorationParser();
        StringReader reader = new StringReader(inputString);
        
        return parser.parse(reader);
    }
    
}
