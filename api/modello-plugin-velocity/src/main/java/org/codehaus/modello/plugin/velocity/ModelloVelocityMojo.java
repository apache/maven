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
package org.codehaus.modello.plugin.velocity;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.modello.maven.AbstractModelloGeneratorMojo;

/**
 * Creates an XML schema from the model.
 *
 * @author <a href="mailto:brett@codehaus.org">Brett Porter</a>
 */
@Mojo(name = "velocity", defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true)
public class ModelloVelocityMojo extends AbstractModelloGeneratorMojo {
    /**
     * The output directory of the generated XML Schema.
     */
    @Parameter(defaultValue = "${project.build.directory}/generated-sources/modello", required = true)
    private File outputDirectory;

    @Parameter
    private List<String> templates;

    @Parameter
    private List<String> params;

    protected String getGeneratorType() {
        return "velocity";
    }

    protected void customizeParameters(Properties parameters) {
        super.customizeParameters(parameters);
        Map<String, String> params = this.params != null
                ? this.params.stream()
                        .collect(Collectors.toMap(
                                s -> s.substring(0, s.indexOf('=')), s -> s.substring(s.indexOf('=') + 1)))
                : Collections.emptyMap();
        parameters.put("basedir", Objects.requireNonNull(getBasedir(), "basedir is null"));
        parameters.put(VelocityGenerator.VELOCITY_TEMPLATES, String.join(",", templates));
        parameters.put(VelocityGenerator.VELOCITY_PARAMETERS, params);
    }

    protected boolean producesCompilableResult() {
        return true;
    }

    public File getOutputDirectory() {
        return outputDirectory;
    }

    public void setOutputDirectory(File outputDirectory) {
        this.outputDirectory = outputDirectory;
    }
}
