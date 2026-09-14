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
package org.apache.maven.resolver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.di.Inject;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.spi.WorkspaceReader;

/**
 * Collects all {@link WorkspaceReader} SPI implementations from core extensions via the
 * maven-di layer.
 *
 * <p>SPI components registered via {@link Named @Named} live in the maven-di layer, not in
 * Plexus/SISU, so {@code container.lookupList()} cannot find them. This holder is a maven-di
 * {@link Singleton @Singleton} that receives all named {@link WorkspaceReader} bindings injected
 * as a {@code Map} at first instantiation — which happens in
 * {@code DefaultMaven.setupWorkspaceReader()}, after {@code buildGraph()} has loaded core
 * extensions and their DI bindings are visible.</p>
 *
 * <p>This class intentionally uses {@code @org.apache.maven.api.di} annotations (not
 * {@code javax.inject}), so it is only discovered by the maven-di injector and bridged to
 * Guice/SISU via {@code SisuDiBridgeModule.BridgeInjectorImpl}, making it accessible via
 * {@code Lookup.lookup(SpiWorkspaceReadersHolder.class)}.</p>
 *
 * <p>The {@code Map} parameter is {@link Nullable} so that when no {@link WorkspaceReader}
 * SPI implementation is registered (no core extension provides one), the maven-di injector
 * injects {@code null} rather than throwing a {@code DIException}.</p>
 *
 * @since 4.1.0
 */
@Named
@Singleton
public class SpiWorkspaceReadersHolder {
    private final List<WorkspaceReader> readers;

    @Inject
    public SpiWorkspaceReadersHolder(@Nullable Map<String, WorkspaceReader> readers) {
        this.readers = readers != null ? new ArrayList<>(readers.values()) : Collections.emptyList();
    }

    public List<WorkspaceReader> getReaders() {
        return readers;
    }
}
