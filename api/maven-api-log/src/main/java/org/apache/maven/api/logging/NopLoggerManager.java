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
package org.apache.maven.api.logging;

/**
 * No-op {@link LoggerManager} used as fallback when no {@link java.util.ServiceLoader}
 * provider is found. All logging is silently discarded.
 */
final class NopLoggerManager implements LoggerManager {

    @Override
    public Logger getLogger(String name) {
        return new NopLogger(name);
    }

    @Override
    public LogSink getLogSink() {
        return null;
    }

    @Override
    public void setLogSink(LogSink sink) {}

    @Override
    public boolean isLevelEnabled(String loggerName, LogLevel level) {
        return false;
    }

    @Override
    public void setRootLoggerLevel(LogLevel level) {}

    @Override
    public void reconfigure() {}

    @Override
    public boolean hasReached(LogLevel level) {
        return false;
    }
}
