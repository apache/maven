package org.apache.maven.project.processor;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;

public class PluginProcessor
    extends BaseProcessor
{
    public void process( Object parent, Object child, Object target, boolean isChildMostSpecialized )
    {
        super.process( parent, child, target, isChildMostSpecialized );
        List<Plugin> t = (List<Plugin>) target;

        if ( parent == null && child == null )
        {
            return;
        }
        else if ( parent == null && child != null )
        {
            Plugin targetPlugin = new Plugin();
            copy( (Plugin) child, targetPlugin );
            t.add( targetPlugin );
        }
        else if ( parent != null && child == null )
        {
            Plugin targetPlugin = new Plugin();
            copy( (Plugin) parent, targetPlugin );
            t.add( targetPlugin );
        }
        else
        // JOIN
        {
            Plugin  targetDependency = new Plugin();
            copy( (Plugin) child, targetDependency );
            copy( (Plugin) parent, targetDependency );
            t.add( targetDependency );
        }       
    }
    
    private static void copy(Plugin p1, Plugin p2)
    {
        if(p2.getArtifactId() == null)
        {
            p2.setArtifactId( p1.getArtifactId() );   
        }
        
        if(p2.getGroupId() == null)
        {
            p2.setGroupId( p1.getGroupId() );    
        }
        
        if(p2.getInherited() == null)
        {
            p2.setInherited( p1.getInherited() );    
        }
        
        if(p2.getVersion() == null)
        {
            p2.setVersion( p1.getVersion() );    
        }
        
        if(p2.getDependencies().isEmpty())
        {
            DependenciesProcessor proc = new DependenciesProcessor();
            proc.process( new ArrayList<Dependency>(), new ArrayList<Dependency>(p1.getDependencies()), p2.getDependencies(), false );            
        }
        else
        {
            DependenciesProcessor proc = new DependenciesProcessor();
            proc.process( new ArrayList<Dependency>(p1.getDependencies()), new ArrayList<Dependency>(), p2.getDependencies(), false );            
        }
         
        
       // p2.setConfiguration( configuration ) merge nodes
        //Goals
        //Executions
        p2.setExtensions(p1.isExtensions());
        
        
        
        
        
    }
}
