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
package org.apache.maven.cling.invoker;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts the paths MSYS2 and Git Bash hand over in their POSIX style, like {@code /c/Users/alice}, to native Windows
 * paths, like {@code C:\Users\alice}.
 */
public final class MsysPaths {

    /** A drive letter, the only single letter directory right below the root of such a path. */
    private static final Pattern DRIVE_PATH = Pattern.compile("^/([a-zA-Z])/(.*)$");

    private MsysPaths() {}

    /**
     * Converts the path on Windows, and returns it as it is anywhere else.
     *
     * @param path the path, may be {@code null}
     * @return the native path, or {@code null} if the path is {@code null}
     */
    public static String toNativePath(String path) {
        return toNativePath(path, isWindows());
    }

    /**
     * Converts the path only when {@code windows} is {@code true}. The flag lets the tests exercise the conversion on
     * any operating system.
     *
     * @param path the path, may be {@code null}
     * @param windows whether the conversion applies
     * @return the native path, or {@code null} if the path is {@code null}
     */
    public static String toNativePath(String path, boolean windows) {
        if (path == null || !windows) {
            return path;
        }
        Matcher matcher = DRIVE_PATH.matcher(path);
        if (matcher.matches()) {
            return matcher.group(1).toUpperCase(Locale.ROOT) + ":\\"
                    + matcher.group(2).replace('/', '\\');
        }
        return path;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("windows");
    }
}
