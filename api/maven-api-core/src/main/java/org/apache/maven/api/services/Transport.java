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
package org.apache.maven.api.services;

import java.io.Closeable;
import java.net.URI;
import java.nio.file.Path;
import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.annotations.Consumer;
import org.apache.maven.api.annotations.Experimental;

/**
 * Transport for specified remote repository (using provided remote repository base URI as root). Must be treated as a
 * resource, best in try-with-resource block.
 *
 * @since 4.0
 */
@Experimental
@Consumer
public interface Transport extends Closeable {
    /**
     * GETs the source URI content into target file (does not have to exist, or will be overwritten if exist). The
     * source MUST BE relative from the {@link RemoteRepository#getUrl()} root.
     *
     * @return {@code true} if the source was GET correctly from source URI into passed target file. Returns
     * {@code false} if source does not exist (then file is intact, if it did not exist, still does not exist).
     * @throws RuntimeException In any other case (GET did not successful and not due not exist).
     */
    boolean get(URI relativeSource, Path target);

    /**
     * PUTs the source file (must exist) to target URI. The target MUST BE relative from the
     * {@link RemoteRepository#getUrl()} root.
     *
     * @throws RuntimeException If PUT fails for any reason.
     */
    void put(Path source, URI relativeTarget);
}
