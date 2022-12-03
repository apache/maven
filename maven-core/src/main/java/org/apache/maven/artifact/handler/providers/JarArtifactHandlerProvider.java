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
package org.apache.maven.artifact.handler.providers;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;

/**
 * {@code jar} artifact handler provider.
 */
@Named("jar")
@Singleton
public class JarArtifactHandlerProvider implements Provider<ArtifactHandler> {
    private final ArtifactHandler artifactHandler;

    @Inject
    public JarArtifactHandlerProvider() {
        this.artifactHandler = new DefaultArtifactHandler("jar", null, null, null, null, false, "java", true);
    }

    @Override
    public ArtifactHandler get() {
        return artifactHandler;
    }
}
