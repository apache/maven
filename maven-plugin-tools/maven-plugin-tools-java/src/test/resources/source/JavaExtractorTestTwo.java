
import org.apache.maven.plugin.AbstractMojo;

/**
 * Create an IDEA project file from a Maven project.
 * @goal ideaTwo
 * @requiresDependencyResolution compile
 */
public class JavaExtractorTestTwo
    extends AbstractMojo
{

    /**
     * Maven project used to generate IDEA project files.
     * @parameter
     * @required
     */
    private String[] project;

    public JavaExtractorTestTwo()
    {
    }

    public void execute()
    {
    }
}
