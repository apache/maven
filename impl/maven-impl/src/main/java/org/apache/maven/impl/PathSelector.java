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
package org.apache.maven.impl;

import java.io.File;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Determines whether a path is selected according to include/exclude patterns.
 * The pathnames used for method parameters will be relative to some base directory
 * and use {@code '/'} as separator, regardless of the hosting operating system.
 *
 * <h2>Syntax</h2>
 * If a pattern contains the {@code ':'} character and the prefix before is longer than 1 character,
 * then that pattern is given verbatim to {@link FileSystem#getPathMatcher(String)}, which interprets
 * the part before {@code ':'} as the syntax (usually {@code "glob"} or {@code "regex"}).
 * If a pattern does not contain the {@code ':'} character, or if the prefix is one character long
 * (interpreted as a Windows drive), then the syntax defaults to a reproduction of the Maven 3 behavior.
 * This is implemented as the {@code "glob"} syntax with the following modifications:
 *
 * <ul>
 *   <li>The platform-specific separator ({@code '\\'} on Windows) is replaced by {@code '/'}.
 *       Note that it means that the backslash cannot be used for escaping characters.</li>
 *   <li>Trailing {@code "/"} is completed as {@code "/**"}.</li>
 *   <li>The {@code "**"} wildcard means "0 or more directories" instead of "1 or more directories".
 *       This is implemented by adding variants of the pattern without the {@code "**"} wildcard.</li>
 * </ul>
 *
 * If above changes are not desired, put an explicit {@code "glob:"} prefix before the patterns.
 * Note that putting such a prefix is recommended anyway for better performances.
 *
 * @author Benjamin Bentmann
 * @author Martin Desruisseaux
 *
 * @see java.nio.file.FileSystem#getPathMatcher(String)
 */
public class PathSelector implements PathMatcher {
    /**
     * Patterns which should be excluded by default, like <abbr>SCM</abbr> files.
     *
     * <p><b>Source:</b> this list is copied from {@code plexus-utils-4.0.2} (released in
     * September 23, 2024), class {@code org.codehaus.plexus.util.AbstractScanner}.</p>
     */
    private static final List<String> DEFAULT_EXCLUDES = List.of(
            // Miscellaneous typical temporary files
            "**/*~",
            "**/#*#",
            "**/.#*",
            "**/%*%",
            "**/._*",

            // CVS
            "**/CVS",
            "**/CVS/**",
            "**/.cvsignore",

            // RCS
            "**/RCS",
            "**/RCS/**",

            // SCCS
            "**/SCCS",
            "**/SCCS/**",

            // Visual SourceSafe
            "**/vssver.scc",

            // MKS
            "**/project.pj",

            // Subversion
            "**/.svn",
            "**/.svn/**",

            // Arch
            "**/.arch-ids",
            "**/.arch-ids/**",

            // Bazaar
            "**/.bzr",
            "**/.bzr/**",

            // SurroundSCM
            "**/.MySCMServerInfo",

            // Mac
            "**/.DS_Store",

            // Serena Dimensions Version 10
            "**/.metadata",
            "**/.metadata/**",

            // Mercurial
            "**/.hg",
            "**/.hg/**",

            // git
            "**/.git",
            "**/.git/**",
            "**/.gitignore",

            // BitKeeper
            "**/BitKeeper",
            "**/BitKeeper/**",
            "**/ChangeSet",
            "**/ChangeSet/**",

            // darcs
            "**/_darcs",
            "**/_darcs/**",
            "**/.darcsrepo",
            "**/.darcsrepo/**",
            "**/-darcs-backup*",
            "**/.darcs-temp-mail");

    /**
     * Maximum number of characters of the prefix before {@code ':'} for handling as a Maven syntax.
     */
    private static final int MAVEN_SYNTAX_THRESHOLD = 1;

    /**
     * A path matcher which accepts all files.
     *
     * @see #simplify()
     */
    private static final PathMatcher INCLUDES_ALL = (path) -> true;

    /**
     * String representation of the normalized include filters.
     * This is kept only for {@link #toString()} implementation.
     */
    private final String[] includePatterns;

    /**
     * String representation of the normalized exclude filters.
     * This is kept only for {@link #toString()} implementation.
     */
    private final String[] excludePatterns;

    /**
     * The matcher for includes. The length of this array is equal to {@link #includePatterns} array length.
     */
    private final PathMatcher[] includes;

    /**
     * The matcher for excludes. The length of this array is equal to {@link #excludePatterns} array length.
     */
    private final PathMatcher[] excludes;

    /**
     * The matcher for all directories to include. This array includes the parents of all those directories,
     * because they need to be accepted before we can walk to the sub-directories.
     * This is an optimization for skipping whole directories when possible.
     */
    private final PathMatcher[] dirIncludes;

    /**
     * The matcher for directories to exclude. This array does <em>not</em> include the parent directories,
     * because they may contain other sub-trees that need to be included.
     * This is an optimization for skipping whole directories when possible.
     */
    private final PathMatcher[] dirExcludes;

    /**
     * The base directory. All files will be relativized to that directory before to be matched.
     */
    private final Path baseDirectory;

    /**
     * Creates a new selector from the given includes and excludes.
     *
     * @param directory the base directory of the files to filter
     * @param includes the patterns of the files to include, or null or empty for including all files
     * @param excludes the patterns of the files to exclude, or null or empty for no exclusion
     * @param useDefaultExcludes whether to augment the excludes with a default set of <abbr>SCM</abbr> patterns
     */
    public PathSelector(
            Path directory, Collection<String> includes, Collection<String> excludes, boolean useDefaultExcludes) {
        includePatterns = normalizePatterns(includes, false);
        excludePatterns = normalizePatterns(addDefaultExcludes(excludes, useDefaultExcludes), true);
        baseDirectory = directory;
        FileSystem system = directory.getFileSystem();
        this.includes = matchers(system, includePatterns);
        this.excludes = matchers(system, excludePatterns);
        dirIncludes = matchers(system, directoryPatterns(includePatterns, false));
        dirExcludes = matchers(system, directoryPatterns(excludePatterns, true));
    }

    /**
     * Returns the given array of excludes, optionally expanded with a default set of excludes.
     *
     * @param excludes the user-specified excludes.
     * @param useDefaultExcludes whether to expand user exclude with the set of default excludes
     * @return the potentially expanded set of excludes to use
     */
    private static Collection<String> addDefaultExcludes(Collection<String> excludes, boolean useDefaultExcludes) {
        if (!useDefaultExcludes) {
            return excludes;
        }
        List<String> defaults = DEFAULT_EXCLUDES;
        if (excludes == null || excludes.isEmpty()) {
            return defaults;
        } else {
            var patterns = new ArrayList<String>(excludes);
            patterns.addAll(DEFAULT_EXCLUDES);
            return patterns;
        }
    }

    /**
     * Whether the given pattern does not specify a syntax, in which case Maven syntax should be used.
     * If the prefix has a length of 1, then it is assumed to be a Windows drive letter rather than a syntax.
     *
     * @param pattern the pattern to test
     * @return whether the patter does not specify a syntax
     */
    private static boolean useMavenSyntax(String pattern) {
        return pattern.indexOf(':') <= MAVEN_SYNTAX_THRESHOLD;
    }

    /**
     * Returns the given array of patterns with path separator normalized to {@code '/'}.
     * Null or empty patterns are ignored, and duplications are removed.
     *
     * @param patterns the patterns to normalize
     * @param excludes whether the patterns are exclude patterns
     * @return normalized patterns without null, empty or duplicated patterns
     */
    private static String[] normalizePatterns(final Collection<String> patterns, final boolean excludes) {
        if (patterns == null || patterns.isEmpty()) {
            return new String[0];
        }
        // TODO: use `LinkedHashSet.newLinkedHashSet(int)` instead with JDK19.
        final var normalized = new LinkedHashSet<String>(patterns.size());
        for (String pattern : patterns) {
            if (pattern != null && !pattern.isEmpty()) {
                final boolean useMavenSyntax = useMavenSyntax(pattern);
                if (useMavenSyntax) {
                    pattern = pattern.replace(File.separatorChar, '/');
                    if (pattern.endsWith("/")) {
                        pattern += "**";
                    }
                    // Following are okay only when "**" means "0 or more directories".
                    while (pattern.endsWith("/**/**")) {
                        pattern = pattern.substring(0, pattern.length() - 3);
                    }
                    while (pattern.startsWith("**/**/")) {
                        pattern = pattern.substring(3);
                    }
                    pattern = pattern.replace("/**/**/", "/**/");
                }
                normalized.add(pattern);
                /*
                 * If the pattern starts or ends with "**", Java GLOB expects a directory level at
                 * that location while Maven seems to consider that "**" can mean "no directory".
                 * Add another pattern for reproducing this effect.
                 */
                if (useMavenSyntax) {
                    addPatternsWithOneDirRemoved(normalized, pattern, 0);
                }
            }
        }
        return simplify(normalized, excludes);
    }

    /**
     * Adds all variants of the given pattern with {@code **} removed.
     * This is used for simulating the Maven behavior where {@code "**} may match zero directory.
     * Tests suggest that we need an explicit GLOB pattern with no {@code "**"} for matching an absence of directory.
     *
     * @param patterns where to add the derived patterns
     * @param pattern  the pattern for which to add derived forms
     * @param end      should be 0 (reserved for recursive invocations of this method)
     */
    private static void addPatternsWithOneDirRemoved(final Set<String> patterns, final String pattern, int end) {
        final int length = pattern.length();
        int start;
        while ((start = pattern.indexOf("**", end)) >= 0) {
            end = start + 2; // 2 is the length of "**".
            if (end < length) {
                if (pattern.charAt(end) != '/') {
                    continue;
                }
                if (start == 0) {
                    end++; // Ommit the leading slash if there is nothing before it.
                }
            }
            if (start > 0) {
                if (pattern.charAt(--start) != '/') {
                    continue;
                }
            }
            String reduced = pattern.substring(0, start) + pattern.substring(end);
            patterns.add(reduced);
            addPatternsWithOneDirRemoved(patterns, reduced, start);
        }
    }

    /**
     * Applies some heuristic rules for simplifying the set of patterns,
     * then returns the patterns as an array.
     *
     * @param patterns the patterns to simplify and return asarray
     * @param excludes whether the patterns are exclude patterns
     * @return the set content as an array, after simplification
     */
    private static String[] simplify(Set<String> patterns, boolean excludes) {
        /*
         * If the "**" pattern is present, it makes all other patterns useless.
         * In the case of include patterns, an empty set means to include everything.
         */
        if (patterns.remove("**")) {
            patterns.clear();
            if (excludes) {
                patterns.add("**");
            }
        }
        return patterns.toArray(String[]::new);
    }

    /**
     * Eventually adds the parent directory of the given patterns, without duplicated values.
     * The patterns given to this method should have been normalized.
     *
     * @param patterns the normalized include or exclude patterns
     * @param excludes whether the patterns are exclude patterns
     * @return pattens of directories to include or exclude
     */
    private static String[] directoryPatterns(final String[] patterns, final boolean excludes) {
        // TODO: use `LinkedHashSet.newLinkedHashSet(int)` instead with JDK19.
        final var directories = new LinkedHashSet<String>(patterns.length);
        for (String pattern : patterns) {
            int s = pattern.indexOf(':');
            if (s <= MAVEN_SYNTAX_THRESHOLD || pattern.startsWith("glob:")) {
                if (excludes) {
                    if (pattern.endsWith("/**")) {
                        directories.add(pattern.substring(0, pattern.length() - 3));
                    }
                } else {
                    if (pattern.regionMatches(++s, "**/", 0, 3)) {
                        s = pattern.indexOf('/', s + 3);
                        if (s < 0) {
                            return new String[0]; // Pattern is "**", so we need to accept everything.
                        }
                        directories.add(pattern.substring(0, s));
                    }
                }
            }
        }
        return simplify(directories, excludes);
    }

    /**
     * Creates the path matchers for the given patterns.
     * If no syntax is specified, the default is {@code glob}.
     */
    private static PathMatcher[] matchers(final FileSystem fs, final String[] patterns) {
        final var matchers = new PathMatcher[patterns.length];
        for (int i = 0; i < patterns.length; i++) {
            String pattern = patterns[i];
            if (useMavenSyntax(pattern)) {
                pattern = "glob:" + pattern;
            }
            matchers[i] = fs.getPathMatcher(pattern);
        }
        return matchers;
    }

    /**
     * {@return a potentially simpler matcher equivalent to this matcher}.
     */
    @SuppressWarnings("checkstyle:MissingSwitchDefault")
    public PathMatcher simplify() {
        if (excludes.length == 0 && dirIncludes.length == 0 && dirExcludes.length == 0) {
            switch (includes.length) {
                case 0:
                    return INCLUDES_ALL;
                case 1:
                    return includes[0];
            }
        }
        return this;
    }

    /**
     * Determines whether a path is selected.
     * This is true if the given file matches an include pattern and no exclude pattern.
     *
     * @param path the pathname to test, must not be {@code null}
     * @return {@code true} if the given path is selected, {@code false} otherwise
     */
    @Override
    public boolean matches(Path path) {
        path = baseDirectory.relativize(path);
        return (includes.length == 0 || isMatched(path, includes))
                && (excludes.length == 0 || !isMatched(path, excludes));
    }

    /**
     * {@return whether the given file matches according to one of the given matchers}.
     */
    private static boolean isMatched(Path path, PathMatcher[] matchers) {
        for (PathMatcher matcher : matchers) {
            if (matcher.matches(path)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines whether a directory could contain selected paths.
     *
     * @param directory the directory pathname to test, must not be {@code null}
     * @return {@code true} if the given directory might contain selected paths, {@code false} if the
     *         directory will definitively not contain selected paths
     */
    public boolean couldHoldSelected(Path directory) {
        if (baseDirectory.equals(directory)) {
            return true;
        }
        directory = baseDirectory.relativize(directory);
        return (dirIncludes.length == 0 || isMatched(directory, dirIncludes))
                && (dirExcludes.length == 0 || !isMatched(directory, dirExcludes));
    }

    /**
     * Appends the elements of the given array in the given buffer.
     * This is a helper method for {@link #toString()} implementations.
     *
     * @param buffer the buffer to add the elements to
     * @param label label identifying the array of elements to add
     * @param patterns the elements to append, or {@code null} if none
     */
    private static void append(StringBuilder buffer, String label, String[] patterns) {
        buffer.append(label).append(": [");
        if (patterns != null) {
            for (int i = 0; i < patterns.length; i++) {
                if (i != 0) {
                    buffer.append(", ");
                }
                buffer.append(patterns[i]);
            }
        }
        buffer.append(']');
    }

    /**
     * {@return a string representation for logging purposes}.
     */
    @Override
    public String toString() {
        var buffer = new StringBuilder();
        append(buffer, "includes", includePatterns);
        append(buffer.append(", "), "excludes", excludePatterns);
        return buffer.toString();
    }
}
