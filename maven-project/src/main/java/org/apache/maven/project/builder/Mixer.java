package org.apache.maven.project.builder;

import org.apache.maven.project.MavenProject;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Model;
import org.apache.maven.shared.model.InterpolatorProperty;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.io.IOException;
import java.util.List;


public interface Mixer 
{

    Model mixPlugin(Plugin plugin, Model model) throws IOException;

    PlexusConfiguration mixPluginAndReturnConfig(Plugin plugin, Xpp3Dom dom, Model model, List<InterpolatorProperty> props)
            throws IOException,  XmlPullParserException;

    Object mixPluginAndReturnConfigAsDom(Plugin plugin, Model model) throws IOException,
        XmlPullParserException;

    Object mixPluginAndReturnConfigAsDom(Plugin plugin, Model model, String xpathExpression) throws IOException,
        XmlPullParserException;    
}
