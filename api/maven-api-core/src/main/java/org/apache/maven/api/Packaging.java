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

import java.util.Map;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Immutable;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.model.PluginContainer;

/**
 * Represents the packaging of a Maven project.
 *
 * <p>The {@code Packaging} class defines the type of artifact that a Maven project produces during the build process.
 * The packaging type determines the structure of the project's output and how Maven will treat the resulting artifact.</p>
 *
 * <p>Common packaging types include {@code jar}, {@code war}, {@code pom}, {@code maven-plugin}, {@code ear}, and others.
 * These types influence various aspects of the build lifecycle, such as which plugins are executed and how dependencies are managed.</p>
 *
 * <p>The {@code Packaging} class is an immutable value object, ensuring that once a packaging type is defined, it cannot be changed.</p>
 *
 * <h2>Standard Packaging Types</h2>
 * <ul>
 *     <li>{@code jar}: Packages the project as a Java Archive (JAR) file.</li>
 *     <li>{@code war}: Packages the project as a Web Application Archive (WAR) file.</li>
 *     <li>{@code pom}: Indicates that the project does not produce a deployable artifact but is used for dependency management or as an aggregator.</li>
 *     <li>{@code maven-plugin}: Packages the project as a Maven plugin.</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 *
 * <pre>
 * {@code
 * Session session = ... // Obtain a Maven session
 * Packaging packaging = session.requirePackaging("jar");
 * System.out.println(packaging.getId()); // Outputs "jar"
 * }
 * </pre>
 *
 * @see org.apache.maven.api.Session#requirePackaging(String)
 * @see org.apache.maven.api.Project#getPackaging()
 * @see org.apache.maven.api.model.Model#getPackaging()
 * @since 4.0.0
 */
@Experimental
@Immutable
public interface Packaging extends ExtensibleEnum {
    /**
     * The packaging id.
     */
    @Nonnull
    @Override
    String id();

    /**
     * The language of this packaging.
     */
    @Nonnull
    default Language language() {
        return type().getLanguage();
    }

    /**
     * The type of main artifact produced by this packaging.
     */
    @Nonnull
    Type type();

    /**
     * Returns the binding to use specifically for this packaging keyed by lifecycle id.
     * This will be used instead of the default packaging definition.
     */
    @Nonnull
    Map<String, PluginContainer> plugins();
}
