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
package org.apache.maven.impl.model.profile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import org.apache.maven.api.services.model.ProfileActivationContext;

/**
 * Helper that implements the OS-aware PATH search used by the {@code executable()} condition function.
 *
 * <p>The search strategy is:
 * <ol>
 *   <li>If {@code name} contains a path separator (i.e. it already looks like a path), treat it as
 *       an absolute or relative file path and check it directly.</li>
 *   <li>Otherwise, retrieve the {@code PATH} value from the activation context's system properties
 *       (Maven normalises env vars to {@code env.PATH} / {@code env.Path} etc.) and split it by the
 *       platform path separator.  Each directory is searched in order.</li>
 *   <li>On Windows, when the candidate does not already have one of the known executable extensions
 *       ({@code .exe}, {@code .cmd}, {@code .bat}, {@code .com}), those extensions are appended and
 *       tried as well.</li>
 * </ol>
 *
 * @since 4.x
 */
class ExecutableFinder {

    /** Windows-specific executable file extensions, in search order. */
    private static final String[] WINDOWS_EXTENSIONS = {".exe", ".cmd", ".bat", ".com"};

    /** The system property key under which Maven exposes the {@code PATH} environment variable. */
    private static final String ENV_PATH_KEY = "env.PATH";

    private ExecutableFinder() {}

    /**
     * Returns {@code true} when {@code name} resolves to an executable file.
     *
     * @param name    the executable name (e.g. {@code "musl-gcc"}) or an absolute/relative path
     * @param context the current profile activation context
     * @return {@code true} if the executable is found and is a regular, executable file
     */
    static boolean isExecutableInPath(String name, ProfileActivationContext context) {
        boolean isWindows = isWindows(context);

        // If the name already contains a path separator treat it as a direct path.
        if (name.contains("/") || name.contains(File.separator)) {
            Path candidate = Path.of(name);
            if (isExecutableFile(candidate, isWindows)) {
                return true;
            }
            // On Windows also try known executable extensions for direct paths.
            if (isWindows && !hasWindowsExtension(name)) {
                for (String ext : WINDOWS_EXTENSIONS) {
                    if (isExecutableFile(Path.of(name + ext), isWindows)) {
                        return true;
                    }
                }
            }
            return false;
        }

        // --- plain name: search PATH ---
        String pathValue = getPathValue(context);
        if (pathValue == null || pathValue.isBlank()) {
            return false;
        }

        String[] dirs = pathValue.split(File.pathSeparator, -1);
        for (String dir : dirs) {
            if (dir.isBlank()) {
                continue;
            }
            Path base = Path.of(dir).resolve(name);
            if (isExecutableFile(base, isWindows)) {
                return true;
            }
            // On Windows also try known executable extensions (unless already present).
            if (isWindows && !hasWindowsExtension(name)) {
                for (String ext : WINDOWS_EXTENSIONS) {
                    Path withExt = Path.of(dir).resolve(name + ext);
                    if (isExecutableFile(withExt, isWindows)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // -----------------------------------------------------------------------
    // Package-private helpers (visible to tests)
    // -----------------------------------------------------------------------

    /**
     * Retrieves the PATH value from the activation context.
     *
     * <p>Maven places env vars in system properties as {@code env.<NAME>}.
     * On Windows, env var names are normalised to upper-case (e.g. {@code env.PATH}).
     *
     * @param context the profile activation context
     * @return the raw PATH string, or {@code null} if not available
     */
    static String getPathValue(ProfileActivationContext context) {
        return context.getSystemProperty(ENV_PATH_KEY);
    }

    // -----------------------------------------------------------------------
    // Private utilities
    // -----------------------------------------------------------------------

    private static boolean isWindows(ProfileActivationContext context) {
        String osName = context.getSystemProperty("os.name");
        return osName != null && osName.toLowerCase(Locale.ROOT).contains("windows");
    }

    /**
     * Returns {@code true} if {@code path} is a regular file that the JVM considers executable.
     *
     * <p>On Windows, {@link Files#isExecutable(Path)} always returns {@code true} for regular
     * files, so this method simply checks that the file exists and is regular.  The caller is
     * responsible for probing Windows executable extensions ({@code .exe}, {@code .cmd}, etc.)
     * when the user-supplied name does not already carry one.  On Unix/POSIX systems the
     * execute permission bit is checked via {@link Files#isExecutable(Path)}.</p>
     */
    private static boolean isExecutableFile(Path path, boolean isWindows) {
        if (!Files.isRegularFile(path)) {
            return false;
        }
        // On Windows Files.isExecutable() always returns true for regular files.
        // On Unix we rely on the execute permission bit.
        return isWindows || Files.isExecutable(path);
    }

    private static boolean hasWindowsExtension(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        for (String ext : WINDOWS_EXTENSIONS) {
            if (lower.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }
}
