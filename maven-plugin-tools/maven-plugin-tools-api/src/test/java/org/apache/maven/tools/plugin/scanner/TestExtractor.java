package org.apache.maven.tools.plugin.scanner;

import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.tools.plugin.extractor.MojoDescriptorExtractor;

import java.util.Collections;
import java.util.Set;

/**
 * @author jdcasey
 */
public class TestExtractor
    implements MojoDescriptorExtractor
{

    public Set execute( MavenProject project )
        throws Exception
    {
        MojoDescriptor desc = new MojoDescriptor();
        desc.setId( "testPluginId" );
        desc.setGoal( "testGoal" );

        return Collections.singleton( desc );
    }

}