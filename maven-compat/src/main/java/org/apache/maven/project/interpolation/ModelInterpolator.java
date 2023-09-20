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
package org.apache.maven.project.interpolation;

import java.io.File;
import java.util.Map;

import org.apache.maven.model.Model;
import org.apache.maven.project.ProjectBuilderConfiguration;

/**
 * @author jdcasey
 */
@Deprecated
public interface ModelInterpolator {
    String DEFAULT_BUILD_TIMESTAMP_FORMAT = "yyyyMMdd-HHmm";

    String BUILD_TIMESTAMP_FORMAT_PROPERTY = "maven.build.timestamp.format";

    String ROLE = ModelInterpolator.class.getName();

    /**
     * @deprecated Use {@link ModelInterpolator#interpolate(Model, File, ProjectBuilderConfiguration, boolean)} instead.
     */
    @Deprecated
    Model interpolate(Model project, Map<String, ?> context) throws ModelInterpolationException;

    /**
     * @deprecated Use {@link ModelInterpolator#interpolate(Model, File, ProjectBuilderConfiguration, boolean)} instead.
     */
    @Deprecated
    Model interpolate(Model model, Map<String, ?> context, boolean strict) throws ModelInterpolationException;

    Model interpolate(Model model, File projectDir, ProjectBuilderConfiguration config, boolean debugEnabled)
            throws ModelInterpolationException;

    String interpolate(
            String src, Model model, File projectDir, ProjectBuilderConfiguration config, boolean debugEnabled)
            throws ModelInterpolationException;
}
