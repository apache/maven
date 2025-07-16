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
package org.apache.maven.impl.model;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.maven.api.Session;
import org.apache.maven.api.model.Dependency;
import org.apache.maven.api.model.DependencyManagement;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.Plugin;
import org.apache.maven.api.services.model.ModelValidator;
import org.apache.maven.impl.model.profile.SimpleProblemCollector;
import org.apache.maven.impl.standalone.ApiRunner;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * JMH Benchmark for measuring the performance gains from PR #2518:
 * Optimize validation performance with lazy SourceHint evaluation.
 *
 * This benchmark measures the performance difference between validating
 * models with different numbers of dependencies (1, 10, 100) to demonstrate
 * how the lazy evaluation optimization scales with project complexity.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 3)
@State(Scope.Benchmark)
public class ModelValidationBenchmark {

    @Param({"1", "10", "100"})
    private int dependencyCount;

    private Session session;
    private ModelValidator validator;
    private Model validModel;
    private Model invalidModel;
    private SimpleProblemCollector problemCollector;

    @Setup(Level.Trial)
    public void setup() {
        session = ApiRunner.createSession();
        validator = new DefaultModelValidator();

        // Create models with different numbers of dependencies
        validModel = createValidModel(dependencyCount);
        invalidModel = createInvalidModel(dependencyCount);
    }

    @Setup(Level.Invocation)
    public void setupInvocation() {
        problemCollector = new SimpleProblemCollector();
    }

    /**
     * Benchmark validation of a valid model (no validation errors).
     * This is the common case where lazy evaluation provides the most benefit
     * since SourceHint strings are never computed.
     */
    @Benchmark
    public void validateValidModel() {
        validator.validateEffectiveModel(session, validModel, ModelValidator.VALIDATION_LEVEL_STRICT, problemCollector);
    }

    /**
     * Benchmark validation of an invalid model (with validation errors).
     * This tests the case where SourceHint strings are actually computed
     * and used in error messages.
     */
    @Benchmark
    public void validateInvalidModel() {
        validator.validateEffectiveModel(
                session, invalidModel, ModelValidator.VALIDATION_LEVEL_STRICT, problemCollector);
    }

    /**
     * Benchmark raw model validation (before inheritance and interpolation).
     * This tests the validation of the raw model as read from the POM file.
     */
    @Benchmark
    public void validateRawModel() {
        validator.validateRawModel(session, validModel, ModelValidator.VALIDATION_LEVEL_STRICT, problemCollector);
    }

    /**
     * Benchmark validation with minimal validation level.
     * This tests performance with reduced validation checks.
     */
    @Benchmark
    public void validateMinimalLevel() {
        validator.validateEffectiveModel(
                session, validModel, ModelValidator.VALIDATION_LEVEL_MINIMAL, problemCollector);
    }

    /**
     * Benchmark validation focusing on dependency management.
     * This creates a model with many managed dependencies to stress-test
     * the SourceHint.dependencyManagementKey() optimization.
     */
    @Benchmark
    public void validateDependencyManagement() {
        Model modelWithManyManagedDeps = createModelWithManyManagedDependencies(dependencyCount);
        validator.validateEffectiveModel(
                session, modelWithManyManagedDeps, ModelValidator.VALIDATION_LEVEL_STRICT, problemCollector);
    }

    /**
     * Creates a valid model with the specified number of dependencies.
     * Includes dependency management and plugins to simulate real-world complexity.
     */
    private Model createValidModel(int dependencyCount) {
        List<Dependency> dependencies = new ArrayList<>();
        List<Dependency> managedDependencies = new ArrayList<>();
        List<Plugin> plugins = new ArrayList<>();

        // Create regular dependencies
        for (int i = 0; i < dependencyCount; i++) {
            dependencies.add(Dependency.newBuilder()
                    .groupId("org.example.group" + i)
                    .artifactId("artifact" + i)
                    .version("1.0.0")
                    .type("jar")
                    .scope("compile")
                    .build());
        }

        // Create managed dependencies (typically fewer than regular dependencies)
        int managedCount = Math.max(1, dependencyCount / 3);
        for (int i = 0; i < managedCount; i++) {
            managedDependencies.add(Dependency.newBuilder()
                    .groupId("org.managed.group" + i)
                    .artifactId("managed-artifact" + i)
                    .version("2.0.0")
                    .type("jar")
                    .scope("compile")
                    .build());
        }

        // Create plugins (typically fewer than dependencies)
        int pluginCount = Math.max(1, dependencyCount / 5);
        for (int i = 0; i < pluginCount; i++) {
            plugins.add(Plugin.newBuilder()
                    .groupId("org.apache.maven.plugins")
                    .artifactId("maven-plugin-" + i)
                    .version("3.0.0")
                    .build());
        }

        return Model.newBuilder()
                .modelVersion("4.0.0")
                .groupId("org.apache.maven.benchmark")
                .artifactId("validation-benchmark")
                .version("1.0.0")
                .packaging("jar")
                .dependencies(dependencies)
                .dependencyManagement(DependencyManagement.newBuilder()
                        .dependencies(managedDependencies)
                        .build())
                .build();
    }

    /**
     * Creates an invalid model with the specified number of dependencies.
     * Some dependencies will have missing required fields to trigger validation errors
     * and exercise the SourceHint generation code paths.
     */
    private Model createInvalidModel(int dependencyCount) {
        List<Dependency> dependencies = new ArrayList<>();
        List<Dependency> managedDependencies = new ArrayList<>();

        // Create dependencies with various validation errors
        for (int i = 0; i < dependencyCount; i++) {
            if (i % 4 == 0) {
                // Missing version (triggers SourceHint.dependencyManagementKey)
                dependencies.add(Dependency.newBuilder()
                        .groupId("org.example.group" + i)
                        .artifactId("artifact" + i)
                        .type("jar")
                        .scope("compile")
                        .build());
            } else if (i % 4 == 1) {
                // Missing groupId (triggers validation error)
                dependencies.add(Dependency.newBuilder()
                        .artifactId("artifact" + i)
                        .version("1.0.0")
                        .type("jar")
                        .scope("compile")
                        .build());
            } else if (i % 4 == 2) {
                // Missing artifactId (triggers validation error)
                dependencies.add(Dependency.newBuilder()
                        .groupId("org.example.group" + i)
                        .version("1.0.0")
                        .type("jar")
                        .scope("compile")
                        .build());
            } else {
                // Valid dependency (some should be valid to test mixed scenarios)
                dependencies.add(Dependency.newBuilder()
                        .groupId("org.example.group" + i)
                        .artifactId("artifact" + i)
                        .version("1.0.0")
                        .type("jar")
                        .scope("compile")
                        .build());
            }
        }

        // Add some invalid managed dependencies too
        int managedCount = Math.max(1, dependencyCount / 3);
        for (int i = 0; i < managedCount; i++) {
            if (i % 2 == 0) {
                // Missing version in dependency management
                managedDependencies.add(Dependency.newBuilder()
                        .groupId("org.managed.group" + i)
                        .artifactId("managed-artifact" + i)
                        .type("jar")
                        .build());
            } else {
                // Valid managed dependency
                managedDependencies.add(Dependency.newBuilder()
                        .groupId("org.managed.group" + i)
                        .artifactId("managed-artifact" + i)
                        .version("2.0.0")
                        .type("jar")
                        .build());
            }
        }

        return Model.newBuilder()
                .modelVersion("4.0.0")
                .groupId("org.apache.maven.benchmark")
                .artifactId("validation-benchmark")
                .version("1.0.0")
                .packaging("jar")
                .dependencies(dependencies)
                .dependencyManagement(DependencyManagement.newBuilder()
                        .dependencies(managedDependencies)
                        .build())
                .build();
    }

    /**
     * Creates a model with many managed dependencies to stress-test
     * the SourceHint.dependencyManagementKey() optimization.
     */
    private Model createModelWithManyManagedDependencies(int dependencyCount) {
        List<Dependency> managedDependencies = new ArrayList<>();

        // Create many managed dependencies with different classifiers and types
        for (int i = 0; i < dependencyCount; i++) {
            String classifier = (i % 3 == 0) ? "sources" : (i % 3 == 1) ? "javadoc" : null;
            String type = (i % 4 == 0) ? "jar" : (i % 4 == 1) ? "war" : (i % 4 == 2) ? "pom" : "ejb";

            managedDependencies.add(Dependency.newBuilder()
                    .groupId("org.managed.group" + i)
                    .artifactId("managed-artifact" + i)
                    .version("2.0.0")
                    .type(type)
                    .classifier(classifier)
                    .scope("compile")
                    .build());
        }

        return Model.newBuilder()
                .modelVersion("4.0.0")
                .groupId("org.apache.maven.benchmark")
                .artifactId("dependency-management-benchmark")
                .version("1.0.0")
                .packaging("pom")
                .dependencyManagement(DependencyManagement.newBuilder()
                        .dependencies(managedDependencies)
                        .build())
                .build();
    }

    /**
     * Getter for dependencyCount (required for test access).
     */
    public int getDependencyCount() {
        return dependencyCount;
    }

    /**
     * Main method to run the benchmark.
     */
    public static void main(String[] args) throws RunnerException {
        Options opts = new OptionsBuilder()
                .include(ModelValidationBenchmark.class.getSimpleName())
                .forks(1)
                .build();
        new Runner(opts).run();
    }
}
