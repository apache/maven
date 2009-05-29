package org.apache.maven.project.artifact;

import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;

public class PluginArtifact
    extends DefaultArtifact
    implements ArtifactWithDependencies
{
    private Plugin plugin;

    public PluginArtifact( Plugin plugin, Artifact pluginArtifact )
    {
        super( plugin.getGroupId(), plugin.getArtifactId(), plugin.getVersion(), null, "maven-plugin", null, new PluginArtifactHandler() );
        this.plugin = plugin;
        setFile( pluginArtifact.getFile() );
        setResolved( true );
    }

    public List<Dependency> getDependencies()
    {
        return plugin.getDependencies();
    }
    
    static class PluginArtifactHandler
        implements ArtifactHandler
    {
        public String getClassifier()
        {
            return null;
        }

        public String getDirectory()
        {
            return null;
        }

        public String getExtension()
        {
            return "jar";
        }

        public String getLanguage()
        {
            return "none";
        }

        public String getPackaging()
        {
            return "maven-plugin";
        }

        public boolean isAddedToClasspath()
        {
            return true;
        }

        public boolean isIncludesDependencies()
        {
            return false;
        }
    }
}
