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
public class OgnlProjectValueExtractorTest
    extends MavenTestCase
{
    private MavenProject project;

    private MavenProjectBuilder builder;

    private MavenGoalExecutionContext context;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        builder = (MavenProjectBuilder) lookup( MavenProjectBuilder.ROLE );

        File f =  new File( basedir, "src/test/resources/pom.xml" );

        project = builder.build( f );

        context = createGoalExecutionContext();
    }

    public void testValueExtractionWithAPomValueContainingAPath()
        throws Exception
    {
        Object value = OgnlProjectValueExtractor.evaluate( "#project.build.directory/classes", context );

        String expected = new File( basedir, "target/test-classes/target/classes" ).getCanonicalPath();

        String actual = new File( value.toString() ).getCanonicalPath();

        assertEquals( expected, actual );
    }

    public void testParameterThatIsAComponent()
        throws Exception
    {
        String role = "#component.org.apache.maven.project.MavenProjectBuilder";

        Object value = OgnlProjectValueExtractor.evaluate( role, context );

        assertNotNull( value );
    }

    public void testLocalRepositoryExtraction()
        throws Exception
    {
        Object value = OgnlProjectValueExtractor.evaluate( "#localRepository", context );

        assertEquals( "local", ((ArtifactRepository)value).getId() );
    }
}
