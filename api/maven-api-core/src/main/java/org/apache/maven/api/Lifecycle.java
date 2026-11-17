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

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Immutable;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.model.Plugin;

/**
 * A Maven lifecycle is a sequence of predefined phases that govern the build process
 * of a Maven project. Each phase represents a specific step, such as compiling the
 * code, running tests, packaging the project, and deploying it. Executing a phase
 * triggers all preceding phases, ensuring that each step of the build process is
 * completed in the correct order. The three main lifecycles in Maven are
 * {@link #DEFAULT default}, {@link #CLEAN clean}, and {@link #SITE site}, with the
 * {@code default} lifecycle being the most commonly used for project builds.
 *
 * @since 4.0.0
 */
@Experimental
@Immutable
public interface Lifecycle extends ExtensibleEnum {

    // =========================
    // Maven defined lifecycles
    // =========================
    String CLEAN = "clean";
    String DEFAULT = "default";
    String SITE = "site";

    // ======================
    // Phase qualifiers
    // ======================

    /**
     * Qualifier prefix for binding a mojo to run <em>before</em> a phase's main executions.
     * Used in plugin descriptors and lifecycle definitions to control execution ordering
     * within the phase graph.
     *
     * <p>Example: a mojo bound to {@code "before:compile"} runs before the mojos bound
     * to the {@code "compile"} phase itself. When a user requests the {@code "compile"}
     * phase (e.g. {@code mvn compile}), all three sub-steps execute:
     * {@code before:compile} → {@code compile} → {@code after:compile}.
     *
     * <p>These qualifiers are for <strong>mojo binding</strong>, not for CLI invocation.
     * Requesting {@code "before:compile"} from the CLI is not supported — the build
     * engine always executes the full phase including all its sub-steps.
     *
     * @see #AFTER
     * @see #AT
     */
    String BEFORE = "before:";

    /**
     * Qualifier prefix for binding a mojo to run <em>after</em> a phase's main executions.
     *
     * @see #BEFORE
     */
    String AFTER = "after:";

    /**
     * Qualifier prefix that resolves to the phase's main step (stripping the prefix).
     * Equivalent to using the bare phase name for binding purposes.
     *
     * @see #BEFORE
     */
    String AT = "at:";

    /**
     * Name or identifier of this lifecycle.
     *
     * @return the unique identifier for this lifecycle
     */
    @Override
    String id();

    /**
     * Collection of main phases for this lifecycle.
     *
     * @return the collection of top-level phases in this lifecycle
     */
    Collection<Phase> phases();

    /**
     * Collection of main phases for this lifecycle used with the Maven 3 builders.
     * Those builders do not operate on a graph, but on the list and expect a slightly
     * different ordering (mainly unit test being executed before packaging).
     *
     * @return the collection of phases in Maven 3 compatible ordering
     */
    default Collection<Phase> v3phases() {
        return phases();
    }

    /**
     * Stream of phases containing all child phases recursively.
     *
     * @return a stream of all phases in this lifecycle, including nested phases
     */
    default Stream<Phase> allPhases() {
        return phases().stream().flatMap(Phase::allPhases);
    }

    /**
     * Collection of aliases for this lifecycle.
     * Aliases map Maven 3 phase names to their Maven 4 equivalents.
     *
     * @return the collection of phase aliases
     */
    Collection<Alias> aliases();

    /**
     * A phase in the lifecycle.
     *
     * <p>A phase is identified by its {@link #name() name} and forms a tree: each phase
     * can contain {@link #phases() sub-phases} and declare {@link #links() ordering links}
     * to other phases. During execution, each phase is decomposed into three sub-steps
     * ({@link #BEFORE before:}<em>name</em>, <em>name</em>, {@link #AFTER after:}<em>name</em>)
     * that mojos can bind to.
     *
     * <p>Predefined phase names for the {@linkplain Lifecycle#DEFAULT default} lifecycle
     * include {@link #COMPILE}, {@link #TEST}, {@link #PACKAGE}, {@link #VERIFY},
     * {@link #INSTALL}, and {@link #DEPLOY}, among others.
     */
    interface Phase {

        // ======================
        // Maven defined phases
        // ======================
        String ALL = "all";
        String EACH = "each";
        String BUILD = "build";
        String INITIALIZE = "initialize";
        String VALIDATE = "validate";
        String SOURCES = "sources";
        String RESOURCES = "resources";
        String COMPILE = "compile";
        String READY = "ready";
        String PACKAGE = "package";
        String VERIFY = "verify";
        String UNIT_TEST = "unit-test";
        String TEST_SOURCES = "test-sources";
        String TEST_RESOURCES = "test-resources";
        String TEST_COMPILE = "test-compile";
        String TEST = "test";
        String INTEGRATION_TEST = "integration-test";
        String INSTALL = "install";
        String DEPLOY = "deploy";
        String CLEAN = "clean";

        /**
         * Returns the name of this phase.
         *
         * @return the phase name
         */
        @Nonnull
        String name();

        /**
         * Returns the list of plugins bound to this phase.
         *
         * @return the list of plugins
         */
        @Nonnull
        List<Plugin> plugins();

        /**
         * Returns the collection of links from this phase to other phases.
         *
         * @return the collection of links
         */
        @Nonnull
        Collection<Link> links();

        /**
         * {@return the list of sub-phases}
         */
        @Nonnull
        List<Phase> phases();

        /**
         * Returns a stream of all phases, including this phase and all nested phases.
         *
         * @return a stream of all phases
         */
        @Nonnull
        Stream<Phase> allPhases();
    }

    /**
     * A phase alias, mostly used to support the Maven 3 phases which are mapped
     * to dynamic phases in Maven 4.
     */
    interface Alias {
        /**
         * Returns the Maven 3 phase name.
         *
         * @return the Maven 3 phase name
         */
        String v3Phase();

        /**
         * Returns the Maven 4 phase name.
         *
         * @return the Maven 4 phase name
         */
        String v4Phase();
    }

    /**
     * A link from a phase to another phase, consisting of a type which can be
     * {@link Kind#BEFORE} or {@link Kind#AFTER}, and a {@link Pointer} to
     * another phase.
     */
    interface Link {
        enum Kind {
            BEFORE,
            AFTER
        }

        /**
         * Returns the kind of link (BEFORE or AFTER).
         *
         * @return the link kind
         */
        Kind kind();

        /**
         * Returns the pointer to the target phase.
         *
         * @return the phase pointer
         */
        Pointer pointer();
    }

    interface Pointer {
        enum Type {
            PROJECT,
            DEPENDENCIES,
            CHILDREN
        }

        /**
         * Returns the name of the target phase.
         *
         * @return the phase name
         */
        String phase();

        /**
         * Returns the type of pointer (PROJECT, DEPENDENCIES, or CHILDREN).
         *
         * @return the pointer type
         */
        Type type();
    }

    interface PhasePointer extends Pointer {
        /**
         * Returns the type of pointer, which is always PROJECT for a PhasePointer.
         *
         * @return the PROJECT pointer type
         */
        @Override
        default Type type() {
            return Type.PROJECT;
        }
    }

    interface DependenciesPointer extends Pointer {
        /**
         * Returns the dependency scope this pointer applies to.
         *
         * @return the dependency scope, or "all" if not specified
         */
        String scope(); // default: all

        /**
         * Returns the type of pointer, which is always DEPENDENCIES for a DependenciesPointer.
         *
         * @return the DEPENDENCIES pointer type
         */
        @Override
        default Type type() {
            return Type.DEPENDENCIES;
        }
    }

    interface ChildrenPointer extends Pointer {
        /**
         * Returns the type of pointer, which is always CHILDREN for a ChildrenPointer.
         *
         * @return the CHILDREN pointer type
         */
        @Override
        default Type type() {
            return Type.CHILDREN;
        }
    }
}
