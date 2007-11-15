package org.apache.maven.lifecycle.binding;

import org.apache.maven.lifecycle.LifecycleLoaderException;
import org.apache.maven.lifecycle.LifecycleSpecificationException;
import org.apache.maven.lifecycle.model.LifecycleBindings;
import org.apache.maven.lifecycle.model.MojoBinding;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

public class DefaultLifecycleBindingManagerTest extends PlexusTestCase
{

    private LifecycleBindingManager mgr;

    public void setUp() throws Exception
    {
        super.setUp();

        mgr = (LifecycleBindingManager) lookup( LifecycleBindingManager.ROLE, "default" );
    }

    public void testLookup()
    {
        assertNotNull( mgr );
    }

    public void testGetBindingsForPackaging_TestMergePluginConfigToBinding()
        throws LifecycleLoaderException, LifecycleSpecificationException
    {
        Model model = new Model();
        model.setGroupId( "group" );
        model.setArtifactId( "artifact" );
        model.setVersion( "1" );

        Build build = new Build();
        model.setBuild( build );

        Plugin plugin = new Plugin();
        build.addPlugin( plugin );

        plugin.setGroupId( "org.apache.maven.plugins" );
        plugin.setArtifactId( "maven-compiler-plugin" );

        Properties pluginConfig = new Properties();

        pluginConfig.setProperty( "test", "value" );
        pluginConfig.setProperty( "test2", "other-value" );

        plugin.setConfiguration( createConfiguration( pluginConfig ) );

        MavenProject project = new MavenProject( model );

        LifecycleBindings lifecycleBindings = mgr.getBindingsForPackaging( project );

        List bindings = lifecycleBindings.getBuildBinding().getCompile().getBindings();

        assertNotNull( bindings );
        assertEquals( 1, bindings.size() );

        MojoBinding binding = (MojoBinding) bindings.get( 0 );

        Xpp3Dom config = (Xpp3Dom) binding.getConfiguration();

        assertNotNull( config );

        assertEquals( "value", config.getChild( "test" ).getValue() );
        assertEquals( "other-value", config.getChild( "test2" ).getValue() );
    }

    public void testGetBindingsForPackaging_TestMergePluginManagementConfigToBinding()
        throws LifecycleLoaderException, LifecycleSpecificationException
    {
        Model model = new Model();
        model.setGroupId( "group" );
        model.setArtifactId( "artifact" );
        model.setVersion( "1" );

        Build build = new Build();
        model.setBuild( build );

        PluginManagement plugMgmt = new PluginManagement();
        build.setPluginManagement( plugMgmt );

        Plugin plugin = new Plugin();
        plugMgmt.addPlugin( plugin );

        plugin.setGroupId( "org.apache.maven.plugins" );
        plugin.setArtifactId( "maven-compiler-plugin" );

        Properties pluginConfig = new Properties();

        pluginConfig.setProperty( "test", "value" );
        pluginConfig.setProperty( "test2", "other-value" );

        plugin.setConfiguration( createConfiguration( pluginConfig ) );

        MavenProject project = new MavenProject( model );

        LifecycleBindings lifecycleBindings = mgr.getBindingsForPackaging( project );

        List bindings = lifecycleBindings.getBuildBinding().getCompile().getBindings();

        assertNotNull( bindings );
        assertEquals( 1, bindings.size() );

        MojoBinding binding = (MojoBinding) bindings.get( 0 );

        Xpp3Dom config = (Xpp3Dom) binding.getConfiguration();

        assertNotNull( config );

        assertEquals( "value", config.getChild( "test" ).getValue() );
        assertEquals( "other-value", config.getChild( "test2" ).getValue() );
    }

    public void testGetProjectCustomBindings_ExecutionConfigShouldOverridePluginConfig()
        throws LifecycleLoaderException, LifecycleSpecificationException
    {
        Model model = new Model();
        model.setGroupId( "group" );
        model.setArtifactId( "artifact" );
        model.setVersion( "1" );

        Build build = new Build();
        model.setBuild( build );

        Plugin plugin = new Plugin();
        build.addPlugin( plugin );

        plugin.setGroupId( "plugin.group" );
        plugin.setArtifactId( "plugin-artifact" );
        plugin.setVersion( "1" );

        Properties pluginConfig = new Properties();

        pluginConfig.setProperty( "test", "value" );
        pluginConfig.setProperty( "test2", "other-value" );

        plugin.setConfiguration( createConfiguration( pluginConfig ) );

        PluginExecution exec = new PluginExecution();
        plugin.addExecution( exec );

        exec.setId( "test-execution" );
        exec.setPhase( "validate" );
        exec.setGoals( Collections.singletonList( "goal" ) );

        Properties execConfig = new Properties();
        execConfig.setProperty( "test", "value2" );

        exec.setConfiguration( createConfiguration( execConfig ) );

        MavenProject project = new MavenProject( model );

        LifecycleBindings lifecycleBindings = mgr.getProjectCustomBindings( project, null );

        List bindings = lifecycleBindings.getBuildBinding().getValidate().getBindings();

        assertNotNull( bindings );
        assertEquals( 1, bindings.size() );

        MojoBinding binding = (MojoBinding) bindings.get( 0 );

        Xpp3Dom config = (Xpp3Dom) binding.getConfiguration();

        assertEquals( "value2", config.getChild( "test" ).getValue() );
        assertEquals( "other-value", config.getChild( "test2" ).getValue() );
    }

    private Object createConfiguration( final Properties configProperties )
    {
        Xpp3Dom config = new Xpp3Dom( "configuration" );
        for ( Iterator it = configProperties.keySet().iterator(); it.hasNext(); )
        {
            String key = (String) it.next();
            String value = configProperties.getProperty( key );

            Xpp3Dom child = new Xpp3Dom( key );
            child.setValue( value );

            config.addChild( child );
        }

        return config;
    }

}
