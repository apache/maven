package org.apache.maven.artifact.repository;

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
import org.apache.maven.artifact.repository.layout.ArtifactPathFormatException;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.repository.Repository;

/**
 * This class is an abstraction of the location from/to resources can be
 * transfered.
 *
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka </a>
 * @version $Id$
 */
public class ArtifactRepository
    extends Repository
{

    private final ArtifactRepositoryLayout layout;

    public ArtifactRepository( String id, String url, ArtifactRepositoryLayout layout )
    {
        super( id, url );

        this.layout = layout;
    }

    public ArtifactRepository( String id, String url, AuthenticationInfo authInfo, ArtifactRepositoryLayout layout )
    {
        super( id, url, authInfo );

        this.layout = layout;
    }

    public String pathOf( Artifact artifact )
        throws ArtifactPathFormatException
    {
        return layout.pathOf( artifact );
    }

}