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
package org.apache.maven.logging;

import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.function.Consumer;

public class LoggingOutputStream extends FilterOutputStream {

    static final byte[] LINE_SEP = System.lineSeparator().getBytes();

    final EolBaos buf;
    final Consumer<String> consumer;

    public LoggingOutputStream(Consumer<String> consumer) {
        this(new EolBaos(), consumer);
    }

    LoggingOutputStream(EolBaos out, Consumer<String> consumer) {
        super(out);
        this.buf = out;
        this.consumer = consumer;
    }

    public PrintStream printStream() {
        return new LoggingPrintStream(this);
    }

    @Override
    public void write(int b) throws IOException {
        super.write(b);
        if (buf.isEol()) {
            String line = new String(buf.toByteArray(), 0, buf.size() - LINE_SEP.length);
            ProjectBuildLogAppender.updateMdc();
            consumer.accept(line);
            buf.reset();
        }
    }

    @Override
    public void flush() throws IOException {
        forceFlush();
    }

    public void forceFlush() {
        if (buf.size() > 0) {
            String line = new String(buf.toByteArray(), 0, buf.size());
            ProjectBuildLogAppender.updateMdc();
            consumer.accept(line);
            buf.reset();
        }
    }

    static class EolBaos extends ByteArrayOutputStream {
        boolean isEol() {
            if (count >= LINE_SEP.length) {
                for (int i = 0; i < LINE_SEP.length; i++) {
                    if (buf[count - LINE_SEP.length + i] != LINE_SEP[i]) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }
    }

    public static class LoggingPrintStream extends PrintStream {
        public LoggingPrintStream(LoggingOutputStream out) {
            super(out, true);
        }

        public void forceFlush() {
            ((LoggingOutputStream) out).forceFlush();
        }
    }

    public static void forceFlush(PrintStream ps) {
        if (ps instanceof LoggingPrintStream loggingPrintStream) {
            loggingPrintStream.forceFlush();
        }
    }
}
