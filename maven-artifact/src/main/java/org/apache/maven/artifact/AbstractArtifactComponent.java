package org.apache.maven.artifact;

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

import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerNotFoundException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactPathFormatException;
import org.apache.maven.artifact.transform.ArtifactTransformation;
import org.codehaus.plexus.logging.AbstractLogEnabled;

import java.util.Iterator;
import java.util.List;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl </a>
 * @version $Id: AbstractArtifactComponent.java,v 1.4 2005/03/08 05:34:52 brett
 *          Exp $
 * @todo refactor away
 */
public class AbstractArtifactComponent
    extends AbstractLogEnabled
{
    private List artifactTransformations;

    private ArtifactHandlerManager artifactHandlerManager;

    protected ArtifactHandler getArtifactHandler( String type )
        throws ArtifactHandlerNotFoundException
    {
        return artifactHandlerManager.getArtifactHandler( type );
    }

    protected String getRemoteRepositoryArtifactPath( Artifact artifact, ArtifactRepository remoteRepository )
        throws ArtifactPathFormatException
    {
        return remoteRepository.pathOf( artifact );
    }

    protected String getLocalRepositoryArtifactPath( Artifact artifact, ArtifactRepository localRepository )
        throws ArtifactPathFormatException
    {
        for ( Iterator i = artifactTransformations.iterator(); i.hasNext(); )
        {
            ArtifactTransformation transform = (ArtifactTransformation) i.next();
            // TODO: perform transformation
        }

        String artifactPath = localRepository.getBasedir() + "/" + localRepository.pathOf( artifact );
        return artifactPath;
    }
}
