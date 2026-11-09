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
package org.apache.maven.api;

/**
 * Configuration constants.
 * <p>
 * Maven 3 carries only the style names of the Maven 4 class, for {@link org.apache.maven.api.services.MessageBuilder}.
 *
 * @since 3.10.1
 */
public final class Constants {

    public static final String MAVEN_STYLE_PREFIX = "maven.style.";

    // Style Names
    public static final String MAVEN_STYLE_TRANSFER_NAME = "transfer";
    public static final String MAVEN_STYLE_TRACE_NAME = "trace";
    public static final String MAVEN_STYLE_DEBUG_NAME = "debug";
    public static final String MAVEN_STYLE_INFO_NAME = "info";
    public static final String MAVEN_STYLE_WARNING_NAME = "warning";
    public static final String MAVEN_STYLE_ERROR_NAME = "error";
    public static final String MAVEN_STYLE_SUCCESS_NAME = "success";
    public static final String MAVEN_STYLE_FAILURE_NAME = "failure";
    public static final String MAVEN_STYLE_STRONG_NAME = "strong";
    public static final String MAVEN_STYLE_MOJO_NAME = "mojo";
    public static final String MAVEN_STYLE_PROJECT_NAME = "project";

    // Default Values
    public static final String MAVEN_STYLE_TRANSFER_DEFAULT = "f:bright-black";
    public static final String MAVEN_STYLE_TRACE_DEFAULT = "bold,f:magenta";
    public static final String MAVEN_STYLE_DEBUG_DEFAULT = "bold,f:cyan";
    public static final String MAVEN_STYLE_INFO_DEFAULT = "bold,f:blue";
    public static final String MAVEN_STYLE_WARNING_DEFAULT = "bold,f:yellow";
    public static final String MAVEN_STYLE_ERROR_DEFAULT = "bold,f:red";
    public static final String MAVEN_STYLE_SUCCESS_DEFAULT = "bold,f:green";
    public static final String MAVEN_STYLE_FAILURE_DEFAULT = "bold,f:red";
    public static final String MAVEN_STYLE_STRONG_DEFAULT = "bold";
    public static final String MAVEN_STYLE_MOJO_DEFAULT = "f:green";
    public static final String MAVEN_STYLE_PROJECT_DEFAULT = "f:cyan";

    /**
     * Maven output color mode.
     * Allowed values are <code>auto</code>, <code>always</code>, <code>never</code>.
     *
     * @since 3.10.1
     */
    public static final String MAVEN_STYLE_COLOR_PROPERTY = MAVEN_STYLE_PREFIX + "color";

    /**
     * Color style for transfer messages.
     * @since 3.10.1
     */
    public static final String MAVEN_STYLE_TRANSFER = MAVEN_STYLE_PREFIX + MAVEN_STYLE_TRANSFER_NAME;

    /**
     * Color style for trace messages.
     * @since 3.10.1
     */
    public static final String MAVEN_STYLE_TRACE = MAVEN_STYLE_PREFIX + MAVEN_STYLE_TRACE_NAME;

    /**
     * Color style for debug messages.
     * @since 3.10.1
     */
    public static final String MAVEN_STYLE_DEBUG = MAVEN_STYLE_PREFIX + MAVEN_STYLE_DEBUG_NAME;

    /**
     * Color style for info messages.
     * @since 3.10.1
     */
    public static final String MAVEN_STYLE_INFO = MAVEN_STYLE_PREFIX + MAVEN_STYLE_INFO_NAME;

    /**
     * Color style for warning messages.
     * @since 3.10.1
     */
    public static final String MAVEN_STYLE_WARNING = MAVEN_STYLE_PREFIX + MAVEN_STYLE_WARNING_NAME;

    /**
     * Color style for error messages.
     * @since 3.10.1
     */
    public static final String MAVEN_STYLE_ERROR = MAVEN_STYLE_PREFIX + MAVEN_STYLE_ERROR_NAME;

    /**
     * Color style for success messages.
     * @since 3.10.1
     */
    public static final String MAVEN_STYLE_SUCCESS = MAVEN_STYLE_PREFIX + MAVEN_STYLE_SUCCESS_NAME;

    /**
     * Color style for failure messages.
     * @since 3.10.1
     */
    public static final String MAVEN_STYLE_FAILURE = MAVEN_STYLE_PREFIX + MAVEN_STYLE_FAILURE_NAME;

    /**
     * Color style for strong messages.
     * @since 3.10.1
     */
    public static final String MAVEN_STYLE_STRONG = MAVEN_STYLE_PREFIX + MAVEN_STYLE_STRONG_NAME;

    /**
     * Color style for mojo messages.
     * @since 3.10.1
     */
    public static final String MAVEN_STYLE_MOJO = MAVEN_STYLE_PREFIX + MAVEN_STYLE_MOJO_NAME;

    /**
     * Color style for project messages.
     * @since 3.10.1
     */
    public static final String MAVEN_STYLE_PROJECT = MAVEN_STYLE_PREFIX + MAVEN_STYLE_PROJECT_NAME;

    private Constants() {}
}
