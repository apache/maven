package org.apache.maven.artifact.ant;

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

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.embed.Embedder;

/**
 * Base class for artifact tasks.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public abstract class AbstractArtifactTask
    extends Task
{
    private Embedder embedder;

    protected ArtifactRepository createArtifactRepository( LocalRepository repository )
    {
        ArtifactRepositoryLayout repositoryLayout = (ArtifactRepositoryLayout) lookup( ArtifactRepositoryLayout.ROLE,
                                                                                       repository.getLayout() );
        return new ArtifactRepository( "local", "file://" + repository.getLocation(), repositoryLayout );
    }

    protected ArtifactRepository createArtifactRepository( RemoteRepository repository )
    {
        ArtifactRepositoryLayout repositoryLayout = (ArtifactRepositoryLayout) lookup( ArtifactRepositoryLayout.ROLE,
                                                                                       repository.getLayout() );
        return new ArtifactRepository( "remote", repository.getUrl(), repositoryLayout );
    }

    protected Object lookup( String role )
    {
        try
        {
            return getEmbedder().lookup( role );
        }
        catch ( ComponentLookupException e )
        {
            throw new BuildException( "Unable to find component: " + role, e );
        }
    }

    private Object lookup( String role, String roleHint )
    {
        try
        {
            return getEmbedder().lookup( role, roleHint );
        }
        catch ( ComponentLookupException e )
        {
            throw new BuildException( "Unable to find component: " + role + "[" + roleHint + "]", e );
        }
    }

    private synchronized Embedder getEmbedder()
    {
        if ( embedder == null )
        {
            embedder = (Embedder) getProject().getReference( Embedder.class.getName() );

            if ( embedder == null )
            {
                embedder = new Embedder();
                try
                {
                    embedder.start();
                }
                catch ( PlexusContainerException e )
                {
                    throw new BuildException( "Unable to start embedder", e );
                }
                getProject().addReference( Embedder.class.getName(), embedder );
            }
        }
        return embedder;
    }
}
