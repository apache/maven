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

import org.apache.maven.cli.logging.AnsiTerminal;

/**
 * Does not install anything: Maven never replaces {@code System.out}/{@code System.err}, it only decides whether
 * to emit colors (see {@link org.apache.maven.cli.logging.AnsiTerminal}). Install calls are just counted, so code calling
 * the JAnsi API, such as maven-shared-utils, keeps working without racing Maven for the console.
 *
 * This class comes from Jansi and is provided for backward compatibility with maven-shared-utils;
 * Maven itself no longer ships Jansi.
 */
@Deprecated
public class AnsiConsole {

    private static final AnsiPrintStream OUT = new AnsiPrintStream();

    private static int installed;

    private AnsiConsole() {}

    public static synchronized void systemInstall() {
        installed++;
    }

    public static synchronized boolean isInstalled() {
        return installed > 0;
    }

    public static synchronized void systemUninstall() {
        if (installed > 0) {
            installed--;
        }
    }

    public static AnsiPrintStream out() {
        return OUT;
    }

    public static int getTerminalWidth() {
        return AnsiTerminal.getWidth();
    }
}
