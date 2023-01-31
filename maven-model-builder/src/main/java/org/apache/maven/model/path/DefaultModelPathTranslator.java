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
package org.apache.maven.model.path;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Reporting;
import org.apache.maven.model.Resource;
import org.apache.maven.model.building.ModelBuildingRequest;

/**
 * Resolves relative paths within a model against a specific base directory.
 *
 * @author Benjamin Bentmann
 */
@Named
@Singleton
public class DefaultModelPathTranslator implements ModelPathTranslator {

    @Inject
    private PathTranslator pathTranslator;

    public DefaultModelPathTranslator setPathTranslator(PathTranslator pathTranslator) {
        this.pathTranslator = pathTranslator;
        return this;
    }

    @Override
    public void alignToBaseDirectory(Model model, File basedir, ModelBuildingRequest request) {
        if (model == null || basedir == null) {
            return;
        }

        Build build = model.getBuild();

        if (build != null) {
            build.setDirectory(alignToBaseDirectory(build.getDirectory(), basedir));

            build.setSourceDirectory(alignToBaseDirectory(build.getSourceDirectory(), basedir));

            build.setTestSourceDirectory(alignToBaseDirectory(build.getTestSourceDirectory(), basedir));

            build.setScriptSourceDirectory(alignToBaseDirectory(build.getScriptSourceDirectory(), basedir));

            for (Resource resource : build.getResources()) {
                resource.setDirectory(alignToBaseDirectory(resource.getDirectory(), basedir));
            }

            for (Resource resource : build.getTestResources()) {
                resource.setDirectory(alignToBaseDirectory(resource.getDirectory(), basedir));
            }

            if (build.getFilters() != null) {
                List<String> filters = new ArrayList<>(build.getFilters().size());
                for (String filter : build.getFilters()) {
                    filters.add(alignToBaseDirectory(filter, basedir));
                }
                build.setFilters(filters);
            }

            build.setOutputDirectory(alignToBaseDirectory(build.getOutputDirectory(), basedir));

            build.setTestOutputDirectory(alignToBaseDirectory(build.getTestOutputDirectory(), basedir));
        }

        Reporting reporting = model.getReporting();

        if (reporting != null) {
            reporting.setOutputDirectory(alignToBaseDirectory(reporting.getOutputDirectory(), basedir));
        }
    }

    private String alignToBaseDirectory(String path, File basedir) {
        return pathTranslator.alignToBaseDirectory(path, basedir);
    }
}
