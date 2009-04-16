package org.apache.maven.project.builder;

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
import java.util.Collections;
import java.util.List;

import org.apache.maven.model.Dependency;


public class DependenciesProcessor
    extends BaseProcessor
{   
    private boolean isDependencyManagement;
    
    public DependenciesProcessor() {}
    
    public DependenciesProcessor(boolean isDependencyManagement) {
        this.isDependencyManagement = isDependencyManagement;
    }
    
    public void process( Object parent, Object child, Object target, boolean isChildMostSpecialized )
    {
        super.process( parent, child, target, isChildMostSpecialized );
        List<Dependency> c = (child != null) ?  (List<Dependency>) child : new ArrayList<Dependency>() ;
        List<Dependency> p = null;
        
        if ( parent != null )
        {
            p = (List<Dependency>) parent;
        }
        List<Dependency> dependencies = (List<Dependency>) target;

        DependencyProcessor processor = new DependencyProcessor(isDependencyManagement);
        if ( ( p == null || p.isEmpty() ) && !c.isEmpty()  )
        {
            for ( Dependency dependency : c )
            {
                processor.process( null, dependency, dependencies, isChildMostSpecialized );
            }
        }
        else
        {
            if ( !c.isEmpty() )
            {

                for ( Dependency parentDependency : p )
                {
                    processor.process( parentDependency, null, dependencies, isChildMostSpecialized );
                }
                
                int length = dependencies.size();
                
                for ( Dependency childDependency : c )
                {
                    processor.process( null, childDependency, dependencies, isChildMostSpecialized );
                }

                //Move elements so child dependencies are first
                List<Dependency> childDependencies = 
                    new ArrayList<Dependency>(dependencies.subList( length - 1 , dependencies.size() ) );
                dependencies.removeAll( childDependencies );
                dependencies.addAll( 0, childDependencies );
            }
            else if( p != null)
            {
                for ( Dependency d2 : p )
                {
                    processor.process( d2, null, dependencies, isChildMostSpecialized );
                }
            }
        }      
    }
}
