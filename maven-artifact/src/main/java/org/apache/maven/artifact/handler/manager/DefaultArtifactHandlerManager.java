package org.apache.maven.artifact.handler.manager;

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
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactPathFormatException;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.transform.ArtifactTransformation;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl </a>
 * @version $Id: DefaultArtifactHandlerManager.java,v 1.1.1.1 2004/08/09
 *          18:37:32 jvanzyl Exp $
 */
public class DefaultArtifactHandlerManager
    implements ArtifactHandlerManager
{
    private List artifactTransformations;

    private Map artifactHandlers;

    private ArtifactRepositoryLayout artifactRepositoryLayout;

    public ArtifactHandler getArtifactHandler( String type )
        throws ArtifactHandlerNotFoundException
    {
        ArtifactHandler handler = (ArtifactHandler) artifactHandlers.get( type );

        if ( handler == null )
        {
            throw new ArtifactHandlerNotFoundException( "Artifact handler for type '" + type + "' cannot be found." );
        }

        return handler;
    }

    public Set getHandlerTypes()
    {
        return artifactHandlers.keySet();
    }

    public String getRemoteRepositoryArtifactPath( Artifact artifact, ArtifactRepository remoteRepository )
        throws ArtifactPathFormatException
    {
        for ( Iterator i = artifactTransformations.iterator(); i.hasNext(); )
        {
            ArtifactTransformation transform = (ArtifactTransformation) i.next();
            // TODO: perform transformation
        }

        return remoteRepository.pathOf( artifact );
    }

    public String getLocalRepositoryArtifactPath( Artifact artifact, ArtifactRepository localRepository )
        throws ArtifactPathFormatException
    {
        for ( Iterator i = artifactTransformations.iterator(); i.hasNext(); )
        {
            ArtifactTransformation transform = (ArtifactTransformation) i.next();
            artifact = transform.transformLocalArtifact( artifact, localRepository );
            // TODO: perform transformation
        }

        return localRepository.getBasedir() + "/" + localRepository.pathOf( artifact );
    }
}