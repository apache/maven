// TODO Attach license header here.
package org.apache.maven.project;

/**
 * @author jdcasey
 *
 * Created on Feb 1, 2005
 */
public interface ProjectDefaultsInjector
{
    public static final String ROLE = DefaultProjectDefaultsInjector.class.getName();

    void injectDefaults( MavenProject project );
}