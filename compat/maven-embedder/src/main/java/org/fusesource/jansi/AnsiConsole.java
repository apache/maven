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

/**
 * Forwards to {@link org.jline.jansi.AnsiConsole} instead of performing its own install, so that code
 * calling the JAnsi API, such as maven-shared-utils, shares the single console Maven itself installs
 * through {@code org.apache.maven.jline.MessageUtils} rather than racing it for ownership of
 * {@code System.out}/{@code System.err}.
 *
 * This class comes from Jansi and is provided for backward compatibility with maven-shared-utils,
 * while Maven has migrated to JLine (into which Jansi has been merged since JLine 3.25.0).
 */
@Deprecated
public class AnsiConsole {

    private static final AnsiPrintStream OUT = new AnsiPrintStream();

    private AnsiConsole() {}

    public static synchronized void systemInstall() {
        org.jline.jansi.AnsiConsole.systemInstall();
    }

    public static synchronized boolean isInstalled() {
        return org.jline.jansi.AnsiConsole.isInstalled();
    }

    public static synchronized void systemUninstall() {
        org.jline.jansi.AnsiConsole.systemUninstall();
    }

    public static AnsiPrintStream out() {
        return OUT;
    }

    public static int getTerminalWidth() {
        return org.jline.jansi.AnsiConsole.getTerminalWidth();
    }
}
