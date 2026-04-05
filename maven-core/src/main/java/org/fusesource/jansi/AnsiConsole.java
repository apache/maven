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
 * Compatibility facade exposing the legacy Jansi Console API.
 */
@Deprecated
@SuppressWarnings("unused")
public final class AnsiConsole {

    private AnsiConsole() {
        // no-op
    }

    public static AnsiPrintStream out() {
        return new AnsiPrintStream(org.jline.jansi.AnsiConsole.out());
    }

    public static AnsiPrintStream err() {
        return new AnsiPrintStream(org.jline.jansi.AnsiConsole.err());
    }

    public static void systemInstall() {
        org.jline.jansi.AnsiConsole.systemInstall();
    }

    public static boolean isInstalled() {
        return org.jline.jansi.AnsiConsole.isInstalled();
    }

    public static void systemUninstall() {
        org.jline.jansi.AnsiConsole.systemUninstall();
    }

    public static int getTerminalWidth() {
        return org.jline.jansi.AnsiConsole.getTerminalWidth();
    }
}
