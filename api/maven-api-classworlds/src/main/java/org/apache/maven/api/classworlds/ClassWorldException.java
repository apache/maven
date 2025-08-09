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
package org.apache.maven.api.classworlds;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;

/**
 * Base exception for class world related errors.
 * <p>
 * This is the root exception type for all class world operations.
 * Specific error conditions are represented by subclasses.
 * </p>
 *
 * @since 4.1.0
 */
@Experimental
public class ClassWorldException extends Exception {

    /**
     * The class world associated with this exception.
     */
    private final ClassWorld world;

    /**
     * Constructs a new ClassWorldException.
     *
     * @param world the class world associated with this exception
     */
    public ClassWorldException(@Nonnull ClassWorld world) {
        this.world = world;
    }

    /**
     * Constructs a new ClassWorldException with the specified detail message.
     *
     * @param world the class world associated with this exception
     * @param message the detail message
     */
    public ClassWorldException(@Nonnull ClassWorld world, @Nullable String message) {
        super(message);
        this.world = world;
    }

    /**
     * Constructs a new ClassWorldException with the specified detail message and cause.
     *
     * @param world the class world associated with this exception
     * @param message the detail message
     * @param cause the cause
     */
    public ClassWorldException(@Nonnull ClassWorld world, @Nullable String message, @Nullable Throwable cause) {
        super(message, cause);
        this.world = world;
    }

    /**
     * Constructs a new ClassWorldException with the specified cause.
     *
     * @param world the class world associated with this exception
     * @param cause the cause
     */
    public ClassWorldException(@Nonnull ClassWorld world, @Nullable Throwable cause) {
        super(cause);
        this.world = world;
    }

    /**
     * Returns the class world associated with this exception.
     *
     * @return the class world
     */
    @Nonnull
    public ClassWorld getWorld() {
        return world;
    }
}
