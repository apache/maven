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

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.function.Function;

import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.services.model.RootLocator;
import org.apache.maven.cli.logging.Slf4jConfiguration;
import org.apache.maven.execution.MavenExecutionRequest;
import org.codehaus.plexus.interpolation.AbstractValueSource;
import org.codehaus.plexus.interpolation.BasicInterpolator;
import org.codehaus.plexus.interpolation.StringSearchInterpolator;
import org.codehaus.plexus.logging.Logger;

import static java.util.Objects.requireNonNull;

/**
 * Various utilities, mostly to bridge "old" and "new" stuff, like Properties vs Maps, File vs Paths, etc.
 */
public final class Utils {
    private Utils() {}

    @Nullable
    public static File toFile(Path path) {
        if (path != null) {
            return path.toFile();
        }
        return null;
    }

    @Nonnull
    public static String stripLeadingAndTrailingQuotes(String str) {
        requireNonNull(str, "str");
        final int length = str.length();
        if (length > 1
                && str.startsWith("\"")
                && str.endsWith("\"")
                && str.substring(1, length - 1).indexOf('"') == -1) {
            str = str.substring(1, length - 1);
        }
        return str;
    }

    @Nonnull
    public static Path getCanonicalPath(Path path) {
        requireNonNull(path, "path");
        try {
            return path.toRealPath();
        } catch (IOException e) {
            return getCanonicalPath(path.getParent()).resolve(path.getFileName());
        }
    }

    @Nonnull
    public static Map<String, String> toMap(Properties properties) {
        requireNonNull(properties, "properties");
        HashMap<String, String> map = new HashMap<>();
        for (String key : properties.stringPropertyNames()) {
            map.put(key, properties.getProperty(key));
        }
        return map;
    }

    @Nonnull
    public static Properties toProperties(Map<String, String> properties) {
        requireNonNull(properties, "properties");
        Properties map = new Properties();
        for (String key : properties.keySet()) {
            map.put(key, properties.get(key));
        }
        return map;
    }

    @Nonnull
    public static BasicInterpolator createInterpolator(Collection<Map<String, String>> properties) {
        StringSearchInterpolator interpolator = new StringSearchInterpolator();
        interpolator.addValueSource(new AbstractValueSource(false) {
            @Override
            public Object getValue(String expression) {
                for (Map<String, String> props : properties) {
                    String val = props.get(expression);
                    if (val != null) {
                        return val;
                    }
                }
                return null;
            }
        });
        return interpolator;
    }

    @Nonnull
    public static Function<String, String> prefix(String prefix, Function<String, String> cb) {
        return s -> {
            String v = null;
            if (s.startsWith(prefix)) {
                v = cb.apply(s.substring(prefix.length()));
            }
            return v;
        };
    }

    @SafeVarargs
    @Nonnull
    public static Function<String, String> or(Function<String, String>... callbacks) {
        return s -> {
            for (Function<String, String> cb : callbacks) {
                String r = cb.apply(s);
                if (r != null) {
                    return r;
                }
            }
            return null;
        };
    }

    public static int toMavenExecutionRequestLoggingLevel(Slf4jConfiguration.Level level) {
        requireNonNull(level, "level");
        return switch (level) {
            case DEBUG -> MavenExecutionRequest.LOGGING_LEVEL_DEBUG;
            case INFO -> MavenExecutionRequest.LOGGING_LEVEL_INFO;
            case ERROR -> MavenExecutionRequest.LOGGING_LEVEL_ERROR;
        };
    }

    public static int toPlexusLoggingLevel(Slf4jConfiguration.Level level) {
        requireNonNull(level, "level");
        return switch (level) {
            case DEBUG -> Logger.LEVEL_DEBUG;
            case INFO -> Logger.LEVEL_INFO;
            case ERROR -> Logger.LEVEL_ERROR;
        };
    }

    @Nullable
    public static Path findRoot(Path topDirectory) {
        requireNonNull(topDirectory, "topDirectory");
        Path rootDirectory =
                ServiceLoader.load(RootLocator.class).iterator().next().findRoot(topDirectory);
        if (rootDirectory != null) {
            return getCanonicalPath(rootDirectory);
        }
        return null;
    }

    @Nonnull
    public static Path findMandatoryRoot(Path topDirectory) {
        requireNonNull(topDirectory, "topDirectory");
        return getCanonicalPath(Optional.ofNullable(
                        ServiceLoader.load(RootLocator.class).iterator().next().findMandatoryRoot(topDirectory))
                .orElseThrow());
    }
}
