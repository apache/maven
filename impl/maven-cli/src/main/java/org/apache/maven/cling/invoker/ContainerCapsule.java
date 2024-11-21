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

import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.cli.InvokerException;
import org.apache.maven.api.services.Lookup;

/**
 * Container capsule.
 */
public interface ContainerCapsule extends AutoCloseable {
    /**
     * Updates the existing capsule logging setup.
     */
    void updateLogging(LookupContext context);

    /**
     * The {@link Lookup} service backed by container in this capsule.
     */
    @Nonnull
    Lookup getLookup();

    /**
     * The TCCL, if implementation requires it.
     */
    @Nonnull
    Optional<ClassLoader> currentThreadClassLoader();

    /**
     * Performs a clean shutdown of backing container.
     */
    @Override
    void close() throws InvokerException;
}
