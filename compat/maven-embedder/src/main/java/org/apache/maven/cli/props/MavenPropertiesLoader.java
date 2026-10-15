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
package org.apache.maven.cli.props;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.UnaryOperator;

import org.apache.maven.api.services.InterpolatorException;
import org.apache.maven.utils.StringInterpolatorException;

/**
 * @deprecated Use {@link org.apache.maven.utils.MavenPropertiesLoader} instead.
 */
@Deprecated
public class MavenPropertiesLoader {
    public static final String INCLUDES_PROPERTY = org.apache.maven.utils.MavenPropertiesLoader.INCLUDES_PROPERTY;

    public static final String OVERRIDE_PREFIX = org.apache.maven.utils.MavenPropertiesLoader.OVERRIDE_PREFIX;

    public static void loadProperties(
            java.util.Properties properties, Path path, UnaryOperator<String> callback, boolean escape)
            throws IOException {
        try {
            org.apache.maven.utils.MavenPropertiesLoader.loadProperties(properties, path, callback, escape);
        } catch (StringInterpolatorException e) {
            throw new InterpolatorException(e.getMessage());
        }
    }

    public static void substitute(MavenProperties props, UnaryOperator<String> callback) {
        try {
            org.apache.maven.utils.MavenPropertiesLoader.substitute(props, callback);
        } catch (StringInterpolatorException e) {
            throw new InterpolatorException(e.getMessage());
        }
    }

    public static String substVars(
            String value, String name, Map<String, String> props, UnaryOperator<String> callback) {
        try {
            return org.apache.maven.utils.MavenPropertiesLoader.substVars(value, name, props, callback);
        } catch (StringInterpolatorException e) {
            throw new InterpolatorException(e.getMessage());
        }
    }
}
