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
package org.apache.maven.internal.impl.model;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import org.apache.maven.api.di.Inject;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.services.BuilderProblem;
import org.apache.maven.api.services.Interpolator;
import org.apache.maven.api.services.InterpolatorException;
import org.apache.maven.api.services.ModelBuilderRequest;
import org.apache.maven.api.services.ModelProblem;
import org.apache.maven.api.services.ModelProblemCollector;
import org.apache.maven.api.services.model.ModelInterpolator;
import org.apache.maven.api.services.model.PathTranslator;
import org.apache.maven.api.services.model.RootLocator;
import org.apache.maven.api.services.model.UrlNormalizer;
import org.apache.maven.internal.impl.model.reflection.ReflectionValueExtractor;
import org.apache.maven.model.v4.MavenTransformer;

@Named
@Singleton
public class DefaultModelInterpolator implements ModelInterpolator {

    private static final String PREFIX_PROJECT = "project.";
    private static final String PREFIX_POM = "pom.";
    private static final List<String> PROJECT_PREFIXES_3_1 = Arrays.asList(PREFIX_POM, PREFIX_PROJECT);
    private static final List<String> PROJECT_PREFIXES_4_0 = Collections.singletonList(PREFIX_PROJECT);

    // MNG-1927, MNG-2124, MNG-3355:
    // If the build section is present and the project directory is non-null, we should make
    // sure interpolation of the directories below uses translated paths.
    // Afterward, we'll double back and translate any paths that weren't covered during interpolation via the
    // code below...
    private static final Set<String> TRANSLATED_PATH_EXPRESSIONS = Set.of(
            "build.directory",
            "build.outputDirectory",
            "build.testOutputDirectory",
            "build.sourceDirectory",
            "build.testSourceDirectory",
            "build.scriptSourceDirectory",
            "reporting.outputDirectory");

    private static final Set<String> URL_EXPRESSIONS = Set.of(
            "project.url",
            "project.scm.url",
            "project.scm.connection",
            "project.scm.developerConnection",
            "project.distributionManagement.site.url");

    private final PathTranslator pathTranslator;
    private final UrlNormalizer urlNormalizer;
    private final RootLocator rootLocator;
    private final Interpolator interpolator;

    @Inject
    public DefaultModelInterpolator(
            PathTranslator pathTranslator,
            UrlNormalizer urlNormalizer,
            RootLocator rootLocator,
            Interpolator interpolator) {
        this.pathTranslator = pathTranslator;
        this.urlNormalizer = urlNormalizer;
        this.rootLocator = rootLocator;
        this.interpolator = interpolator;
    }

    interface InnerInterpolator {
        String interpolate(String value);
    }

    @Override
    public Model interpolateModel(
            Model model, Path projectDir, ModelBuilderRequest request, ModelProblemCollector problems) {
        InnerInterpolator innerInterpolator = createInterpolator(model, projectDir, request, problems);
        return new MavenTransformer(innerInterpolator::interpolate).visit(model);
    }

    private InnerInterpolator createInterpolator(
            Model model, Path projectDir, ModelBuilderRequest request, ModelProblemCollector problems) {

        Map<String, Optional<String>> cache = new HashMap<>();
        Function<String, Optional<String>> ucb =
                v -> Optional.ofNullable(callback(model, projectDir, request, problems, v));
        UnaryOperator<String> cb = v -> cache.computeIfAbsent(v, ucb).orElse(null);
        BinaryOperator<String> postprocessor = (e, v) -> postProcess(projectDir, request, e, v);
        return value -> {
            try {
                return interpolator.interpolate(value, cb, postprocessor, false);
            } catch (InterpolatorException e) {
                problems.add(BuilderProblem.Severity.ERROR, ModelProblem.Version.BASE, e.getMessage(), e);
                return null;
            }
        };
    }

    protected List<String> getProjectPrefixes(ModelBuilderRequest request) {
        return request.getRequestType() == ModelBuilderRequest.RequestType.BUILD_PROJECT
                ? PROJECT_PREFIXES_4_0
                : PROJECT_PREFIXES_3_1;
    }

    String callback(
            Model model,
            Path projectDir,
            ModelBuilderRequest request,
            ModelProblemCollector problems,
            String expression) {
        String value = doCallback(model, projectDir, request, problems, expression);
        if (value != null) {
            // value = postProcess(projectDir, request, expression, value);
        }
        return value;
    }

    private String postProcess(Path projectDir, ModelBuilderRequest request, String expression, String value) {
        // path translation
        String exp = unprefix(expression, getProjectPrefixes(request));
        if (TRANSLATED_PATH_EXPRESSIONS.contains(exp)) {
            value = pathTranslator.alignToBaseDirectory(value, projectDir);
        }
        // normalize url
        if (URL_EXPRESSIONS.contains(expression)) {
            value = urlNormalizer.normalize(value);
        }
        return value;
    }

    private String unprefix(String expression, List<String> prefixes) {
        for (String prefix : prefixes) {
            if (expression.startsWith(prefix)) {
                return expression.substring(prefix.length());
            }
        }
        return expression;
    }

    String doCallback(
            Model model,
            Path projectDir,
            ModelBuilderRequest request,
            ModelProblemCollector problems,
            String expression) {
        // basedir (the prefixed combos are handled below)
        if ("basedir".equals(expression)) {
            return projectProperty(model, projectDir, expression, false);
        }
        // timestamp
        if ("build.timestamp".equals(expression) || "maven.build.timestamp".equals(expression)) {
            return new MavenBuildTimestamp(request.getSession().getStartTime(), model.getProperties())
                    .formattedTimestamp();
        }
        // prefixed model reflection
        for (String prefix : getProjectPrefixes(request)) {
            if (expression.startsWith(prefix)) {
                String subExpr = expression.substring(prefix.length());
                String v = projectProperty(model, projectDir, subExpr, true);
                if (v != null) {
                    return v;
                }
            }
        }
        // user properties
        String value = request.getUserProperties().get(expression);
        // model properties
        if (value == null) {
            value = model.getProperties().get(expression);
        }
        // system properties
        if (value == null) {
            value = request.getSystemProperties().get(expression);
        }
        // environment variables
        if (value == null) {
            value = request.getSystemProperties().get("env." + expression);
        }
        // un-prefixed model reflection
        if (value == null) {
            value = projectProperty(model, projectDir, expression, false);
        }
        return value;
    }

    String projectProperty(Model model, Path projectDir, String subExpr, boolean prefixed) {
        if (projectDir != null) {
            if (subExpr.equals("basedir")) {
                return projectDir.toAbsolutePath().toString();
            } else if (subExpr.startsWith("basedir.")) {
                try {
                    Object value = ReflectionValueExtractor.evaluate(subExpr, projectDir.toAbsolutePath(), true);
                    if (value != null) {
                        return value.toString();
                    }
                } catch (Exception e) {
                    // addFeedback("Failed to extract \'" + expression + "\' from: " + root, e);
                }
            } else if (prefixed && subExpr.equals("baseUri")) {
                return projectDir.toAbsolutePath().toUri().toASCIIString();
            } else if (prefixed && subExpr.startsWith("baseUri.")) {
                try {
                    Object value = ReflectionValueExtractor.evaluate(
                            subExpr, projectDir.toAbsolutePath().toUri(), true);
                    if (value != null) {
                        return value.toString();
                    }
                } catch (Exception e) {
                    // addFeedback("Failed to extract \'" + expression + "\' from: " + root, e);
                }
            } else if (prefixed && subExpr.equals("rootDirectory")) {
                return rootLocator.findMandatoryRoot(projectDir).toString();
            } else if (prefixed && subExpr.startsWith("rootDirectory.")) {
                try {
                    Object value =
                            ReflectionValueExtractor.evaluate(subExpr, rootLocator.findMandatoryRoot(projectDir), true);
                    if (value != null) {
                        return value.toString();
                    }
                } catch (Exception e) {
                    // addFeedback("Failed to extract \'" + expression + "\' from: " + root, e);
                }
            }
        }
        try {
            Object value = ReflectionValueExtractor.evaluate(subExpr, model, false);
            if (value != null) {
                return value.toString();
            }
        } catch (Exception e) {
            // addFeedback("Failed to extract \'" + expression + "\' from: " + root, e);
        }
        return null;
    }
}
