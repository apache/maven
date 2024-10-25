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
package org.apache.maven.toolchain.building;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.api.Session;
import org.apache.maven.api.services.BuilderProblem;
import org.apache.maven.api.services.Source;
import org.apache.maven.api.services.ToolchainsBuilderException;
import org.apache.maven.api.services.ToolchainsBuilderRequest;
import org.apache.maven.api.services.ToolchainsBuilderResult;
import org.apache.maven.api.services.xml.ToolchainsXmlFactory;
import org.apache.maven.building.FileSource;
import org.apache.maven.building.Problem;
import org.apache.maven.building.ProblemCollector;
import org.apache.maven.building.ProblemCollectorFactory;
import org.apache.maven.toolchain.model.PersistedToolchains;
import org.codehaus.plexus.interpolation.os.OperatingSystemUtils;

/**
 *
 * @since 3.3.0
 * @deprecated since 4.0.0, use {@link org.apache.maven.api.services.ToolchainsBuilder} instead
 */
@Named
@Singleton
@Deprecated(since = "4.0.0")
public class DefaultToolchainsBuilder implements ToolchainsBuilder {
    private final org.apache.maven.api.services.ToolchainsBuilder builder;
    private final ToolchainsXmlFactory toolchainsXmlFactory;

    @Inject
    public DefaultToolchainsBuilder(
            org.apache.maven.api.services.ToolchainsBuilder builder, ToolchainsXmlFactory toolchainsXmlFactory) {
        this.builder = builder;
        this.toolchainsXmlFactory = toolchainsXmlFactory;
    }

    @Override
    public ToolchainsBuildingResult build(ToolchainsBuildingRequest request) throws ToolchainsBuildingException {
        try {
            ToolchainsBuilderResult result = builder.build(ToolchainsBuilderRequest.builder()
                    .session((Session) java.lang.reflect.Proxy.newProxyInstance(
                            Session.class.getClassLoader(),
                            new Class[] {Session.class},
                            (Object proxy, Method method, Object[] args) -> {
                                if ("getSystemProperties".equals(method.getName())) {
                                    Map<String, String> properties = new HashMap<>();
                                    Properties env = OperatingSystemUtils.getSystemEnvVars();
                                    env.stringPropertyNames()
                                            .forEach(k -> properties.put("env." + k, env.getProperty(k)));
                                    return properties;
                                } else if ("getUserProperties".equals(method.getName())) {
                                    return Map.of();
                                } else if ("getService".equals(method.getName())) {
                                    if (args[0] == ToolchainsXmlFactory.class) {
                                        return toolchainsXmlFactory;
                                    }
                                }
                                return null;
                            }))
                    .installationToolchainsSource(convert(request.getGlobalToolchainsSource()))
                    .userToolchainsSource(convert(request.getUserToolchainsSource()))
                    .build());

            return new DefaultToolchainsBuildingResult(
                    new PersistedToolchains(result.getEffectiveToolchains()), convert(result.getProblems()));
        } catch (ToolchainsBuilderException e) {
            throw new ToolchainsBuildingException(convert(e.getProblems()));
        }
    }

    private Source convert(org.apache.maven.building.Source source) {
        if (source instanceof FileSource fs) {
            return Source.fromPath(fs.getPath());
        } else if (source != null) {
            return new Source() {
                @Override
                public Path getPath() {
                    return null;
                }

                @Override
                public InputStream openStream() throws IOException {
                    return source.getInputStream();
                }

                @Override
                public String getLocation() {
                    return source.getLocation();
                }

                @Override
                public Source resolve(String relative) {
                    return null;
                }
            };
        } else {
            return null;
        }
    }

    private List<Problem> convert(List<BuilderProblem> problems) {
        ProblemCollector collector = ProblemCollectorFactory.newInstance(null);
        problems.forEach(p -> collector.add(
                Problem.Severity.valueOf(p.getSeverity().name()),
                p.getMessage(),
                p.getLineNumber(),
                p.getColumnNumber(),
                p.getException()));
        return collector.getProblems();
    }
}
