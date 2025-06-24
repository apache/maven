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
package org.apache.maven.api.cli.logging;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.maven.api.cli.Logger;

import static java.util.Objects.requireNonNull;

/**
 * Early CLI {@link Logger} that simply accumulates log entries until some point a real logger can emit them. This
 * logger is created at start, and it exists while no logging is available yet.
 */
public class AccumulatingLogger implements Logger {
    private final AtomicReference<List<Entry>> entries = new AtomicReference<>(new CopyOnWriteArrayList<>());

    @Override
    public void log(Level level, String message, Throwable error) {
        requireNonNull(level, "level");
        requireNonNull(message, "message");
        entries.get().add(new Entry(level, message, error));
    }

    @Override
    public List<Entry> drain() {
        return entries.getAndSet(new CopyOnWriteArrayList<>());
    }
}
