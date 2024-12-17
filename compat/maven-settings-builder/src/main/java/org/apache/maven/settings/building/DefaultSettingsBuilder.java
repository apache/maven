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
package org.apache.maven.settings.building;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.maven.api.Session;
import org.apache.maven.api.services.BuilderProblem;
import org.apache.maven.api.services.SettingsBuilderException;
import org.apache.maven.api.services.SettingsBuilderRequest;
import org.apache.maven.api.services.SettingsBuilderResult;
import org.apache.maven.api.services.xml.SettingsXmlFactory;
import org.apache.maven.building.FileSource;
import org.apache.maven.building.Source;
import org.apache.maven.settings.Settings;

/**
 * Builds the effective settings from a user settings file and/or a global settings file.
 *
 * @deprecated since 4.0.0, use {@link org.apache.maven.api.services.SettingsBuilder} instead
 */
@Named
@Singleton
@Deprecated(since = "4.0.0")
public class DefaultSettingsBuilder implements SettingsBuilder {

    private final org.apache.maven.internal.impl.DefaultSettingsBuilder builder;
    private final org.apache.maven.internal.impl.DefaultSettingsXmlFactory settingsXmlFactory;

    @Inject
    public DefaultSettingsBuilder(
            org.apache.maven.internal.impl.DefaultSettingsBuilder builder,
            org.apache.maven.internal.impl.DefaultSettingsXmlFactory settingsXmlFactory) {
        this.builder = builder;
        this.settingsXmlFactory = settingsXmlFactory;
    }

    @Override
    public SettingsBuildingResult build(SettingsBuildingRequest request) throws SettingsBuildingException {

        try {
            SettingsBuilderResult result = builder.build(SettingsBuilderRequest.builder()
                    .session((Session) java.lang.reflect.Proxy.newProxyInstance(
                            Session.class.getClassLoader(),
                            new Class[] {Session.class},
                            (Object proxy, Method method, Object[] args) -> {
                                if ("getSystemProperties".equals(method.getName())) {
                                    return request.getSystemProperties().entrySet().stream()
                                            .collect(Collectors.toMap(
                                                    e -> e.getKey().toString(),
                                                    e -> e.getValue().toString()));
                                } else if ("getUserProperties".equals(method.getName())) {
                                    return request.getUserProperties().entrySet().stream()
                                            .collect(Collectors.toMap(
                                                    e -> e.getKey().toString(),
                                                    e -> e.getValue().toString()));
                                } else if ("getService".equals(method.getName())) {
                                    if (args[0] == SettingsXmlFactory.class) {
                                        return settingsXmlFactory;
                                    }
                                }
                                return null;
                            }))
                    .installationSettingsSource(
                            toSource(request.getGlobalSettingsFile(), request.getGlobalSettingsSource()))
                    .projectSettingsSource(
                            toSource(request.getProjectSettingsFile(), request.getProjectSettingsSource()))
                    .userSettingsSource(toSource(request.getUserSettingsFile(), request.getUserSettingsSource()))
                    .build());

            return new DefaultSettingsBuildingResult(
                    new Settings(result.getEffectiveSettings()),
                    convert(result.getProblems().problems().toList()));
        } catch (SettingsBuilderException e) {
            throw new SettingsBuildingException(
                    convert(e.getProblemCollector().problems().toList()));
        }
    }

    private org.apache.maven.api.services.Source toSource(File file, Source source) {
        if (file != null && file.exists()) {
            return org.apache.maven.api.services.Source.fromPath(file.toPath());
        } else if (source instanceof FileSource fs) {
            return org.apache.maven.api.services.Source.fromPath(fs.getPath());
        } else if (source != null) {
            return new org.apache.maven.api.services.Source() {
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
                public org.apache.maven.api.services.Source resolve(String relative) {
                    return null;
                }
            };
        } else {
            return null;
        }
    }

    private List<SettingsProblem> convert(List<BuilderProblem> problems) {
        return problems.stream().map(this::convert).toList();
    }

    private SettingsProblem convert(BuilderProblem problem) {
        return new DefaultSettingsProblem(
                problem.getMessage(),
                SettingsProblem.Severity.valueOf(problem.getSeverity().name()),
                problem.getSource(),
                problem.getLineNumber(),
                problem.getColumnNumber(),
                problem.getException());
    }
}
