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
package org.apache.maven.cling.invoker;

import java.util.Optional;

import org.apache.maven.api.services.Lookup;
import org.apache.maven.internal.impl.DefaultLookup;
import org.codehaus.plexus.DefaultPlexusContainer;

import static java.util.Objects.requireNonNull;
import static org.apache.maven.cling.invoker.Utils.toPlexusLoggingLevel;

/**
 * Container capsule backed by Plexus Container.
 */
public class PlexusContainerCapsule implements ContainerCapsule {
    private final ClassLoader previousClassLoader;
    private final DefaultPlexusContainer plexusContainer;
    private final Lookup lookup;

    public PlexusContainerCapsule(
            LookupContext context, ClassLoader previousClassLoader, DefaultPlexusContainer plexusContainer) {
        this.previousClassLoader = requireNonNull(previousClassLoader, "previousClassLoader");
        this.plexusContainer = requireNonNull(plexusContainer, "plexusContainer");
        this.lookup = new DefaultLookup(plexusContainer);
        updateLogging(context);
    }

    @Override
    public void updateLogging(LookupContext context) {
        plexusContainer.getLoggerManager().setThresholds(toPlexusLoggingLevel(context.loggerLevel));
        org.slf4j.Logger l = context.loggerFactory.getLogger(this.getClass().getName());
        context.logger = (level, message, error) -> l.atLevel(org.slf4j.event.Level.valueOf(level.name()))
                .setCause(error)
                .log(message);
    }

    @Override
    public Lookup getLookup() {
        return lookup;
    }

    @Override
    public Optional<ClassLoader> currentThreadClassLoader() {
        return Optional.of(plexusContainer.getContainerRealm());
    }

    @Override
    public void close() {
        try {
            plexusContainer.dispose();
        } finally {
            Thread.currentThread().setContextClassLoader(previousClassLoader);
        }
    }
}
