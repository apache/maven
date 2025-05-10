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

import java.util.List;

import org.apache.maven.api.services.InterpolatorException;
import org.apache.maven.api.services.ModelBuilderException;
import org.apache.maven.api.services.VersionParser;
import org.apache.maven.api.services.model.ProfileActivationContext;

import static org.apache.maven.impl.model.profile.ConditionParser.toInt;

/**
 * Provides a set of functions for evaluating profile activation conditions.
 * These functions can be used in profile activation expressions to determine
 * whether a profile should be activated based on various criteria.
 */
@SuppressWarnings("unused")
public class ConditionFunctions {
    private final ProfileActivationContext context;
    private final VersionParser versionParser;

    /**
     * Constructs a new ConditionFunctions instance.
     *
     * @param context The profile activation context
     * @param versionParser The version parser for comparing versions
     */
    public ConditionFunctions(ProfileActivationContext context, VersionParser versionParser) {
        this.context = context;
        this.versionParser = versionParser;
    }

    /**
     * Returns the length of the given string.
     *
     * @param args A list containing a single string argument
     * @return The length of the string
     * @throws IllegalArgumentException if the number of arguments is not exactly one
     */
    public Object length(List<Object> args) {
        if (args.size() != 1) {
            throw new IllegalArgumentException("length function requires exactly one argument");
        }
        String s = ConditionParser.toString(args.getFirst());
        return s.length();
    }

    /**
     * Converts the given string to uppercase.
     *
     * @param args A list containing a single string argument
     * @return The uppercase version of the input string
     * @throws IllegalArgumentException if the number of arguments is not exactly one
     */
    public Object upper(List<Object> args) {
        if (args.size() != 1) {
            throw new IllegalArgumentException("upper function requires exactly one argument");
        }
        String s = ConditionParser.toString(args.getFirst());
        return s.toUpperCase();
    }

    /**
     * Converts the given string to lowercase.
     *
     * @param args A list containing a single string argument
     * @return The lowercase version of the input string
     * @throws IllegalArgumentException if the number of arguments is not exactly one
     */
    public Object lower(List<Object> args) {
        if (args.size() != 1) {
            throw new IllegalArgumentException("lower function requires exactly one argument");
        }
        String s = ConditionParser.toString(args.getFirst());
        return s.toLowerCase();
    }

    /**
     * Returns a substring of the given string.
     *
     * @param args A list containing 2 or 3 arguments: the string, start index, and optionally end index
     * @return The substring
     * @throws IllegalArgumentException if the number of arguments is not 2 or 3
     */
    public Object substring(List<Object> args) {
        if (args.size() < 2 || args.size() > 3) {
            throw new IllegalArgumentException("substring function requires 2 or 3 arguments");
        }
        String s = ConditionParser.toString(args.getFirst());
        int start = toInt(args.get(1));
        int end = args.size() == 3 ? toInt(args.get(2)) : s.length();
        return s.substring(start, end);
    }

    /**
     * Finds the index of a substring within a string.
     *
     * @param args A list containing two strings: the main string and the substring to find
     * @return The index of the substring, or -1 if not found
     * @throws IllegalArgumentException if the number of arguments is not exactly two
     */
    public Object indexOf(List<Object> args) {
        if (args.size() != 2) {
            throw new IllegalArgumentException("indexOf function requires exactly two arguments");
        }
        String s = ConditionParser.toString(args.getFirst());
        String substring = ConditionParser.toString(args.get(1));
        return s.indexOf(substring);
    }

    /**
     * Checks if a string contains a given substring.
     *
     * @param args A list containing two strings: the main string and the substring to check
     * @return true if the main string contains the substring, false otherwise
     * @throws IllegalArgumentException if the number of arguments is not exactly two
     */
    public Object contains(List<Object> args) {
        if (args.size() != 2) {
            throw new IllegalArgumentException("contains function requires exactly two arguments");
        }
        String s = ConditionParser.toString(args.getFirst());
        String substring = ConditionParser.toString(args.get(1));
        return s.contains(substring);
    }

    /**
     * Checks if a string matches a given regular expression.
     *
     * @param args A list containing two strings: the string to check and the regex pattern
     * @return true if the string matches the regex, false otherwise
     * @throws IllegalArgumentException if the number of arguments is not exactly two
     */
    public Object matches(List<Object> args) {
        if (args.size() != 2) {
            throw new IllegalArgumentException("matches function requires exactly two arguments");
        }
        String s = ConditionParser.toString(args.getFirst());
        String regex = ConditionParser.toString(args.get(1));
        return s.matches(regex);
    }

    /**
     * Negates a boolean value.
     *
     * @param args A list containing a single boolean argument
     * @return The negation of the input boolean
     * @throws IllegalArgumentException if the number of arguments is not exactly one
     */
    public Object not(List<Object> args) {
        if (args.size() != 1) {
            throw new IllegalArgumentException("not function requires exactly one argument");
        }
        return !ConditionParser.toBoolean(args.getFirst());
    }

    /**
     * Implements an if-then-else operation.
     *
     * @param args A list containing three arguments: condition, value if true, value if false
     * @return The second argument if the condition is true, the third argument otherwise
     * @throws IllegalArgumentException if the number of arguments is not exactly three
     */
    @SuppressWarnings("checkstyle:MethodName")
    public Object if_(List<Object> args) {
        if (args.size() != 3) {
            throw new IllegalArgumentException("if function requires exactly three arguments");
        }
        boolean condition = ConditionParser.toBoolean(args.getFirst());
        return condition ? args.get(1) : args.get(2);
    }

    /**
     * Checks if a file or directory exists at the given path.
     *
     * @param args A list containing a single string argument representing the path
     * @return {@code true} if the file or directory exists, {@code false} otherwise
     * @throws IllegalArgumentException if the number of arguments is not exactly one
     * @throws ModelBuilderException if a problem occurs while walking the file system
     * @throws InterpolatorException if an error occurs during interpolation
     */
    public Object exists(List<Object> args) {
        if (args.size() != 1) {
            throw new IllegalArgumentException("exists function requires exactly one argument");
        }
        String path = ConditionParser.toString(args.getFirst());
        return context.exists(path, true);
    }

    /**
     * Checks if a file or directory is missing at the given path.
     *
     * @param args A list containing a single string argument representing the path
     * @return {@code true} if the file or directory does not exist, {@code false} otherwise
     * @throws IllegalArgumentException if the number of arguments is not exactly one
     * @throws ModelBuilderException if a problem occurs while walking the file system
     * @throws InterpolatorException if an error occurs during interpolation
     */
    public Object missing(List<Object> args) {
        if (args.size() != 1) {
            throw new IllegalArgumentException("missing function requires exactly one argument");
        }
        String path = ConditionParser.toString(args.getFirst());
        return !context.exists(path, true);
    }

    /**
     * Checks if a version is within a specified version range.
     *
     * @param args A list containing two strings: the version to check and the version range
     * @return true if the version is within the range, false otherwise
     * @throws IllegalArgumentException if the number of arguments is not exactly two
     */
    public Object inrange(List<Object> args) {
        if (args.size() != 2) {
            throw new IllegalArgumentException("inrange function requires exactly two arguments");
        }
        String version = ConditionParser.toString(args.getFirst());
        String range = ConditionParser.toString(args.get(1));
        return versionParser.parseVersionRange(range).contains(versionParser.parseVersion(version));
    }
}
