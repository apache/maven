package org.apache.maven.tools.plugin.scanner;

import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.tools.plugin.extractor.MojoDescriptorExtractor;

import java.util.Collections;
import java.util.List;

/**
 * @author jdcasey
 */
public class TestExtractor
    implements MojoDescriptorExtractor
{

    public List execute( MavenProject project, PluginDescriptor pluginDescriptor )
    {
        MojoDescriptor desc = new MojoDescriptor();
        desc.setPluginDescriptor( pluginDescriptor );
        desc.setGoal( "testGoal" );
        
        return Collections.singletonList( desc );
    }

}