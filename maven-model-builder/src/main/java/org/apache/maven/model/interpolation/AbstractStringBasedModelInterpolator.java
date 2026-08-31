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
package org.apache.maven.model.interpolation;

import javax.inject.Inject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

import org.apache.maven.model.Model;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.path.PathTranslator;
import org.apache.maven.model.path.UrlNormalizer;
import org.apache.maven.model.root.RootLocator;
import org.codehaus.plexus.interpolation.AbstractValueSource;
import org.codehaus.plexus.interpolation.InterpolationPostProcessor;
import org.codehaus.plexus.interpolation.MapBasedValueSource;
import org.codehaus.plexus.interpolation.ObjectBasedValueSource;
import org.codehaus.plexus.interpolation.PrefixAwareRecursionInterceptor;
import org.codehaus.plexus.interpolation.PrefixedObjectValueSource;
import org.codehaus.plexus.interpolation.PrefixedValueSourceWrapper;
import org.codehaus.plexus.interpolation.RecursionInterceptor;
import org.codehaus.plexus.interpolation.SingleResponseValueSource;
import org.codehaus.plexus.interpolation.ValueSource;

/**
 * Use a regular expression search to find and resolve expressions within the POM.
 *
 * @author jdcasey Created on Feb 3, 2005
 */
public abstract class AbstractStringBasedModelInterpolator implements ModelInterpolator {

    /**
     * User property for opting back into the previous behavior of interpolating
     * repository-resolved models (built at {@link ModelBuildingRequest#VALIDATION_LEVEL_MINIMAL})
     * against the full set of session properties (system, environment and CLI).
     * When set to {@code "false"} (default), such models are interpolated only against
     * their own {@code <properties>}, preventing property leaking from the requesting
     * build into transitive POMs. When set to {@code "true"}, full interpolation is
     * applied as in previous Maven versions.
     * <p>
     * In Maven 4.x this constant is promoted to
     * {@code org.apache.maven.api.Constants.MAVEN_MODEL_DEPENDENCY_INTERPOLATION_FULL}
     * with {@code @Config} so it appears in the auto-generated configuration documentation.
     */
    public static final String FULL_EXTERNAL_INTERPOLATION_PROPERTY = "maven.model.dependencyInterpolation.full";

    private static final List<String> PROJECT_PREFIXES = Arrays.asList("pom.", "project.");

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

    @Inject
    private PathTranslator pathTranslator;

    @Inject
    private UrlNormalizer urlNormalizer;

    @Inject
    private ModelVersionProcessor versionProcessor;

    @Inject
    private RootLocator rootLocator;

    public AbstractStringBasedModelInterpolator setPathTranslator(PathTranslator pathTranslator) {
        this.pathTranslator = pathTranslator;
        return this;
    }

    public AbstractStringBasedModelInterpolator setUrlNormalizer(UrlNormalizer urlNormalizer) {
        this.urlNormalizer = urlNormalizer;
        return this;
    }

    public AbstractStringBasedModelInterpolator setVersionPropertiesProcessor(ModelVersionProcessor processor) {
        this.versionProcessor = processor;
        return this;
    }

    public AbstractStringBasedModelInterpolator setRootLocator(RootLocator rootLocator) {
        this.rootLocator = rootLocator;
        return this;
    }

    protected List<ValueSource> createValueSources(
            final Model model,
            final File projectDir,
            final ModelBuildingRequest config,
            final ModelProblemCollector problems) {
        Properties modelProperties = model.getProperties();

        ValueSource modelValueSource1 = new PrefixedObjectValueSource(PROJECT_PREFIXES, model, false);
        if (config.getValidationLevel() >= ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_2_0) {
            modelValueSource1 = new ProblemDetectingValueSource(modelValueSource1, "pom.", "project.", problems);
        }

        ValueSource modelValueSource2 = new ObjectBasedValueSource(model);
        if (config.getValidationLevel() >= ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_2_0) {
            modelValueSource2 = new ProblemDetectingValueSource(modelValueSource2, "", "project.", problems);
        }

        // NOTE: Order counts here!
        List<ValueSource> valueSources = new ArrayList<>(11);

        if (projectDir != null) {
            ValueSource basedirValueSource = new PrefixedValueSourceWrapper(
                    new AbstractValueSource(false) {
                        @Override
                        public Object getValue(String expression) {
                            if ("basedir".equals(expression)) {
                                return projectDir.getAbsolutePath();
                            }
                            return null;
                        }
                    },
                    PROJECT_PREFIXES,
                    true);
            valueSources.add(basedirValueSource);

            ValueSource rootDirectoryValueSource = new PrefixedValueSourceWrapper(
                    new AbstractValueSource(false) {
                        @Override
                        public Object getValue(String expression) {
                            if ("rootDirectory".equals(expression)) {
                                return rootLocator
                                        .findMandatoryRoot(projectDir.toPath())
                                        .toAbsolutePath()
                                        .toString();
                            }
                            return null;
                        }
                    },
                    PROJECT_PREFIXES,
                    true);
            valueSources.add(rootDirectoryValueSource);

            ValueSource baseUriValueSource = new PrefixedValueSourceWrapper(
                    new AbstractValueSource(false) {
                        @Override
                        public Object getValue(String expression) {
                            if ("baseUri".equals(expression)) {
                                return projectDir
                                        .getAbsoluteFile()
                                        .toPath()
                                        .toUri()
                                        .toASCIIString();
                            }
                            return null;
                        }
                    },
                    PROJECT_PREFIXES,
                    false);
            valueSources.add(baseUriValueSource);
            valueSources.add(new BuildTimestampValueSource(config.getBuildStartTime(), modelProperties));
        }

        valueSources.add(modelValueSource1);

        // Models built at VALIDATION_LEVEL_MINIMAL are the models Maven builds while resolving
        // dependency, parent and BOM-import POMs from a repository, not the operator's own
        // project. Such models interpolate only against their own properties and a small set
        // of environment-independent expressions; everything else in the user/system property
        // space stays uninterpolated. Operator project builds use a higher validation level and
        // keep the full set of value sources, unchanged from previous behavior.
        boolean restricted = restrictExternalModelInterpolation(config);

        ValueSource userPropertiesValueSource = new MapBasedValueSource(config.getUserProperties());
        valueSources.add(restricted ? restrictToSafeExpressions(userPropertiesValueSource) : userPropertiesValueSource);

        // Overwrite existing values in model properties. Otherwise it's not possible
        // to define them via command line e.g.: mvn -Drevision=6.5.7 ...
        versionProcessor.overwriteModelProperties(modelProperties, config);
        valueSources.add(new MapBasedValueSource(modelProperties));

        ValueSource systemPropertiesValueSource = new MapBasedValueSource(config.getSystemProperties());
        valueSources.add(
                restricted ? restrictToSafeExpressions(systemPropertiesValueSource) : systemPropertiesValueSource);

        if (!restricted) {
            valueSources.add(new AbstractValueSource(false) {
                @Override
                public Object getValue(String expression) {
                    return config.getSystemProperties().getProperty("env." + expression);
                }
            });
        }

        valueSources.add(modelValueSource2);

        // last source: make sure Maven Repo Central is present (if is present anywhere else, it will prevail this one)
        valueSources.add(new SingleResponseValueSource(MAVEN_REPO_CENTRAL_KEY, DEFAULT_MAVEN_REPO_CENTRAL_URL));

        return valueSources;
    }

    private static boolean restrictExternalModelInterpolation(ModelBuildingRequest config) {
        return config.getValidationLevel() < ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_2_0
                && !Boolean.parseBoolean(config.getSystemProperties().getProperty(FULL_EXTERNAL_INTERPOLATION_PROPERTY))
                && !Boolean.parseBoolean(config.getUserProperties().getProperty(FULL_EXTERNAL_INTERPOLATION_PROPERTY));
    }

    private static ValueSource restrictToSafeExpressions(ValueSource source) {
        return new AbstractValueSource(false) {
            @Override
            public Object getValue(String expression) {
                return isSafeExternalExpression(expression) ? source.getValue(expression) : null;
            }
        };
    }

    /**
     * Expressions that models built at {@link ModelBuildingRequest#VALIDATION_LEVEL_MINIMAL}
     * may still resolve from the session properties: JVM- and Maven-defined properties, plus
     * the CI-friendly version properties (MNG-5895). All other expressions are left literal.
     */
    private static boolean isSafeExternalExpression(String expression) {
        return expression.startsWith("java.")
                || expression.startsWith("os.")
                || expression.startsWith("maven.")
                || "file.separator".equals(expression)
                || "path.separator".equals(expression)
                || "line.separator".equals(expression)
                || "revision".equals(expression)
                || "changelist".equals(expression)
                || "sha1".equals(expression);
    }

    protected List<? extends InterpolationPostProcessor> createPostProcessors(
            final Model model, final File projectDir, final ModelBuildingRequest config) {
        List<InterpolationPostProcessor> processors = new ArrayList<>(2);
        if (projectDir != null) {
            processors.add(new PathTranslatingPostProcessor(
                    PROJECT_PREFIXES, TRANSLATED_PATH_EXPRESSIONS,
                    projectDir, pathTranslator));
        }
        processors.add(new UrlNormalizingPostProcessor(urlNormalizer));
        return processors;
    }

    protected RecursionInterceptor createRecursionInterceptor() {
        return new PrefixAwareRecursionInterceptor(PROJECT_PREFIXES);
    }
}
