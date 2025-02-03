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

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Immutable;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.cli.logging.AccumulatingLogger;
import org.apache.maven.api.services.Lookup;
import org.apache.maven.api.services.LookupException;
import org.apache.maven.api.services.MessageBuilderFactory;

import static java.util.Objects.requireNonNull;

/**
 * Represents a request to parse Maven command-line arguments.
 * This interface encapsulates all the necessary information needed to parse
 * Maven commands and arguments into an {@link InvokerRequest}.
 *
 * @since 4.0.0
 */
@Immutable
@Experimental
public interface ParserRequest {
    /**
     * Returns the Maven command to be executed. This command is used in some invokers (ie forked) but also to
     * present help to user.
     *
     * @return the command string
     */
    @Nonnull
    String command();

    /**
     * Returns the Maven command name (ie "Maven"). This string is used in some invokers to complete error messages.
     *
     * @return the command (human) name
     */
    @Nonnull
    String commandName();

    /**
     * Returns the logger to be used during the parsing process.
     *
     * @return the logger instance
     */
    @Nonnull
    Logger logger();

    /**
     * Returns the factory for creating message builders.
     *
     * @return the message builder factory
     */
    @Nonnull
    MessageBuilderFactory messageBuilderFactory();

    /**
     * Returns the command-line arguments to be parsed.
     *
     * @return a list of argument strings
     */
    @Nonnull
    List<String> args();

    /**
     * Per-request {@link Lookup} for customization.
     *
     * @return a lookup possibly with custom components
     */
    @Nonnull
    Lookup lookup();

    /**
     * Returns the current working directory for the Maven execution.
     * If not explicitly set, this value will be detected during parsing.
     *
     * @return the current working directory path, or null if not set
     */
    @Nullable
    Path cwd();

    /**
     * Returns the Maven home directory.
     * If not explicitly set, this value will be detected during parsing.
     *
     * @return the Maven home directory path, or null if not set
     */
    @Nullable
    Path mavenHome();

    /**
     * Returns the user's home directory.
     * If not explicitly set, this value will be detected during parsing.
     *
     * @return the user's home directory path, or null if not set
     */
    @Nullable
    Path userHome();

    /**
     * Returns the input stream to be used for the Maven execution.
     * If not set, {@link System#in} will be used by default.
     *
     * @return the input stream, or null if not set
     */
    @Nullable
    InputStream stdIn();

    /**
     * Returns the output stream to be used for the Maven execution.
     * If not set, {@link System#out} will be used by default.
     *
     * @return the output stream, or null if not set
     */
    @Nullable
    OutputStream stdOut();

    /**
     * Returns the error stream to be used for the Maven execution.
     * If not set, {@link System#err} will be used by default.
     *
     * @return the error stream, or null if not set
     */
    @Nullable
    OutputStream stdErr();

    /**
     * Returns {@code true} if this call happens in "embedded" mode, for example by another application that
     * embeds Maven. When running in "embedded" mode, Maven will not try to grab system terminal and will use
     * provided {@link #stdIn()} or {@link InputStream#nullInputStream()} as standard in stream.
     */
    boolean embedded();

    /**
     * Creates a new Builder instance for constructing a Maven ParserRequest.
     *
     * @param args the command-line arguments
     * @param messageBuilderFactory the factory for creating message builders
     * @return a new Builder instance
     */
    @Nonnull
    static Builder mvn(@Nonnull String[] args, @Nonnull MessageBuilderFactory messageBuilderFactory) {
        return mvn(Arrays.asList(args), messageBuilderFactory);
    }

    /**
     * Creates a new Builder instance for constructing a Maven ParserRequest.
     *
     * @param args the command-line arguments
     * @param messageBuilderFactory the factory for creating message builders
     * @return a new Builder instance
     */
    @Nonnull
    static Builder mvn(@Nonnull List<String> args, @Nonnull MessageBuilderFactory messageBuilderFactory) {
        return builder(Tools.MVN_CMD, Tools.MVN_NAME, args, messageBuilderFactory);
    }

    /**
     * Creates a new Builder instance for constructing a Maven Encrypting Tool ParserRequest.
     *
     * @param args the command-line arguments
     * @param messageBuilderFactory the factory for creating message builders
     * @return a new Builder instance
     */
    @Nonnull
    static Builder mvnenc(@Nonnull String[] args, @Nonnull MessageBuilderFactory messageBuilderFactory) {
        return mvnenc(Arrays.asList(args), messageBuilderFactory);
    }

    /**
     * Creates a new Builder instance for constructing a Maven Encrypting Tool ParserRequest.
     *
     * @param args the command-line arguments
     * @param messageBuilderFactory the factory for creating message builders
     * @return a new Builder instance
     */
    @Nonnull
    static Builder mvnenc(@Nonnull List<String> args, @Nonnull MessageBuilderFactory messageBuilderFactory) {
        return builder(Tools.MVNENC_CMD, Tools.MVNENC_NAME, args, messageBuilderFactory);
    }

    /**
     * Creates a new Builder instance for constructing a Maven Shell Tool ParserRequest.
     *
     * @param args the command-line arguments
     * @param messageBuilderFactory the factory for creating message builders
     * @return a new Builder instance
     */
    @Nonnull
    static Builder mvnsh(@Nonnull String[] args, @Nonnull MessageBuilderFactory messageBuilderFactory) {
        return mvnsh(Arrays.asList(args), messageBuilderFactory);
    }

    /**
     * Creates a new Builder instance for constructing a Maven Shell Tool ParserRequest.
     *
     * @param args the command-line arguments
     * @param messageBuilderFactory the factory for creating message builders
     * @return a new Builder instance
     */
    @Nonnull
    static Builder mvnsh(@Nonnull List<String> args, @Nonnull MessageBuilderFactory messageBuilderFactory) {
        return builder(Tools.MVNSHELL_CMD, Tools.MVNSHELL_NAME, args, messageBuilderFactory);
    }

    /**
     * Creates a new Builder instance for constructing a ParserRequest.
     *
     * @param command the Maven command to be executed
     * @param commandName the Maven command Name to be executed
     * @param args the command-line arguments
     * @param messageBuilderFactory the factory for creating message builders
     * @return a new Builder instance
     */
    @Nonnull
    static Builder builder(
            @Nonnull String command,
            @Nonnull String commandName,
            @Nonnull List<String> args,
            @Nonnull MessageBuilderFactory messageBuilderFactory) {
        return new Builder(command, commandName, args, messageBuilderFactory);
    }

    class Builder {
        private final String command;
        private final String commandName;
        private final List<String> args;
        private final MessageBuilderFactory messageBuilderFactory;

        private final Logger logger;
        private Lookup lookup = EMPTY_LOOKUP;
        private Path cwd;
        private Path mavenHome;
        private Path userHome;
        private InputStream stdIn;
        private OutputStream stdOut;
        private OutputStream stdErr;
        private boolean embedded = false;

        private Builder(
                String command, String commandName, List<String> args, MessageBuilderFactory messageBuilderFactory) {
            this.command = requireNonNull(command, "command");
            this.commandName = requireNonNull(commandName, "commandName");
            this.args = requireNonNull(args, "args");
            this.messageBuilderFactory = requireNonNull(messageBuilderFactory, "messageBuilderFactory");
            this.logger = new AccumulatingLogger();
        }

        public Builder lookup(@Nonnull Lookup lookup) {
            this.lookup = requireNonNull(lookup);
            return this;
        }

        public Builder cwd(Path cwd) {
            this.cwd = cwd;
            return this;
        }

        public Builder mavenHome(Path mavenHome) {
            this.mavenHome = mavenHome;
            return this;
        }

        public Builder userHome(Path userHome) {
            this.userHome = userHome;
            return this;
        }

        public Builder stdIn(InputStream stdIn) {
            this.stdIn = stdIn;
            return this;
        }

        public Builder stdOut(OutputStream stdOut) {
            this.stdOut = stdOut;
            return this;
        }

        public Builder stdErr(OutputStream stdErr) {
            this.stdErr = stdErr;
            return this;
        }

        public Builder embedded(boolean embedded) {
            this.embedded = embedded;
            return this;
        }

        public ParserRequest build() {
            return new ParserRequestImpl(
                    command,
                    commandName,
                    List.copyOf(args),
                    logger,
                    messageBuilderFactory,
                    lookup,
                    cwd,
                    mavenHome,
                    userHome,
                    stdIn,
                    stdOut,
                    stdErr,
                    embedded);
        }

        @SuppressWarnings("ParameterNumber")
        private static class ParserRequestImpl implements ParserRequest {
            private final String command;
            private final String commandName;
            private final Logger logger;
            private final MessageBuilderFactory messageBuilderFactory;
            private final List<String> args;
            private final Lookup lookup;
            private final Path cwd;
            private final Path mavenHome;
            private final Path userHome;
            private final InputStream stdIn;
            private final OutputStream stdOut;
            private final OutputStream stdErr;
            private final boolean embedded;

            private ParserRequestImpl(
                    String command,
                    String commandName,
                    List<String> args,
                    Logger logger,
                    MessageBuilderFactory messageBuilderFactory,
                    Lookup lookup,
                    Path cwd,
                    Path mavenHome,
                    Path userHome,
                    InputStream stdIn,
                    OutputStream stdOut,
                    OutputStream stdErr,
                    boolean embedded) {
                this.command = requireNonNull(command, "command");
                this.commandName = requireNonNull(commandName, "commandName");
                this.args = List.copyOf(requireNonNull(args, "args"));
                this.logger = requireNonNull(logger, "logger");
                this.messageBuilderFactory = requireNonNull(messageBuilderFactory, "messageBuilderFactory");
                this.lookup = requireNonNull(lookup, "lookup");
                this.cwd = cwd;
                this.mavenHome = mavenHome;
                this.userHome = userHome;
                this.stdIn = stdIn;
                this.stdOut = stdOut;
                this.stdErr = stdErr;
                this.embedded = embedded;
            }

            @Override
            public String command() {
                return command;
            }

            @Override
            public String commandName() {
                return commandName;
            }

            @Override
            public Logger logger() {
                return logger;
            }

            @Override
            public MessageBuilderFactory messageBuilderFactory() {
                return messageBuilderFactory;
            }

            @Override
            public List<String> args() {
                return args;
            }

            @Override
            public Lookup lookup() {
                return lookup;
            }

            @Override
            public Path cwd() {
                return cwd;
            }

            @Override
            public Path mavenHome() {
                return mavenHome;
            }

            @Override
            public Path userHome() {
                return userHome;
            }

            @Override
            public InputStream stdIn() {
                return stdIn;
            }

            @Override
            public OutputStream stdOut() {
                return stdOut;
            }

            @Override
            public OutputStream stdErr() {
                return stdErr;
            }

            @Override
            public boolean embedded() {
                return embedded;
            }
        }

        private static final Lookup EMPTY_LOOKUP = new Lookup() {
            @Override
            public <T> T lookup(Class<T> type) {
                throw new LookupException("empty lookup");
            }

            @Override
            public <T> T lookup(Class<T> type, String name) {
                throw new LookupException("empty lookup");
            }

            @Override
            public <T> Optional<T> lookupOptional(Class<T> type) {
                return Optional.empty();
            }

            @Override
            public <T> Optional<T> lookupOptional(Class<T> type, String name) {
                return Optional.empty();
            }

            @Override
            public <T> List<T> lookupList(Class<T> type) {
                return List.of();
            }

            @Override
            public <T> Map<String, T> lookupMap(Class<T> type) {
                return Map.of();
            }
        };
    }
}
