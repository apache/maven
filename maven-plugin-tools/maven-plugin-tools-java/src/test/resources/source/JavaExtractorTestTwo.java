
import org.apache.maven.plugin.AbstractPlugin;
import org.apache.maven.plugin.PluginExecutionRequest;
import org.apache.maven.plugin.PluginExecutionResponse;

/**
 * @goal ideaTwo
 * @description Create an IDEA project file from a Maven project.
 * @requiresDependencyResolution compile
 * @prereq foo
 * @prereq bar
 * @parameter name="project"
 * type="String[]"
 * required="true"
 * validator="org.foo.validator"
 * expression="#project"
 * description="Maven project used to generate IDEA project files."
 */
public class JavaExtractorTestTwo
    extends AbstractPlugin
{
    protected String var;

    public JavaExtractorTestTwo()
    {
    }

    public void execute( PluginExecutionRequest request, PluginExecutionResponse response )
        throws Exception
    {
    }
}
