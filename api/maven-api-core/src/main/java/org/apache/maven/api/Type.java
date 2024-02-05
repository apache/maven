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
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.model.Dependency;

/**
 * A dependency's {@code Type} is uniquely identified by a {@code String},
 * and semantically represents a known <i>kind</i> of dependency.
 * <p>
 * It provides information about the file type (or extension) of the associated artifact,
 * its default classifier, and how the artifact will be used in the build when creating
 * various build paths.
 * <p>
 * For example, the type {@code java-source} has a {@code jar} extension and a
 * {@code sources} classifier. The artifact and its dependencies should be added
 * to the build path.
 *
 * @since 4.0.0
 */
@Experimental
@Immutable
public interface Type extends ExtensibleEnum {
    /**
     * Returns the dependency type id.
     * The id uniquely identifies this <i>dependency type</i>.
     *
     * @return the id of this type, never {@code null}.
     */
    @Nonnull
    String id();

    /**
     * Returns the dependency type language.
     *
     * @return the language of this type, never {@code null}.
     */
    @Nonnull
    Language getLanguage();

    /**
     * Get the file extension of artifacts of this type.
     *
     * @return the file extension, never {@code null}.
     */
    @Nonnull
    String getExtension();

    /**
     * Get the default classifier associated to the dependency type.
     * The default classifier can be overridden when specifying
     * the {@link Dependency#getClassifier()}.
     *
     * @return the default classifier, or {@code null}.
     */
    @Nullable
    String getClassifier();

    /**
     * Specifies if the artifact should be added to the build path.
     *
     * @return if the artifact should be added to the build path
     */
    boolean isBuildPathConstituent();

    /**
     * Specifies if the artifact already embeds its own dependencies.
     * This is the case for JEE packages or similar artifacts such as
     * WARs, EARs, etc.
     *
     * @return if the artifact's dependencies are included in the artifact
     */
    boolean isIncludesDependencies();
}
