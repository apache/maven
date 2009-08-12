package org.apache.maven.artifact.resolver.filter;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.artifact.Artifact;

/**
 * Filter to only retain objects in the given artifactScope or better.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public class ScopeArtifactFilter
    implements ArtifactFilter
{
    private final boolean compileScope;

    private final boolean runtimeScope;

    private final boolean testScope;

    private final boolean providedScope;

    private final boolean systemScope;

    private final String scope;
    
    public ScopeArtifactFilter( String scope )
    {
        this.scope = scope;
        
        if ( Artifact.SCOPE_COMPILE.equals( scope ) )
        {
            systemScope = true;
            providedScope = true;
            compileScope = true;
            runtimeScope = false;
            testScope = false;
        }
        else if ( Artifact.SCOPE_RUNTIME.equals( scope ) )
        {
            systemScope = false;
            providedScope = false;
            compileScope = true;
            runtimeScope = true;
            testScope = false;
        }
        else if ( Artifact.SCOPE_COMPILE_PLUS_RUNTIME.equals( scope ) )
        {
            systemScope = true;
            providedScope = true;
            compileScope = true;
            runtimeScope = true;
            testScope = false;
        }
        else if ( Artifact.SCOPE_RUNTIME_PLUS_SYSTEM.equals( scope ) )
        {
            systemScope = true;
            providedScope = false;
            compileScope = true;
            runtimeScope = true;
            testScope = false;
        }
        else if ( Artifact.SCOPE_TEST.equals( scope ) )
        {
            systemScope = true;
            providedScope = true;
            compileScope = true;
            runtimeScope = true;
            testScope = true;
        }
        else
        {
            systemScope = false;
            providedScope = false;
            compileScope = false;
            runtimeScope = false;
            testScope = false;
        }
    }

    public boolean include( Artifact artifact )
    {
        if ( Artifact.SCOPE_COMPILE.equals( artifact.getScope() ) )
        {
            return compileScope;
        }
        else if ( Artifact.SCOPE_RUNTIME.equals( artifact.getScope() ) )
        {
            return runtimeScope;
        }
        else if ( Artifact.SCOPE_TEST.equals( artifact.getScope() ) )
        {
            return testScope;
        }
        else if ( Artifact.SCOPE_PROVIDED.equals( artifact.getScope() ) )
        {
            return providedScope;
        }
        else if ( Artifact.SCOPE_SYSTEM.equals( artifact.getScope() ) )
        {
            return systemScope;
        }
        else
        {
            return true;
        }
    }

    public String getScope()
    {
        return scope;
    }

    @Override
    public int hashCode()
    {
        int hash = 17;
        hash = hash * 31 + ( compileScope ? 1 : 0 );
        hash = hash * 31 + ( runtimeScope ? 1 : 0 );
        hash = hash * 31 + ( testScope ? 1 : 0 );
        hash = hash * 31 + ( providedScope ? 1 : 0 );
        hash = hash * 31 + ( systemScope ? 1 : 0 );
        
        hash = hash * 31 + ( scope != null ? scope.hashCode() : 0);
        
        return hash;
    }

    @Override
    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        
        if ( !( obj instanceof ScopeArtifactFilter ) )
        {
            return false;
        }
        
        ScopeArtifactFilter other = (ScopeArtifactFilter) obj;

        return compileScope == other.compileScope
                && runtimeScope == other.runtimeScope
                && testScope == other.testScope
                && providedScope == other.providedScope
                && systemScope == other.systemScope
                && equals( scope, other.scope );
    }

    private static boolean equals( String str1, String str2 )
    {
        return str1 != null ? str1.equals( str2 ) : str2 == null;
    }
}
