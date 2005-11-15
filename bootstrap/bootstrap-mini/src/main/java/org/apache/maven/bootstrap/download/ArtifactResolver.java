package org.apache.maven.bootstrap.download;

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

import org.apache.maven.bootstrap.model.Repository;
import org.apache.maven.bootstrap.model.Dependency;

import java.util.Collection;
import java.io.File;

/**
 * Artifact resolver.
 */
public interface ArtifactResolver
{
    void downloadDependencies( Collection dependencies )
        throws DownloadFailedException;

    Repository getLocalRepository();

    void addBuiltArtifact( String groupId, String artifactId, String type, File jarFile );

    boolean isAlreadyBuilt( Dependency dep );

    File getArtifactFile( Dependency dependency );

    boolean isOnline();

    boolean isAlreadyBuilt( String key );
}
