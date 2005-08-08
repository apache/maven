package org.apache.maven.project.overlay;

import org.apache.maven.model.Build;
import org.apache.maven.model.Extension;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.Resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BuildOverlay
    extends Build
{
    
    private final Build build;
    
    private List resources;
    private List testResources;

    public BuildOverlay( Build build )
    {
        if ( build == null )
        {
            this.build = new Build();
            
            this.resources = new ArrayList();
            
            this.testResources = new ArrayList();
        }
        else
        {
            this.build = build;
            
            this.resources = new ArrayList( build.getResources() );
            
            this.testResources = new ArrayList( build.getTestResources() );
        }
    }

    public void addExtension( Extension extension )
    {
        build.addExtension( extension );
    }

    public void addPlugin( Plugin plugin )
    {
        build.addPlugin( plugin );
    }

    public void addResource( Resource resource )
    {
        resources.add( resource );
    }

    public void addTestResource( Resource resource )
    {
        testResources.add( resource );
    }

    public boolean equals( Object obj )
    {
        return build.equals( obj );
    }

    public void flushPluginMap()
    {
        build.flushPluginMap();
    }

    public String getDefaultGoal()
    {
        return build.getDefaultGoal();
    }

    public String getDirectory()
    {
        return build.getDirectory();
    }

    public List getExtensions()
    {
        return build.getExtensions();
    }

    public String getFinalName()
    {
        return build.getFinalName();
    }

    public String getOutputDirectory()
    {
        return build.getOutputDirectory();
    }

    public PluginManagement getPluginManagement()
    {
        return build.getPluginManagement();
    }

    public List getPlugins()
    {
        return build.getPlugins();
    }

    public Map getPluginsAsMap()
    {
        return build.getPluginsAsMap();
    }

    public List getResources()
    {
        return resources;
    }

    public String getScriptSourceDirectory()
    {
        return build.getScriptSourceDirectory();
    }

    public String getSourceDirectory()
    {
        return build.getSourceDirectory();
    }

    public String getTestOutputDirectory()
    {
        return build.getTestOutputDirectory();
    }

    public List getTestResources()
    {
        return testResources;
    }

    public String getTestSourceDirectory()
    {
        return build.getTestSourceDirectory();
    }

    public int hashCode()
    {
        return build.hashCode();
    }

    public void removeExtension( Extension extension )
    {
        build.removeExtension( extension );
    }

    public void removePlugin( Plugin plugin )
    {
        build.removePlugin( plugin );
    }

    public void removeResource( Resource resource )
    {
        resources.remove( resource );
    }

    public void removeTestResource( Resource resource )
    {
        testResources.remove( resource );
    }

    public void setDefaultGoal( String defaultGoal )
    {
        build.setDefaultGoal( defaultGoal );
    }

    public void setDirectory( String directory )
    {
        build.setDirectory( directory );
    }

    public void setExtensions( List extensions )
    {
        build.setExtensions( extensions );
    }

    public void setFinalName( String finalName )
    {
        build.setFinalName( finalName );
    }

    public void setOutputDirectory( String outputDirectory )
    {
        build.setOutputDirectory( outputDirectory );
    }

    public void setPluginManagement( PluginManagement pluginManagement )
    {
        build.setPluginManagement( pluginManagement );
    }

    public void setPlugins( List plugins )
    {
        build.setPlugins( plugins );
    }

    public void setResources( List resources )
    {
        this.resources = resources;
    }

    public void setScriptSourceDirectory( String scriptSourceDirectory )
    {
        build.setScriptSourceDirectory( scriptSourceDirectory );
    }

    public void setSourceDirectory( String sourceDirectory )
    {
        build.setSourceDirectory( sourceDirectory );
    }

    public void setTestOutputDirectory( String testOutputDirectory )
    {
        build.setTestOutputDirectory( testOutputDirectory );
    }

    public void setTestResources( List testResources )
    {
        this.testResources = testResources;
    }

    public void setTestSourceDirectory( String testSourceDirectory )
    {
        build.setTestSourceDirectory( testSourceDirectory );
    }

    public String toString()
    {
        return build.toString();
    }

}
