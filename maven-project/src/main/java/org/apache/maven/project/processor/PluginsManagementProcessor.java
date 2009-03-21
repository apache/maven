package org.apache.maven.project.processor;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * Used for applying plugin management to the pom (not for inheritance).
 *
 */
public class PluginsManagementProcessor extends BaseProcessor
{
    public void process( Object parent, Object child, Object target, boolean isChildMostSpecialized )
    {
        super.process( parent, child, target, isChildMostSpecialized );
        
        List<Plugin> pluginManagement = (List<Plugin> ) child;
        List<Plugin> targetPlugin = (List<Plugin>) target;
        
        for(Plugin depMng : pluginManagement)
        {
            for(Plugin targetDep : targetPlugin)
            {
                if(match(depMng, targetDep))
                {
                    copy(depMng, targetDep );      
                }                
            }
        }
    }

    private static boolean match( Plugin d1, Plugin d2 )
    {
        return getId( d1 ).equals( getId( d2 ) ) ;
    }

    private static String getId( Plugin d )
    {
        StringBuilder sb = new StringBuilder();
        sb.append( d.getGroupId() ).append( ":" ).append( d.getArtifactId() );
        return sb.toString();
    }      
    
    private static void copy(Plugin source, Plugin target)
    {
        if(target.getArtifactId() == null)
        {
            target.setArtifactId( source.getArtifactId() );   
        }
        
        target.setGroupId( source.getGroupId() );    
        
        if(target.getInherited() == null)
        {
            target.setInherited( source.getInherited() );    
        }
        
        if(target.getVersion() == null)
        {
            target.setVersion( source.getVersion() );    
        }
        
        
        for( PluginExecution pe : source.getExecutions())
        {
            PluginExecution idMatch = contains(pe, target.getExecutions());
            if(idMatch != null)//Join
            {
               copyPluginExecution(pe, idMatch);    
            }
            else 
            {
                PluginExecution targetPe = new PluginExecution();
                copyPluginExecution(pe, targetPe); 
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
    
    private static void copyPluginExecution(PluginExecution source, PluginExecution target)
    {
        if(target.getId() != null)
        {
            target.setId( source.getId() );
        }
        
        if(target.getInherited() == null)
        {
            source.setInherited( target.getInherited() );
        }
        
        if(target.getPhase() != null)
        {
            source.setPhase( target.getPhase() );
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
        if(target.getConfiguration() != null)
        {
            target.setConfiguration( Xpp3Dom.mergeXpp3Dom( (Xpp3Dom) source.getConfiguration(), (Xpp3Dom) target.getConfiguration() ));     
        }
        else
        {
            target.setConfiguration( source.getConfiguration() );
        }           
    }
    
    /*
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
        
        for( PluginExecution pe : p1.getExecutions())
        {
            PluginExecution p = new PluginExecution();
            p.setId( pe.getId() );
            p.setInherited( pe.getInherited() );
            p.setPhase( pe.getPhase() );
            p.setGoals( new ArrayList<String>(pe.getGoals()) );
            p2.addExecution( p );
        }
    
     //   if(p2.getDependencies().isEmpty())
     //   {
            DependenciesProcessor proc = new DependenciesProcessor();
            proc.process( new ArrayList<Dependency>(), new ArrayList<Dependency>(p1.getDependencies()), p2.getDependencies(), false );            
    //    }
    //    else
    //    {
    //        DependenciesProcessor proc = new DependenciesProcessor();
    //        proc.process( new ArrayList<Dependency>(p1.getDependencies()), new ArrayList<Dependency>(), p2.getDependencies(), false );            
    //    }

        if(p1.getConfiguration() != null)
        {
            //TODO: Not copying
            if(p2.getConfiguration() != null)
            {
                p2.setConfiguration( Xpp3Dom.mergeXpp3Dom( (Xpp3Dom) p1.getConfiguration(), (Xpp3Dom) p2.getConfiguration() ));     
            }
            else
            {
                p2.setConfiguration( p1.getConfiguration() );
            }            
        }
       
       // p2.setConfiguration( configuration ) merge nodes
        //Goals
        p2.setExtensions(p1.isExtensions());
    }
    */
}
