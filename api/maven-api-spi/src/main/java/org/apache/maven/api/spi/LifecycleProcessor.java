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
 * <p>Processors are called each time the lifecycle phase map is rebuilt (potentially multiple
 * times per build). Implementations must be stateless and idempotent — the same lifecycle
 * input must always produce the same output. They receive the output of the previous processor
 * in the chain.
 *
 * <p>See {@code ReactorXmlLifecycleProcessor} in {@code impl/maven-cli} for a concrete
 * reference implementation that injects phases from {@code .mvn/reactor.xml}.
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
