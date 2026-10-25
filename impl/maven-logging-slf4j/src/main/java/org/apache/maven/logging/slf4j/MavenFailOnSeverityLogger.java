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

import org.apache.maven.api.logging.LogLevel;
import org.apache.maven.api.logging.LoggerManager;
import org.apache.maven.logging.impl.DefaultLoggerManager;

/**
 * A logger that records severity levels for --fail-on-severity support.
 * Delegates recording to {@link DefaultLoggerManager#record(LogLevel)}.
 */
public class MavenFailOnSeverityLogger extends MavenSimpleLogger {

    MavenFailOnSeverityLogger(String name) {
        super(name);
    }

    @Override
    public void warn(String msg) {
        super.warn(msg);
        record(LogLevel.WARN);
    }

    @Override
    public void warn(String format, Object arg) {
        super.warn(format, arg);
        record(LogLevel.WARN);
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        super.warn(format, arg1, arg2);
        record(LogLevel.WARN);
    }

    @Override
    public void warn(String format, Object... argArray) {
        super.warn(format, argArray);
        record(LogLevel.WARN);
    }

    @Override
    public void warn(String msg, Throwable t) {
        super.warn(msg, t);
        record(LogLevel.WARN);
    }

    @Override
    public void error(String msg) {
        super.error(msg);
        record(LogLevel.ERROR);
    }

    @Override
    public void error(String format, Object arg) {
        super.error(format, arg);
        record(LogLevel.ERROR);
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        super.error(format, arg1, arg2);
        record(LogLevel.ERROR);
    }

    @Override
    public void error(String format, Object... argArray) {
        super.error(format, argArray);
        record(LogLevel.ERROR);
    }

    @Override
    public void error(String msg, Throwable t) {
        super.error(msg, t);
        record(LogLevel.ERROR);
    }

    private static void record(LogLevel level) {
        LoggerManager mgr = LoggerManager.get();
        if (mgr instanceof DefaultLoggerManager dlm) {
            dlm.record(level);
        }
    }
}
