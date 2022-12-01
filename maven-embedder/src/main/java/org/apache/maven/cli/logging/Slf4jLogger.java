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
package org.apache.maven.cli.logging;

import org.codehaus.plexus.logging.Logger;

/**
 * Adapt an SLF4J logger to a Plexus logger, ignoring Plexus logger API parts that are not classical and
 * probably not really used.
 *
 * @author Jason van Zyl
 * @since 3.1.0
 */
public class Slf4jLogger implements Logger {

    private org.slf4j.Logger logger;

    public Slf4jLogger(org.slf4j.Logger logger) {
        this.logger = logger;
    }

    public void debug(String message) {
        logger.debug(message);
    }

    public void debug(String message, Throwable throwable) {
        logger.debug(message, throwable);
    }

    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    public void info(String message) {
        logger.info(message);
    }

    public void info(String message, Throwable throwable) {
        logger.info(message, throwable);
    }

    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    public void warn(String message) {
        logger.warn(message);
    }

    public void warn(String message, Throwable throwable) {
        logger.warn(message, throwable);
    }

    public boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }

    public void error(String message) {
        logger.error(message);
    }

    public void error(String message, Throwable throwable) {
        logger.error(message, throwable);
    }

    public boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }

    public void fatalError(String message) {
        logger.error(message);
    }

    public void fatalError(String message, Throwable throwable) {
        logger.error(message, throwable);
    }

    public boolean isFatalErrorEnabled() {
        return logger.isErrorEnabled();
    }

    /**
     * <b>Warning</b>: ignored (always return <code>0 == Logger.LEVEL_DEBUG</code>).
     */
    public int getThreshold() {
        return 0;
    }

    /**
     * <b>Warning</b>: ignored.
     */
    public void setThreshold(int threshold) {}

    /**
     * <b>Warning</b>: ignored (always return <code>null</code>).
     */
    public Logger getChildLogger(String name) {
        return null;
    }

    public String getName() {
        return logger.getName();
    }
}
