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

import java.util.Map;
import java.util.HashMap;
import java.io.File;

/**
 */
public abstract class AbstractArtifactResolver
    implements ArtifactResolver
{
    private Repository localRepository;

    private Map builtArtifacts = new HashMap();

    protected AbstractArtifactResolver( Repository localRepository )
    {
        if ( localRepository == null )
        {
            System.err.println( "local repository not specified" );

            System.exit( 1 );
        }

        this.localRepository = localRepository;
    }

    public Repository getLocalRepository()
    {
        return localRepository;
    }

    public void addBuiltArtifact( String groupId, String artifactId, String type, File jarFile )
    {
        builtArtifacts.put( groupId + ":" + artifactId + ":" + type, jarFile );
    }

    public boolean isAlreadyBuilt( Dependency dep )
    {
        return builtArtifacts.containsKey( dep.getConflictId() );
    }

    public boolean isAlreadyBuilt( String conflictId )
    {
        return builtArtifacts.containsKey( conflictId );
    }

    public File getArtifactFile( Dependency dependency )
    {
        if ( isAlreadyBuilt( dependency ) )
        {
            return (File) builtArtifacts.get( dependency.getConflictId() );
        }
        else
        {
            return localRepository.getArtifactFile( dependency );
        }
    }
}
