package org.apache.maven.tools.plugin.scanner;

import org.apache.maven.project.MavenProject;

import java.util.Set;

/**
 * @author jdcasey
 */
public interface MojoScanner
{
    
    String ROLE = MojoScanner.class.getName();
    
    Set execute(MavenProject project) throws Exception;

}
