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

import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Exclusion;

public class DependencyManagementProcessor extends BaseProcessor
{
    public void process( Object parent, Object child, Object target, boolean isChildMostSpecialized )
    {
        super.process( parent, child, target, isChildMostSpecialized );
        
        List<Dependency> depManagement = (List<Dependency> ) child;
        List<Dependency> targetDependencies = (List<Dependency>) target;
        
        for(Dependency depMng : depManagement)
        {
            for(Dependency targetDep : targetDependencies)
            {
                if(match(depMng, targetDep))
                {
                    copy(depMng, targetDep );      
                }                
            }
        }
    }
    
    private static void copy( Dependency dependency, Dependency targetDependency )
    {
        if ( targetDependency.getArtifactId() == null )
        {
            targetDependency.setArtifactId( dependency.getArtifactId() );
        }

        if ( targetDependency.getClassifier() == null )
        {
            targetDependency.setClassifier( dependency.getClassifier() );
        }

        if ( targetDependency.getGroupId() == null )
        {
            targetDependency.setGroupId( dependency.getGroupId() );
        }

        if ( targetDependency.getScope() == null )
        {
            targetDependency.setScope( dependency.getScope() );
        }

        if ( targetDependency.getSystemPath() == null )
        {
            targetDependency.setSystemPath( dependency.getSystemPath() );
        }

        if ( targetDependency.getType() == null )
        {
            targetDependency.setType( dependency.getType() );
        }

        if ( targetDependency.getVersion() == null )
        {
            targetDependency.setVersion( dependency.getVersion() );
        }

        if ( !dependency.getExclusions().isEmpty() )
        {
            List<Exclusion> targetExclusions = targetDependency.getExclusions();
            for ( Exclusion e : dependency.getExclusions() )
            {
                if ( !containsExclusion( e, targetExclusions ) )
                {
                    Exclusion e1 = new Exclusion();
                    e1.setArtifactId( e.getArtifactId() );
                    e1.setGroupId( e.getGroupId() );
                    targetDependency.addExclusion( e1 );
                }
            }
        }

        targetDependency.setOptional( dependency.isOptional() );
    }

    private static boolean containsExclusion( Exclusion exclusion, List<Exclusion> exclusions )
    {
        if(exclusions == null || exclusions.isEmpty())
        {
            return false;
        }
        
        for ( Exclusion e : exclusions )
        {
            if ( e.getGroupId().equals( exclusion.getGroupId() )
                && e.getArtifactId().equals( exclusion.getArtifactId() ) )
            {
                return true;
            }
        }
        return false;
    }
    
    private boolean match( Dependency d1, Dependency d2 )
    {
        return getId( d1 ).equals( getId( d2 ) );    
    }

    private String getId( Dependency d )
    {
        StringBuilder sb = new StringBuilder();
        sb.append( d.getGroupId() ).append( ":" ).append( d.getArtifactId() );        
        return sb.toString();
    }   
}
