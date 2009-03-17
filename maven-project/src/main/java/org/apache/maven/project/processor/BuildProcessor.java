package org.apache.maven.project.processor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.maven.model.Build;
import org.apache.maven.model.BuildBase;
import org.apache.maven.model.Extension;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.Resource;

public class BuildProcessor
    extends BaseProcessor
{
    public BuildProcessor( Collection<Processor> processors )
    {
        super( processors );
    }
    
    public void processWithProfile( BuildBase build, Model target )    
    {
        processBuild(null, build, target, false, true );   
    }
    
    private void processBuild(Model p, BuildBase build, Model t, boolean isChildMostSpecialized, boolean isProfile)
    {

        if(t.getBuild() == null)
        {
            t.setBuild( new Build() );
        }    
        
        PluginsProcessor pluginsProcessor = new PluginsProcessor();
        if(build == null && !( p == null || p.getBuild() == null))
        {
            copy(p.getBuild(), t.getBuild(), isProfile);   
            pluginsProcessor.process( p.getBuild().getPlugins(), null, t.getBuild().getPlugins(), isChildMostSpecialized );  
            inheritManagement(p.getBuild().getPluginManagement(), null, t.getBuild());
        }
        else if(build != null && !( p == null || p.getBuild() == null))
        {
            copy(build, t.getBuild(), isProfile);
            copy(p.getBuild(), t.getBuild(), isProfile); 

            pluginsProcessor.process( p.getBuild().getPlugins(), build.getPlugins(), t.getBuild().getPlugins(), isChildMostSpecialized );  
            inheritManagement(p.getBuild().getPluginManagement(), build.getPluginManagement(), t.getBuild());
        } 
        else if(build != null )
        {
            copy(build, t.getBuild(), isProfile);
            pluginsProcessor.process( null, build.getPlugins(), t.getBuild().getPlugins(), isChildMostSpecialized ); 
            inheritManagement(null, build.getPluginManagement(), t.getBuild());
        }           
    }

    public void process( Object parent, Object child, Object target, boolean isChildMostSpecialized )
    {
        super.process( parent, child, target, isChildMostSpecialized );
        Model t = (Model) target;
        Model c = (Model) child;
        Model p = (Model) parent;
       
        processBuild(p, c.getBuild(), t, isChildMostSpecialized, false );
    }
    
    private static void inheritManagement(PluginManagement parent, PluginManagement child, Build target)
    {  
        PluginsProcessor proc = new PluginsProcessor();
        List<Plugin> p = (parent == null) ? new ArrayList<Plugin>() : parent.getPlugins();
        List<Plugin> c = (child == null) ? new ArrayList<Plugin>() : child.getPlugins();
        
        if(!c.isEmpty() || !p.isEmpty())
        {
            if(target.getPluginManagement() == null)
            {
                target.setPluginManagement( new PluginManagement() );
            }
            proc.process( p, c, target.getPluginManagement().getPlugins(), false );
        } 
        
    }

    private static void copy(BuildBase source, Build target, boolean isProfile)    
    {
        if(target.getFinalName() == null)
        {
            target.setFinalName( source.getFinalName() );    
        }
        
        if(target.getDefaultGoal() == null)
        {
            target.setDefaultGoal( source.getDefaultGoal() );   
        }
        
        if(target.getDirectory() == null)
        {
            target.setDirectory( source.getDirectory() );    
        }    
        
        List<String> filters = new ArrayList<String>(target.getFilters());
        for(String filter : source.getFilters())
        {
            if(!filters.contains( filter ))
            {
                filters.add( filter );
            }
        }

       // SortedSet<String> s = new TreeSet<String>( new ArrayList<String>( target.getFilters() ) );
       // s.addAll( source.getFilters() );
       // List<String> l = Arrays.asList(s.toArray( new String[s.size()]) );
        
        target.setFilters( filters );
             
        if(target.getResources().isEmpty())
        {
            for(Resource resource : source.getResources())
            {
                Resource r = new Resource();
                r.setDirectory( resource.getDirectory());
                r.setFiltering( resource.isFiltering() );
                r.setMergeId( resource.getMergeId() );
                r.setTargetPath( resource.getTargetPath() );
                r.setExcludes( new ArrayList<String>(resource.getExcludes()) );
                r.setIncludes( new ArrayList<String>(resource.getIncludes()) );
                target.getResources().add( r );
            }           
        }
        
        if(target.getTestResources().isEmpty())
        {
            for(Resource resource : source.getTestResources())
            {
                Resource r = new Resource();
                r.setDirectory( resource.getDirectory());
                r.setFiltering( resource.isFiltering() );
                r.setMergeId( resource.getMergeId() );
                r.setTargetPath( resource.getTargetPath() );
                r.setExcludes( new ArrayList<String>(resource.getExcludes()) );
                r.setIncludes( new ArrayList<String>(resource.getIncludes()) );
                target.getTestResources().add( r );
            }           
        }    
        if(!isProfile)
        {
            copyBuild((Build) source, target);
        }
    }
    
    private static void copyBuild(Build source, Build target)
    {
        if(target.getOutputDirectory() == null)
        {
            target.setOutputDirectory( source.getOutputDirectory() );    
        }
        
        if(target.getScriptSourceDirectory() == null)
        {
            target.setScriptSourceDirectory( source.getScriptSourceDirectory() );    
        }
        
        if(target.getSourceDirectory() == null)
        {
            target.setSourceDirectory( source.getSourceDirectory() );    
        }
        
        if(target.getTestOutputDirectory() == null)
        {
            target.setTestOutputDirectory( source.getTestOutputDirectory() );    
        }
        
        if(target.getTestSourceDirectory() == null)
        {
            target.setTestSourceDirectory( source.getTestSourceDirectory() );    
        }        
          
        for(Extension extension : source.getExtensions())
        {
            Extension e = new Extension();
            e.setArtifactId( extension.getArtifactId() );
            e.setGroupId( extension.getGroupId() );
            e.setVersion( extension.getVersion() );
            target.addExtension( e );
        }
    }
}
