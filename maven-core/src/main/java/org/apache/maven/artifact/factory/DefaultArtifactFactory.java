package org.apache.maven.artifact.factory;

/*
 * Copyright 2001-2004 The Apache Software Foundation.
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
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Dependency;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class DefaultArtifactFactory
    implements ArtifactFactory
{
    public Set createArtifacts( List dependencies, ArtifactRepository localRepository )
    {
        Set projectArtifacts = new HashSet();

        for ( Iterator i = dependencies.iterator(); i.hasNext(); )
        {
            Dependency d = (Dependency) i.next();

            Artifact artifact = createArtifact( d, localRepository );

            projectArtifacts.add( artifact );
        }

        return projectArtifacts;
    }

    public Artifact createArtifact( Dependency dependency, ArtifactRepository localRepository )
    {
        Artifact artifact = new DefaultArtifact( dependency.getGroupId(),
                                                 dependency.getArtifactId(),
                                                 dependency.getVersion(),
                                                 dependency.getType() );

        return artifact;
    }
}
