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

import junit.framework.TestCase;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.decoration.GoalDecoratorBindings;
import org.apache.maven.lifecycle.MavenGoalExecutionContext;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.embed.Embedder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * @author jdcasey
 */
public class GoalDecorationPhaseTest extends TestCase
{

    private static final String BASEDIR = "./decoration-unitTest-project";

    private static final String DECORATOR_SCRIPT =
        "<decorators defaultGoal=\"jar:jar\">" +
        "<preGoal name=\"compiler:compile\" attain=\"compiler:init-fs\"/>" +
        "<preGoal name=\"compiler:compile\" attain=\"compiler:init-repo\"/>" +
        "<postGoal name=\"compiler:compile\" attain=\"test:test\"/>" +
        "<postGoal name=\"compiler:compile\" attain=\"jar:jar\"/>" +
        "</decorators>";

    protected void setUp()
    {
        File basedir = new File( BASEDIR );
        if ( !basedir.exists() )
        {
            basedir.mkdir();
        }

        File mavenScriptFile = new File( basedir, GoalDecorationPhase.MAVEN_SCRIPT );
        try
        {
            BufferedWriter out = new BufferedWriter( new FileWriter( mavenScriptFile ) );
            out.write( DECORATOR_SCRIPT );
            out.flush();
            out.close();
        }
        catch ( IOException e )
        {
            e.printStackTrace();
        }
    }

    protected void tearDown()
    {
        File basedir = new File( BASEDIR );
        File mavenScript = new File( basedir, GoalDecorationPhase.MAVEN_SCRIPT );
        mavenScript.delete();
        basedir.delete();
    }

    public void testShouldConstructWithNoArgs()
    {
        new GoalDecorationPhase();
    }

    public void testShouldParseDecoratorsFromFile() throws Exception
    {
        MavenProject project = new MavenProject( new Model() );
        project.setFile( new File( new File( BASEDIR ), "project.xml" ) );

        Embedder embedder = new Embedder();

        embedder.start();

        MojoDescriptor descriptor = new MojoDescriptor();

        ArtifactRepository localRepository = new ArtifactRepository();

        MavenGoalExecutionContext context = new MavenGoalExecutionContext( embedder.getContainer(),
                                                                   project,
                                                                   descriptor,
                                                                   localRepository );

        GoalDecorationPhase phase = new GoalDecorationPhase();

        phase.execute( context );

        GoalDecoratorBindings bindings = context.getGoalDecoratorBindings();

        assertNotNull( bindings );

        assertEquals( "jar:jar", bindings.getDefaultGoal() );

        assertEquals( 2, bindings.getPreGoals( "compiler:compile" ).size() );

        assertEquals( 2, bindings.getPostGoals( "compiler:compile" ).size() );
    }
}
