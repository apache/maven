package org.apache.maven.artifact.handler.manager;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.codehaus.plexus.util.StringUtils;

import java.util.Map;
import java.util.Set;

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

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl </a>
 * @version $Id: DefaultArtifactHandlerManager.java,v 1.1.1.1 2004/08/09
 *          18:37:32 jvanzyl Exp $
 */
public class DefaultArtifactHandlerManager
    implements ArtifactHandlerManager
{
    private Map artifactHandlers;

    public ArtifactHandler getArtifactHandler( String type ) throws ArtifactHandlerNotFoundException
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

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    private String layout;

    public String getLayout()
    {
        if ( layout == null )
        {
            return "${groupId}/${directory}/${artifactId}-${version}.${extension}";
        }

        return layout;
    }

    public String localRepositoryPath( Artifact artifact, ArtifactRepository localRepository )
         throws ArtifactHandlerNotFoundException
    {
        return localRepository.getBasedir() + "/" + path( artifact );
    }

    public String artifactUrl( Artifact artifact, ArtifactRepository remoteRepository )
        throws ArtifactHandlerNotFoundException
    {
        return remoteRepository.getUrl() + "/" + path( artifact );
    }

    public String path( Artifact artifact ) throws ArtifactHandlerNotFoundException
    {
        ArtifactHandler handler = getArtifactHandler( artifact.getType() );

        return interpolateLayout( artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), handler
            .directory(), handler.extension() );
    }

    private String interpolateLayout( String groupId, String artifactId, String version, String directory,
        String extension )
    {
        String layout = getLayout();

        layout = StringUtils.replace( layout, "${groupId}", groupId );

        layout = StringUtils.replace( layout, "${artifactId}", artifactId );

        layout = StringUtils.replace( layout, "${directory}", directory );

        layout = StringUtils.replace( layout, "${version}", version );

        layout = StringUtils.replace( layout, "${extension}", extension );

        return layout;
    }
}