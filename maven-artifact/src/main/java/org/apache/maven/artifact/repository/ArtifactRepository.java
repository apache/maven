package org.apache.maven.artifact.repository;

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

import org.apache.maven.wagon.repository.Repository;
import org.codehaus.plexus.util.StringUtils;

/**
 * This class is an abstraction of the location from/to resources
 * can be transfered.
 *
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 * @version $Id$
 */
public class ArtifactRepository
    extends Repository
{
    public ArtifactRepository()
    {
    }

    public ArtifactRepository( String id, String url)
    {
        super( id, url );
    }

    /*
    private String layout;

    public String getLayout()
    {
        if ( layout == null )
        {
            return "${groupId}/${type}s/${artifactId}-${version}.${extension}";
        }

        return layout;
    }

    public String artifactPath( Artifact artifact )
    {
        return interpolateLayout( artifact.getGroupId(),
                                  artifact.getArtifactId(),
                                  artifact.getVersion(),
                                  artifact.getType(),
                                  artifact.getExtension() );
    }

    public String fullArtifactPath( Artifact artifact )
    {
        return getBasedir() + "/" + artifactPath( artifact );
    }

    public String artifactUrl( Artifact artifact )
    {
        return getUrl() + "/" + artifactPath( artifact );
    }

    private String interpolateLayout( String groupId, String artifactId, String version, String type, String extension )
    {
        String layout = getLayout();

        layout = StringUtils.replace( layout, "${groupId}", groupId );

        layout = StringUtils.replace( layout, "${artifactId}", artifactId );

        layout = StringUtils.replace( layout, "${type}", type );

        layout = StringUtils.replace( layout, "${version}", version );

        layout = StringUtils.replace( layout, "${extension}", extension );

        return layout;
    }
    */
}
