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

import java.util.concurrent.atomic.AtomicReference;

import org.apache.maven.logging.api.LogLevelRecorder;

/**
 * Responsible for keeping state of whether the threshold of the --fail-on-severity flag has been hit.
 */
public class DefaultLogLevelRecorder implements LogLevelRecorder {
    private Level maxAllowed;
    private final AtomicReference<Level> maxReached = new AtomicReference<>(Level.DEBUG);

    @Override
    public boolean hasReachedMaxLevel() {
        return maxReached.get().ordinal() > maxAllowed.ordinal();
    }

    @Override
    public Level getMaxLevelReached() {
        return maxReached.get();
    }

    @Override
    public Level getMaxLevelAllowed() {
        return maxAllowed;
    }

    @Override
    public void setMaxLevelAllowed(Level level) {
        this.maxAllowed = level;
    }

    @Override
    public void reset() {
        this.maxAllowed = null;
        this.maxReached.set(Level.DEBUG);
    }

    public void record(org.slf4j.event.Level logLevel) {
        Level level =
                switch (logLevel) {
                    case TRACE, DEBUG -> Level.DEBUG;
                    case INFO -> Level.INFO;
                    case WARN -> Level.WARN;
                    case ERROR -> Level.ERROR;
                };
        while (true) {
            Level r = maxReached.get();
            if (level.ordinal() > r.ordinal()) {
                if (!maxReached.compareAndSet(r, level)) {
                    continue;
                }
            }
            break;
        }
    }

    public boolean metThreshold() {
        return maxAllowed != null && maxReached.get().ordinal() >= maxAllowed.ordinal();
    }
}
