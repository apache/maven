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
package org.fusesource.jansi;

import java.io.OutputStream;
import java.io.PrintStream;

/**
 * Forwards everything written to it to whatever {@code System.out} currently is, which is the stream
 * JLine installed. Unlike real JAnsi, this stream does not decide for itself whether to strip, force or
 * pass through ANSI escapes - that decision belongs to the terminal Maven already set up - so
 * {@link #setMode} is a no-op.
 *
 * This class comes from Jansi and is provided for backward compatibility with maven-shared-utils,
 * while Maven has migrated to JLine (into which Jansi has been merged since JLine 3.25.0).
 */
@Deprecated
public class AnsiPrintStream extends PrintStream {

    AnsiPrintStream() {
        super(
                new OutputStream() {
                    @Override
                    public void write(int b) {
                        System.out.write(b);
                    }

                    @Override
                    public void write(byte[] b, int off, int len) {
                        System.out.write(b, off, len);
                    }

                    @Override
                    public void flush() {
                        System.out.flush();
                    }
                },
                true);
    }

    public AnsiMode getMode() {
        return AnsiMode.Default;
    }

    public void setMode(AnsiMode mode) {
        // no-op: the terminal Maven installed already decides whether ANSI is forced, stripped or kept
    }

    public int getTerminalWidth() {
        return AnsiConsole.getTerminalWidth();
    }
}
