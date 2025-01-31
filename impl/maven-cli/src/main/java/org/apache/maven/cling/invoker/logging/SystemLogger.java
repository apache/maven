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

import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.cli.Logger;

/**
 * System {@link Logger}. Uses provided {@link PrintStream}s or {@link System} ones as fallback.
 * Supports only two levels: ERROR and WARNING, that is emitted to STDERR and STDOUT.
 */
public class SystemLogger implements Logger {

    private final PrintWriter out;
    private final PrintWriter err;

    public SystemLogger() {
        this(null, null);
    }

    public SystemLogger(@Nullable OutputStream out, @Nullable OutputStream err) {
        this.out = new PrintWriter(toPsOrDef(out, System.out), true);
        this.err = new PrintWriter(toPsOrDef(err, System.err), true);
    }

    private PrintStream toPsOrDef(OutputStream outputStream, PrintStream def) {
        if (outputStream == null) {
            return def;
        }
        if (outputStream instanceof PrintStream ps) {
            return ps;
        }
        return new PrintStream(outputStream);
    }

    //
    // These are the only methods we need in our primordial logger
    //

    @Override
    public void log(Level level, String message, Throwable error) {
        PrintWriter pw = level == Level.ERROR ? err : level == Level.WARN ? out : null;
        if (pw != null) {
            pw.println("[" + level.name() + "] " + message);
            if (error != null) {
                error.printStackTrace(pw);
            }
        }
    }
}
