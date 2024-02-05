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
package org.apache.maven.api;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Immutable;
import org.apache.maven.api.annotations.Nonnull;

/**
 * Interface representing a Maven project packaging.
 * <p>
 * TODO: define how to plug in new packaging definitions using the SPI.
 *   the packaging are currently defined by Maven 3 {@code Provider<LifecycleMapping>}
 *
 * @since 4.0.0
 */
@Experimental
@Immutable
public interface Packaging extends ExtensibleEnum {
    /**
     * The packaging id.
     */
    @Nonnull
    String id();

    /**
     * The language of this packaging.
     */
    @Nonnull
    default Language language() {
        return getType().getLanguage();
    }

    /**
     * The type of main artifact produced by this packaging.
     */
    @Nonnull
    Type getType();
}
