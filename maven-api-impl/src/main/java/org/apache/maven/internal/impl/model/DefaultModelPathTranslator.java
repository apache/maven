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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.apache.maven.api.di.Inject;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.model.Build;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.Reporting;
import org.apache.maven.api.model.Resource;
import org.apache.maven.api.services.ModelBuilderRequest;
import org.apache.maven.api.services.model.ModelPathTranslator;
import org.apache.maven.api.services.model.PathTranslator;

/**
 * Resolves relative paths within a model against a specific base directory.
 *
 */
@Named
@Singleton
public class DefaultModelPathTranslator implements ModelPathTranslator {

    private final PathTranslator pathTranslator;

    @Inject
    public DefaultModelPathTranslator(PathTranslator pathTranslator) {
        this.pathTranslator = pathTranslator;
    }

    @Override
    public Model alignToBaseDirectory(Model model, Path basedir, ModelBuilderRequest request) {
        if (model == null || basedir == null) {
            return model;
        }

        Build build = model.getBuild();
        Build newBuild = null;
        if (build != null) {
            newBuild = Build.newBuilder(build)
                    .directory(alignToBaseDirectory(build.getDirectory(), basedir))
                    .sourceDirectory(alignToBaseDirectory(build.getSourceDirectory(), basedir))
                    .testSourceDirectory(alignToBaseDirectory(build.getTestSourceDirectory(), basedir))
                    .scriptSourceDirectory(alignToBaseDirectory(build.getScriptSourceDirectory(), basedir))
                    .resources(map(build.getResources(), r -> alignToBaseDirectory(r, basedir)))
                    .testResources(map(build.getTestResources(), r -> alignToBaseDirectory(r, basedir)))
                    .filters(map(build.getFilters(), s -> alignToBaseDirectory(s, basedir)))
                    .outputDirectory(alignToBaseDirectory(build.getOutputDirectory(), basedir))
                    .testOutputDirectory(alignToBaseDirectory(build.getTestOutputDirectory(), basedir))
                    .build();
        }

        Reporting reporting = model.getReporting();
        Reporting newReporting = null;
        if (reporting != null) {
            newReporting = Reporting.newBuilder(reporting)
                    .outputDirectory(alignToBaseDirectory(reporting.getOutputDirectory(), basedir))
                    .build();
        }
        if (newBuild != build || newReporting != reporting) {
            model = Model.newBuilder(model)
                    .build(newBuild)
                    .reporting(newReporting)
                    .build();
        }
        return model;
    }

    private <T> List<T> map(List<T> resources, Function<T, T> mapper) {
        List<T> newResources = null;
        if (resources != null) {
            for (int i = 0; i < resources.size(); i++) {
                T resource = resources.get(i);
                T newResource = mapper.apply(resource);
                if (newResource != null) {
                    if (newResources == null) {
                        newResources = new ArrayList<>(resources);
                    }
                    newResources.set(i, newResource);
                }
            }
        }
        return newResources;
    }

    private Resource alignToBaseDirectory(Resource resource, Path basedir) {
        if (resource != null) {
            String newDir = mayAlignToBaseDirectoryOrNull(resource.getDirectory(), basedir);
            if (newDir != null) {
                return resource.withDirectory(newDir);
            }
        }
        return resource;
    }

    private String alignToBaseDirectory(String path, Path basedir) {
        String newPath = mayAlignToBaseDirectoryOrNull(path, basedir);
        if (newPath != null) {
            return newPath;
        }
        return path;
    }

    /**
     * Returns aligned path or {@code null} if no need for change.
     */
    private String mayAlignToBaseDirectoryOrNull(String path, Path basedir) {
        String newPath = pathTranslator.alignToBaseDirectory(path, basedir);
        return Objects.equals(path, newPath) ? null : newPath;
    }
}
