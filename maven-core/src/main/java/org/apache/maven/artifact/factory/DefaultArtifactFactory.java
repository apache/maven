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
import org.apache.maven.artifact.construction.ArtifactConstructionSupport;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Dependency;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class DefaultArtifactFactory
    extends ArtifactConstructionSupport
    implements ArtifactFactory
{
    public Set createArtifacts( List dependencies, String inheritedScope )
    {
        Set projectArtifacts = new HashSet();

        for ( Iterator i = dependencies.iterator(); i.hasNext(); )
        {
            Dependency d = (Dependency) i.next();

            Artifact artifact = createArtifact( d, inheritedScope );
            if ( artifact != null )
            {
                projectArtifacts.add( artifact );
            }
        }

        return projectArtifacts;
    }

    private Artifact createArtifact( Dependency dependency, String inheritedScope )
    {
        return createArtifact( dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(),
                               dependency.getScope(), dependency.getType(), inheritedScope );
    }

}
