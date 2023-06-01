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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.maven.api.model.Model;
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
import org.codehaus.plexus.interpolation.ValueSource;

/**
 * Use a regular expression search to find and resolve expressions within the POM.
 *
 * @author jdcasey Created on Feb 3, 2005
 */
public abstract class AbstractStringBasedModelInterpolator implements ModelInterpolator {
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
    public AbstractStringBasedModelInterpolator(
            PathTranslator pathTranslator, UrlNormalizer urlNormalizer, RootLocator rootLocator) {
        this.pathTranslator = pathTranslator;
        this.urlNormalizer = urlNormalizer;
        this.rootLocator = rootLocator;
    }

    @Override
    public org.apache.maven.model.Model interpolateModel(
            org.apache.maven.model.Model model,
            File projectDir,
            ModelBuildingRequest request,
            ModelProblemCollector problems) {
        return new org.apache.maven.model.Model(interpolateModel(model.getDelegate(), projectDir, request, problems));
    }

    protected List<String> getProjectPrefixes(ModelBuildingRequest config) {
        return config.getValidationLevel() >= ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_4_0
                ? PROJECT_PREFIXES_4_0
                : PROJECT_PREFIXES_3_1;
    }

    protected List<ValueSource> createValueSources(
            final Model model,
            final File projectDir,
            final ModelBuildingRequest config,
            ModelProblemCollector problems) {
        Map<String, String> modelProperties = model.getProperties();

        ValueSource projectPrefixValueSource;
        ValueSource prefixlessObjectBasedValueSource;
        if (config.getValidationLevel() >= ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_4_0) {
            projectPrefixValueSource = new PrefixedObjectValueSource(PROJECT_PREFIXES_4_0, model, false);
            prefixlessObjectBasedValueSource = new ObjectBasedValueSource(model);
        } else {
            projectPrefixValueSource = new PrefixedObjectValueSource(PROJECT_PREFIXES_3_1, model, false);
            if (config.getValidationLevel() >= ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_2_0) {
                projectPrefixValueSource =
                        new ProblemDetectingValueSource(projectPrefixValueSource, PREFIX_POM, PREFIX_PROJECT, problems);
            }

            prefixlessObjectBasedValueSource = new ObjectBasedValueSource(model);
            if (config.getValidationLevel() >= ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_2_0) {
                prefixlessObjectBasedValueSource =
                        new ProblemDetectingValueSource(prefixlessObjectBasedValueSource, "", PREFIX_PROJECT, problems);
            }
        }

        // NOTE: Order counts here!
        List<ValueSource> valueSources = new ArrayList<>(9);

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
                    getProjectPrefixes(config),
                    true);
            valueSources.add(basedirValueSource);

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
                    getProjectPrefixes(config),
                    false);
            valueSources.add(baseUriValueSource);
            valueSources.add(new BuildTimestampValueSource(config.getBuildStartTime(), modelProperties));
        }

        valueSources.add(new PrefixedValueSourceWrapper(
                new AbstractValueSource(false) {
                    @Override
                    public Object getValue(String expression) {
                        if ("rootDirectory".equals(expression)) {
                            Path base = projectDir != null ? projectDir.toPath() : null;
                            Path root = rootLocator.findMandatoryRoot(base);
                            return root.toFile().getPath();
                        }
                        return null;
                    }
                },
                getProjectPrefixes(config)));

        valueSources.add(projectPrefixValueSource);

        valueSources.add(new MapBasedValueSource(config.getUserProperties()));

        valueSources.add(new MapBasedValueSource(modelProperties));

        valueSources.add(new MapBasedValueSource(config.getSystemProperties()));

        valueSources.add(new AbstractValueSource(false) {
            @Override
            public Object getValue(String expression) {
                return config.getSystemProperties().getProperty("env." + expression);
            }
        });

        valueSources.add(prefixlessObjectBasedValueSource);

        return valueSources;
    }

    protected List<? extends InterpolationPostProcessor> createPostProcessors(
            final Model model, final File projectDir, final ModelBuildingRequest config) {
        List<InterpolationPostProcessor> processors = new ArrayList<>(2);
        if (projectDir != null) {
            processors.add(new PathTranslatingPostProcessor(
                    getProjectPrefixes(config), TRANSLATED_PATH_EXPRESSIONS, projectDir, pathTranslator));
        }
        processors.add(new UrlNormalizingPostProcessor(urlNormalizer));
        return processors;
    }

    protected RecursionInterceptor createRecursionInterceptor(ModelBuildingRequest config) {
        return new PrefixAwareRecursionInterceptor(getProjectPrefixes(config));
    }
}
