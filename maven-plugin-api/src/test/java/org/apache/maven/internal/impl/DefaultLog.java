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
package org.apache.maven.internal.impl;

import java.util.function.Supplier;

import org.apache.maven.api.plugin.Log;
import org.slf4j.Logger;

// to not depend on maven-core we copy it to pass tests
public class DefaultLog implements Log {
    private final Logger logger;

    public DefaultLog(final Logger logger) {
        this.logger = logger;
    }

    // @VisibleForTests
    public Logger getLogger() {
        return logger;
    }

    @Override
    public boolean isDebugEnabled() {
        return false;
    }

    @Override
    public void debug(final CharSequence content) {}

    @Override
    public void debug(final CharSequence content, final Throwable error) {}

    @Override
    public void debug(final Throwable error) {}

    @Override
    public void debug(final Supplier<String> content) {}

    @Override
    public void debug(final Supplier<String> content, final Throwable error) {}

    @Override
    public boolean isInfoEnabled() {
        return false;
    }

    @Override
    public void info(final CharSequence content) {}

    @Override
    public void info(final CharSequence content, final Throwable error) {}

    @Override
    public void info(final Throwable error) {}

    @Override
    public void info(final Supplier<String> content) {}

    @Override
    public void info(final Supplier<String> content, final Throwable error) {}

    @Override
    public boolean isWarnEnabled() {
        return false;
    }

    @Override
    public void warn(final CharSequence content) {}

    @Override
    public void warn(final CharSequence content, final Throwable error) {}

    @Override
    public void warn(final Throwable error) {}

    @Override
    public void warn(final Supplier<String> content) {}

    @Override
    public void warn(final Supplier<String> content, final Throwable error) {}

    @Override
    public boolean isErrorEnabled() {
        return false;
    }

    @Override
    public void error(final CharSequence content) {}

    @Override
    public void error(final CharSequence content, final Throwable error) {}

    @Override
    public void error(final Throwable error) {}

    @Override
    public void error(final Supplier<String> content) {}

    @Override
    public void error(final Supplier<String> content, final Throwable error) {}
}
