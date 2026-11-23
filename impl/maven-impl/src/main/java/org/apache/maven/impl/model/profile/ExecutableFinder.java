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
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.apache.maven.api.services.model.ProfileActivationContext;

/**
 * OS-aware PATH search used by the {@code executable()} condition function.
 * Explicit paths are checked directly; plain names are searched only in nonempty PATH entries.
 * Maven normalizes Windows environment variable names, including {@code Path}, to {@code env.PATH}.
 * On Windows, quoted PATH directories are supported and suffixes from {@code env.PATHEXT} are
 * tried in order, unless the name already has one of those suffixes. An absent PATHEXT defaults
 * to {@code .COM;.EXE;.BAT;.CMD}; an empty PATHEXT disables suffix probing.
 * Exact files are checked before appending suffixes, preserving explicit-file support.
 * No executable is launched and no implicit current-directory search is performed.
 * <p>
 * {@code env.PATH} is split on the {@code path.separator} system property, falling back to
 * {@link File#pathSeparator} when it is missing or empty.
 * At runtime both values are identical, so the property is only an override point: it allows the unit tests
 * to exercise Windows and POSIX separators independently of the host OS, and copes with environments that
 * explicitly set {@code path.separator}.
 *
 * @since 4.x
 */
class ExecutableFinder {

    private static final String DEFAULT_WINDOWS_EXTENSIONS = ".COM;.EXE;.BAT;.CMD";

    private ExecutableFinder() {}

    /**
     * Checks for a regular executable file without running it.
     *
     * @param name executable name or absolute/relative path
     * @param context current activation context, including environment properties
     * @return whether the executable can be found
     */
    static boolean isExecutableInPath(String name, ProfileActivationContext context) {
        if (name == null || name.isBlank()) {
            return false;
        }
        var windows = isWindows(context);
        var extensions = windows ? windowsExtensions(context) : List.<String>of();

        if (name.contains("/") || name.contains("\\")) {
            return matches(null, name, windows, extensions);
        }

        var pathValue = context.getSystemProperty("env.PATH");
        if (pathValue == null || pathValue.isBlank()) {
            return false;
        }
        var separator = context.getSystemProperty("path.separator");
        // path.separator may be overridden in tests to simulate a non-native host OS;
        // at runtime it always matches File.pathSeparator.
        var pathSeparator = separator == null || separator.isEmpty() ? File.pathSeparator : separator;
        var delimiter =
                ":".equals(pathSeparator) || ";".equals(pathSeparator) ? pathSeparator : Pattern.quote(pathSeparator);
        for (String directory : pathValue.split(delimiter, -1)) {
            if (windows && directory.length() >= 2 && directory.startsWith("\"") && directory.endsWith("\"")) {
                directory = directory.substring(1, directory.length() - 1);
            }
            if (!directory.isBlank() && matches(directory, name, windows, extensions)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> windowsExtensions(ProfileActivationContext context) {
        var value = context.getSystemProperty("env.PATHEXT");
        var extensions = new LinkedHashSet<String>();
        for (var entry : (value == null ? DEFAULT_WINDOWS_EXTENSIONS : value).split(";")) {
            var extension = entry.trim().toLowerCase(Locale.ROOT);
            // PATHEXT entries are suffixes, not paths. Ignore malformed entries.
            if (extension.startsWith(".")
                    && extension.length() > 1
                    && !extension.contains("/")
                    && !extension.contains("\\")
                    && !extension.contains(":")
                    && extension.indexOf('\0') < 0) {
                extensions.add(extension);
            }
        }
        return List.copyOf(extensions);
    }

    private static boolean matches(String directory, String name, boolean windows, List<String> extensions) {
        try {
            var candidate =
                    directory == null ? Path.of(name) : Path.of(directory).resolve(name);
            if (isExecutableFile(candidate, windows)) {
                return true;
            }
            var lowerName = name.toLowerCase(Locale.ROOT);
            if (extensions.stream().anyMatch(lowerName::endsWith)) {
                return false;
            }
            for (var extension : extensions) {
                var withExtension = directory == null
                        ? Path.of(name + extension)
                        : Path.of(directory).resolve(name + extension);
                if (isExecutableFile(withExtension, windows)) {
                    return true;
                }
            }
        } catch (InvalidPathException e) {
            // An invalid PATH entry must not hide executables in subsequent directories.
        }
        return false;
    }

    private static boolean isWindows(ProfileActivationContext context) {
        var osName = context.getSystemProperty("os.name");
        return osName != null && osName.toLowerCase(Locale.ROOT).contains("windows");
    }

    private static boolean isExecutableFile(Path path, boolean windows) {
        // Windows uses PATHEXT rather than the POSIX execute permission bit.
        return Files.isRegularFile(path) && (windows || Files.isExecutable(path));
    }
}
