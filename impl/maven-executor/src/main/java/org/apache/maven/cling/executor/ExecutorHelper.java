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
package org.apache.maven.cling.executor;

import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.cli.Executor;
import org.apache.maven.api.cli.ExecutorException;
import org.apache.maven.api.cli.ExecutorRequest;

/**
 * Helper class for routing Maven execution based on preferences and/or issued execution requests.
 */
public interface ExecutorHelper extends ExecutorTool {
    /**
     * The modes of execution.
     */
    enum Mode {
        /**
         * Automatically decide. For example, presence of {@link ExecutorRequest#environmentVariables()} or
         * {@link ExecutorRequest#jvmArguments()} will result in choosing {@link #FORKED} executor. Otherwise,
         * {@link #EMBEDDED} executor is preferred.
         */
        AUTO,
        /**
         * Forces embedded execution. May fail if {@link ExecutorRequest} contains input unsupported by executor.
         */
        EMBEDDED,
        /**
         * Forces forked execution. Always carried out, most isolated and "most correct", but is slow as it uses child process.
         */
        FORKED
    }

    /**
     * Returns the preferred mode of this helper.
     */
    @Nonnull
    Mode getDefaultMode();

    /**
     * Creates pre-populated builder for {@link ExecutorRequest}. Users of helper must use this method to create
     * properly initialized request builder.
     */
    @Nonnull
    ExecutorRequest.Builder executorRequest();

    /**
     * Executes the request with preferred mode executor.
     */
    default int execute(ExecutorRequest executorRequest) throws ExecutorException {
        return execute(getDefaultMode(), executorRequest);
    }

    /**
     * Executes the request with passed in mode executor.
     */
    int execute(Mode mode, ExecutorRequest executorRequest) throws ExecutorException;

    /**
     * High level operation, returns the version of the Maven covered by this helper. This method call caches
     * underlying operation, and is safe to invoke as many times needed.
     *
     * @see Executor#mavenVersion(ExecutorRequest)
     */
    @Nonnull
    String mavenVersion();
}
