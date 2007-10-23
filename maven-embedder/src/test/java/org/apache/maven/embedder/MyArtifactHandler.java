package org.apache.maven.embedder;

import org.apache.maven.artifact.handler.ArtifactHandler;

/** @author Jason van Zyl */
public class MyArtifactHandler
    implements ArtifactHandler
{
    public String getExtension()
    {
        return "jar";
    }

    public String getDirectory()
    {
        throw new UnsupportedOperationException( "Not supported yet." );
    }

    public String getClassifier()
    {
        return null;
    }

    public String getPackaging()
    {
        return "mkleint";
    }

    public boolean isIncludesDependencies()
    {
        return false;
    }

    public String getLanguage()
    {
        return "java";
    }

    public boolean isAddedToClasspath()
    {
        return true;
    }
}
