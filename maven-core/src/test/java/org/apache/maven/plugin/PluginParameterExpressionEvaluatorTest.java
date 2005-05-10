package org.apache.maven.plugin;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
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

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.monitor.event.DefaultEventDispatcher;
import org.apache.maven.monitor.logging.DefaultLog;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;

import java.io.File;
import java.util.Collections;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl </a>
 * @version $Id: PluginParameterExpressionEvaluatorTest.java,v 1.5 2005/03/08
 *          06:06:21 jdcasey Exp $
 */
public class PluginParameterExpressionEvaluatorTest
    extends PlexusTestCase
{
    public void testValueExtractionWithAPomValueContainingAPath()
        throws Exception
    {
        String expected = getTestFile( "target/test-classes/target/classes" ).getCanonicalPath();

        Build build = new Build();
        build.setDirectory( expected.substring( 0, expected.length() - "/classes".length() ) );

        Model model = new Model();
        model.setBuild( build );

        MavenProject project = new MavenProject( model );
        project.setFile( new File( "pom.xml" ).getCanonicalFile() );

        ExpressionEvaluator expressionEvaluator = createExpressionEvaluator( project );

        Object value = expressionEvaluator.evaluate( "${project.build.directory}/classes" );
        String actual = new File( value.toString() ).getCanonicalPath();

        assertEquals( expected, actual );
    }

    private static MavenSession createSession( MavenProject project, PlexusContainer container,
                                               ArtifactRepository repo )
    {
        return new MavenSession( project, container, new Settings(), repo, new DefaultEventDispatcher(), Collections.EMPTY_LIST );
    }

    public void testLocalRepositoryExtraction()
        throws Exception
    {
        ExpressionEvaluator expressionEvaluator = createExpressionEvaluator( createDefaultProject() );
        Object value = expressionEvaluator.evaluate( "${localRepository}" );

        assertEquals( "local", ( (ArtifactRepository) value ).getId() );
    }

    public void testTwoExpressions()
        throws Exception
    {
        Build build = new Build();
        build.setDirectory( "expected-directory" );
        build.setFinalName( "expected-finalName" );

        Model model = new Model();
        model.setBuild( build );


        ExpressionEvaluator expressionEvaluator = createExpressionEvaluator( new MavenProject( model ) );

        Object value = expressionEvaluator.evaluate( "${project.build.directory}/${project.build.finalName}" );

        assertEquals( "expected-directory/expected-finalName", value );
    }

    private MavenProject createDefaultProject()
    {
        return new MavenProject( new Model() );
    }

    private ExpressionEvaluator createExpressionEvaluator( MavenProject project )
        throws Exception
    {
        ArtifactRepositoryLayout repoLayout = (ArtifactRepositoryLayout) lookup( ArtifactRepositoryLayout.ROLE,
                                                                                 "legacy" );

        ArtifactRepository repo = new ArtifactRepository( "local", "target/repo", repoLayout );

        PlexusContainer container = getContainer();
        MavenSession session = createSession( project, container, repo );

        ExpressionEvaluator expressionEvaluator = new PluginParameterExpressionEvaluator( session, null, container.getLogger() );
        return expressionEvaluator;
    }
}