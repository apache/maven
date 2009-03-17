package org.apache.maven.project.processor;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.codehaus.plexus.util.xml.Xpp3Dom;

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
        {//Plugin targetPlugin = new Plugin();
            
            boolean isAdd = true;
            Plugin targetPlugin = find((Plugin) child, t);
            if(targetPlugin == null) 
            {
                targetPlugin = new Plugin();
            }
            else
            {
                isAdd = false;
            }
            
            copy( (Plugin) child, targetPlugin, isChildMostSpecialized );
            if(isAdd) t.add( targetPlugin );
        }
        else if ( parent != null && child == null )
        {
            //Plugin targetPlugin = new Plugin();
            
            boolean isAdd = true;
            Plugin targetPlugin = find((Plugin) parent, t);
            if(targetPlugin == null) 
            {
                targetPlugin = new Plugin();
            }
            else
            {
                isAdd = false;
            }
            
            copy( (Plugin) parent, targetPlugin, false );
            if(isAdd) t.add( targetPlugin );
        }
        else
        // JOIN
        {          
            if( match( (Plugin) parent, (Plugin) child) )
            {
                boolean isAdd = true;
                Plugin targetPlugin = find((Plugin) parent, t);
                if(targetPlugin == null) 
                {
                    targetPlugin = new Plugin();
                }
                else
                {
                    isAdd = false;
                }                 
                copy( (Plugin) parent, targetPlugin, false );
                copy( (Plugin) child, targetPlugin, isChildMostSpecialized );
                if(isAdd) t.add( targetPlugin ); 
            } 
            else
            {
                Plugin targetPlugin = new Plugin();
                copy( (Plugin) parent, targetPlugin, false );
                copy( (Plugin) child, targetPlugin, isChildMostSpecialized );
                t.add( targetPlugin );    
            }  
        }       
    }
    
    private static Plugin find(Plugin p1, List<Plugin> plugins)
    {
        for(Plugin t : plugins)
        {
            if(match(p1, t)){
                return t;
            }
        }
        
        return null;
    }
    
    private static boolean match( Plugin d1, Plugin d2 )
    {
        return getId( d1 ).equals( getId( d2 ));
    }   
    
    private static String getId( Plugin d )
    {
        StringBuilder sb = new StringBuilder();
        sb.append( d.getGroupId() ).append( ":" ).append( d.getArtifactId() ).append( ":" );
        return sb.toString();
    }       
    
    private static void copy(Plugin source, Plugin target, boolean isChild)
    {
        if(!isChild && source.getInherited() != null && !source.getInherited().equalsIgnoreCase( "true" ))
        {
            return;
        }
        
        if(source.getArtifactId() != null)
        {
            target.setArtifactId( source.getArtifactId() );   
        }
        
        target.setGroupId( source.getGroupId() );    
        
        if(source.getInherited() != null)
        {
            target.setInherited( source.getInherited() );    
        }
        
        if(source.getVersion() != null)
        {
            target.setVersion( source.getVersion() );    
        }
        
        
        for( PluginExecution pe : source.getExecutions())
        {
            PluginExecution idMatch = contains(pe, target.getExecutions());
            if(idMatch != null)//Join
            {
               copyPluginExecution(pe, idMatch, isChild);    
            }
            else 
            {
                PluginExecution targetPe = new PluginExecution();
                copyPluginExecution(pe, targetPe, isChild); 
                target.addExecution( targetPe );
            }
            
        }
     
        DependenciesProcessor proc = new DependenciesProcessor();
        if(target.getDependencies().isEmpty())
        {
            
            proc.process( new ArrayList<Dependency>(), new ArrayList<Dependency>(source.getDependencies()), target.getDependencies(), false );            
        }
        else
        {
            proc.process( new ArrayList<Dependency>(source.getDependencies()), new ArrayList<Dependency>(), target.getDependencies(), false );            
        }

        if(source.getConfiguration() != null)
        {
            //TODO: Not copying
            if(target.getConfiguration() != null)
            {
                target.setConfiguration( Xpp3Dom.mergeXpp3Dom( (Xpp3Dom) source.getConfiguration(), (Xpp3Dom) target.getConfiguration() ));     
            }
            else
            {
                target.setConfiguration( source.getConfiguration() );
            }
                
        }
       
       // p2.setConfiguration( configuration ) merge nodes
        //Goals
        target.setExtensions(source.isExtensions()); 
        
    }
    
    private static PluginExecution contains(PluginExecution pe, List<PluginExecution> executions)
    {
        String executionId = (pe.getId() != null) ? pe.getId() : "";
        for(PluginExecution e : executions)
        {
            String id = (e.getId() != null) ? e.getId() : "";
            if(executionId.equals( id ))
            {
                return  e;
            }
        }
        return null;
    }
    
    private static void copyPluginExecution(PluginExecution source, PluginExecution target, boolean isChild)
    {
        if(!isChild && source.getInherited() != null && !source.getInherited().equalsIgnoreCase( "true" ))
        {
            return;
        }            
        target.setId( source.getId() );
        
        if(isChild && source.getInherited() != null)
        {
            target.setInherited( source.getInherited() );
        }
           
        if(source.getPhase() != null)
        {
            target.setPhase( source.getPhase() );
        }
        
        List<String> goals = new ArrayList<String>(target.getGoals());
        for(String goal : source.getGoals())
        {
            if(!goals.contains( goal ))
            {
                goals.add( goal );    
            }
            
        }    
        target.setGoals( goals );
        
        if(source.getConfiguration() != null)
        {
            if(target.getConfiguration() != null)
            {
                target.setConfiguration( Xpp3Dom.mergeXpp3Dom( (Xpp3Dom) source.getConfiguration(), (Xpp3Dom) target.getConfiguration() ));     
            }
            else
            {
                target.setConfiguration( source.getConfiguration() );
            }            
        }
      
    }
}
