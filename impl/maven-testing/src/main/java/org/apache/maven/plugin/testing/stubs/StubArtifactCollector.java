package org.apache.maven.plugin.testing.stubs;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ResolutionNode;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;

/**
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @version $Id$
 */
public class StubArtifactCollector
    implements ArtifactCollector
{
    /**
     * Default constructor
     */
    public StubArtifactCollector()
    {
        super();
    }

    /** {@inheritDoc} */
    public ArtifactResolutionResult collect( Set theArtifacts, Artifact theOriginatingArtifact,
                                            ArtifactRepository theLocalRepository, List theRemoteRepositories,
                                            ArtifactMetadataSource theSource, ArtifactFilter theFilter,
                                            List theListeners )
        throws ArtifactResolutionException
    {
        Set nodes = new HashSet();
        ArtifactResolutionResult arr = new ArtifactResolutionResult();

        Iterator iter = theArtifacts.iterator();
        while ( iter.hasNext() )
        {
            nodes.add( new ResolutionNode( (Artifact) iter.next(), theRemoteRepositories ) );
        }
        arr.setArtifactResolutionNodes( nodes );
        return arr;
    }

    /** {@inheritDoc} */
    public ArtifactResolutionResult collect( Set theArtifacts, Artifact theOriginatingArtifact, Map theManagedVersions,
                                            ArtifactRepository theLocalRepository, List theRemoteRepositories,
                                            ArtifactMetadataSource theSource, ArtifactFilter theFilter,
                                            List theListeners )
        throws ArtifactResolutionException
    {
        Set nodes = new HashSet();
        ArtifactResolutionResult arr = new ArtifactResolutionResult();

        Iterator iter = theArtifacts.iterator();
        while ( iter.hasNext() )
        {
            nodes.add( new ResolutionNode( (Artifact) iter.next(), theRemoteRepositories ) );
        }
        arr.setArtifactResolutionNodes( nodes );
        return arr;
    }
}
