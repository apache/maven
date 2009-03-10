package org.apache.maven.project.processor;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.maven.model.Build;
import org.apache.maven.model.Extension;
import org.apache.maven.model.Model;
import org.apache.maven.model.Resource;

public class BuildProcessor
    extends BaseProcessor
{
    public BuildProcessor( Collection<Processor> processors )
    {
        super( processors );
    }

    public void process( Object parent, Object child, Object target, boolean isChildMostSpecialized )
    {
        super.process( parent, child, target, isChildMostSpecialized );
        Model t = (Model) target;
        Model c = (Model) child;
        Model p = (Model) parent;
        
        if(t.getBuild() == null)
        {
            t.setBuild( new Build() );
        }
        
        if(c.getBuild() == null && !( p == null || p.getBuild() == null))
        {
            copy(p.getBuild(), t.getBuild());     
        }
        else if(c.getBuild() != null && !( p == null || p.getBuild() == null))
        {
            copy(c.getBuild(), t.getBuild());
            copy(p.getBuild(), t.getBuild()); 
        } 
        else if(c.getBuild() != null )
        {
            copy(c.getBuild(), t.getBuild());
        }    
    }
    
    private static void copy(Build source, Build target)
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
        
        if(target.getOutputDirectory() == null)
        {
            target.setOutputDirectory( target.getOutputDirectory() );    
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
        
        target.getFilters().addAll( new ArrayList<String>(source.getFilters()) );
        
        for(Extension extension : source.getExtensions())
        {
            Extension e = new Extension();
            e.setArtifactId( extension.getArtifactId() );
            e.setGroupId( extension.getGroupId() );
            e.setVersion( extension.getVersion() );
            target.addExtension( e );
        }
        
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
    }
}
