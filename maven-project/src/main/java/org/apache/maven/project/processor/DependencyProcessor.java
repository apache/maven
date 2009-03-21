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

import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Exclusion;

public class DependencyProcessor
    extends BaseProcessor
{
    
    public void process( Object parent, Object child, Object target, boolean isChildMostSpecialized )
    {
        super.process( parent, child, target, isChildMostSpecialized );

        List<Dependency> t = (List<Dependency>) target;

        if ( parent == null && child == null )
        {
            return;
        }
        else if ( parent == null && child != null )
        {
            boolean isAdd = true;
            Dependency targetDependency = contains((Dependency) child, t);
            if(targetDependency == null)
            {
                targetDependency = new Dependency();    
            }
            else
            {
                isAdd = false;
            }
            //Dependency targetDependency = new Dependency();
            copy( (Dependency) child, targetDependency, false );
            if(isAdd) t.add( targetDependency );
        }
        else if ( parent != null && child == null )
        {
            boolean isAdd = true;
            Dependency targetDependency = contains((Dependency) parent, t);
            if(targetDependency == null)
            {
                targetDependency = new Dependency();    
            }
            else
            {
                isAdd = false;
            }
            //Dependency targetDependency = new Dependency();
            copy( (Dependency) parent, targetDependency, true );
            if(isAdd) t.add( targetDependency );
        }
        else
        // JOIN
        {
            Dependency targetDependency = new Dependency();
            copy( (Dependency) child, targetDependency, false );
            copy( (Dependency) parent, targetDependency, true );
            t.add( targetDependency );               
        }
    }
    
    private Dependency contains(Dependency d1, List<Dependency> dependencies)
    {
        for(Dependency d : dependencies)
        {
            if( match(d, d1))
            {
                return d;
            }
        }
        return null;
    }
    
    private boolean match( Dependency d1, Dependency d2 )
    {
        // TODO: Version ranges ?
        return getId( d1 ).equals( getId( d2 ) );
   
    }

    private String getId( Dependency d )
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( d.getGroupId() ).append( ":" ).append( d.getArtifactId() );
        sb.append( ":" ).append(
                                d.getType() == null ? "jar"
                                                : d.getType() ).append(
                                                               ":" ).append(
                                                                             d.getClassifier() );
        
        return sb.toString();
    }
    private static void copy( Dependency source, Dependency targetDependency, boolean isParent )
    {
        //IF target exists and is parent, don't override
        if ( source.getArtifactId() != null && (targetDependency.getArtifactId() == null || !isParent) )
        {
            targetDependency.setArtifactId( source.getArtifactId() );
        }

        if ( source.getClassifier() != null )
        {
            targetDependency.setClassifier( source.getClassifier() );
        }

        if ( source.getGroupId() != null )
        {
            targetDependency.setGroupId( source.getGroupId() );
        }

        if ( source.getScope() != null && (targetDependency.getScope() == null || !isParent) )
        {
            targetDependency.setScope( source.getScope() );
        }

        if ( source.getSystemPath() != null )
        {
            targetDependency.setSystemPath( source.getSystemPath() );
        }

        if ( source.getType() != null && (targetDependency.getScope() == null || !isParent))
        {
            targetDependency.setType( source.getType() );
        }

        if ( source.getVersion() != null && (targetDependency.getVersion() == null || !isParent)  )
        {
            targetDependency.setVersion( source.getVersion() );
        }

        if ( !source.getExclusions().isEmpty() )
        {
            List<Exclusion> targetExclusions = targetDependency.getExclusions();
            for ( Exclusion e : source.getExclusions() )
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

        targetDependency.setOptional( source.isOptional() );
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
}
