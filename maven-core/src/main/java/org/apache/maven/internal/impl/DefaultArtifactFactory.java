package org.apache.maven.internal.impl;

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

import javax.inject.Named;

import java.security.InvalidParameterException;

import org.apache.maven.api.Artifact;
import org.apache.maven.api.services.ArtifactFactory;
import org.apache.maven.api.services.ArtifactFactoryException;
import org.apache.maven.api.services.ArtifactFactoryRequest;
import org.eclipse.aether.artifact.ArtifactType;

@Named
public class DefaultArtifactFactory implements ArtifactFactory
{
    @Override
    public Artifact create( ArtifactFactoryRequest request ) throws ArtifactFactoryException, InvalidParameterException
    {
        DefaultSession session = (DefaultSession) request.getSession();
        ArtifactType type = null;
        if ( request.getType() != null )
        {
            type = session.getSession().getArtifactTypeRegistry().get( request.getType() );
        }
        return new DefaultArtifact(
                session,
                new org.eclipse.aether.artifact.DefaultArtifact(
                    request.getGroupId(),
                    request.getArtifactId(),
                    request.getClassifier(),
                    request.getExtension(),
                    request.getVersion(),
                    type ) );
    }
}
