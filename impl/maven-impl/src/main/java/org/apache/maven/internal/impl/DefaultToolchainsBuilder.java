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
import java.util.Map;
import java.util.function.Function;

import org.apache.maven.api.di.Inject;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.services.BuilderProblem;
import org.apache.maven.api.services.Interpolator;
import org.apache.maven.api.services.ProblemCollector;
import org.apache.maven.api.services.Source;
import org.apache.maven.api.services.ToolchainsBuilder;
import org.apache.maven.api.services.ToolchainsBuilderException;
import org.apache.maven.api.services.ToolchainsBuilderRequest;
import org.apache.maven.api.services.ToolchainsBuilderResult;
import org.apache.maven.api.services.xml.ToolchainsXmlFactory;
import org.apache.maven.api.services.xml.XmlReaderException;
import org.apache.maven.api.services.xml.XmlReaderRequest;
import org.apache.maven.api.toolchain.PersistedToolchains;
import org.apache.maven.internal.impl.model.DefaultInterpolator;
import org.apache.maven.toolchain.v4.MavenToolchainsMerger;
import org.apache.maven.toolchain.v4.MavenToolchainsTransformer;

/**
 * Builds the effective toolchains from a user toolchains file and/or a global toolchains file.
 *
 */
@Named
public class DefaultToolchainsBuilder implements ToolchainsBuilder {

    private final MavenToolchainsMerger toolchainsMerger = new MavenToolchainsMerger();

    private final Interpolator interpolator;

    private final ToolchainsXmlFactory toolchainsXmlFactory;

    public DefaultToolchainsBuilder() {
        this(new DefaultInterpolator(), new DefaultToolchainsXmlFactory());
    }

    @Inject
    public DefaultToolchainsBuilder(Interpolator interpolator, ToolchainsXmlFactory toolchainsXmlFactory) {
        this.interpolator = interpolator;
        this.toolchainsXmlFactory = toolchainsXmlFactory;
    }

    @Override
    public ToolchainsBuilderResult build(ToolchainsBuilderRequest request) throws ToolchainsBuilderException {
        ProblemCollector<BuilderProblem> problems = DefaultProblemCollector.create(request.getSession());

        Source installationSource = request.getInstallationToolchainsSource().orElse(null);
        PersistedToolchains installation = readToolchains(installationSource, request, problems);

        Source userSource = request.getUserToolchainsSource().orElse(null);
        PersistedToolchains user = readToolchains(userSource, request, problems);

        PersistedToolchains effective = toolchainsMerger.merge(user, installation, false, null);

        if (problems.hasErrors()) {
            throw new ToolchainsBuilderException("Error building toolchains", problems);
        }

        return new DefaultToolchainsBuilderResult(effective, problems);
    }

    private PersistedToolchains readToolchains(
            Source toolchainsSource, ToolchainsBuilderRequest request, ProblemCollector<BuilderProblem> problems) {
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
                toolchains = toolchainsXmlFactory.read(XmlReaderRequest.builder()
                        .inputStream(is)
                        .location(toolchainsSource.getLocation())
                        .strict(true)
                        .build());
            } catch (XmlReaderException e) {
                InputStream is = toolchainsSource.openStream();
                if (is == null) {
                    return PersistedToolchains.newInstance();
                }
                toolchains = toolchainsXmlFactory.read(XmlReaderRequest.builder()
                        .inputStream(is)
                        .location(toolchainsSource.getLocation())
                        .strict(false)
                        .build());
                Location loc = e.getCause() instanceof XMLStreamException xe ? xe.getLocation() : null;
                problems.reportProblem(new DefaultBuilderProblem(
                        toolchainsSource.getLocation(),
                        loc != null ? loc.getLineNumber() : -1,
                        loc != null ? loc.getColumnNumber() : -1,
                        e,
                        e.getMessage(),
                        BuilderProblem.Severity.WARNING));
            }
        } catch (XmlReaderException e) {
            Location loc = e.getCause() instanceof XMLStreamException xe ? xe.getLocation() : null;
            problems.reportProblem(new DefaultBuilderProblem(
                    toolchainsSource.getLocation(),
                    loc != null ? loc.getLineNumber() : -1,
                    loc != null ? loc.getColumnNumber() : -1,
                    e,
                    "Non-parseable toolchains " + toolchainsSource.getLocation() + ": " + e.getMessage(),
                    BuilderProblem.Severity.FATAL));
            return PersistedToolchains.newInstance();
        } catch (IOException e) {
            problems.reportProblem(new DefaultBuilderProblem(
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
            PersistedToolchains toolchains,
            ToolchainsBuilderRequest request,
            ProblemCollector<BuilderProblem> problems) {
        Map<String, String> userProperties = request.getSession().getUserProperties();
        Map<String, String> systemProperties = request.getSession().getSystemProperties();
        Function<String, String> src = Interpolator.chain(userProperties::get, systemProperties::get);
        return new MavenToolchainsTransformer(value -> value != null ? interpolator.interpolate(value, src) : null)
                .visit(toolchains);
    }

    /**
     * Collects the output of the toolchains builder.
     *
     */
    static class DefaultToolchainsBuilderResult implements ToolchainsBuilderResult {

        private final PersistedToolchains effectiveToolchains;

        private final ProblemCollector<BuilderProblem> problems;

        DefaultToolchainsBuilderResult(
                PersistedToolchains effectiveToolchains, ProblemCollector<BuilderProblem> problems) {
            this.effectiveToolchains = effectiveToolchains;
            this.problems = (problems != null) ? problems : ProblemCollector.empty();
        }

        @Override
        public PersistedToolchains getEffectiveToolchains() {
            return effectiveToolchains;
        }

        @Override
        public ProblemCollector<BuilderProblem> getProblems() {
            return problems;
        }
    }
}
