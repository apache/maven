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

import org.apache.maven.api.di.Inject;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.model.ActivationFile;
import org.apache.maven.api.services.Interpolator;
import org.apache.maven.api.services.InterpolatorException;
import org.apache.maven.api.services.model.PathTranslator;
import org.apache.maven.api.services.model.ProfileActivationContext;
import org.apache.maven.api.services.model.RootLocator;

/**
 * Finds an absolute path for {@link ActivationFile#getExists()} or {@link ActivationFile#getMissing()}
 */
@Named
@Singleton
public class ProfileActivationFilePathInterpolator {

    private final PathTranslator pathTranslator;

    private final RootLocator rootLocator;

    private final Interpolator interpolator;

    @Inject
    public ProfileActivationFilePathInterpolator(
            PathTranslator pathTranslator, RootLocator rootLocator, Interpolator interpolator) {
        this.pathTranslator = pathTranslator;
        this.rootLocator = rootLocator;
        this.interpolator = interpolator;
    }

    /**
     * Interpolates given {@code path}.
     *
     * @return absolute path or {@code null} if the input was {@code null}
     */
    public String interpolate(String path, ProfileActivationContext context) throws InterpolatorException {
        if (path == null) {
            return null;
        }

        Path basedir = context.getModel().getProjectDirectory();

        String absolutePath = interpolator.interpolate(path, s -> {
            if ("basedir".equals(s) || "project.basedir".equals(s)) {
                return basedir != null ? basedir.toFile().getAbsolutePath() : null;
            }
            if ("project.rootDirectory".equals(s)) {
                Path root = rootLocator.findMandatoryRoot(basedir);
                return root.toFile().getAbsolutePath();
            }
            String r = context.getModel().getProperties().get(s);
            if (r == null) {
                r = context.getUserProperties().get(s);
            }
            if (r == null) {
                r = context.getSystemProperties().get(s);
            }
            return r;
        });

        return pathTranslator.alignToBaseDirectory(absolutePath, basedir);
    }
}
