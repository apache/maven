package org.apache.maven.artifact.metadata;

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

import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.wagon.ResourceDoesNotExistException;

import java.io.File;
import java.io.IOException;
import java.util.Date;

/**
 * Methods used by the old artifact metadata. To be removed in beta-2.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public interface LegacyArtifactMetadata
    extends ArtifactMetadata, Comparable
{
    void readFromFile( File file )
        throws IOException;

    void retrieveFromRemoteRepository( ArtifactRepository repository, WagonManager wagonManager, String checksumPolicy )
        throws ArtifactMetadataRetrievalException, ResourceDoesNotExistException;

    void storeInLocalRepository( ArtifactRepository localRepository )
        throws ArtifactMetadataRetrievalException;

    boolean newerThanFile( File file );

    String constructVersion();

    Date getLastModified();
}
