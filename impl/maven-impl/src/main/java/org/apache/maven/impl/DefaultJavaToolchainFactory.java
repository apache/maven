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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.maven.api.JavaToolchain;
import org.apache.maven.api.Toolchain;
import org.apache.maven.api.Version;
import org.apache.maven.api.VersionConstraint;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.di.Inject;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.services.ToolchainFactory;
import org.apache.maven.api.services.ToolchainFactoryException;
import org.apache.maven.api.services.VersionParser;
import org.apache.maven.api.services.VersionParserException;
import org.apache.maven.api.toolchain.ToolchainModel;
import org.apache.maven.api.xml.XmlNode;
import org.apache.maven.impl.util.Os;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named("jdk")
@Singleton
public class DefaultJavaToolchainFactory implements ToolchainFactory {

    public static final String KEY_JAVAHOME = "jdkHome"; // NOI18N

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultJavaToolchainFactory.class);

    final VersionParser versionParser;

    @Inject
    public DefaultJavaToolchainFactory(VersionParser versionParser) {
        this.versionParser = versionParser;
    }

    @Nonnull
    @Override
    public JavaToolchain createToolchain(@Nonnull ToolchainModel model) {
        // populate the provides section
        Map<String, Predicate<String>> matchers = model.getProvides().entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    if (value == null) {
                        throw new ToolchainFactoryException(
                                "Provides token '" + key + "' doesn't have any value configured.");
                    }
                    return "version".equals(key) ? new VersionMatcher(versionParser, value) : new ExactMatcher(value);
                }));

        // compute and normalize the java home
        XmlNode dom = model.getConfiguration();
        XmlNode javahome = dom != null ? dom.child(KEY_JAVAHOME) : null;
        if (javahome == null || javahome.value() == null) {
            throw new ToolchainFactoryException(
                    "Java toolchain without the " + KEY_JAVAHOME + " configuration element.");
        }
        Path normal = Paths.get(javahome.value()).normalize();
        if (!Files.exists(normal)) {
            throw new ToolchainFactoryException("Non-existing JDK home configuration at " + normal.toAbsolutePath());
        }
        String javaHome = normal.toString();

        return new DefaultJavaToolchain(model, javaHome, matchers);
    }

    @Nonnull
    @Override
    public Optional<Toolchain> createDefaultToolchain() {
        return Optional.empty();
    }

    static class DefaultJavaToolchain implements JavaToolchain {

        final ToolchainModel model;
        final String javaHome;
        final Map<String, Predicate<String>> matchers;

        DefaultJavaToolchain(ToolchainModel model, String javaHome, Map<String, Predicate<String>> matchers) {
            this.model = model;
            this.javaHome = javaHome;
            this.matchers = matchers;
        }

        @Override
        public String getJavaHome() {
            return javaHome;
        }

        @Override
        public String getType() {
            return "jdk";
        }

        @Override
        public ToolchainModel getModel() {
            return model;
        }

        @Override
        public String findTool(String toolName) {
            Path toRet = findTool(toolName, Paths.get(getJavaHome()).normalize());
            if (toRet != null) {
                return toRet.toAbsolutePath().toString();
            }
            return null;
        }

        private static Path findTool(String toolName, Path installDir) {
            Path bin = installDir.resolve("bin"); // NOI18N
            if (Files.isDirectory(bin)) {
                if (Os.IS_WINDOWS) {
                    Path tool = bin.resolve(toolName + ".exe");
                    if (Files.exists(tool)) {
                        return tool;
                    }
                }
                Path tool = bin.resolve(toolName);
                if (Files.exists(tool)) {
                    return tool;
                }
            }
            return null;
        }

        @Override
        public boolean matchesRequirements(Map<String, String> requirements) {
            for (Map.Entry<String, String> requirement : requirements.entrySet()) {
                String key = requirement.getKey();

                Predicate<String> matcher = matchers.get(key);

                if (matcher == null) {
                    LOGGER.debug("Toolchain {} is missing required property: {}", this, key);
                    return false;
                }
                if (!matcher.test(requirement.getValue())) {
                    LOGGER.debug("Toolchain {} doesn't match required property: {}", this, key);
                    return false;
                }
            }
            return true;
        }

        @Override
        public String toString() {
            return "JDK[" + getJavaHome() + "]";
        }
    }

    static final class ExactMatcher implements Predicate<String> {

        final String provides;

        ExactMatcher(String provides) {
            this.provides = provides;
        }

        @Override
        public boolean test(String requirement) {
            return provides.equalsIgnoreCase(requirement);
        }

        @Override
        public String toString() {
            return provides;
        }
    }

    static final class VersionMatcher implements Predicate<String> {

        final VersionParser versionParser;
        final Version version;

        VersionMatcher(VersionParser versionParser, String version) {
            this.versionParser = versionParser;
            this.version = versionParser.parseVersion(version);
        }

        @Override
        public boolean test(String requirement) {
            try {
                VersionConstraint constraint = versionParser.parseVersionConstraint(requirement);
                return constraint.contains(version);
            } catch (VersionParserException ex) {
                return false;
            }
        }

        @Override
        public String toString() {
            return version.toString();
        }
    }
}
