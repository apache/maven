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

/**
 * Exception thrown when attempting to retrieve a class realm with an identifier
 * that does not exist in the class world.
 *
 * @since 4.1.0
 */
@Experimental
public class NoSuchRealmException extends ClassWorldException {

    /**
     * The missing realm identifier.
     */
    private final String id;

    /**
     * Constructs a new NoSuchRealmException.
     *
     * @param world the class world
     * @param id the missing realm identifier
     */
    public NoSuchRealmException(@Nonnull ClassWorld world, @Nonnull String id) {
        super(world, "No such realm: " + id);
        this.id = id;
    }

    /**
     * Returns the missing realm identifier.
     *
     * @return the realm identifier
     */
    @Nonnull
    public String getId() {
        return id;
    }
}
