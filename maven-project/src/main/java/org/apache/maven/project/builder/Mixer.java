package org.apache.maven.project.builder;

import org.apache.maven.project.MavenProject;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Model;
import org.codehaus.plexus.configuration.PlexusConfiguration;

import java.io.IOException;


public interface Mixer 
{

    Model mixPlugin(Plugin plugin, Model model) throws IOException;

    PlexusConfiguration mixPluginAndReturnConfig(Plugin plugin, Model model) throws IOException;
}
