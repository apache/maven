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
package org.apache.maven.logging.system;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.apache.maven.api.logging.LogLevel;
import org.apache.maven.api.logging.Logger;
import org.apache.maven.api.logging.LoggerManager;

/**
 * Adapter from {@link System.Logger} to Maven's {@link Logger} API.
 * <p>
 * Level checks and event dispatch are resolved dynamically through
 * {@link LoggerManager#get()} on every call, so instances created
 * during early bootstrap automatically switch to the real logging
 * implementation once it is installed.
 *
 * @since 4.2.0
 */
final class MavenSystemLogger implements System.Logger {

    private final String name;

    MavenSystemLogger(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isLoggable(Level level) {
        LogLevel mapped = mapLevel(level);
        if (mapped == null) {
            return level != Level.OFF;
        }
        return LoggerManager.get().isLevelEnabled(name, mapped);
    }

    @Override
    public void log(Level level, ResourceBundle bundle, String msg, Throwable thrown) {
        LogLevel mapped = mapLevel(level);
        if (mapped == null || !LoggerManager.get().isLevelEnabled(name, mapped)) {
            return;
        }
        String resolved = resolveBundle(bundle, msg);
        LoggerManager.get().getLogger(name).at(mapped).withThrowable(thrown).log(resolved);
    }

    @Override
    public void log(Level level, ResourceBundle bundle, String format, Object... params) {
        LogLevel mapped = mapLevel(level);
        if (mapped == null || !LoggerManager.get().isLevelEnabled(name, mapped)) {
            return;
        }
        String message = formatMessage(bundle, format, params);
        LoggerManager.get().getLogger(name).at(mapped).log(message);
    }

    private static LogLevel mapLevel(Level level) {
        return switch (level) {
            case ALL, TRACE -> LogLevel.TRACE;
            case DEBUG -> LogLevel.DEBUG;
            case INFO -> LogLevel.INFO;
            case WARNING -> LogLevel.WARN;
            case ERROR -> LogLevel.ERROR;
            case OFF -> null;
        };
    }

    private static String resolveBundle(ResourceBundle bundle, String msg) {
        if (bundle != null && msg != null) {
            try {
                return bundle.getString(msg);
            } catch (MissingResourceException ignored) {
            }
        }
        return msg;
    }

    private static String formatMessage(ResourceBundle bundle, String format, Object... params) {
        String msg = resolveBundle(bundle, format);
        if (params != null && params.length > 0 && msg != null) {
            try {
                return MessageFormat.format(msg, params);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return msg;
    }
}
