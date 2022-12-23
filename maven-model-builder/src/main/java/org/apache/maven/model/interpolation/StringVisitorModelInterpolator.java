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
import javax.inject.Named;
import javax.inject.Singleton;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.api.model.Model;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelProblem.Severity;
import org.apache.maven.model.building.ModelProblem.Version;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.building.ModelProblemCollectorRequest;
import org.apache.maven.model.path.PathTranslator;
import org.apache.maven.model.path.UrlNormalizer;
import org.apache.maven.model.v4.MavenTransformer;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.InterpolationPostProcessor;
import org.codehaus.plexus.interpolation.RecursionInterceptor;
import org.codehaus.plexus.interpolation.StringSearchInterpolator;
import org.codehaus.plexus.interpolation.ValueSource;

/**
 * StringVisitorModelInterpolator
 *
 * @since 3.6.2
 */
@Named
@Singleton
public class StringVisitorModelInterpolator extends AbstractStringBasedModelInterpolator {
    @Inject
    public StringVisitorModelInterpolator(PathTranslator pathTranslator, UrlNormalizer urlNormalizer) {
        super(pathTranslator, urlNormalizer);
    }

    interface InnerInterpolator {
        String interpolate(String value);
    }

    @Override
    public Model interpolateModel(
            Model model, File projectDir, ModelBuildingRequest config, ModelProblemCollector problems) {
        List<? extends ValueSource> valueSources = createValueSources(model, projectDir, config);
        List<? extends InterpolationPostProcessor> postProcessors = createPostProcessors(model, projectDir, config);

        InnerInterpolator innerInterpolator = createInterpolator(valueSources, postProcessors, problems);

        return new MavenTransformer(innerInterpolator::interpolate).visit(model);
    }

    private InnerInterpolator createInterpolator(
            List<? extends ValueSource> valueSources,
            List<? extends InterpolationPostProcessor> postProcessors,
            final ModelProblemCollector problems) {
        final Map<String, String> cache = new HashMap<>();
        final StringSearchInterpolator interpolator = new StringSearchInterpolator();
        interpolator.setCacheAnswers(true);
        for (ValueSource vs : valueSources) {
            interpolator.addValueSource(vs);
        }
        for (InterpolationPostProcessor postProcessor : postProcessors) {
            interpolator.addPostProcessor(postProcessor);
        }
        final RecursionInterceptor recursionInterceptor = createRecursionInterceptor();
        return value -> {
            if (value != null && value.contains("${")) {
                String c = cache.get(value);
                if (c == null) {
                    try {
                        c = interpolator.interpolate(value, recursionInterceptor);
                    } catch (InterpolationException e) {
                        problems.add(new ModelProblemCollectorRequest(Severity.ERROR, Version.BASE)
                                .setMessage(e.getMessage())
                                .setException(e));
                    }
                    cache.put(value, c);
                }
                return c;
            }
            return value;
        };
    }
}
