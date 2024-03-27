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

import java.util.Set;

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
 * class-paths or module-paths.
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
     * Artifact type name for a POM file.
     */
    String POM = "pom";

    /**
     * Artifact type name for a BOM file.
     */
    String BOM = "bom";

    /**
     * Artifact type name for a JAR file that can be placed either on the class-path or on the module-path.
     * The path (classes or modules) is chosen by the plugin, possibly using heuristic rules.
     * This is the behavior of Maven 3.
     */
    String JAR = "jar";

    /**
     * Artifact type name for a fat-JAR file that can be placed either on the class-path or on the module-path.
     * The fat-JAR is a self-contained JAR and its transitive dependencies will not be resolved, if any.
     * The path (classes or modules) is chosen by the plugin, possibly using heuristic rules.
     * This is the behavior of Maven 3.
     */
    String FATJAR = "fatjar";

    /**
     * Artifact type name for a JAR file to unconditionally place on the class-path.
     * If the JAR is modular, its module information are ignored.
     * This type is new in Maven 4.
     */
    String CLASSPATH_JAR = "classpath-jar";

    /**
     * Artifact type name for a fat-JAR file to unconditionally place on the class-path.
     * The fat-JAR is a self-contained JAR and its transitive dependencies will not be resolved, if any.
     * If the fat-JAR is modular, its module information are ignored.
     * This type is new in Maven 4.
     */
    String CLASSPATH_FATJAR = "classpath-fatjar";

    /**
     * Artifact type name for a JAR file to unconditionally place on the module-path.
     * If the JAR is not modular, then it is loaded by Java as an unnamed module.
     * This type is new in Maven 4.
     */
    String MODULAR_JAR = "modular-jar";

    /**
     * Artifact type name for a JAR file to unconditionally place on the module-path.
     * The fat-JAR is a self-contained JAR and its transitive dependencies will not be resolved, if any.
     * If the fat-JAR is not modular, then it is loaded by Java as an unnamed module.
     * This type is new in Maven 4.
     */
    String MODULAR_FATJAR = "modular-fatjar";

    /**
     * Artifact type name for source code packaged in a JAR file.
     */
    String JAVA_SOURCE = "java-source";

    /**
     * Artifact type name for javadoc packaged in a JAR file.
     */
    String JAVADOC = "javadoc";

    /**
     * Artifact type name for a Maven plugin.
     */
    String MAVEN_PLUGIN = "maven-plugin";

    /**
     * Artifact type name for a JAR file containing test classes. If the main artifact is placed on the class-path
     * ({@value #JAR} or {@value #CLASSPATH_JAR} types), then the test artifact will also be placed on the class-path.
     * Otherwise, if the main artifact is placed on the module-path ({@value #JAR} or {@value #MODULAR_JAR} types),
     * then the test artifact will be added using {@code --patch-module} option.
     */
    String TEST_JAR = "test-jar";

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
     * Specifies if the artifact already embeds its own dependencies.
     * This is the case for JEE packages or similar artifacts such as
     * WARs, EARs, etc.
     *
     * @return if the artifact's dependencies are included in the artifact
     */
    boolean isIncludesDependencies();

    /**
     * Types of path (class-path, module-path, …) where the dependency can be placed.
     * For most deterministic builds, the array length should be 1. In such case,
     * the dependency will be unconditionally placed on the specified type of path
     * and no heuristic rule will be involved.
     *
     * <p>It is nevertheless common to specify two or more types of path. For example,
     * a Java library may be compatible with either the class-path or the module-path,
     * and the user may have provided no instruction about which type to use. In such
     * case, the plugin may apply rules for choosing a path. See for example
     * {@link JavaPathType#CLASSES} and {@link JavaPathType#MODULES}.</p>
     */
    @Nonnull
    Set<PathType> getPathTypes();
}
