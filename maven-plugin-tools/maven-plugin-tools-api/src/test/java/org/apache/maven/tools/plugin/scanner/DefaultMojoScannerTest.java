package org.apache.maven.tools.plugin.scanner;

import junit.framework.TestCase;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * @author jdcasey
 */
public class DefaultMojoScannerTest
    extends TestCase
{

    public void testShouldFindOneDescriptorFromTestExtractor() throws Exception
    {
        Map extractors = Collections.singletonMap( "test", new TestExtractor() );

        MojoScanner scanner = new DefaultMojoScanner( extractors );

        Build build = new Build();
        build.setSourceDirectory( "testdir" );

        Model model = new Model();
        model.setBuild( build );

        MavenProject project = new MavenProject( model );
        project.setFile( new File( "." ) );

        Set descriptors = scanner.execute( project );

        assertEquals( 1, descriptors.size() );

        MojoDescriptor desc = (MojoDescriptor) descriptors.iterator().next();
        assertEquals( "testPluginId", desc.getId() );
        assertEquals( "testGoal", desc.getGoal() );
    }

}