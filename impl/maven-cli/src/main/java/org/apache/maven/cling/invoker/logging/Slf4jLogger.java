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
package org.apache.maven.cling.invoker.logging;

import org.apache.maven.api.cli.Logger;

import static java.util.Objects.requireNonNull;

/**
 * Proto {@link Logger} that just passes to now functioning {@link org.slf4j.Logger} instance.
 */
public class Slf4jLogger implements Logger {
    private final org.slf4j.Logger logger;

    public Slf4jLogger(org.slf4j.Logger logger) {
        this.logger = requireNonNull(logger, "logger");
    }

    @Override
    public void log(Level level, String message, Throwable error) {
        switch (level) {
            case ERROR -> logger.error(message, error);
            case WARN -> logger.warn(message, error);
            case INFO -> logger.info(message, error);
            case DEBUG -> logger.debug(message, error);
            default -> logger.error("UNKNOWN LEVEL {}: {}", level, message, error);
        }
    }
}
