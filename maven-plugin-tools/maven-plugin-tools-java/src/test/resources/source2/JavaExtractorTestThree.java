package source2;

import org.apache.maven.plugin.AbstractMojo;

/**
 * Tests the implementation argument of the parameter annotation.
 *
 * @goal ideaThree
 * @requiresDependencyResolution compile
 */
public class JavaExtractorTestThree
    extends AbstractMojo
{
    /**
     * @parameter implementation=source2.sub.MyBla
     * @required
     */
    private Bla bla;

    public JavaExtractorTestThree()
    {
    }

    public void execute()
    {
        if ( getLog() != null )
        {
            getLog().info( "bla: " + bla );
        }
    }
}
