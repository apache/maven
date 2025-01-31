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
package org.apache.maven.cling.invoker.logging;

import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Objects;

import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.cli.Logger;

import static java.util.Objects.requireNonNull;

/**
 * System {@link Logger}. Uses provided {@link PrintStream}s or {@link System#err} ones as fallback.
 * This logger is used in case of "early failures" (when no logging may be set up yet).
 */
public class SystemLogger implements Logger {
    private final PrintWriter out;
    private final Level threshold;

    public SystemLogger() {
        this(null, null);
    }

    public SystemLogger(@Nullable OutputStream out, @Nullable Level threshold) {
        this.out = new PrintWriter(toPsOrDef(out, System.err), true);
        this.threshold = Objects.requireNonNullElse(threshold, Level.INFO);
    }

    private PrintStream toPsOrDef(OutputStream outputStream, PrintStream def) {
        if (outputStream == null) {
            return def;
        }
        if (outputStream instanceof PrintStream ps) {
            return ps;
        }
        return new PrintStream(outputStream, true);
    }

    @Override
    public void log(Level level, String message, Throwable error) {
        requireNonNull(level, "level");
        requireNonNull(message, "message");
        if (level.ordinal() >= threshold.ordinal()) {
            out.println("[" + level.name() + "] " + message);
            if (error != null) {
                error.printStackTrace(out);
            }
        }
    }
}
