package jar;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;

public abstract class AbstractMng2734Mojo
    extends AbstractMojo
{

    /**
     * Location of the file.
     * @parameter expression="${project.build.directory}/generated/src/main/java"
     * @required
     */
    private File generatorTargetDir;

    protected final File getGeneratorTargetDir()
    {
        return generatorTargetDir;
    }

}
