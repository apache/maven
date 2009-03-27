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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;

/*
 * hold original pom
 * Track where a property is from
 */
public class ModelProcessor
    extends BaseProcessor
{

    public ModelProcessor( Collection<Processor> processors )
    {
        super( processors );
    }

    public void process( Object parent, Object child, Object target, boolean isChildMostSpecialized )
    {
        super.process( parent, child, target, isChildMostSpecialized );
        
        Model c = (Model) child;
        Model t = (Model) target;
        Model p = null;
        if ( parent != null )
        {
            p = (Model) parent;
        }

        // Version
        if ( c.getVersion() == null )
        {
            if ( c.getParent() != null )
            {
                t.setVersion( c.getParent().getVersion() );
            }
        }
        else
        {
            t.setVersion( c.getVersion() );
        }

        // GroupId
        if ( c.getGroupId() == null )
        {
            if ( c.getParent() != null )
            {
                t.setGroupId( c.getParent().getGroupId() );
            }
        }
        else
        {
            t.setGroupId( c.getGroupId() );
        }
        
        // ArtifactId
        if ( c.getArtifactId() == null )
        {
            if ( c.getParent() != null )
            {
                t.setArtifactId( c.getParent().getArtifactId() );
            }
        }
        else
        {
            t.setArtifactId( c.getArtifactId() );
        }        

        t.setModelVersion( c.getModelVersion() );
        if(c.getPackaging() != null)
        {
            t.setPackaging( c.getPackaging() );    
        }
        else
        {
            t.setPackaging( "jar" );
        }
        
        if ( isChildMostSpecialized )
        {
            t.setName( c.getName() );
            t.setDescription( c.getDescription() );
        }

        if ( c.getInceptionYear() != null )
        {
            t.setInceptionYear( c.getInceptionYear() );
        }
        else if ( p != null )
        {
            t.setInceptionYear( p.getInceptionYear() );
        }
        
        if ( c.getUrl() != null )
        {
            t.setUrl(c.getUrl());         
        }       
        else if(p != null && p.getUrl() != null)
        {
            t.setUrl( p.getUrl() +  t.getArtifactId() );
        }
        else if (t.getUrl() != null)
        {
            t.setUrl( t.getUrl() + "/" + t.getArtifactId() );
        }
        
        //Dependencies
        List<Dependency> deps = new ArrayList<Dependency>();
        DependenciesProcessor dependenciesProcessor = new DependenciesProcessor();
        dependenciesProcessor.process( (p != null) ? p.getDependencies() : null, c.getDependencies(), deps, isChildMostSpecialized );
             
        if(deps.size() > 0)
        {
            t.getDependencies().addAll( deps );
        }  
        
        //Dependency Management
        List<Dependency> mngDeps = new ArrayList<Dependency>();
        dependenciesProcessor.process( (p != null && p.getDependencyManagement() != null) ? p.getDependencyManagement().getDependencies(): null,
                        (c.getDependencyManagement() != null) ? c.getDependencyManagement().getDependencies(): null, mngDeps, isChildMostSpecialized );
        if(mngDeps.size() > 0)
        {
            if(t.getDependencyManagement() == null)
            {
                t.setDependencyManagement( new DependencyManagement() );    
            }
            t.getDependencyManagement().getDependencies().addAll( mngDeps );
        }
    }
}
