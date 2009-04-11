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
            {   //PluginManagement is first in ordering
                if(match(depMng, targetDep) )
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
          
        List<PluginExecution> executions = new ArrayList<PluginExecution>();
        for( PluginExecution pe : source.getExecutions())
        {
            PluginExecution idMatch = contains(pe, target.getExecutions());
            if(idMatch != null)//Join
            {
               copyPluginExecution(pe, idMatch);   
               target.getExecutions().remove( idMatch );
               executions.add( idMatch );
            }
            else 
            {
                PluginExecution targetPe = new PluginExecution();
                copyPluginExecution(pe, targetPe); 
                executions.add( targetPe );
            }      
        }
        
        executions.addAll( target.getExecutions() );
        target.setExecutions( executions );
     
        DependenciesProcessor proc = new DependenciesProcessor(true);
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
                target.setConfiguration( Xpp3Dom.mergeXpp3Dom( (Xpp3Dom) target.getConfiguration(), (Xpp3Dom) source.getConfiguration() ));     
            }
            else
            {
                target.setConfiguration( source.getConfiguration() );
            }             
        }

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
        
        if ( target.getInherited() == null )
        {
            target.setInherited( source.getInherited() );
        }

        if ( target.getPhase() == null )
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
                //Target is dominant
                target.setConfiguration( Xpp3Dom.mergeXpp3Dom( (Xpp3Dom) target.getConfiguration(), (Xpp3Dom) source.getConfiguration() ));     
            }
            else
            {
                target.setConfiguration( source.getConfiguration() );
            }            
        }        
    }
}
