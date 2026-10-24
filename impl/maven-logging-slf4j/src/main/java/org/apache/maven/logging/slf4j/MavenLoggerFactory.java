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
package org.apache.maven.logging.slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.maven.api.logging.LoggerManager;
import org.apache.maven.logging.impl.DefaultLoggerManager;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

/**
 * LogFactory for Maven which creates SLF4J loggers that delegate level checks
 * and severity recording to the shared {@link DefaultLoggerManager}.
 */
public class MavenLoggerFactory implements ILoggerFactory {
    static volatile MavenLoggerFactory instance;

    final DefaultLoggerManager logManager;
    final ConcurrentMap<String, Logger> loggerMap = new ConcurrentHashMap<>();

    public MavenLoggerFactory() {
        instance = this;
        LoggerManager mgr = LoggerManager.get();
        if (mgr instanceof DefaultLoggerManager dlm) {
            logManager = dlm;
        } else {
            logManager = new DefaultLoggerManager();
            LoggerManager.set(logManager);
        }
        logManager.addReconfigurationListener(this::updateExistingLoggers);
        MavenSimpleLogger.lazyInit();
    }

    /**
     * Return an appropriate {@link Logger} instance by name.
     */
    @Override
    public Logger getLogger(String name) {
        return loggerMap.computeIfAbsent(name, this::getNewLoggingInstance);
    }

    protected Logger getNewLoggingInstance(String name) {
        return new MavenFailOnSeverityLogger(name);
    }

    public void reconfigure() {
        // Calling LoggerManager.reconfigure() will fire the reconfigurationListener,
        // which calls updateExistingLoggers() — no need to call it here explicitly.
        LoggerManager.get().reconfigure();
    }

    private void updateExistingLoggers() {
        loggerMap.values().forEach(l -> {
            if (l instanceof MavenSimpleLogger msl) {
                msl.configure(logManager.getDefaultLogLevel());
            }
        });
    }
}
