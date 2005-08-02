package org.apache.maven.artifact.resolver.filter;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Filter to only retain objects in the given scope or better.
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

    public ScopeArtifactFilter( String scope )
    {
        if ( DefaultArtifact.SCOPE_COMPILE.equals( scope ) )
        {
            providedScope = true;
            compileScope = true;
            runtimeScope = false;
            testScope = false;
        }
        else if ( DefaultArtifact.SCOPE_RUNTIME.equals( scope ) )
        {
            providedScope = false;
            compileScope = true;
            runtimeScope = true;
            testScope = false;
        }
        else if ( DefaultArtifact.SCOPE_TEST.equals( scope ) )
        {
            providedScope = true;
            compileScope = true;
            runtimeScope = true;
            testScope = true;
        }
        else
        {
            providedScope = false;
            compileScope = false;
            runtimeScope = false;
            testScope = false;
        }
    }

    public boolean include( Artifact artifact )
    {
        if ( DefaultArtifact.SCOPE_COMPILE.equals( artifact.getScope() ) )
        {
            return compileScope;
        }
        else if ( DefaultArtifact.SCOPE_RUNTIME.equals( artifact.getScope() ) )
        {
            return runtimeScope;
        }
        else if ( DefaultArtifact.SCOPE_TEST.equals( artifact.getScope() ) )
        {
            return testScope;
        }
        else if ( DefaultArtifact.SCOPE_PROVIDED.equals( artifact.getScope() ) )
        {
            return providedScope;
        }
        else
        {
            // TODO: should this be true? Does it even happen?
            return false;
        }
    }
}
