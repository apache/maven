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
package org.apache.maven.api.spi;

import org.apache.maven.api.Lifecycle;
import org.apache.maven.api.annotations.Consumer;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.di.Named;

/**
 * SPI for transforming Maven lifecycles at session initialisation time.
 *
 * <p>A {@code LifecycleProcessor} receives each built-in (or extension-contributed) lifecycle
 * and may return a modified version of it — for example to inject additional phases into the
 * lifecycle DAG.  Multiple processors are applied in order; each one receives the output of the
 * previous one.
 *
 * <p>Processors are called once per lifecycle per session, after the base lifecycles have been
 * assembled by the {@link LifecycleProvider}s, so they have access to full session context.
 *
 * <p>Example — injecting a {@code docker-push} phase after {@code deploy}:
 * <pre>{@code
 * @Named
 * public class DockerLifecycleProcessor implements LifecycleProcessor {
 *     public Lifecycle process(Lifecycle lifecycle) {
 *         if (!Lifecycle.DEFAULT.equals(lifecycle.id())) return lifecycle;
 *         return LifecycleProcessor.withInjectedPhase(lifecycle,
 *                 "docker-push", "deploy", null);
 *     }
 * }
 * }</pre>
 *
 * @since 4.1.0
 */
@Experimental
@Consumer
@Named
public interface LifecycleProcessor extends SpiService {

    /**
     * Processes a lifecycle, optionally returning a transformed version.
     *
     * @param lifecycle the lifecycle to process; never {@code null}
     * @return the processed lifecycle; must not be {@code null}; may be the same instance
     *         if no transformation is needed
     */
    @Nonnull
    Lifecycle process(@Nonnull Lifecycle lifecycle);
}
