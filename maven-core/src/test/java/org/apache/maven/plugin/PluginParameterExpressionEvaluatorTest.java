package org.apache.maven.plugin;

import org.apache.maven.MavenTestCase;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.lifecycle.goal.MavenGoalExecutionContext;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;

import java.io.File;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class PluginParameterExpressionEvaluatorTest
    extends MavenTestCase
{
    private MavenProject project;

    private MavenGoalExecutionContext context;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        File f =  getTestFile( "src/test/resources/pom.xml" );

        project = getProject( f );

        context = createGoalExecutionContext();
    }

    public void testValueExtractionWithAPomValueContainingAPath()
        throws Exception
    {
        Object value = PluginParameterExpressionEvaluator.evaluate( "#project.build.directory/classes", context.getSession() );

        String expected = getTestFile( "target/test-classes/target/classes" ).getCanonicalPath();

        String actual = new File( value.toString() ).getCanonicalPath();

        assertEquals( expected, actual );
    }

    public void testParameterThatIsAComponent()
        throws Exception
    {
        String role = "#component.org.apache.maven.project.MavenProjectBuilder";

        Object value = PluginParameterExpressionEvaluator.evaluate( role, context.getSession() );

        assertNotNull( value );
    }

    public void testLocalRepositoryExtraction()
        throws Exception
    {
        Object value = PluginParameterExpressionEvaluator.evaluate( "#localRepository", context.getSession() );

        assertEquals( "local", ((ArtifactRepository)value).getId() );
    }
}
