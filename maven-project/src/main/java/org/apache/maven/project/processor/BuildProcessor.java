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
import java.util.Collection;
import java.util.List;

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
            copyFilters(p.getBuild(), t.getBuild());
            pluginsProcessor.process( p.getBuild().getPlugins(), null, t.getBuild().getPlugins(), isChildMostSpecialized );  
            inheritManagement(p.getBuild().getPluginManagement(), null, t.getBuild());
        }
        else if(build != null && !( p == null || p.getBuild() == null))
        {            
            copy(p.getBuild(), t.getBuild(), isProfile); 
            copy(build, t.getBuild(), isProfile);
            
            copyFilters(build, t.getBuild());           
            copyFilters(p.getBuild(), t.getBuild());                   
    
            pluginsProcessor.process( p.getBuild().getPlugins(), build.getPlugins(), t.getBuild().getPlugins(), isChildMostSpecialized );  
            inheritManagement(p.getBuild().getPluginManagement(), build.getPluginManagement(), t.getBuild());
        } 
        else if(build != null )
        {
            copy(build, t.getBuild(), isProfile);
            copyFilters(build, t.getBuild());
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
    
    private static void copyFilters(BuildBase source, Build target)
    {     
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
    }

    private static void copy(BuildBase source, Build target, boolean isProfile)    
    {
        if(source.getFinalName() != null)
        {
            target.setFinalName( source.getFinalName() );    
        }
        
        if(source.getDefaultGoal() != null)
        {
            target.setDefaultGoal( source.getDefaultGoal() );   
        }
        
        if(source.getDirectory() != null)
        {
            target.setDirectory( source.getDirectory() );    
        }  
        
        if(!source.getResources().isEmpty())
        {
            List<Resource> resources = new ArrayList<Resource>();
            for(Resource resource : source.getResources())
            {
                Resource r = new Resource();
                r.setDirectory( resource.getDirectory());
                r.setFiltering( resource.isFiltering() );
                r.setMergeId( resource.getMergeId() );
                r.setTargetPath( resource.getTargetPath() );
                r.setExcludes( new ArrayList<String>(resource.getExcludes()) );
                r.setIncludes( new ArrayList<String>(resource.getIncludes()) );
                resources.add( r );
            }           
            target.setResources( resources );
        }
        
        if(!source.getTestResources().isEmpty())
        {
            List<Resource> resources = new ArrayList<Resource>();
            for(Resource resource : source.getTestResources())
            {
                Resource r = new Resource();
                r.setDirectory( resource.getDirectory());
                r.setFiltering( resource.isFiltering() );
                r.setMergeId( resource.getMergeId() );
                r.setTargetPath( resource.getTargetPath() );
                r.setExcludes( new ArrayList<String>(resource.getExcludes()) );
                r.setIncludes( new ArrayList<String>(resource.getIncludes()) );
                resources.add( r );
            }   
            target.setTestResources( resources );
        } 
        if(!isProfile)
        {
            copyBuild((Build) source, target);
        }
    }
    
    private static void copyBuild(Build source, Build target)
    {
        if(source.getOutputDirectory() != null)
        {
            target.setOutputDirectory( source.getOutputDirectory() );    
        }
        
        if(source.getScriptSourceDirectory() != null)
        {
            target.setScriptSourceDirectory( source.getScriptSourceDirectory() );    
        }
        
        if(source.getSourceDirectory() != null)
        {
            target.setSourceDirectory( source.getSourceDirectory() );    
        }
        
        if(source.getTestOutputDirectory() != null)
        {
            target.setTestOutputDirectory( source.getTestOutputDirectory() );    
        }
        
        if(source.getTestSourceDirectory() != null)
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
