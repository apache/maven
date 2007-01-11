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

import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;

import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl </a>
 * @version $Id: DefaultArtifactHandlerManager.java,v 1.1.1.1 2004/08/09
 *          18:37:32 jvanzyl Exp $
 */
public class DefaultArtifactHandlerManager
    implements ArtifactHandlerManager
{
    private Map artifactHandlers;

    public ArtifactHandler getArtifactHandler( String type )
    {
        ArtifactHandler handler = (ArtifactHandler) artifactHandlers.get( type );

        if ( handler == null )
        {
            handler = new DefaultArtifactHandler( type );
        }

        return handler;
    }

    public void addHandlers( Map handlers )
    {
        artifactHandlers.putAll( handlers );
    }

    public Set getHandlerTypes()
    {
        return artifactHandlers.keySet();
    }
}
