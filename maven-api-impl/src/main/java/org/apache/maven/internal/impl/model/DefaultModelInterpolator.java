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

import java.net.URI;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.api.di.Inject;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.services.BuilderProblem;
import org.apache.maven.api.services.ModelBuilderRequest;
import org.apache.maven.api.services.ModelProblem;
import org.apache.maven.api.services.ModelProblemCollector;
import org.apache.maven.api.services.model.ModelInterpolator;
import org.apache.maven.api.services.model.PathTranslator;
import org.apache.maven.api.services.model.RootLocator;
import org.apache.maven.api.services.model.UrlNormalizer;
import org.apache.maven.model.v4.MavenTransformer;
import org.codehaus.plexus.interpolation.AbstractDelegatingValueSource;
import org.codehaus.plexus.interpolation.AbstractValueSource;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.InterpolationPostProcessor;
import org.codehaus.plexus.interpolation.MapBasedValueSource;
import org.codehaus.plexus.interpolation.PrefixAwareRecursionInterceptor;
import org.codehaus.plexus.interpolation.PrefixedValueSourceWrapper;
import org.codehaus.plexus.interpolation.QueryEnabledValueSource;
import org.codehaus.plexus.interpolation.RecursionInterceptor;
import org.codehaus.plexus.interpolation.StringSearchInterpolator;
import org.codehaus.plexus.interpolation.ValueSource;
import org.codehaus.plexus.interpolation.reflection.ReflectionValueExtractor;
import org.codehaus.plexus.interpolation.util.ValueSourceUtils;

@Named
@Singleton
public class DefaultModelInterpolator implements ModelInterpolator {

    private static final String PREFIX_PROJECT = "project.";
    private static final String PREFIX_POM = "pom.";
    private static final List<String> PROJECT_PREFIXES_3_1 = Arrays.asList(PREFIX_POM, PREFIX_PROJECT);
    private static final List<String> PROJECT_PREFIXES_4_0 = Collections.singletonList(PREFIX_PROJECT);

    private static final Collection<String> TRANSLATED_PATH_EXPRESSIONS;

    static {
        Collection<String> translatedPrefixes = new HashSet<>();

        // MNG-1927, MNG-2124, MNG-3355:
        // If the build section is present and the project directory is non-null, we should make
        // sure interpolation of the directories below uses translated paths.
        // Afterward, we'll double back and translate any paths that weren't covered during interpolation via the
        // code below...
        translatedPrefixes.add("build.directory");
        translatedPrefixes.add("build.outputDirectory");
        translatedPrefixes.add("build.testOutputDirectory");
        translatedPrefixes.add("build.sourceDirectory");
        translatedPrefixes.add("build.testSourceDirectory");
        translatedPrefixes.add("build.scriptSourceDirectory");
        translatedPrefixes.add("reporting.outputDirectory");

        TRANSLATED_PATH_EXPRESSIONS = translatedPrefixes;
    }

    private final PathTranslator pathTranslator;
    private final UrlNormalizer urlNormalizer;
    private final RootLocator rootLocator;

    @Inject
    public DefaultModelInterpolator(
            PathTranslator pathTranslator, UrlNormalizer urlNormalizer, RootLocator rootLocator) {
        this.pathTranslator = pathTranslator;
        this.urlNormalizer = urlNormalizer;
        this.rootLocator = rootLocator;
    }

    interface InnerInterpolator {
        String interpolate(String value);
    }

    @Override
    public Model interpolateModel(
            Model model, Path projectDir, ModelBuilderRequest request, ModelProblemCollector problems) {
        List<? extends ValueSource> valueSources = createValueSources(model, projectDir, request, problems);
        List<? extends InterpolationPostProcessor> postProcessors = createPostProcessors(model, projectDir, request);

        InnerInterpolator innerInterpolator = createInterpolator(valueSources, postProcessors, request, problems);

        return new MavenTransformer(innerInterpolator::interpolate).visit(model);
    }

    private InnerInterpolator createInterpolator(
            List<? extends ValueSource> valueSources,
            List<? extends InterpolationPostProcessor> postProcessors,
            ModelBuilderRequest request,
            ModelProblemCollector problems) {
        Map<String, String> cache = new HashMap<>();
        StringSearchInterpolator interpolator = new StringSearchInterpolator();
        interpolator.setCacheAnswers(true);
        for (ValueSource vs : valueSources) {
            interpolator.addValueSource(vs);
        }
        for (InterpolationPostProcessor postProcessor : postProcessors) {
            interpolator.addPostProcessor(postProcessor);
        }
        RecursionInterceptor recursionInterceptor = createRecursionInterceptor(request);
        return value -> {
            if (value != null && value.contains("${")) {
                String c = cache.get(value);
                if (c == null) {
                    try {
                        c = interpolator.interpolate(value, recursionInterceptor);
                    } catch (InterpolationException e) {
                        problems.add(BuilderProblem.Severity.ERROR, ModelProblem.Version.BASE, e.getMessage(), e);
                    }
                    cache.put(value, c);
                }
                return c;
            }
            return value;
        };
    }

    protected List<String> getProjectPrefixes(ModelBuilderRequest request) {
        return request.getRequestType() == ModelBuilderRequest.RequestType.BUILD_POM
                ? PROJECT_PREFIXES_4_0
                : PROJECT_PREFIXES_3_1;
    }

    protected List<ValueSource> createValueSources(
            Model model, Path projectDir, ModelBuilderRequest request, ModelProblemCollector problems) {
        Map<String, String> modelProperties = model.getProperties();

        ValueSource projectPrefixValueSource;
        ValueSource prefixlessObjectBasedValueSource;
        if (request.getRequestType() == ModelBuilderRequest.RequestType.BUILD_POM) {
            projectPrefixValueSource = new PrefixedObjectValueSource(PROJECT_PREFIXES_4_0, model, false);
            prefixlessObjectBasedValueSource = new ObjectBasedValueSource(model);
        } else {
            projectPrefixValueSource = new PrefixedObjectValueSource(PROJECT_PREFIXES_3_1, model, false);
            projectPrefixValueSource =
                    new ProblemDetectingValueSource(projectPrefixValueSource, PREFIX_POM, PREFIX_PROJECT, problems);

            prefixlessObjectBasedValueSource = new ObjectBasedValueSource(model);
            prefixlessObjectBasedValueSource =
                    new ProblemDetectingValueSource(prefixlessObjectBasedValueSource, "", PREFIX_PROJECT, problems);
        }

        // NOTE: Order counts here!
        List<ValueSource> valueSources = new ArrayList<>(9);

        if (projectDir != null) {
            ValueSource basedirValueSource = new PrefixedValueSourceWrapper(
                    new AbstractValueSource(false) {
                        @Override
                        public Object getValue(String expression) {
                            if ("basedir".equals(expression)) {
                                return projectDir.toAbsolutePath().toString();
                            } else if (expression.startsWith("basedir.")) {
                                Path basedir = projectDir.toAbsolutePath();
                                return new ObjectBasedValueSource(basedir)
                                        .getValue(expression.substring("basedir.".length()));
                            }
                            return null;
                        }
                    },
                    getProjectPrefixes(request),
                    true);
            valueSources.add(basedirValueSource);

            ValueSource baseUriValueSource = new PrefixedValueSourceWrapper(
                    new AbstractValueSource(false) {
                        @Override
                        public Object getValue(String expression) {
                            if ("baseUri".equals(expression)) {
                                return projectDir.toAbsolutePath().toUri().toASCIIString();
                            } else if (expression.startsWith("baseUri.")) {
                                URI baseUri = projectDir.toAbsolutePath().toUri();
                                return new ObjectBasedValueSource(baseUri)
                                        .getValue(expression.substring("baseUri.".length()));
                            }
                            return null;
                        }
                    },
                    getProjectPrefixes(request),
                    false);
            valueSources.add(baseUriValueSource);
            valueSources.add(new BuildTimestampValueSource(request.getSession().getStartTime(), modelProperties));
        }

        valueSources.add(new PrefixedValueSourceWrapper(
                new AbstractValueSource(false) {
                    @Override
                    public Object getValue(String expression) {
                        if ("rootDirectory".equals(expression)) {
                            Path root = rootLocator.findMandatoryRoot(projectDir);
                            return root.toFile().getPath();
                        } else if (expression.startsWith("rootDirectory.")) {
                            Path root = rootLocator.findMandatoryRoot(projectDir);
                            return new ObjectBasedValueSource(root)
                                    .getValue(expression.substring("rootDirectory.".length()));
                        }
                        return null;
                    }
                },
                getProjectPrefixes(request)));

        valueSources.add(projectPrefixValueSource);

        valueSources.add(new MapBasedValueSource(request.getUserProperties()));

        valueSources.add(new MapBasedValueSource(modelProperties));

        valueSources.add(new MapBasedValueSource(request.getSystemProperties()));

        valueSources.add(new AbstractValueSource(false) {
            @Override
            public Object getValue(String expression) {
                return request.getSystemProperties().get("env." + expression);
            }
        });

        valueSources.add(prefixlessObjectBasedValueSource);

        return valueSources;
    }

    protected List<? extends InterpolationPostProcessor> createPostProcessors(
            Model model, Path projectDir, ModelBuilderRequest request) {
        List<InterpolationPostProcessor> processors = new ArrayList<>(2);
        if (projectDir != null) {
            processors.add(new PathTranslatingPostProcessor(
                    getProjectPrefixes(request), TRANSLATED_PATH_EXPRESSIONS, projectDir, pathTranslator));
        }
        processors.add(new UrlNormalizingPostProcessor(urlNormalizer));
        return processors;
    }

    protected RecursionInterceptor createRecursionInterceptor(ModelBuilderRequest request) {
        return new PrefixAwareRecursionInterceptor(getProjectPrefixes(request));
    }

    static class PathTranslatingPostProcessor implements InterpolationPostProcessor {

        private final Collection<String> unprefixedPathKeys;
        private final Path projectDir;
        private final PathTranslator pathTranslator;
        private final List<String> expressionPrefixes;

        PathTranslatingPostProcessor(
                List<String> expressionPrefixes,
                Collection<String> unprefixedPathKeys,
                Path projectDir,
                PathTranslator pathTranslator) {
            this.expressionPrefixes = expressionPrefixes;
            this.unprefixedPathKeys = unprefixedPathKeys;
            this.projectDir = projectDir;
            this.pathTranslator = pathTranslator;
        }

        @Override
        public Object execute(String expression, Object value) {
            if (value != null) {
                expression = ValueSourceUtils.trimPrefix(expression, expressionPrefixes, true);
                if (unprefixedPathKeys.contains(expression)) {
                    return pathTranslator.alignToBaseDirectory(String.valueOf(value), projectDir);
                }
            }
            return null;
        }
    }

    /**
     * Ensures that expressions referring to URLs evaluate to normalized URLs.
     *
     */
    static class UrlNormalizingPostProcessor implements InterpolationPostProcessor {

        private static final Set<String> URL_EXPRESSIONS;

        static {
            Set<String> expressions = new HashSet<>();
            expressions.add("project.url");
            expressions.add("project.scm.url");
            expressions.add("project.scm.connection");
            expressions.add("project.scm.developerConnection");
            expressions.add("project.distributionManagement.site.url");
            URL_EXPRESSIONS = expressions;
        }

        private final UrlNormalizer normalizer;

        UrlNormalizingPostProcessor(UrlNormalizer normalizer) {
            this.normalizer = normalizer;
        }

        @Override
        public Object execute(String expression, Object value) {
            if (value != null && URL_EXPRESSIONS.contains(expression)) {
                return normalizer.normalize(value.toString());
            }

            return null;
        }
    }

    /**
     * Wraps an arbitrary object with an {@link ObjectBasedValueSource} instance, then
     * wraps that source with a {@link PrefixedValueSourceWrapper} instance, to which
     * this class delegates all of its calls.
     */
    public static class PrefixedObjectValueSource extends AbstractDelegatingValueSource
            implements QueryEnabledValueSource {

        /**
         * Wrap the specified root object, allowing the specified expression prefix.
         * @param prefix the prefix.
         * @param root the root of the graph.
         */
        public PrefixedObjectValueSource(String prefix, Object root) {
            super(new PrefixedValueSourceWrapper(new ObjectBasedValueSource(root), prefix));
        }

        /**
         * Wrap the specified root object, allowing the specified list of expression
         * prefixes and setting whether the {@link PrefixedValueSourceWrapper} allows
         * unprefixed expressions.
         * @param possiblePrefixes The possible prefixes.
         * @param root The root of the graph.
         * @param allowUnprefixedExpressions if we allow undefined expressions or not.
         */
        public PrefixedObjectValueSource(
                List<String> possiblePrefixes, Object root, boolean allowUnprefixedExpressions) {
            super(new PrefixedValueSourceWrapper(
                    new ObjectBasedValueSource(root), possiblePrefixes, allowUnprefixedExpressions));
        }

        /**
         * {@inheritDoc}
         */
        public String getLastExpression() {
            return ((QueryEnabledValueSource) getDelegate()).getLastExpression();
        }
    }

    /**
     * Wraps an object, providing reflective access to the object graph of which the
     * supplied object is the root. Expressions like 'child.name' will translate into
     * 'rootObject.getChild().getName()' for non-boolean properties, and
     * 'rootObject.getChild().isName()' for boolean properties.
     */
    public static class ObjectBasedValueSource extends AbstractValueSource {

        private final Object root;

        /**
         * Construct a new value source, using the supplied object as the root from
         * which to start, and using expressions split at the dot ('.') to navigate
         * the object graph beneath this root.
         * @param root the root of the graph.
         */
        public ObjectBasedValueSource(Object root) {
            super(true);
            this.root = root;
        }

        /**
         * <p>Split the expression into parts, tokenized on the dot ('.') character. Then,
         * starting at the root object contained in this value source, apply each part
         * to the object graph below this root, using either 'getXXX()' or 'isXXX()'
         * accessor types to resolve the value for each successive expression part.
         * Finally, return the result of the last expression part's resolution.</p>
         *
         * <p><b>NOTE:</b> The object-graph nagivation actually takes place via the
         * {@link ReflectionValueExtractor} class.</p>
         */
        public Object getValue(String expression) {
            if (expression == null || expression.trim().isEmpty()) {
                return null;
            }

            try {
                return ReflectionValueExtractor.evaluate(expression, root, false);
            } catch (Exception e) {
                addFeedback("Failed to extract \'" + expression + "\' from: " + root, e);
            }

            return null;
        }
    }

    /**
     * Wraps another value source and intercepts interpolated expressions, checking for problems.
     *
     */
    static class ProblemDetectingValueSource implements ValueSource {

        private final ValueSource valueSource;

        private final String bannedPrefix;

        private final String newPrefix;

        private final ModelProblemCollector problems;

        ProblemDetectingValueSource(
                ValueSource valueSource, String bannedPrefix, String newPrefix, ModelProblemCollector problems) {
            this.valueSource = valueSource;
            this.bannedPrefix = bannedPrefix;
            this.newPrefix = newPrefix;
            this.problems = problems;
        }

        @Override
        public Object getValue(String expression) {
            Object value = valueSource.getValue(expression);

            if (value != null && expression.startsWith(bannedPrefix)) {
                String msg = "The expression ${" + expression + "} is deprecated.";
                if (newPrefix != null && !newPrefix.isEmpty()) {
                    msg += " Please use ${" + newPrefix + expression.substring(bannedPrefix.length()) + "} instead.";
                }
                problems.add(BuilderProblem.Severity.WARNING, ModelProblem.Version.V20, msg);
            }

            return value;
        }

        @Override
        public List getFeedback() {
            return valueSource.getFeedback();
        }

        @Override
        public void clearFeedback() {
            valueSource.clearFeedback();
        }
    }

    static class BuildTimestampValueSource extends AbstractValueSource {
        private final Instant startTime;
        private final Map<String, String> properties;

        BuildTimestampValueSource(Instant startTime, Map<String, String> properties) {
            super(false);
            this.startTime = startTime;
            this.properties = properties;
        }

        @Override
        public Object getValue(String expression) {
            if ("build.timestamp".equals(expression) || "maven.build.timestamp".equals(expression)) {
                return new MavenBuildTimestamp(startTime, properties).formattedTimestamp();
            }
            return null;
        }
    }
}
