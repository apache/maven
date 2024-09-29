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
package org.apache.maven.api.cli;

public interface Logger {

    enum Level {
        DEBUG,
        INFO,
        WARN,
        ERROR
    }

    default void log(Level level, String message) {
        log(level, message, null);
    }

    void log(Level level, String message, Throwable error);

    default void debug(String message) {
        log(Level.DEBUG, message);
    }

    default void debug(String message, Throwable error) {
        log(Level.DEBUG, message, error);
    }

    default void info(String message) {
        log(Level.INFO, message);
    }

    default void info(String message, Throwable error) {
        log(Level.INFO, message, error);
    }

    default void warn(String message) {
        log(Level.WARN, message);
    }

    default void warn(String message, Throwable error) {
        log(Level.WARN, message, error);
    }

    default void error(String message) {
        log(Level.ERROR, message);
    }

    default void error(String message, Throwable error) {
        log(Level.ERROR, message, error);
    }
}
