/* Created on Jul 14, 2004 */
package org.apache.maven.lifecycle.phase;

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

import org.apache.maven.MavenTestCase;
import org.apache.maven.decoration.GoalDecoratorBindings;
import org.apache.maven.lifecycle.MavenGoalExecutionContext;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

/**
 * @author jdcasey
 */
public class GoalDecorationPhaseTest
    extends MavenTestCase
{
    private static final String DECORATOR_SCRIPT =
        "<decorators defaultGoal=\"jar:jar\">" +
        "<preGoal name=\"compiler:compile\" attain=\"compiler:init-fs\"/>" +
        "<preGoal name=\"compiler:compile\" attain=\"compiler:init-repo\"/>" +
        "<postGoal name=\"compiler:compile\" attain=\"test:test\"/>" +
        "<postGoal name=\"compiler:compile\" attain=\"jar:jar\"/>" +
        "</decorators>";

    protected void setUp()
        throws Exception
    {
        super.setUp();

        File mavenScriptFile = new File( basedir, "target/test-classes/" + GoalDecorationPhase.MAVEN_SCRIPT );

        BufferedWriter out = new BufferedWriter( new FileWriter( mavenScriptFile ) );

        out.write( DECORATOR_SCRIPT );

        out.flush();

        out.close();
    }

    public void testShouldConstructWithNoArgs()
    {
        new GoalDecorationPhase();
    }

    public void testShouldParseDecoratorsFromFile() throws Exception
    {
        MavenGoalExecutionContext context = createGoalExecutionContext();

        GoalDecorationPhase phase = new GoalDecorationPhase();

        phase.execute( context );

        GoalDecoratorBindings bindings = context.getGoalDecoratorBindings();

        assertNotNull( bindings );

        assertEquals( "jar:jar", bindings.getDefaultGoal() );

        assertEquals( 2, bindings.getPreGoals( "compiler:compile" ).size() );

        assertEquals( 2, bindings.getPostGoals( "compiler:compile" ).size() );
    }
}
