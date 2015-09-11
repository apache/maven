package org.apache.maven.artifact.repository.metadata;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.artifact.repository.ArtifactRepository;

import java.util.Iterator;
import java.util.List;

/**
 * Metadata for the group directory of the repository.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class GroupRepositoryMetadata
    extends AbstractRepositoryMetadata
{
    private final String groupId;

    public GroupRepositoryMetadata( String groupId )
    {
        super( new Metadata() );
        this.groupId = groupId;
    }

    public boolean storedInGroupDirectory()
    {
        return true;
    }

    public boolean storedInArtifactVersionDirectory()
    {
        return false;
    }

    public String getGroupId()
    {
        return groupId;
    }

    public String getArtifactId()
    {
        return null;
    }

    public String getBaseVersion()
    {
        return null;
    }

    public void addPluginMapping( String goalPrefix,
                                  String artifactId )
    {
        addPluginMapping( goalPrefix, artifactId, artifactId );
    }

    public void addPluginMapping( String goalPrefix,
                                  String artifactId,
                                  String name )
    {
        List<Plugin> plugins = getMetadata().getPlugins();
        boolean found = false;
        for ( Iterator<Plugin> i = plugins.iterator(); i.hasNext() && !found; )
        {
            Plugin plugin = i.next();
            if ( plugin.getPrefix().equals( goalPrefix ) )
            {
                found = true;
            }
        }
        if ( !found )
        {
            Plugin plugin = new Plugin();
            plugin.setPrefix( goalPrefix );
            plugin.setArtifactId( artifactId );
            plugin.setName( name );


            getMetadata().addPlugin( plugin );
        }
    }

    public Object getKey()
    {
        return groupId;
    }

    public boolean isSnapshot()
    {
        return false;
    }

    public ArtifactRepository getRepository()
    {
        return null;
    }

    public void setRepository( ArtifactRepository remoteRepository )
    {
        // intentionally blank
    }
}
