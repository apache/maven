
import org.apache.maven.plugin.AbstractMojo;

/**
 * Create an IDEA project file from a Maven project.
 *
 * @goal ideaOne
 *
 * @requiresDependencyResolution
 *
 */
public class JavaExtractorTestOne
    extends AbstractMojo
{
    /**
     * Maven project used to generate IDEA project files.
     * @parameter expression="#project"
     * @required
     */
    protected String[] project;

    public JavaExtractorTestOne()
    {
    }

    public void execute()
    {
    }
}
