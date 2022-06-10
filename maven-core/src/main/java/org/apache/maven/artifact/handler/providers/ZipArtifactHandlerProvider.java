package org.apache.maven.artifact.handler.providers;

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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@code zip} artifact handler provider.
 */
@Named( "zip" )
@Singleton
public class ZipArtifactHandlerProvider
    implements Provider<ArtifactHandler>
{
    private static final boolean ADDED_TO_CLASSPATH = Boolean.parseBoolean( System.getProperty(
            "maven.artifactHandler.zip.addedToClasspath", Boolean.TRUE.toString() ) );
    private final ArtifactHandler artifactHandler;

    @Inject
    public ZipArtifactHandlerProvider()
    {
        this.artifactHandler = new DefaultArtifactHandler(
                "zip",
                null,
                null,
                null,
                null,
                false,
                "java",
                ADDED_TO_CLASSPATH
        );

        // TODO: here only to verify proper set up
        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.info( "addedToClasspath={}", ADDED_TO_CLASSPATH );
    }

    @Override
    public ArtifactHandler get()
    {
        return artifactHandler;
    }
}
