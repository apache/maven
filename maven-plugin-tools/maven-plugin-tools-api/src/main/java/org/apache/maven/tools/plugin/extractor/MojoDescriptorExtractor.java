package org.apache.maven.tools.plugin.extractor;

import org.apache.maven.project.MavenProject;

import java.util.Set;

/**
 * @author jdcasey
 */
public interface MojoDescriptorExtractor
{
    
    String ROLE = MojoDescriptorExtractor.class.getName();

    Set execute(String sourceDir, MavenProject project) 
        throws Exception;
    
}
