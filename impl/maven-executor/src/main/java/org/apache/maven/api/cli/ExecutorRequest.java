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
package org.apache.maven.api.cli;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Immutable;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;

import static java.util.Objects.requireNonNull;

/**
 * Represents a request to execute Maven with command-line arguments.
 * This interface encapsulates all the necessary information needed to execute
 * Maven command with arguments. The arguments were not parsed, they are just passed over
 * to executed tool.
 *
 * @since 4.0.0
 */
@Immutable
@Experimental
public interface ExecutorRequest {
    /**
     * The command to execute, ie "mvn".
     */
    @Nonnull
    String command();

    /**
     * The immutable list of arguments to pass to the command.
     */
    @Nonnull
    List<String> arguments();

    /**
     * Returns the current working directory for the Maven execution.
     * This is typically the directory from which Maven was invoked.
     *
     * @return the current working directory path
     */
    @Nonnull
    Path cwd();

    /**
     * Returns the Maven installation directory.
     * This is usually set by the Maven launcher script using the "maven.home" system property.
     *
     * @return the Maven installation directory path
     */
    @Nonnull
    Path installationDirectory();

    /**
     * Returns the user's home directory.
     * This is typically obtained from the "user.home" system property.
     *
     * @return the user's home directory path
     */
    @Nonnull
    Path userHomeDirectory();

    /**
     * Returns the map of environment variables to set before executing process.
     * This property is used ONLY by executors that spawn a new JVM.
     *
     * @return an Optional containing the map of environment variables, or empty if not specified
     */
    @Nonnull
    Optional<Map<String, String>> environmentVariables();

    /**
     * Returns the list of extra JVM arguments to be passed to the forked process.
     * These arguments allow for customization of the JVM environment in which tool will run.
     * This property is used ONLY by executors that spawn a new JVM.
     *
     * @return an Optional containing the list of extra JVM arguments, or empty if not specified
     */
    @Nonnull
    Optional<List<String>> jvmArguments();

    /**
     * Optional consumer for STD out of the Maven. If given, this consumer will get all output from the std out of
     * Maven. Note: whether consumer gets to consume anything depends on invocation arguments passed in
     * {@link #arguments()}, as if log file is set, not much will go to stdout.
     *
     * @return an Optional containing the stdout consumer, or empty if not specified.
     */
    Optional<OutputStream> stdoutConsumer();

    /**
     * Returns {@link Builder} for this instance.
     */
    @Nonnull
    default Builder toBuilder() {
        return new Builder(
                command(),
                arguments(),
                cwd(),
                installationDirectory(),
                userHomeDirectory(),
                environmentVariables().orElse(null),
                jvmArguments().orElse(null),
                stdoutConsumer().orElse(null));
    }

    /**
     * Returns new empty builder.
     */
    @Nonnull
    static Builder empyBuilder() {
        return new Builder();
    }

    /**
     * Returns new builder pre-set to run Maven. The discovery of maven home is attempted.
     */
    @Nonnull
    static Builder mavenBuilder(@Nullable Path installationDirectory) {
        return new Builder(
                "mvn",
                null,
                getCanonicalPath(Paths.get(System.getProperty("user.dir"))),
                installationDirectory != null ? getCanonicalPath(installationDirectory) : discoverMavenHome(),
                getCanonicalPath(Paths.get(System.getProperty("user.home"))),
                null,
                null,
                null);
    }

    class Builder {
        private String command;
        private List<String> arguments;
        private Path cwd;
        private Path installationDirectory;
        private Path userHomeDirectory;
        private Map<String, String> environmentVariables;
        private List<String> jvmArguments;
        private OutputStream stdoutConsumer;

        private Builder() {}

        @SuppressWarnings("ParameterNumber")
        private Builder(
                String command,
                List<String> arguments,
                Path cwd,
                Path installationDirectory,
                Path userHomeDirectory,
                Map<String, String> environmentVariables,
                List<String> jvmArguments,
                OutputStream stdoutConsumer) {
            this.command = command;
            this.arguments = arguments;
            this.cwd = cwd;
            this.installationDirectory = installationDirectory;
            this.userHomeDirectory = userHomeDirectory;
            this.environmentVariables = environmentVariables;
            this.jvmArguments = jvmArguments;
            this.stdoutConsumer = stdoutConsumer;
        }

        @Nonnull
        public Builder command(String command) {
            this.command = requireNonNull(command, "command");
            return this;
        }

        @Nonnull
        public Builder arguments(List<String> arguments) {
            this.arguments = requireNonNull(arguments, "arguments");
            return this;
        }

        @Nonnull
        public Builder argument(String argument) {
            if (arguments == null) {
                arguments = new ArrayList<>();
            }
            this.arguments.add(requireNonNull(argument, "argument"));
            return this;
        }

        @Nonnull
        public Builder cwd(Path cwd) {
            this.cwd = requireNonNull(cwd, "cwd");
            return this;
        }

        @Nonnull
        public Builder installationDirectory(Path installationDirectory) {
            this.installationDirectory = requireNonNull(installationDirectory, "installationDirectory");
            return this;
        }

        @Nonnull
        public Builder userHomeDirectory(Path userHomeDirectory) {
            this.userHomeDirectory = requireNonNull(userHomeDirectory, "userHomeDirectory");
            return this;
        }

        @Nonnull
        public Builder environmentVariables(Map<String, String> environmentVariables) {
            this.environmentVariables = environmentVariables;
            return this;
        }

        @Nonnull
        public Builder environmentVariable(String key, String value) {
            requireNonNull(key, "env key");
            requireNonNull(value, "env value");
            if (environmentVariables == null) {
                this.environmentVariables = new HashMap<>();
            }
            this.environmentVariables.put(key, value);
            return this;
        }

        @Nonnull
        public Builder jvmArguments(List<String> jvmArguments) {
            this.jvmArguments = jvmArguments;
            return this;
        }

        @Nonnull
        public Builder jvmArgument(String jvmArgument) {
            if (jvmArguments == null) {
                jvmArguments = new ArrayList<>();
            }
            this.jvmArguments.add(requireNonNull(jvmArgument, "jvmArgument"));
            return this;
        }

        @Nonnull
        public Builder stdoutConsumer(OutputStream stdoutConsumer) {
            this.stdoutConsumer = stdoutConsumer;
            return this;
        }

        @Nonnull
        public ExecutorRequest build() {
            return new Impl(
                    command,
                    arguments,
                    cwd,
                    installationDirectory,
                    userHomeDirectory,
                    environmentVariables,
                    jvmArguments,
                    stdoutConsumer);
        }

        private static class Impl implements ExecutorRequest {
            private final String command;
            private final List<String> arguments;
            private final Path cwd;
            private final Path installationDirectory;
            private final Path userHomeDirectory;
            private final Map<String, String> environmentVariables;
            private final List<String> jvmArguments;
            private final OutputStream stdoutConsumer;

            @SuppressWarnings("ParameterNumber")
            private Impl(
                    String command,
                    List<String> arguments,
                    Path cwd,
                    Path installationDirectory,
                    Path userHomeDirectory,
                    Map<String, String> environmentVariables,
                    List<String> jvmArguments,
                    OutputStream stdoutConsumer) {
                this.command = requireNonNull(command);
                this.arguments = arguments == null ? List.of() : List.copyOf(arguments);
                this.cwd = requireNonNull(cwd);
                this.installationDirectory = requireNonNull(installationDirectory);
                this.userHomeDirectory = requireNonNull(userHomeDirectory);
                this.environmentVariables = environmentVariables != null ? Map.copyOf(environmentVariables) : null;
                this.jvmArguments = jvmArguments != null ? List.copyOf(jvmArguments) : null;
                this.stdoutConsumer = stdoutConsumer;
            }

            @Override
            public String command() {
                return command;
            }

            @Override
            public List<String> arguments() {
                return arguments;
            }

            @Override
            public Path cwd() {
                return cwd;
            }

            @Override
            public Path installationDirectory() {
                return installationDirectory;
            }

            @Override
            public Path userHomeDirectory() {
                return userHomeDirectory;
            }

            @Override
            public Optional<Map<String, String>> environmentVariables() {
                return Optional.ofNullable(environmentVariables);
            }

            @Override
            public Optional<List<String>> jvmArguments() {
                return Optional.ofNullable(jvmArguments);
            }

            @Override
            public Optional<OutputStream> stdoutConsumer() {
                return Optional.ofNullable(stdoutConsumer);
            }

            @Override
            public String toString() {
                return "Impl{" + "command='"
                        + command + '\'' + ", arguments="
                        + arguments + ", cwd="
                        + cwd + ", installationDirectory="
                        + installationDirectory + ", userHomeDirectory="
                        + userHomeDirectory + ", environmentVariables="
                        + environmentVariables + ", jvmArguments="
                        + jvmArguments + ", stdoutConsumer="
                        + stdoutConsumer + '}';
            }
        }
    }

    @Nonnull
    static Path discoverMavenHome() {
        String mavenHome = System.getProperty("maven.home");
        if (mavenHome == null) {
            throw new ExecutorException("requires maven.home Java System Property set");
        }
        return getCanonicalPath(Paths.get(mavenHome));
    }

    @Nonnull
    static Path getCanonicalPath(Path path) {
        requireNonNull(path, "path");
        try {
            return path.toRealPath();
        } catch (IOException e) {
            return getCanonicalPath(path.getParent()).resolve(path.getFileName());
        }
    }
}
