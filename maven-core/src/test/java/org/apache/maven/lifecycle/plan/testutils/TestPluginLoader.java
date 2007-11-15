package org.apache.maven.lifecycle.plan.testutils;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.MojoBindingUtils;
import org.apache.maven.lifecycle.model.MojoBinding;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.loader.PluginLoader;
import org.apache.maven.plugin.loader.PluginLoaderException;
import org.apache.maven.project.MavenProject;

import java.util.HashMap;
import java.util.Map;

public class TestPluginLoader
    implements PluginLoader
{

    private static final Map DEFAULT_PLUGIN_DESCRIPTORS;

    private static final Map DEFAULT_PLUGIN_PREFIXES;

    static
    {
        Map descriptors = new HashMap();
        Map prefixes = new HashMap();

        {
            PluginDescriptor pd = createPluginDescriptor( "maven-resources-plugin", "resources",
                                                          "org.apache.maven.plugins", "1" );

            createMojoDescriptor( pd, "resources" );
            createMojoDescriptor( pd, "testResources" );

            descriptors.put( pd.getId(), pd );
            prefixes.put( pd.getGoalPrefix(), pd );
        }

        {
            PluginDescriptor pd = createPluginDescriptor( "maven-compiler-plugin", "compiler",
                                                          "org.apache.maven.plugins", "1" );

            createMojoDescriptor( pd, "compile" );
            createMojoDescriptor( pd, "testCompile" );

            descriptors.put( pd.getId(), pd );
            prefixes.put( pd.getGoalPrefix(), pd );
        }

        {
            PluginDescriptor pd = createPluginDescriptor( "maven-surefire-plugin", "surefire",
                                                          "org.apache.maven.plugins", "1" );

            createMojoDescriptor( pd, "test" );

            descriptors.put( pd.getId(), pd );
            prefixes.put( pd.getGoalPrefix(), pd );
        }

        {
            PluginDescriptor pd = createPluginDescriptor( "maven-jar-plugin", "jar", "org.apache.maven.plugins", "1" );

            createMojoDescriptor( pd, "jar" );

            descriptors.put( pd.getId(), pd );
            prefixes.put( pd.getGoalPrefix(), pd );
        }

        DEFAULT_PLUGIN_DESCRIPTORS = descriptors;
        DEFAULT_PLUGIN_PREFIXES = prefixes;
    }

    private Map pluginDescriptors = new HashMap( DEFAULT_PLUGIN_DESCRIPTORS );

    private Map pluginPrefixes = new HashMap( DEFAULT_PLUGIN_PREFIXES );

    private Map components = new HashMap();

    public static MojoDescriptor createMojoDescriptor( PluginDescriptor pd, String goal )
    {
        MojoDescriptor md = new MojoDescriptor();
        md.setPluginDescriptor( pd );
        md.setGoal( goal );
        pd.addComponentDescriptor( md );

        return md;
    }

    public static PluginDescriptor createPluginDescriptor( String artifactId, String goalPrefix, String groupId,
                                                           String version )
    {
        PluginDescriptor pd = new PluginDescriptor();
        pd.setGroupId( groupId );
        pd.setArtifactId( artifactId );
        pd.setGoalPrefix( goalPrefix );
        pd.setVersion( version );

        return pd;
    }

    public PluginDescriptor findPluginForPrefix( String prefix, MavenProject project, MavenSession session )
        throws PluginLoaderException
    {
//        System.out.println( "Find plugin for prefix: " + prefix + " in project: " + project.getId() );

        return (PluginDescriptor) pluginPrefixes.get( prefix );
    }

    public PluginDescriptor loadPlugin( Plugin plugin, MavenProject project, MavenSession session )
        throws PluginLoaderException
    {
//        System.out.println( "Load plugin from model definition: " + plugin.getKey() + " in project: " + project.getId() );

        return (PluginDescriptor) pluginDescriptors.get( plugin.getKey() );
    }

    public PluginDescriptor loadPlugin( MojoBinding mojoBinding, MavenProject project, MavenSession session )
        throws PluginLoaderException
    {
//        System.out.println( "Load plugin for mojo binding: " + MojoBindingUtils.toString( mojoBinding )
//                            + " in project: " + project.getId() );

        return (PluginDescriptor) pluginDescriptors.get( MojoBindingUtils.createPluginKey( mojoBinding ) );
    }

    public PluginDescriptor loadReportPlugin( ReportPlugin plugin, MavenProject project, MavenSession session )
        throws PluginLoaderException
    {
        System.out.println( "Load report plugin from model definition: " + plugin.getKey() + " in project: "
                            + project.getId() );

        return (PluginDescriptor) pluginDescriptors.get( plugin.getKey() );
    }

    public PluginDescriptor loadReportPlugin( MojoBinding mojoBinding, MavenProject project, MavenSession session )
        throws PluginLoaderException
    {
        System.out.println( "Load report plugin for mojo binding: " + MojoBindingUtils.toString( mojoBinding )
                            + " in project: " + project.getId() );

        return (PluginDescriptor) pluginDescriptors.get( MojoBindingUtils.createPluginKey( mojoBinding ) );
    }

    public void addPluginDescriptor( PluginDescriptor pd )
    {
        pluginDescriptors.put( pd.getId(), pd );
        pluginPrefixes.put( pd.getGoalPrefix(), pd );
    }

}
