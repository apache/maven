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
import org.apache.maven.api.logging.LogEventBuilder;

import static java.util.Objects.requireNonNull;

/**
 * CLI {@link Logger} that delegates to a {@link org.apache.maven.api.logging.Logger}
 * obtained from {@link LoggerManager}. Replaces the SLF4J-based implementation
 * used after the early bootstrap drain.
 */
@SuppressWarnings("deprecation")
public class LogManagerLogger implements Logger {
    private final org.apache.maven.api.logging.Logger logger;

    public LogManagerLogger(String name) {
        this.logger = org.apache.maven.api.logging.Logger.of(requireNonNull(name, "name"));
    }

    @Override
    public void log(Level level, String message, Throwable error) {
        requireNonNull(level, "level");
        requireNonNull(message, "message");
        LogEventBuilder builder =
                switch (level) {
                    case ERROR -> logger.atError();
                    case WARN -> logger.atWarn();
                    case INFO -> logger.atInfo();
                    case DEBUG -> logger.atDebug();
                };
        if (error != null) {
            builder.withThrowable(error);
        }
        builder.log(message);
    }
}
