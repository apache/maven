package org.apache.maven.plugin.coreit;

import java.util.Enumeration;
import java.util.Properties;

import org.apache.maven.model.Model;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

/**
 * @goal verify-property
 * @phase validate
 */
public class PropertyInterpolationVerifierMojo extends AbstractMojo {

    /**
     * @parameter expression="${project}"
     */
    private MavenProject project;

    /**
     * @parameter expression="${properties}"
     */
    private Properties properties;


    public void execute() throws MojoExecutionException, MojoFailureException {
        Model model = project.getModel();
        if (properties == null) {
            return;
        }

        Enumeration e = properties.propertyNames();
        while (e.hasMoreElements()) {
            String name = (String) e.nextElement();
            String value = properties.getProperty(name);
            if(!value.equals(model.getProperties().getProperty(name))) {
                throw new MojoExecutionException("Properties do not match: Name = " + name + ", Value = " + value);
            }

            if(value.indexOf("${") > -1) {
                 throw new MojoExecutionException("Unresolved value: Name = " + name + ", Value = " + value);
            }

            getLog().info("Property match: Name = " + name + ", Value = " + value);
        }
    }
}
