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
    
    public ArtifactRepository( String id, String url )
    {
        super( id, url );
    }

    public ArtifactRepository( String id, String url, AuthenticationInfo authInfo )
    {
        super( id, url, authInfo );
    }
}