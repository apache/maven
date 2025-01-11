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

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.cling.logging.Slf4jConfiguration;
import org.apache.maven.execution.MavenExecutionRequest;
import org.codehaus.plexus.interpolation.AbstractValueSource;
import org.codehaus.plexus.interpolation.BasicInterpolator;
import org.codehaus.plexus.interpolation.StringSearchInterpolator;

import static java.util.Objects.requireNonNull;

/**
 * Various internal utilities used in org.apache.maven.cling.invoker and its subpackages.
 * Not documented, tested, or intended for external uses.
 */
public final class InvokerUtils {
    private InvokerUtils() {}

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

}
