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

import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.logging.ProjectBuildLogAppender;
import org.codehaus.plexus.logging.Logger;

/**
 * Adapt an SLF4J logger to a Plexus logger, ignoring Plexus logger API parts that are not classical and
 * probably not really used.
 *
 * @since 3.1.0
 */
public class Slf4jLogger implements Logger {

    private final org.slf4j.Logger logger;
    private final String projectId;

    public Slf4jLogger(org.slf4j.Logger logger) {
        this.logger = logger;
        this.projectId = ProjectBuildLogAppender.getProjectId();
    }

    @Override
    public void debug(String message) {
        setMdc();
        logger.debug(message);
    }

    @Override
    public void debug(String message, Throwable throwable) {
        setMdc();
        logger.debug(message, throwable);
    }

    @Override
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    @Override
    public void info(String message) {
        setMdc();
        logger.info(message);
    }

    @Override
    public void info(String message, Throwable throwable) {
        setMdc();
        logger.info(message, throwable);
    }

    @Override
    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    @Override
    public void warn(String message) {
        setMdc();
        logger.warn(message);
    }

    @Override
    public void warn(String message, Throwable throwable) {
        setMdc();
        logger.warn(message, throwable);
    }

    @Override
    public boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }

    @Override
    public void error(String message) {
        setMdc();
        logger.error(message);
    }

    @Override
    public void error(String message, Throwable throwable) {
        setMdc();
        logger.error(message, throwable);
    }

    @Override
    public boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }

    @Override
    public void fatalError(String message) {
        setMdc();
        logger.error(message);
    }

    @Override
    public void fatalError(String message, Throwable throwable) {
        setMdc();
        logger.error(message, throwable);
    }

    @Override
    public boolean isFatalErrorEnabled() {
        return logger.isErrorEnabled();
    }

    /**
     * <b>Warning</b>: ignored (always return <code>0 == Logger.LEVEL_DEBUG</code>).
     */
    @Override
    public int getThreshold() {
        return 0;
    }

    /**
     * <b>Warning</b>: ignored.
     */
    @Override
    public void setThreshold(int threshold) {}

    /**
     * <b>Warning</b>: ignored (always return <code>null</code>).
     */
    @Override
    @Nullable
    public Logger getChildLogger(String name) {
        return null;
    }

    @Override
    public String getName() {
        return logger.getName();
    }

    private void setMdc() {
        if (projectId != null && ProjectBuildLogAppender.getProjectId() == null) {
            ProjectBuildLogAppender.setProjectId(projectId);
        }
    }
}
