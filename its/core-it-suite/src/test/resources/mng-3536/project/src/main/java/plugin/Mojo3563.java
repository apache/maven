package plugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.model.Model;

import java.io.File;

/**
 * Maven Mojo for executing nunit tests
 *
 * @goal validate
 * @phase validate
 */
public class Mojo3563 extends AbstractMojo {

    /**
     * @parameter expression="${project}"
     */
    private MavenProject project;

    public void execute() throws MojoExecutionException, MojoFailureException {
        Model model = project.getModel();
        String property = model.getProperties().getProperty("test");
        if (property == null) {
            throw new MojoExecutionException("Could not find property.");
        }

        File testFile = new File(property.substring(property.indexOf(":") + 1));
        if (!testFile.exists()) {
            throw new MojoExecutionException("Test file does not exist: File = " + testFile.getAbsolutePath() + ", Property = " + property);
        }
        getLog().info("Property = " + property);

    }
}
