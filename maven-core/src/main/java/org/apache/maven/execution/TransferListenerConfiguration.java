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
package org.apache.maven.execution;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * The known modes of "transfer listener" logging.
 *
 * @since 4.0.0
 */
public final class TransferListenerConfiguration {

    public enum Mode {
        /**
         * Silent, nothing logged that is transfer related.
         */
        QUIET,

        /**
         * Classic: as you knew Maven 3 since beginning.
         */
        CLASSIC,

        /**
         * Classic "light": similar to CLASSIC but without "Downloading..." lines (only "Downloaded...").
         */
        CLASSIC_LIGHT,

        /**
         * Summary: No in-build log pollution, short summary of repoId/baseUrl/resourceCount at build end.
         */
        SUMMARY
    }

    /**
     * Is the build executing on multiple threads?
     */
    private final boolean parallel;

    /**
     * Should be JAnsi used to color messages?
     */
    private final boolean colored;

    /**
     * Should progress be logged?
     */
    private final boolean progress;

    /**
     * Should logging be verbose?
     */
    private final boolean verbose;

    /**
     * The effective mode.
     */
    private final Mode mode;

    private TransferListenerConfiguration(
            boolean parallel, boolean colored, boolean progress, boolean verbose, Mode mode) {
        this.parallel = parallel;
        this.colored = colored;
        this.progress = progress;
        this.verbose = verbose;
        this.mode = requireNonNull(mode);
    }

    /**
     * Is build using multiple threads?
     */
    public boolean isParallel() {
        return parallel;
    }

    /**
     * Is JAnsi used or not.
     */
    public boolean isColored() {
        return colored;
    }

    /**
     * Should the progress be tracked or not.
     */
    public boolean isProgress() {
        return progress;
    }

    /**
     * Is logging meant to be verbose or not.
     */
    public boolean isVerbose() {
        return verbose;
    }

    /**
     * The mode of logging.
     */
    public Mode getMode() {
        return mode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TransferListenerConfiguration that = (TransferListenerConfiguration) o;
        return parallel == that.parallel
                && colored == that.colored
                && progress == that.progress
                && verbose == that.verbose
                && mode == that.mode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(parallel, colored, progress, verbose, mode);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + "parallel="
                + parallel + ", colored="
                + colored + ", progress="
                + progress + ", verbose="
                + verbose + ", mode="
                + mode + '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private boolean parallel = false;
        private boolean colored = true;

        private boolean progress = true;

        private boolean verbose = false;

        private Mode mode = Mode.CLASSIC;

        public Builder withParallel(boolean parallel) {
            this.parallel = parallel;
            return this;
        }

        public Builder withColored(boolean colored) {
            this.colored = colored;
            return this;
        }

        public Builder withProgress(boolean progress) {
            this.progress = progress;
            return this;
        }

        public Builder withVerbose(boolean verbose) {
            this.verbose = verbose;
            return this;
        }

        public Builder withMode(Mode mode) {
            this.mode = requireNonNull(mode);
            return this;
        }

        public TransferListenerConfiguration build() {
            return new TransferListenerConfiguration(parallel, colored, progress, verbose, mode);
        }
    }
}
