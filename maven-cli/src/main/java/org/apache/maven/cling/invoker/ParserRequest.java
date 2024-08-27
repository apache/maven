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
package org.apache.maven.cling.invoker;

import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Path;

import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.services.MessageBuilderFactory;
import org.apache.maven.jline.JLineMessageBuilderFactory;
import org.slf4j.Logger;

import static java.util.Objects.requireNonNull;

public interface ParserRequest {

    /**
     * Mandatory: Logger to use at very early stages. Defaults to {@link ProtoLogger}.
     */
    @Nonnull
    Logger logger();

    /**
     * Mandatory: MessageBuilderFactory to use. Defaults to {@link JLineMessageBuilderFactory}.
     */
    @Nonnull
    MessageBuilderFactory messageBuilderFactory();

    /**
     * Mandatory: the arguments.
     */
    @Nonnull
    String[] args();

    /**
     * Optional: the current working directory. If not given, is detected.
     */
    @Nullable
    Path cwd();

    /**
     * Optional: the current maven home directory. If not given, is detected.
     */
    @Nullable
    Path mavenHome();

    /**
     * Optional: the user home directory. If not given, is detected.
     */
    @Nullable
    Path userHome();

    /**
     * Optional: the STDIN to use. If not given, {@link System#in} is used.
     */
    @Nullable
    InputStream in();

    /**
     * Optional: the STDOUT to use. If not given, {@link System#out} is used.
     */
    @Nullable
    PrintStream out();

    /**
     * Optional: the STDERR to use. If not given, {@link System#err} is used.
     */
    @Nullable
    PrintStream err();

    @Nonnull
    static Builder builder(String[] args) {
        return new Builder(args);
    }

    class Builder {
        private final String[] args;
        private Logger logger = new ProtoLogger();
        private MessageBuilderFactory messageBuilderFactory = new JLineMessageBuilderFactory();
        private Path cwd;
        private Path mavenHome;
        private Path userHome;
        private InputStream in;
        private PrintStream out;
        private PrintStream err;

        private Builder(String[] args) {
            this.args = requireNonNull(args);
        }

        public Builder logger(Logger logger) {
            this.logger = requireNonNull(logger);
            return this;
        }

        public Builder messageBuilderFactory(MessageBuilderFactory messageBuilderFactory) {
            this.messageBuilderFactory = requireNonNull(messageBuilderFactory);
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

        public Builder in(InputStream in) {
            this.in = in;
            return this;
        }

        public Builder out(PrintStream out) {
            this.out = out;
            if (this.logger instanceof ProtoLogger) {
                this.logger = new ProtoLogger(this.out, this.err);
            }
            return this;
        }

        public Builder err(PrintStream err) {
            this.err = err;
            if (this.logger instanceof ProtoLogger) {
                this.logger = new ProtoLogger(this.out, this.err);
            }
            return this;
        }

        public ParserRequest build() {
            return new ParserRequestImpl(logger, messageBuilderFactory, args, cwd, mavenHome, userHome, in, out, err);
        }

        @SuppressWarnings("ParameterNumber")
        private static class ParserRequestImpl implements ParserRequest {
            private final Logger logger;
            private final MessageBuilderFactory messageBuilderFactory;
            private final String[] args;
            private final Path cwd;
            private final Path mavenHome;
            private final Path userHome;
            private final InputStream in;
            private final PrintStream out;
            private final PrintStream err;

            private ParserRequestImpl(
                    Logger logger,
                    MessageBuilderFactory messageBuilderFactory,
                    String[] args,
                    Path cwd,
                    Path mavenHome,
                    Path userHome,
                    InputStream in,
                    PrintStream out,
                    PrintStream err) {
                this.logger = logger;
                this.messageBuilderFactory = messageBuilderFactory;
                this.args = args;
                this.cwd = cwd;
                this.mavenHome = mavenHome;
                this.userHome = userHome;
                this.in = in;
                this.out = out;
                this.err = err;
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
            public String[] args() {
                return args;
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
            public InputStream in() {
                return in;
            }

            @Override
            public PrintStream out() {
                return out;
            }

            @Override
            public PrintStream err() {
                return err;
            }
        }
    }
}
