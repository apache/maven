package org.apache.maven.plugins.antWithRefs;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

public class ArtifactsTask
    extends Task
{

    public void execute()
        throws BuildException
    {
        log( String.valueOf( getProject().getReference( "artifacts" ) ) );
    }

}
