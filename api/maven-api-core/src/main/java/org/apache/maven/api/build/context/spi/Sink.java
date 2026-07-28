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
package org.apache.maven.api.build.context.spi;

import java.nio.file.Path;
import java.util.Collection;

import org.apache.maven.api.annotations.Consumer;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.NotThreadSafe;

/**
 * Receives diagnostic messages produced during a build context commit.
 * Implementations typically forward these to the IDE or build log.
 *
 * @since 4.0.0
 */
@Experimental
@NotThreadSafe
@Consumer
public interface Sink {

    /**
     * Reports messages for a resource.
     *
     * @param resource the resource path the messages belong to
     * @param isNew    {@code true} if the resource is new in this build
     * @param messages the diagnostic messages to report
     */
    void messages(@Nonnull Path resource, boolean isNew, @Nonnull Collection<Message> messages);

    /**
     * Clears all previously reported messages for a resource.
     *
     * @param resource the resource path to clear messages for
     */
    void clear(@Nonnull Path resource);
}
