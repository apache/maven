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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.function.UnaryOperator;

import org.apache.maven.impl.model.DefaultInterpolator;

@Deprecated
public class MavenPropertiesLoader {

    public static final String INCLUDES_PROPERTY = "${includes}"; // includes

    public static final String OVERRIDE_PREFIX =
            "maven.override."; // prefix that marks that system property should override defaults.

    public static void loadProperties(
            java.util.Properties properties, Path path, UnaryOperator<String> callback, boolean escape)
            throws IOException {
        MavenProperties sp = new MavenProperties(false);
        if (Files.exists(path)) {
            sp.load(path);
        }
        properties.forEach(
                (k, v) -> sp.put(k.toString(), escape ? DefaultInterpolator.escape(v.toString()) : v.toString()));
        loadIncludes(path, sp, callback);
        substitute(sp, callback);
        sp.forEach(properties::setProperty);
    }

    public static void substitute(MavenProperties props, UnaryOperator<String> callback) {
        for (Enumeration<?> e = props.propertyNames(); e.hasMoreElements(); ) {
            String name = (String) e.nextElement();
            String value = props.getProperty(name);
            if (value == null) {
                value = callback.apply(name);
            }
            if (name.startsWith(OVERRIDE_PREFIX)) {
                String overrideName = name.substring(OVERRIDE_PREFIX.length());
                props.put(overrideName, substVars(value, name, props, callback));
            } else {
                props.put(name, substVars(value, name, props, callback));
            }
        }
        props.keySet().removeIf(k -> k.startsWith(OVERRIDE_PREFIX));
    }

    private static MavenProperties loadPropertiesFile(Path path, boolean failIfNotFound, UnaryOperator<String> callback)
            throws IOException {
        MavenProperties configProps = new MavenProperties(null, false);
        if (Files.exists(path) || failIfNotFound) {
            configProps.load(path);
            loadIncludes(path, configProps, callback);
            trimValues(configProps);
        }
        return configProps;
    }

    private static void loadIncludes(Path configProp, MavenProperties configProps, UnaryOperator<String> callback)
            throws IOException {
        String includes = configProps.get(INCLUDES_PROPERTY);
        if (includes != null) {
            includes = substVars(includes, INCLUDES_PROPERTY, configProps, callback);
            StringTokenizer st = new StringTokenizer(includes, "?\",", true);
            if (st.countTokens() > 0) {
                String location;
                do {
                    location = nextLocation(st);
                    if (location != null) {
                        boolean mandatory = true;
                        if (location.startsWith("?")) {
                            mandatory = false;
                            location = location.substring(1);
                        }
                        Path path = configProp.resolveSibling(location);
                        MavenProperties props = loadPropertiesFile(path, mandatory, s -> {
                            var v = callback.apply(s);
                            return v != null ? v : configProps.getProperty(s);
                        });
                        configProps.putAll(props);
                    }
                } while (location != null);
            }
        }
        configProps.remove(INCLUDES_PROPERTY);
    }

    private static void trimValues(MavenProperties configProps) {
        configProps.replaceAll((k, v) -> v.trim());
    }

    private static String nextLocation(StringTokenizer st) {
        boolean optional = false;
        String retVal = null;

        if (st.countTokens() > 0) {
            String tokenList = "?\",";
            StringBuilder tokBuf = new StringBuilder(10);
            String tok;
            boolean inQuote = false;
            boolean tokStarted = false;
            boolean exit = false;
            while ((st.hasMoreTokens()) && (!exit)) {
                tok = st.nextToken(tokenList);
                switch (tok) {
                    case "\"":
                        inQuote = !inQuote;
                        if (inQuote) {
                            tokenList = "\"";
                        } else {
                            tokenList = "\" ";
                        }
                        break;
                    case ",":
                        if (tokStarted) {
                            retVal = tokBuf.toString();
                            tokStarted = false;
                            tokBuf = new StringBuilder(10);
                            exit = true;
                        }
                        break;
                    case "?":
                        optional = true;
                        break;
                    default:
                        tokStarted = true;
                        tokBuf.append(tok.trim());
                        break;
                }
            }

            // Handle case where end of token stream and
            // still got data
            if ((!exit) && tokStarted) {
                retVal = tokBuf.toString();
            }
        }

        return optional ? "?" + retVal : retVal;
    }

    public static String substVars(
            String value, String name, Map<String, String> props, UnaryOperator<String> callback) {
        return DefaultInterpolator.substVars(value, name, null, props, callback, null, false);
    }
}
