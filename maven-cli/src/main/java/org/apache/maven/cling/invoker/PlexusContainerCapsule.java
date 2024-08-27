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

import org.apache.maven.api.services.Lookup;
import org.apache.maven.internal.impl.DefaultLookup;
import org.codehaus.plexus.PlexusContainer;

import static java.util.Objects.requireNonNull;

/**
 * Container capsule backed by Plexus Container.
 */
public class PlexusContainerCapsule implements ContainerCapsule {
    private final PlexusContainer plexusContainer;
    private final Lookup lookup;

    public PlexusContainerCapsule(PlexusContainer plexusContainer) {
        this.plexusContainer = requireNonNull(plexusContainer, "plexusContainer");
        this.lookup = new DefaultLookup(plexusContainer);
    }

    @Override
    public Lookup getLookup() {
        return lookup;
    }

    @Override
    public void close() {
        plexusContainer.dispose();
    }
}
