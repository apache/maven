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
package org.apache.maven.slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.maven.logging.api.LogLevelRecorder;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

/**
 * LogFactory for Maven which can create a simple logger or one which, if set, fails the build on a severity threshold.
 */
public class MavenLoggerFactory implements LogLevelRecorder, ILoggerFactory {
    final DefaultLogLevelRecorder logLevelRecorder = new DefaultLogLevelRecorder();
    final ConcurrentMap<String, Logger> loggerMap = new ConcurrentHashMap<>();

    public MavenLoggerFactory() {
        MavenSimpleLogger.lazyInit();
    }

    @Override
    public boolean hasReachedMaxLevel() {
        return logLevelRecorder.metThreshold();
    }

    @Override
    public Level getMaxLevelReached() {
        return null;
    }

    @Override
    public Level getMaxLevelAllowed() {
        return null;
    }

    @Override
    public void setMaxLevelAllowed(Level level) {
        this.logLevelRecorder.setMaxLevelAllowed(level);
    }
    /**
     * Return an appropriate {@link Logger} instance by name.
     */
    @Override
    public Logger getLogger(String name) {
        return loggerMap.computeIfAbsent(name, this::getNewLoggingInstance);
    }

    @Override
    public void reset() {
        logLevelRecorder.reset();
    }

    protected Logger getNewLoggingInstance(String name) {
        return new MavenFailOnSeverityLogger(name, logLevelRecorder);
    }

    public void reconfigure() {
        SimpleLoggerConfiguration config = MavenSimpleLogger.CONFIG_PARAMS;
        config.init();
        loggerMap.values().forEach(l -> {
            if (l instanceof MavenSimpleLogger msl) {
                msl.configure(config.defaultLogLevel);
            }
        });
        logLevelRecorder.reset();
    }
}
