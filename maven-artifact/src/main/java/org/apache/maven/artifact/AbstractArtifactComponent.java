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
import org.codehaus.plexus.logging.AbstractLogEnabled;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class AbstractArtifactComponent
    extends AbstractLogEnabled
{
    private ArtifactHandlerManager artifactHandlerManager;

    protected ArtifactHandler getArtifactHandler( String type )
        throws ArtifactHandlerNotFoundException
    {
        return artifactHandlerManager.getArtifactHandler( type );
    }

    protected String path( Artifact artifact )
        throws ArtifactHandlerNotFoundException
    {
        return artifactHandlerManager.path( artifact );
    }

    protected void setLocalRepositoryPath( Artifact artifact, ArtifactRepository localRepository )
        throws ArtifactHandlerNotFoundException
    {
        artifact.setPath( artifactHandlerManager.localRepositoryPath( artifact, localRepository ) );
    }
}
