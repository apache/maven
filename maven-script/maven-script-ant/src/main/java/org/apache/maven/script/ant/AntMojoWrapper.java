package org.apache.maven.script.ant;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.ContextEnabled;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Path;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;
import org.codehaus.plexus.component.MapOrientedComponent;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.factory.ant.AntComponentExecutionException;
import org.codehaus.plexus.component.factory.ant.AntScriptInvoker;
import org.codehaus.plexus.component.repository.ComponentRequirement;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * @author John Casey
 * @author Jason van Zyl
 */
public class AntMojoWrapper
    extends AbstractMojo
    implements ContextEnabled,
    MapOrientedComponent
{
    private AntScriptInvoker scriptInvoker;

    private Map pluginContext;

    private Project antProject;

    private MavenProject mavenProject;

    private PluginDescriptor pluginDescriptor;

    public AntMojoWrapper( AntScriptInvoker scriptInvoker )
    {
        this.scriptInvoker = scriptInvoker;

    }

    public void execute()
        throws MojoExecutionException
    {
        antProject = scriptInvoker.getProject();

        mavenProject = (MavenProject) pluginContext.get( "project" );

        pluginDescriptor = (PluginDescriptor) pluginContext.get( "pluginDescriptor" );

        unpackFileBasedResources();

        addClasspathReferences();

        try
        {
            scriptInvoker.invoke();
        }
        catch ( AntComponentExecutionException e )
        {
            throw new MojoExecutionException( "Failed to execute: " + e.getMessage(), e );
        }
    }

    public void setPluginContext( Map pluginContext )
    {
        this.pluginContext = pluginContext;
    }

    public Map getPluginContext()
    {
        return pluginContext;
    }

    public void addComponentRequirement( ComponentRequirement requirementDescriptor,
                                         Object requirementValue )
        throws ComponentConfigurationException
    {
        scriptInvoker.addComponentRequirement( requirementDescriptor, requirementValue );
    }

    public void setComponentConfiguration( Map componentConfiguration )
        throws ComponentConfigurationException
    {
        scriptInvoker.setComponentConfiguration( componentConfiguration );
    }

    private void unpackFileBasedResources()
        throws MojoExecutionException
    {
        // What we need to write out any resources in the plugin to the target directory of the
        // mavenProject using the Ant-based plugin:
        //
        // 1. Need a reference to the plugin JAR itself
        // 2. Need a reference to the ${basedir} of the mavenProject

        File pluginJar = pluginDescriptor.getPluginArtifact().getFile();

        String resourcesPath = pluginDescriptor.getArtifactId();

        File outputDirectory = new File( mavenProject.getBuild().getDirectory() );

        try
        {
            UnArchiver ua = new ZipUnArchiver( pluginJar );

            ua.extract( resourcesPath, outputDirectory );
        }
        catch ( ArchiverException e )
        {
            throw new MojoExecutionException( "Error extracting resources from your Ant-based plugin.", e );
        }
    }

    private void addClasspathReferences()
        throws MojoExecutionException
    {
        try
        {
            // Compile classpath
            Path p = new Path( antProject );

            p.setPath( StringUtils.join( mavenProject.getCompileClasspathElements().iterator(), File.pathSeparator ) );

            antProject.addReference( "maven.compile.classpath", p );

            // Runtime classpath
            p = new Path( antProject );

            p.setPath( StringUtils.join( mavenProject.getRuntimeClasspathElements().iterator(), File.pathSeparator ) );

            antProject.addReference( "maven.runtime.classpath", p );

            // Test classpath
            p = new Path( antProject );

            p.setPath( StringUtils.join( mavenProject.getTestClasspathElements().iterator(), File.pathSeparator ) );

            antProject.addReference( "maven.test.classpath", p );

            // Plugin dependency classpath

            p = getPathFromArtifacts( pluginDescriptor.getArtifacts(), antProject );
            System.out.println( "p = " + p );
            antProject.addReference( "maven.plugin.classpath", p );
        }
        catch ( DependencyResolutionRequiredException e )
        {
            throw new MojoExecutionException( "Error creating classpath references for Ant-based plugin scripts.", e  );
        }
    }

    public Path getPathFromArtifacts( Collection artifacts,
                                      Project antProject )
        throws DependencyResolutionRequiredException
    {
        List list = new ArrayList( artifacts.size() );

        for ( Iterator i = artifacts.iterator(); i.hasNext(); )
        {
            Artifact a = (Artifact) i.next();

            File file = a.getFile();

            if ( file == null )
            {
                throw new DependencyResolutionRequiredException( a );
            }

            list.add( file.getPath() );
        }

        Path p = new Path( antProject );

        p.setPath( StringUtils.join( list.iterator(), File.pathSeparator ) );

        return p;
    }
}
