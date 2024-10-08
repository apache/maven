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
package org.apache.maven.internal.impl.model.profile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.api.di.Inject;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.model.Activation;
import org.apache.maven.api.model.Profile;
import org.apache.maven.api.services.BuilderProblem.Severity;
import org.apache.maven.api.services.ModelProblem.Version;
import org.apache.maven.api.services.ModelProblemCollector;
import org.apache.maven.api.services.VersionParser;
import org.apache.maven.api.services.model.ProfileActivationContext;
import org.apache.maven.api.services.model.ProfileActivator;
import org.apache.maven.api.services.model.RootLocator;
import org.apache.maven.internal.impl.model.ProfileActivationFilePathInterpolator;
import org.apache.maven.internal.impl.model.profile.ConditionParser.ExpressionFunction;

import static org.apache.maven.internal.impl.model.profile.ConditionParser.toBoolean;

/**
 * Determines profile activation based on the given condition.
 */
@Named("condition")
@Singleton
public class ConditionProfileActivator implements ProfileActivator {

    final VersionParser versionParser;
    final ProfileActivationFilePathInterpolator interpolator;
    final RootLocator rootLocator;

    @Inject
    public ConditionProfileActivator(
            VersionParser versionParser, ProfileActivationFilePathInterpolator interpolator, RootLocator rootLocator) {
        this.versionParser = versionParser;
        this.interpolator = interpolator;
        this.rootLocator = rootLocator;
    }

    @Override
    public boolean isActive(Profile profile, ProfileActivationContext context, ModelProblemCollector problems) {
        if (profile.getActivation() == null || profile.getActivation().getCondition() == null) {
            return false;
        }
        String condition = profile.getActivation().getCondition();
        try {
            Map<String, ExpressionFunction> functions = new HashMap<>();
            functions.put("length", args -> args.get(0).toString().length());
            functions.put("uppercase", args -> args.get(0).toString().toUpperCase());
            functions.put("lowercase", args -> args.get(0).toString().toLowerCase());
            functions.put(
                    "concat",
                    args -> String.join(
                            "", args.stream().map(ConditionParser::toString).toArray(String[]::new)));
            functions.put("substring", args -> {
                String str = args.get(0).toString();
                int start = ((Number) args.get(1)).intValue();
                int end = args.size() > 2 ? ((Number) args.get(2)).intValue() : str.length();
                return str.substring(start, end);
            });
            functions.put("min", args -> args.stream()
                    .map(Comparable.class::cast)
                    .min(Comparator.naturalOrder())
                    .orElseThrow());
            functions.put("max", args -> args.stream()
                    .map(Comparable.class::cast)
                    .max(Comparator.naturalOrder())
                    .orElseThrow());
            functions.put("not", args -> !(toBoolean(args.get(0))));
            functions.put("if", args -> args.size() == 3 ? (toBoolean(args.get(0)) ? args.get(1) : args.get(2)) : null);
            functions.put(
                    "contains",
                    args -> args.get(0).toString().contains(args.get(1).toString()));
            functions.put(
                    "matches",
                    args -> args.get(0).toString().matches(args.get(1).toString()));

            functions.put("inrange", args -> versionParser
                    .parseVersionRange(args.get(1).toString())
                    .contains(versionParser.parseVersion(args.get(0).toString())));

            functions.put(
                    "exists",
                    args -> Files.exists(
                            Paths.get(interpolator.interpolate(args.get(0).toString(), context))));
            functions.put(
                    "missing",
                    args -> !Files.exists(
                            Paths.get(interpolator.interpolate(args.get(0).toString(), context))));

            functions.put("property", args -> {
                String name = args.get(0).toString();
                if ("project.basedir".equals(name)) {
                    Path basedir = context.getProjectDirectory();
                    return basedir != null ? basedir.toFile().getAbsolutePath() : null;
                }
                if ("project.rootDirectory".equals(name)) {
                    Path basedir = context.getProjectDirectory();
                    Path root = rootLocator.findMandatoryRoot(basedir);
                    return root.toFile().getAbsolutePath();
                }
                String v = context.getUserProperties().get(name);
                if (v == null) {
                    v = context.getProjectProperties().get(name);
                }
                if (v == null) {
                    v = context.getSystemProperties().get(name);
                }
                return v;
            });

            return toBoolean(new ConditionParser(functions).parse(condition));
        } catch (Exception e) {
            problems.add(
                    Severity.ERROR, Version.V41, "Error parsing profile activation condition: " + e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean presentInConfig(Profile profile, ProfileActivationContext context, ModelProblemCollector problems) {
        Activation activation = profile.getActivation();
        if (activation == null) {
            return false;
        }
        return activation.getCondition() != null && !activation.getCondition().isBlank();
    }
}
