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
package org.apache.maven.cling.logging;

import org.codehaus.plexus.logging.Logger;

/**
 * Adapts a {@link org.apache.maven.api.logging.Logger} to the Plexus
 * {@link Logger} interface for legacy component compatibility.
 */
public class LogApiLogger implements Logger {

    private final org.apache.maven.api.logging.Logger logger;

    public LogApiLogger(org.apache.maven.api.logging.Logger logger) {
        this.logger = logger;
    }

    @Override
    public void debug(String message) {
        logger.debug(message);
    }

    @Override
    public void debug(String message, Throwable throwable) {
        logger.debug(message, throwable);
    }

    @Override
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    @Override
    public void info(String message) {
        logger.info(message);
    }

    @Override
    public void info(String message, Throwable throwable) {
        logger.info(message, throwable);
    }

    @Override
    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    @Override
    public void warn(String message) {
        logger.warn(message);
    }

    @Override
    public void warn(String message, Throwable throwable) {
        logger.warn(message, throwable);
    }

    @Override
    public boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }

    @Override
    public void error(String message) {
        logger.error(message);
    }

    @Override
    public void error(String message, Throwable throwable) {
        logger.error(message, throwable);
    }

    @Override
    public boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }

    @Override
    public void fatalError(String message) {
        logger.error(message);
    }

    @Override
    public void fatalError(String message, Throwable throwable) {
        logger.error(message, throwable);
    }

    @Override
    public boolean isFatalErrorEnabled() {
        return logger.isErrorEnabled();
    }

    @Override
    public int getThreshold() {
        return 0;
    }

    @Override
    public void setThreshold(int threshold) {}

    @Override
    public Logger getChildLogger(String name) {
        return new LogApiLogger(logger.child(name));
    }

    @Override
    public String getName() {
        return logger.name();
    }
}
