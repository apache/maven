package org.apache.maven.artifact.handler.manager;

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

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;

/**
 * @author Jason van Zyl
 */
@Named
@Singleton
public class DefaultArtifactHandlerManager
    implements ArtifactHandlerManager
{

    @Inject
    private Map<String, ArtifactHandler> artifactHandlers;

    private Map<String, ArtifactHandler> allHandlers = new ConcurrentHashMap<>();

    public ArtifactHandler getArtifactHandler( String type )
    {
        ArtifactHandler handler = allHandlers.get( type );

        if ( handler == null )
        {
            handler = artifactHandlers.get( type );

            if ( handler == null )
            {
                handler = new DefaultArtifactHandler( type );
            }
            else
            {
                allHandlers.put( type, handler );
            }
        }

        return handler;
    }

    public void addHandlers( Map<String, ArtifactHandler> handlers )
    {
        // legacy support for maven-gpg-plugin:1.0
        allHandlers.putAll( handlers );
    }

    @Deprecated
    public Set<String> getHandlerTypes()
    {
        return artifactHandlers.keySet();
    }

}
