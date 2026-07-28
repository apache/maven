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

import java.io.Serializable;
import java.nio.file.Path;
import java.util.Map;

import org.apache.maven.api.annotations.Consumer;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.annotations.ThreadSafe;

/**
 * Provides the environment configuration needed to initialize a build context,
 * including state file location, workspace, parameters, and an optional finalizer.
 *
 * @since 4.0.0
 */
@Experimental
@ThreadSafe
@Consumer
public interface BuildContextEnvironment {

    /**
     * {@return the path to the file where build context state is persisted}
     */
    @Nonnull
    Path getStateFile();

    /**
     * {@return the workspace that provides file system abstraction}
     */
    @Nonnull
    Workspace getWorkspace();

    /**
     * {@return the configuration parameters for the build context}
     */
    @Nonnull
    Map<String, Serializable> getParameters();

    /**
     * {@return the optional context finalizer, or {@code null} if none is configured}
     */
    @Nullable
    BuildContextFinalizer getFinalizer();
}
