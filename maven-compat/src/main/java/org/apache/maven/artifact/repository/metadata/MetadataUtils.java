package org.apache.maven.artifact.repository.metadata;

import java.util.ArrayList;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * Assists in handling repository metadata.
 * 
 * @author Benjamin Bentmann
 */
class MetadataUtils
{

    // TODO: Kill this class once MODELLO-191 is released

    public static Metadata cloneMetadata( Metadata src )
    {
        if ( src == null )
        {
            return null;
        }

        Metadata target = new Metadata();

        target.setGroupId( src.getGroupId() );
        target.setArtifactId( src.getArtifactId() );
        target.setVersion( src.getVersion() );
        target.setVersioning( cloneVersioning( src.getVersioning() ) );

        for ( Plugin plugin : src.getPlugins() )
        {
            target.addPlugin( clonePlugin( plugin ) );
        }

        return target;
    }

    public static Plugin clonePlugin( Plugin src )
    {
        if ( src == null )
        {
            return null;
        }

        Plugin target = new Plugin();

        target.setArtifactId( src.getArtifactId() );
        target.setName( src.getName() );
        target.setPrefix( src.getPrefix() );

        return target;
    }

    public static Versioning cloneVersioning( Versioning src )
    {
        if ( src == null )
        {
            return null;
        }

        Versioning target = new Versioning();

        target.setLastUpdated( src.getLastUpdated() );
        target.setLatest( src.getLatest() );
        target.setRelease( src.getRelease() );
        target.setSnapshot( cloneSnapshot( src.getSnapshot() ) );
        target.setVersions( new ArrayList<String>( src.getVersions() ) );

        return target;
    }

    public static Snapshot cloneSnapshot( Snapshot src )
    {
        if ( src == null )
        {
            return null;
        }

        Snapshot target = new Snapshot();

        target.setBuildNumber( src.getBuildNumber() );
        target.setLocalCopy( src.isLocalCopy() );
        target.setTimestamp( src.getTimestamp() );

        return target;
    }

}
