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
package org.apache.maven.plugin.logging;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Logger with "standard" output and error output stream.
 *
 *
 * @deprecated Use SLF4J directly
 */
@Deprecated
public class SystemStreamLog implements Log {
    /**
     * @see Log#debug(CharSequence)
     */
    public void debug(CharSequence content) {
        print("debug", content);
    }

    /**
     * @see Log#debug(CharSequence, Throwable)
     */
    public void debug(CharSequence content, Throwable error) {
        print("debug", content, error);
    }

    /**
     * @see Log#debug(Throwable)
     */
    public void debug(Throwable error) {
        print("debug", error);
    }

    /**
     * @see Log#info(CharSequence)
     */
    public void info(CharSequence content) {
        print("info", content);
    }

    /**
     * @see Log#info(CharSequence, Throwable)
     */
    public void info(CharSequence content, Throwable error) {
        print("info", content, error);
    }

    /**
     * @see Log#info(Throwable)
     */
    public void info(Throwable error) {
        print("info", error);
    }

    /**
     * @see Log#warn(CharSequence)
     */
    public void warn(CharSequence content) {
        print("warn", content);
    }

    /**
     * @see Log#warn(CharSequence, Throwable)
     */
    public void warn(CharSequence content, Throwable error) {
        print("warn", content, error);
    }

    /**
     * @see Log#warn(Throwable)
     */
    public void warn(Throwable error) {
        print("warn", error);
    }

    /**
     * @see Log#error(CharSequence)
     */
    public void error(CharSequence content) {
        System.err.println("[error] " + content.toString());
    }

    /**
     * @see Log#error(CharSequence, Throwable)
     */
    public void error(CharSequence content, Throwable error) {
        StringWriter sWriter = new StringWriter();
        PrintWriter pWriter = new PrintWriter(sWriter);

        error.printStackTrace(pWriter);

        System.err.println("[error] " + content + System.lineSeparator() + System.lineSeparator() + sWriter);
    }

    /**
     * @see Log#error(Throwable)
     */
    public void error(Throwable error) {
        StringWriter sWriter = new StringWriter();
        PrintWriter pWriter = new PrintWriter(sWriter);

        error.printStackTrace(pWriter);

        System.err.println("[error] " + sWriter);
    }

    /**
     * @see Log#isDebugEnabled()
     */
    public boolean isDebugEnabled() {
        // TODO Not sure how best to set these for this implementation...
        return false;
    }

    /**
     * @see Log#isInfoEnabled()
     */
    public boolean isInfoEnabled() {
        return true;
    }

    /**
     * @see Log#isWarnEnabled()
     */
    public boolean isWarnEnabled() {
        return true;
    }

    /**
     * @see Log#isErrorEnabled()
     */
    public boolean isErrorEnabled() {
        return true;
    }

    private void print(String prefix, CharSequence content) {
        System.out.println("[" + prefix + "] " + content.toString());
    }

    private void print(String prefix, Throwable error) {
        StringWriter sWriter = new StringWriter();
        PrintWriter pWriter = new PrintWriter(sWriter);

        error.printStackTrace(pWriter);

        System.out.println("[" + prefix + "] " + sWriter);
    }

    private void print(String prefix, CharSequence content, Throwable error) {
        StringWriter sWriter = new StringWriter();
        PrintWriter pWriter = new PrintWriter(sWriter);

        error.printStackTrace(pWriter);

        System.out.println("[" + prefix + "] " + content + System.lineSeparator() + System.lineSeparator() + sWriter);
    }
}
