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
    String BEFORE = "before:";
    String AFTER = "after:";
    String AT = "at:";

    /**
     * Name or identifier of this lifecycle.
     *
     * @return the unique identifier for this lifecycle
     */
    @Override
    String id();

    /**
     * Collection of main phases for this lifecycle
     */
    Collection<Phase> phases();

    /**
     * Collection of main phases for this lifecycle used with the Maven 3 builders.
     * Those builders does not operate on a graph, but on the list and expect a slightly
     * different ordering (mainly unit test being executed before packaging).
     */
    default Collection<Phase> v3phases() {
        return phases();
    }

    /**
     * Stream of phases containing all child phases recursively.
     */
    default Stream<Phase> allPhases() {
        return phases().stream().flatMap(Phase::allPhases);
    }

    /**
     * Collection of aliases.
     */
    Collection<Alias> aliases();

    /**
     * A phase in the lifecycle.
     *
     * A phase is identified by its name. It also contains a list of plugins bound to that phase,
     * a list of {@link Link links}, and a list of sub-phases.  This forms a tree of phases.
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

        @Nonnull
        String name();

        @Nonnull
        List<Plugin> plugins();

        @Nonnull
        Collection<Link> links();

        /**
         * {@return the list of sub-phases}
         */
        @Nonnull
        List<Phase> phases();

        @Nonnull
        Stream<Phase> allPhases();
    }

    /**
     * A phase alias, mostly used to support the Maven 3 phases which are mapped
     * to dynamic phases in Maven 4.
     */
    interface Alias {
        String v3Phase();

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

        Kind kind();

        Pointer pointer();
    }

    interface Pointer {
        enum Type {
            PROJECT,
            DEPENDENCIES,
            CHILDREN
        }

        String phase();

        Type type();
    }

    interface PhasePointer extends Pointer {
        default Type type() {
            return Type.PROJECT;
        }
    }

    interface DependenciesPointer extends Pointer {
        String scope(); // default: all

        default Type type() {
            return Type.DEPENDENCIES;
        }
    }

    interface ChildrenPointer extends Pointer {
        default Type type() {
            return Type.CHILDREN;
        }
    }
}
