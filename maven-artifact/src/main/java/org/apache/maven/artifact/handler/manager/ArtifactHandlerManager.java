package org.apache.maven.artifact.handler.manager;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.repository.ArtifactRepository;

import java.util.Set;

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

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public interface ArtifactHandlerManager
{
    String ROLE = ArtifactHandlerManager.class.getName();

    ArtifactHandler getArtifactHandler( String type )
        throws ArtifactHandlerNotFoundException;

    String localRepositoryPath( Artifact artifact, ArtifactRepository localRepository );

    String path( Artifact artifact );

    Set getHandlerTypes();

}
