package org.apache.maven.tools.plugin.scanner;

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

/**
 * @author jdcasey
 */
public class DefaultMojoScannerTest
    extends TestCase
{

    public void testShouldFindOneDescriptorFromTestExtractor()
        throws Exception
    {
        Map extractors = Collections.singletonMap( "test", new TestExtractor() );

        MojoScanner scanner = new DefaultMojoScanner( extractors );

        Build build = new Build();
        build.setSourceDirectory( "testdir" );

        Model model = new Model();
        model.setBuild( build );

        MavenProject project = new MavenProject( model );
        project.setFile( new File( "." ) );

        PluginDescriptor pluginDescriptor = new PluginDescriptor();
        pluginDescriptor.setGroupId( "groupId" );
        pluginDescriptor.setArtifactId( "artifactId" );
        pluginDescriptor.setVersion( "version" );
        pluginDescriptor.setGoalPrefix( "testId" );
        
        scanner.populatePluginDescriptor( project, pluginDescriptor );

        List descriptors = pluginDescriptor.getMojos();
        
        assertEquals( 1, descriptors.size() );

        MojoDescriptor desc = (MojoDescriptor) descriptors.iterator().next();
        assertEquals( pluginDescriptor, desc.getPluginDescriptor() );
        assertEquals( "testGoal", desc.getGoal() );
    }

}