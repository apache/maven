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
package org.apache.maven.internal.impl;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.api.di.Named;
import org.apache.maven.api.services.BuilderProblem;
import org.apache.maven.api.services.Source;
import org.apache.maven.api.services.ToolchainsBuilder;
import org.apache.maven.api.services.ToolchainsBuilderException;
import org.apache.maven.api.services.ToolchainsBuilderRequest;
import org.apache.maven.api.services.ToolchainsBuilderResult;
import org.apache.maven.api.services.xml.ToolchainsXmlFactory;
import org.apache.maven.api.services.xml.XmlReaderException;
import org.apache.maven.api.services.xml.XmlReaderRequest;
import org.apache.maven.api.toolchain.PersistedToolchains;
import org.apache.maven.toolchain.v4.MavenToolchainsMerger;
import org.apache.maven.toolchain.v4.MavenToolchainsTransformer;
import org.codehaus.plexus.interpolation.EnvarBasedValueSource;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.MapBasedValueSource;
import org.codehaus.plexus.interpolation.RegexBasedInterpolator;

/**
 * Builds the effective settings from a user settings file and/or a global settings file.
 *
 */
@Named
public class DefaultToolchainsBuilder implements ToolchainsBuilder {

    private final MavenToolchainsMerger toolchainsMerger = new MavenToolchainsMerger();

    @Override
    public ToolchainsBuilderResult build(ToolchainsBuilderRequest request) throws ToolchainsBuilderException {
        List<BuilderProblem> problems = new ArrayList<>();

        Source globalSettingsSource = getSettingsSource(
                request.getGlobalToolchainsPath().orElse(null),
                request.getGlobalToolchainsSource().orElse(null));
        PersistedToolchains globalSettings = readToolchains(globalSettingsSource, request, problems);

        Source userSettingsSource = getSettingsSource(
                request.getUserToolchainsPath().orElse(null),
                request.getUserToolchainsSource().orElse(null));
        PersistedToolchains userSettings = readToolchains(userSettingsSource, request, problems);

        userSettings = toolchainsMerger.merge(userSettings, globalSettings, false, null);

        if (hasErrors(problems)) {
            throw new ToolchainsBuilderException("Error building settings", problems);
        }

        return new DefaultToolchainsBuilderResult(userSettings, problems);
    }

    private boolean hasErrors(List<BuilderProblem> problems) {
        if (problems != null) {
            for (BuilderProblem problem : problems) {
                if (BuilderProblem.Severity.ERROR.compareTo(problem.getSeverity()) >= 0) {
                    return true;
                }
            }
        }

        return false;
    }

    private Source getSettingsSource(Path settingsPath, Source settingsSource) {
        if (settingsSource != null) {
            return settingsSource;
        } else if (settingsPath != null && Files.exists(settingsPath)) {
            return new PathSource(settingsPath);
        }
        return null;
    }

    private PersistedToolchains readToolchains(
            Source toolchainsSource, ToolchainsBuilderRequest request, List<BuilderProblem> problems) {
        if (toolchainsSource == null) {
            return PersistedToolchains.newInstance();
        }

        PersistedToolchains toolchains;

        try {
            try {
                InputStream is = toolchainsSource.openStream();
                if (is == null) {
                    return PersistedToolchains.newInstance();
                }
                toolchains = request.getSession()
                        .getService(ToolchainsXmlFactory.class)
                        .read(XmlReaderRequest.builder()
                                .inputStream(is)
                                .location(toolchainsSource.getLocation())
                                .strict(true)
                                .build());
            } catch (XmlReaderException e) {
                InputStream is = toolchainsSource.openStream();
                if (is == null) {
                    return PersistedToolchains.newInstance();
                }
                toolchains = request.getSession()
                        .getService(ToolchainsXmlFactory.class)
                        .read(XmlReaderRequest.builder()
                                .inputStream(is)
                                .location(toolchainsSource.getLocation())
                                .strict(false)
                                .build());
                Location loc = e.getCause() instanceof XMLStreamException xe ? xe.getLocation() : null;
                problems.add(new DefaultBuilderProblem(
                        toolchainsSource.getLocation(),
                        loc != null ? loc.getLineNumber() : -1,
                        loc != null ? loc.getColumnNumber() : -1,
                        e,
                        e.getMessage(),
                        BuilderProblem.Severity.WARNING));
            }
        } catch (XmlReaderException e) {
            Location loc = e.getCause() instanceof XMLStreamException xe ? xe.getLocation() : null;
            problems.add(new DefaultBuilderProblem(
                    toolchainsSource.getLocation(),
                    loc != null ? loc.getLineNumber() : -1,
                    loc != null ? loc.getColumnNumber() : -1,
                    e,
                    "Non-parseable toolchains " + toolchainsSource.getLocation() + ": " + e.getMessage(),
                    BuilderProblem.Severity.FATAL));
            return PersistedToolchains.newInstance();
        } catch (IOException e) {
            problems.add(new DefaultBuilderProblem(
                    toolchainsSource.getLocation(),
                    -1,
                    -1,
                    e,
                    "Non-readable toolchains " + toolchainsSource.getLocation() + ": " + e.getMessage(),
                    BuilderProblem.Severity.FATAL));
            return PersistedToolchains.newInstance();
        }

        toolchains = interpolate(toolchains, request, problems);

        return toolchains;
    }

    private PersistedToolchains interpolate(
            PersistedToolchains toolchains, ToolchainsBuilderRequest request, List<BuilderProblem> problems) {

        RegexBasedInterpolator interpolator = new RegexBasedInterpolator();

        interpolator.addValueSource(new MapBasedValueSource(request.getSession().getUserProperties()));

        interpolator.addValueSource(new MapBasedValueSource(request.getSession().getSystemProperties()));

        try {
            interpolator.addValueSource(new EnvarBasedValueSource());
        } catch (IOException e) {
            problems.add(new DefaultBuilderProblem(
                    null,
                    -1,
                    -1,
                    e,
                    "Failed to use environment variables for interpolation: " + e.getMessage(),
                    BuilderProblem.Severity.WARNING));
        }

        return new MavenToolchainsTransformer(value -> {
                    try {
                        return value != null ? interpolator.interpolate(value) : null;
                    } catch (InterpolationException e) {
                        problems.add(new DefaultBuilderProblem(
                                null,
                                -1,
                                -1,
                                e,
                                "Failed to interpolate settings: " + e.getMessage(),
                                BuilderProblem.Severity.WARNING));
                        return value;
                    }
                })
                .visit(toolchains);
    }

    /**
     * Collects the output of the settings builder.
     *
     */
    static class DefaultToolchainsBuilderResult implements ToolchainsBuilderResult {

        private final PersistedToolchains effectiveToolchains;

        private final List<BuilderProblem> problems;

        DefaultToolchainsBuilderResult(PersistedToolchains effectiveToolchains, List<BuilderProblem> problems) {
            this.effectiveToolchains = effectiveToolchains;
            this.problems = (problems != null) ? problems : new ArrayList<>();
        }

        @Override
        public PersistedToolchains getEffectiveToolchains() {
            return effectiveToolchains;
        }

        @Override
        public List<BuilderProblem> getProblems() {
            return problems;
        }
    }
}
