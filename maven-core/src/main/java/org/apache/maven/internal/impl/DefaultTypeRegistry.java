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
package org.apache.maven.internal.impl;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.api.Type;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.services.TypeRegistry;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;

import static org.apache.maven.internal.impl.Utils.nonNull;

@Named
@Singleton
public class DefaultTypeRegistry
    implements TypeRegistry
{

    private final ArtifactHandlerManager manager;

    @Inject
    public DefaultTypeRegistry( ArtifactHandlerManager manager )
    {
        this.manager = nonNull( manager, "artifactHandlerManager" );
    }

    @Override
    @Nonnull
    public Type getType( String id )
    {
        // Copy data as the ArtifacHandler is not immutable, but Type should be.
        ArtifactHandler handler = manager.getArtifactHandler( nonNull( id, "id" ) );
        String extension = handler.getExtension();
        String classifier = handler.getClassifier();
        boolean includeDependencies = handler.isIncludesDependencies();
        boolean addedToClasspath = handler.isAddedToClasspath();
        return new Type()
        {
            @Override
            public String getName()
            {
                return id;
            }

            @Override
            public String getExtension()
            {
                return extension;
            }

            @Override
            public String getClassifier()
            {
                return classifier;
            }

            @Override
            public boolean isIncludesDependencies()
            {
                return includeDependencies;
            }

            @Override
            public boolean isAddedToClasspath()
            {
                return addedToClasspath;
            }
        };
    }

}
