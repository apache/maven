package org.apache.maven.artifact.handler;

import java.io.File;

import org.apache.maven.artifact.Artifact;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @version $Id$
 */
public class SourceHandler
    extends AbstractArtifactHandler
{
    public File source( String basedir, Artifact artifact )
    {
        return new File( basedir, artifact.getArtifactId() + "-" + artifact.getVersion() + "-sources." + extension() );
    }

    public String extension()
    {
        return "jar";
    }

    public String directory()
    {
        return "sources";
    }

    public String packageGoal()
    {
        return "source:source";
    }
}
