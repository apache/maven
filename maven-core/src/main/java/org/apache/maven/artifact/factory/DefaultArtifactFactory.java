package org.apache.maven.artifact.factory;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Dependency;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

// TODO: packaging is very confusing - this isn't in artifact after all

public class DefaultArtifactFactory
    implements ArtifactFactory
{
    public Set createArtifacts( List dependencies, ArtifactRepository localRepository, String inheritedScope )
    {
        Set projectArtifacts = new HashSet();

        for ( Iterator i = dependencies.iterator(); i.hasNext(); )
        {
            Dependency d = (Dependency) i.next();

            Artifact artifact = createArtifact( d, localRepository, inheritedScope );
            if ( artifact != null )
            {
                projectArtifacts.add( artifact );
            }
        }

        return projectArtifacts;
    }

    public Artifact createArtifact( Dependency dependency, ArtifactRepository localRepository, String inheritedScope )
    {
        return createArtifact( dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(),
                               dependency.getScope(), dependency.getType(), dependency.getType(), inheritedScope );
    }

    public Artifact createArtifact( String groupId, String artifactId, String version, String scope, String type,
                                    String extension, String inheritedScope )
    {
        // TODO: can refactor, use scope handler

        // if this artifact is test, and the dependency is test, don't transitively create
        if ( Artifact.SCOPE_TEST.equals( inheritedScope ) && Artifact.SCOPE_TEST.equals( scope ) )
        {
            return null;
        }

        // TODO: localRepository not used (should be used here to resolve path?
        String desiredScope = Artifact.SCOPE_RUNTIME;
        if ( Artifact.SCOPE_COMPILE.equals( scope ) && inheritedScope == null )
        {
            desiredScope = Artifact.SCOPE_COMPILE;
        }

        // vvv added to retain compile scope. Remove if you want compile inherited as runtime
        else if ( Artifact.SCOPE_COMPILE.equals( scope ) && Artifact.SCOPE_COMPILE.equals( inheritedScope ) )
        {
            desiredScope = Artifact.SCOPE_COMPILE;
        }
        // ^^^ added to retain compile scope. Remove if you want compile inherited as runtime

        if ( Artifact.SCOPE_TEST.equals( scope ) || Artifact.SCOPE_TEST.equals( inheritedScope ) )
        {
            desiredScope = Artifact.SCOPE_TEST;
        }
        return new DefaultArtifact( groupId, artifactId, version, desiredScope, type, extension );
    }
}
