package org.apache.maven.plugin;

import org.apache.maven.MavenTestCase;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.user.UserModel;
import org.apache.maven.monitor.event.DefaultEventDispatcher;
import org.apache.maven.monitor.logging.DefaultLog;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusContainer;

import java.io.File;
import java.util.Collections;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl </a>
 * @version $Id: PluginParameterExpressionEvaluatorTest.java,v 1.5 2005/03/08
 *          06:06:21 jdcasey Exp $
 */
public class PluginParameterExpressionEvaluatorTest
    extends MavenTestCase
{
    private MavenProject project;

    protected void setUp() throws Exception
    {
        super.setUp();

        File f = getTestFile( "src/test/resources/pom.xml" );

        project = getProject( f );
    }

    public void testValueExtractionWithAPomValueContainingAPath() throws Exception
    {
        String expected = getTestFile( "target/test-classes/target/classes" ).getCanonicalPath();

        ArtifactRepository repo = new ArtifactRepository( "local", "here" );
        PluginManager mgr = (PluginManager) lookup( PluginManager.ROLE );

        PlexusContainer container = getContainer();
        MavenSession session = new MavenSession( container,
                                                 mgr,
                                                 new UserModel(),
                                                 repo,
                                                 new DefaultEventDispatcher(),
                                                 new DefaultLog( container.getLogger() ),
                                                 Collections.EMPTY_LIST );

        Build build = new Build();
        build.setDirectory( expected.substring( 0, expected.length() - "/classes".length() ) );

        Model model = new Model();
        model.setBuild( build );

        MavenProject project = new MavenProject( model );
        project.setFile( new File( "pom.xml" ).getCanonicalFile() );

        session.setProject( project );

        Object value = PluginParameterExpressionEvaluator.evaluate( "#project.build.directory/classes", session );

        String actual = new File( value.toString() ).getCanonicalPath();

        System.out.println( "Expected value: " + expected );
        System.out.println( "Resolved value: " + actual );

        assertEquals( expected, actual );
    }

    public void testParameterThatIsAComponent() throws Exception
    {
        String role = "#component.org.apache.maven.project.MavenProjectBuilder";

        ArtifactRepository repo = new ArtifactRepository( "test", "http://www.test.com" );
        PluginManager mgr = (PluginManager) lookup( PluginManager.ROLE );

        PlexusContainer container = getContainer();
        MavenSession session = new MavenSession( container,
                                                 mgr,
                                                 new UserModel(),
                                                 repo,
                                                 new DefaultEventDispatcher(),
                                                 new DefaultLog( container.getLogger() ),
                                                 Collections.EMPTY_LIST );
        Object value = PluginParameterExpressionEvaluator.evaluate( role, session );

        assertNotNull( value );
    }

    public void testLocalRepositoryExtraction() throws Exception
    {
        ArtifactRepository repo = new ArtifactRepository( "local", "target/repo" );
        PluginManager mgr = (PluginManager) lookup( PluginManager.ROLE );

        PlexusContainer container = getContainer();
        MavenSession session = new MavenSession( container,
                                                 mgr,
                                                 new UserModel(),
                                                 repo,
                                                 new DefaultEventDispatcher(),
                                                 new DefaultLog( container.getLogger() ),
                                                 Collections.EMPTY_LIST );

        Object value = PluginParameterExpressionEvaluator.evaluate( "#localRepository", session );

        assertEquals( "local", ((ArtifactRepository) value).getId() );
    }
}