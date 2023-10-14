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
package org.apache.maven.interpolation;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.io.File;

import org.apache.maven.api.model.Model;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.interpolation.StringVisitorModelInterpolator;
import org.apache.maven.model.path.PathTranslator;
import org.apache.maven.model.path.UrlNormalizer;
import org.apache.maven.model.root.RootLocator;

import static java.util.Collections.singletonList;

/**
 * Add support for implicit local location of artifacts for pom reference.
 * <p>
 * Mainly enables to make the interpolator resolver aware.
 */
@Named
@Singleton
public class ExtendedStringVisitorModelInterpolator extends StringVisitorModelInterpolator {
    private final ThreadLocal<Model.Builder> model = new ThreadLocal<>();

    @Inject
    public ExtendedStringVisitorModelInterpolator(
            final PathTranslator pathTranslator,
            final UrlNormalizer urlNormalizer,
            final RootLocator rootLocator,
            final ResolverValueSource resolverValueSource) {
        super(pathTranslator, urlNormalizer, rootLocator, singletonList(resolverValueSource));
        resolverValueSource.setModelProvider(() -> model.get().build());
    }

    @Override
    protected void beforeTransform(final Model.Builder builder, final Model target) {
        model.set(builder);
    }

    @Override
    public Model interpolateModel(
            final Model model,
            final File projectDir,
            final ModelBuildingRequest config,
            final ModelProblemCollector problems) {
        try {
            return super.interpolateModel(model, projectDir, config, problems);
        } finally {
            this.model.remove();
        }
    }
}
