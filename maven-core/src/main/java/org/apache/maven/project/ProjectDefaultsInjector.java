// TODO Attach license header here.
package org.apache.maven.project;

/**
 * @author jdcasey
 *
 * Created on Feb 1, 2005
 */
public interface ProjectDefaultsInjector
{
    String ROLE = ProjectDefaultsInjector.class.getName();

    void injectDefaults( MavenProject project );
}